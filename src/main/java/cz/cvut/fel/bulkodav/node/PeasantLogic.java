package cz.cvut.fel.bulkodav.node;

import cz.cvut.fel.bulkodav.communication.CommunicationLink;
import cz.cvut.fel.bulkodav.communication.Message;
import cz.cvut.fel.bulkodav.communication.NodeInfo;
import cz.cvut.fel.bulkodav.communication.TopologyInfo;
import cz.cvut.fel.bulkodav.exceptions.OperationException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static cz.cvut.fel.bulkodav.communication.MessageType.*;
import static cz.cvut.fel.bulkodav.node.Direction.LEFT;
import static cz.cvut.fel.bulkodav.node.Direction.RIGHT;

/**
 * The {@link PeasantLogic} class provides data and functionality a non-leader node needs to function.
 */
class PeasantLogic implements NodeLogic
{
    private CommunicationLink king;
    private CommunicationLink leftNode;
    private CommunicationLink rightNode;
    private boolean isElectionParticipant;
    private final static Logger logger = Logger.getLogger(PeasantLogic.class);
    private NodeInfo myInfo;
    private Node node;

    /**
     * The constructor for {@link cz.cvut.fel.bulkodav.node.PeasantLogic} class.
     *
     * @param king      The object which communicates with the king.
     * @param leftNode  The object which communicates with the left node.
     * @param rightNode The object which communicates with the right node.
     */
    PeasantLogic(Node node, CommunicationLink king, CommunicationLink leftNode, CommunicationLink rightNode)
    {
        this.node = node;
        this.myInfo = node.getNodeInfo();
        this.king = king;
        this.leftNode = leftNode;
        this.rightNode = rightNode;
    }

    /**
     * Gets the names of online users separated by a coma.
     * @return the names of online users separated by a coma.
     * @throws OperationException if the operation fails.
     */
    String getOnlineUsers() throws OperationException
    {
        king.sendMessage(new Message("Show me online users.", king.getInfo(), myInfo, ONLINE_USERS));
        String errorText = "The leader node: " + king.getName() + " might have disconnected. Try the operation again after the topology is corrected.";
        try
        {
            Message response = king.readMessage();
            if (response.getMessageType() == ONLINE_USERS)
            {
                return response.getContent();
            } else
            {
                return "unknown, some error probably occurred. Try again later.";
            }
        } catch (IOException e)
        {
            handleNodeDeath(king.getInfo());
            throw new OperationException(errorText);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String handleNodeDeath(NodeInfo deadNodeInfo)
    {
        String deadNodeName = deadNodeInfo.getName();
        int deadNodeId = deadNodeInfo.hashCode();
        NodeInfo kingInfo = king.getInfo();
        int kingId = kingInfo.hashCode();

        if (deadNodeId != kingId)
        {
            king.sendMessage(new Message(deadNodeName, king.getInfo(), myInfo, TOPOLOGY_CHANGED));
        } else
        {
            handleElection(myInfo.hashCode(), getNextHop(king.getName()).getName(), true);
        }
        return deadNodeName;
    }

    /**
     * {@inheritDoc}
     */
    public void closeCommunicationWithNodes()
    {
        logger.info("Node " + myInfo + "\n - closing communication with other nodes in the ring.\n");
        king.close();
        leftNode.close();
        rightNode.close();
    }

    /**
     * {@inheritDoc}
     */
    public CommunicationLink getRightNode()
    {
        return rightNode;
    }

    /**
     * {@inheritDoc}
     */
    public CommunicationLink getLeftNode()
    {
        return leftNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TopologyInfo> getTopologyInfo() throws OperationException
    {
        king.sendMessage(new Message("", king.getInfo(), myInfo, MAP_TOPOLOGY));
        try
        {
            Message response = king.readMessage();
            if (response.getMessageType() != CONFIRM)
                throw new OperationException("Error while getting topology info. " +
                        "Some node might have disconnected unexpectedly. Try again later.");
            String string = response.getContent();
            List<TopologyInfo> infos = new ArrayList<>();
//            infos.add(new TopologyInfo(myInfo.getName(), leftNode.getName(), rightNode.getName()));
            for (String str : string.split(" "))
            {
                infos.add(king.deserializeTopologyInfo(str));
            }
            return infos;
        } catch (IOException e)
        {
            handleNodeDeath(king.getInfo());
            throw new OperationException("Failed to get the topology info. " +
                    "The king node: " + king.getName() + " disconnected unexpectedly. Try again later.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMessage(String text) throws OperationException
    {
        String errorText = "Unknown error while sending a message.";
        king.sendMessage(new Message(text, king.getInfo(), node.getNodeInfo(), CHAT));
        try
        {
            Message response = king.readMessage();
            if (response.getMessageType() != CONFIRM)
                throw new OperationException(errorText);
        } catch (IOException e)
        {
            handleNodeDeath(king.getInfo());
            throw new OperationException(errorText + "The leader node: " + king.getName() + " disconnected unexpectedly.");
        }
    }

    /**
     * Calls the {@link PeasantLogic#setLeftNode(CommunicationLink)}, {@link PeasantLogic#setRightNode(CommunicationLink)}
     * or {@link PeasantLogic#setRightNode(CommunicationLink)} depending on the provided direction.
     *
     * @param direction   The direction of the new node.
     * @param newNodeInfo The newNodeId on which the node will listen.
     */
    boolean createNewNodeInDirection(Direction direction, NodeInfo newNodeInfo)
    {
        String name = newNodeInfo.getName();
        String stringDirection = direction.toString();
        try
        {
            CommunicationLink communicationLink = new CommunicationLink(newNodeInfo);
            logger.info("Node " + myInfo + "\n - new " + stringDirection + " is: " + name + "\n");
            if (direction == LEFT)
            {
                setLeftNode(communicationLink);
            } else if (direction == RIGHT)
            {
                setRightNode(communicationLink);
            } else
            {
                setKing(communicationLink);
            }
            return true;
        } catch (IOException e)
        {
            logger.error("Node " + myInfo + "\n - could not set "
                    + name + " as " + stringDirection + " node.", e);
            handleNodeDeath(newNodeInfo);
            return false;
        }
    }

    /**
     * Handles the election of a new node.
     *
     * @param electionId The election message.
     * @param senderName The sender of the election message.
     */
    void handleElection(int electionId, String senderName, boolean startedLocally)
    {
        int myId = myInfo.hashCode();
        node.shutDownListenerByName(king.getName());
        int kingId = king.getInfo().hashCode();

        CommunicationLink nextHop = getNextHop(senderName);
        if (nextHop.getInfo().hashCode() == kingId) nextHop = getNextHop(nextHop.getName());

        String messageContent;
        if (electionId > myId)
        {
            messageContent = Integer.toString(electionId);
        } else if (electionId == myId && (!startedLocally || nextHop.getInfo().hashCode() == kingId))
        {
            logger.info("Node " + myInfo + "\n - " + "I am the new king!\n");
            node.promoteToKing();
            return;
        } else
        {
            if (isElectionParticipant()) return;
            messageContent = Integer.toString(myId);
        }
        isElectionParticipant = true;

        logger.info("Node " + myInfo + "\n - " + "sending : " + messageContent + " to " + nextHop.getName() + "\n");
        nextHop.sendMessage(new Message(messageContent, nextHop.getInfo(), myInfo, KING_IS_DEAD));
    }

    /**
     * Corrects the topology after the death of a node.
     *
     * @param deadNodeInfo The info about the dead node.
     * @param newNodeInfo  The info about the substitute for the dead node.
     */
    void correctTopologyAfterNodeDeath(NodeInfo deadNodeInfo, NodeInfo newNodeInfo)
    {
        String deadNodeName = deadNodeInfo.getName();
        logger.info("Node " + myInfo + "\n - " + deadNodeName + " disconnected unexpectedly. Correcting neighbours.\n");

        if (deadNodeInfo.hashCode() == leftNode.getInfo().hashCode())
        {
            boolean success = createNewNodeInDirection(LEFT, newNodeInfo);
            if (!success) node.shutDownListenerByName(newNodeInfo.getName());
            return;
        }

        if (deadNodeInfo.hashCode() == rightNode.getInfo().hashCode())
        {
            node.shutDownListenerByName(rightNode.getName());
            boolean success = createNewNodeInDirection(RIGHT, newNodeInfo);
            if (!success) node.shutDownListenerByName(newNodeInfo.getName());
        }
    }

    /**
     * Gets the value which says whether this node is a participant in an ongoing leader election.
     *
     * @return The value which says whether this node is a participant in an ongoing leader election.
     */
    boolean isElectionParticipant()
    {
        return isElectionParticipant;
    }

    /**
     * Check if the node is dead by sending it a message requesting for its name.
     * If reading the message fails, the node probably died.
     *
     * @param node The node to be checked.
     * @return True if the node is dead, otherwise false.
     */
    boolean isNodeDead(CommunicationLink node)
    {
        node.sendMessage(new Message("Are you online?", node.getInfo(), myInfo, NAME));
        try
        {
            node.readMessage();
            return false;
        } catch (IOException e)
        {
            return true;
        }
    }

    /**
     * Gets the communicator with the next node by it's name. If the previous name was the one of the left node,
     * the next hop is the right node and vice versa.
     *
     * @param previousHopName The name of previous hop.
     * @return The communication link to the next hop
     */
    CommunicationLink getNextHop(String previousHopName)
    {
        return previousHopName.equals(leftNode.getName()) ? rightNode : leftNode;
    }

    /**
     * Gets the object which communicates with the king.
     *
     * @return The object which communicates with the king.
     */
    CommunicationLink getKing()
    {
        return king;
    }

    /**
     * Gets the king's name.
     *
     * @return The king's name.
     */
    String getKingsName()
    {
        return king.getName();
    }

    private void setKing(CommunicationLink communicationLink)
    {
        king.close();
        king = communicationLink;
    }

    /**
     * Sets the object which communicates with the left node. Closes communication with the previous left node,
     *
     * @param leftNode The object which communicates with the left node.
     */
    private void setLeftNode(CommunicationLink leftNode)
    {
        this.leftNode.close();
        this.leftNode = leftNode;
    }

    /**
     * Sets the object which communicates with the right node.
     *
     * @param rightNode The object which communicates with the right node.
     */
    private void setRightNode(CommunicationLink rightNode)
    {
        if (this.rightNode.getPort() != rightNode.getPort())
            this.rightNode.close();

        this.rightNode = rightNode;
    }

    /**
     * Sets {@link PeasantLogic#isElectionParticipant} to false.
     */
    void electionIsOver()
    {
        isElectionParticipant = false;
    }
}
