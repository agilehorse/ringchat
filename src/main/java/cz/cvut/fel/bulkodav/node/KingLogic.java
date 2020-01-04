package cz.cvut.fel.bulkodav.node;

import cz.cvut.fel.bulkodav.communication.*;
import cz.cvut.fel.bulkodav.exceptions.CommunicationException;
import cz.cvut.fel.bulkodav.exceptions.OperationException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static cz.cvut.fel.bulkodav.communication.MessageType.*;
import static cz.cvut.fel.bulkodav.node.ConnectionState.offline;

/**
 * The {@link KingLogic} class provides data and functionality a leader node needs to function.
 */
public class KingLogic implements NodeLogic
{
    private final Node node;
    private List<Integer> nodeIds;
    private Map<Integer, CommunicationLink> allNodes;
    private final NodeInfo myInfo;
    private final static Logger logger = Logger.getLogger(KingLogic.class);

    /**
     * The constructor for {@link cz.cvut.fel.bulkodav.node.KingLogic} class.
     *
     * @param node The node.
     */
    public KingLogic(Node node)
    {
        this.node = node;
        this.myInfo = node.getNodeInfo();
        nodeIds = new ArrayList<>();
        allNodes = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    public String handleNodeDeath(NodeInfo deadNode)
    {
        int deadNodeIndex = nodeIds.indexOf(deadNode.hashCode());
        CommunicationLink leftOfDead = getNodeByIndex(deadNodeIndex - 1);
        CommunicationLink rightOfDead = getNodeByIndex(deadNodeIndex + 1);

        notifyNeighbourOfDead(deadNode, leftOfDead, rightOfDead);
        notifyNeighbourOfDead(deadNode, rightOfDead, leftOfDead);

        logger.info("Node " + myInfo + "\n - Removing node: " + deadNode + "\n");
        removeNodeById(deadNode.hashCode());

        return deadNode.getName();
    }

    /**
     * {@inheritDoc}
     */
    public void closeCommunicationWithNodes()
    {
        logger.info("Node " + myInfo + "\n - closing communication with other nodes in the ring.\n");
        for (CommunicationLink node : allNodes.values())
        {
            node.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public CommunicationLink getLeftNode()
    {
        if (allNodes.size() == 0)
        {
            return null;
        }
        try
        {
            return allNodes.get(nodeIds.get(0));
        } catch (Exception e)
        {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TopologyInfo> getTopologyInfo() throws OperationException
    {
        List<TopologyInfo> list = new ArrayList<>();
        int size = nodeIds.size();
        String myName = myInfo.getName();

        if (size != 0)
        {
            list.add(new TopologyInfo(myName, getLeftNode().getName(), getRightNode().getName()));
            NodeInfo currentNodeInfo = null;
            for (CommunicationLink node : allNodes.values())
            {
                try
                {
                    currentNodeInfo = node.getInfo();
                    node.sendMessage(new Message("Send me your info", currentNodeInfo, myInfo, MessageType.MAP_TOPOLOGY));
                    list.add(node.readTopologyInfo());
                } catch (CommunicationException e)
                {
                    handleNodeDeath(currentNodeInfo);
                    throw new OperationException("Node " + myInfo + "\n - Failed to get the topology. " +
                            currentNodeInfo.getName() + " disconnected unexpectedly. Try again later.");
                }
            }
        } else
        {
            list.add(new TopologyInfo(myName, myName, myName));
        }
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMessage(String text)
    {
        forwardToAll(text, myInfo);
    }

    /**
     * {@inheritDoc}
     */
    public CommunicationLink getRightNode()
    {
        if (allNodes.size() == 0)
        {
            return null;
        }
        try
        {
            return allNodes.get(nodeIds.get(nodeIds.size() - 1));
        } catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Notifies the neighbour of the dead node about the change in topology.
     * The neighbour will obtain a new neighbour to substitute for the dead node.
     *
     * @param deadNodeInfo          The info about the dead node.
     * @param neighbour             The neighbour of the dead node.
     * @param potentialNewNeighbour The neighbour of the dead node from the other side. Can be null if it was a king.
     */
    private void notifyNeighbourOfDead(NodeInfo deadNodeInfo, CommunicationLink neighbour,
                                       CommunicationLink potentialNewNeighbour)
    {
        if (neighbour != null)
        {
            NodeInfo newNeighbourId = potentialNewNeighbour == null ? myInfo : potentialNewNeighbour.getInfo();

            neighbour.sendMessage(new Message(neighbour.serializeId(deadNodeInfo) + " " + neighbour.serializeId(newNeighbourId),
                    neighbour.getInfo(), myInfo, MessageType.TOPOLOGY_CHANGED));
        }
    }

    /**
     * Gets the list of names of connected node.
     *
     * @return The list of names of connected node.
     */
    List<String> getNodeNames()
    {
        return allNodes.values().stream().map(CommunicationLink::getName).collect(Collectors.toList());
    }

    /**
     * Gets the link for the communication with a node by the node's name.
     *
     * @param name The name of the node.
     * @return The object for communication with a node.
     */
    CommunicationLink getNodeByName(String name)
    {
        return getNodeById(Objects.hash(name));
    }

    /**
     * Gets the list of all nodes.
     *
     * @return The list of nodes.
     */
    List<CommunicationLink> getAllNodes()
    {
        return new ArrayList<>(allNodes.values());
    }

    /**
     * Gets the names of online users separated by a comma and a space.
     *
     * @param senderName the name of the node that request to know online users.
     * @return The names of online users separated by a comma and a space.
     */
    String getOnlineUsers(String senderName)
    {
        List<String> users = new ArrayList<>();
        for (CommunicationLink node : allNodes.values())
        {
            NodeInfo currentNodeInfo = node.getInfo();
            if (currentNodeInfo.getName().equals(senderName))
            {
                users.add(senderName);
            } else
            {
                node.sendMessage(new Message("Checking if you are alive", currentNodeInfo, myInfo, NAME));
                try
                {
                    users.add(node.readMessage().getContent());
                } catch (IOException e)
                {
                    handleNodeDeath(currentNodeInfo);
                    logger.error("Node " + myInfo + "\n - " + currentNodeInfo.getName() + " disconnected unexpectedly.");
                }
            }
        }
        users.add(0, myInfo.getName());
        return String.join(", ", users);
    }

    /**
     * Gets the object for the communication with a node by the node's id.
     *
     * @param id The id of the node.
     * @return The object for communication with a node.
     */
    private CommunicationLink getNodeById(Integer id)
    {
        if (allNodes.size() == 0)
        {
            return null;
        }
        try
        {
            return allNodes.get(id);
        } catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Adds a new node.
     *
     * @param communicationLink The object for the communication with the node.
     */
    void addNode(CommunicationLink communicationLink)
    {
        int nodeId = communicationLink.getInfo().hashCode();
        assert !nodeIds.contains(nodeId);
        nodeIds.add(nodeId);
        allNodes.put(nodeId, communicationLink);
    }

    /**
     * Removes a node by its id.
     *
     * @param id The id of the node to be removed.
     */
    private void removeNodeById(Integer id)
    {
        if (allNodes.size() == 0)
        {
            return;
        }
        nodeIds.remove(id);
        CommunicationLink removedNode = allNodes.remove(id);
        removedNode.close();
    }

    /**
     * Get's the node by it's index in the list of all nodes.
     *
     * @param index The index of the node.
     * @return The object for communication with a node, or null if there are no nodes, or the index is out of range.
     */
    private CommunicationLink getNodeByIndex(int index)
    {
        if (allNodes.size() == 0)
        {
            return null;
        }
        try
        {
            return allNodes.get(nodeIds.get(index));
        } catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Finds out the node with the biggest id and sends it a message that this king is logging off.
     */
    void tryToFindNextKing()
    {
        if (nodeIds.size() != 0)
        {
            int nextKingId = Collections.max(nodeIds);
            CommunicationLink nextKing = allNodes.get(nextKingId);
            nextKing.sendMessage(new Message(
                    Integer.toString(nextKingId), nextKing.getInfo(), myInfo, KING_IS_DEAD));
        }
    }

    /**
     * Forwards a received chat message to all other nodes, except the sender.
     *
     * @param text       The content of the chat message.
     * @param senderInfo The info about the sender.
     */
    void forwardToAll(String text, NodeInfo senderInfo)
    {
        for (CommunicationLink node : allNodes.values())
        {
            NodeInfo nodeInfo = node.getInfo();
            if (nodeInfo.hashCode() != senderInfo.hashCode())
            {
                node.sendMessage(new Message(text, nodeInfo, senderInfo, CHAT));
                try
                {
                    node.readMessage();
                } catch (IOException e)
                {
                    handleNodeDeath(nodeInfo);
                    logger.error("Node " + myInfo + "\n - Failed to forward message to: " +
                            nodeInfo.getName() + ", they disconnected unexpectedly.");
                }
            }
        }
    }

    /**
     * Refreshes ui of the leader node so that it doesn't contain an information about dead node.
     * Also notifies other nodes to do so too.
     * @param deadNodeName the name of the dead node.
     */
    void removeDeadNodeFromUi(String deadNodeName)
    {
        try
        {
            logger.info("Node " + myInfo + "\n - " + deadNodeName + " logged off.\n");
            UserStateChange userStateChange;
            if (node.hasUi())
            {
                userStateChange = new UserStateChange(deadNodeName, offline, getTopologyInfo());
                node.refreshUi(userStateChange);
            } else userStateChange = new UserStateChange(deadNodeName, offline, null);

            for (CommunicationLink node : getAllNodes())
            {
                node.sendMessage(new Message("off ".concat(deadNodeName), node.getInfo(), myInfo, ONLINE_USERS));
                node.sendUserChange(userStateChange);
            }
        } catch (OperationException e)
        {
            node.notifyUiAboutException(e.getMessage());
        }
    }
}
