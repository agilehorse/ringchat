package cz.cvut.fel.bulkodav.node;

import cz.cvut.fel.bulkodav.communication.CommunicationLink;
import cz.cvut.fel.bulkodav.communication.Message;
import cz.cvut.fel.bulkodav.communication.NodeInfo;
import cz.cvut.fel.bulkodav.communication.TopologyInfo;
import cz.cvut.fel.bulkodav.exceptions.CommunicationException;
import cz.cvut.fel.bulkodav.exceptions.OperationException;
import org.apache.log4j.Logger;

import java.io.IOException;

import static cz.cvut.fel.bulkodav.communication.MessageType.*;
import static cz.cvut.fel.bulkodav.node.ConnectionState.online;

/**
 * The {@link KingHandler} class handles incoming messages sent from a specific node sent to a leader node.
 */
public class KingHandler implements MessageHandler
{
    private final Node king;
    private final NodeInfo myInfo;
    private final static Logger logger = Logger.getLogger(KingHandler.class);

    /**
     * The constructor for {@link cz.cvut.fel.bulkodav.node.KingHandler} class.
     *
     * @param node The leader node who uses an instance of this class to handle messages from one other node connected to it.
     */
    public KingHandler(Node node)
    {
        this.king = node;
        myInfo = node.getNodeInfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleGreetingsMessage(Message message, CommunicationLink messageSender)
    {
        KingLogic kingLogic = king.getKingLogic();
        NodeInfo newNodeId = messageSender.deserializeId(message.getContent());
        String senderName = newNodeId.getName();
        CommunicationLink kingsRightNode = kingLogic.getRightNode();
        NodeInfo rightNodeId = kingsRightNode == null ? myInfo : kingsRightNode.getInfo();

        if (kingLogic.getNodeNames().contains(senderName) || senderName.equals(myInfo.getName()))
        {
            messageSender.sendMessage(new Message("name", null, myInfo, REJECT));
            return;
        }

        String messageContent = messageSender.serializeId(rightNodeId);
        messageSender.sendMessage(new Message(messageContent, newNodeId, myInfo, GREETINGS));

        try
        {
            Message response = messageSender.readMessage();
            if (response.getMessageType() == CONFIRM)
            {
                kingLogic.addNode(new CommunicationLink(newNodeId));
                messageSender.sendMessage(new Message(kingLogic.getOnlineUsers(senderName),
                        newNodeId, myInfo, ONLINE_USERS));
            } else return;

            logger.info("Node " + myInfo + "\n - " + senderName + " is online.\n");
            notifyOthers(kingsRightNode, messageSender);
        } catch (IOException e)
        {
            logger.error(e);
        }
    }

    /**
     * Notifies other nodes about connection of a new node.
     *
     * @param kingsRightNode The node to the right of the king.
     * @param newNode        Newly connected node.
     */
    private void notifyOthers(CommunicationLink kingsRightNode, CommunicationLink newNode)
    {
        NodeInfo newNodeInfo = newNode.getInfo();
        int newNodeId = newNodeInfo.hashCode();
        NodeInfo currentNodeInfo = null;
        try
        {
            if (kingsRightNode != null)
            {
                NodeInfo rightNodeInfo = kingsRightNode.getInfo();
                if (rightNodeInfo.hashCode() != newNodeId)
                {
                    currentNodeInfo = rightNodeInfo;
                    kingsRightNode.sendMessage(new Message(kingsRightNode.serializeId(newNodeInfo),
                            rightNodeInfo, myInfo, TOPOLOGY_CHANGED));
                }
            }

            UserStateChange userStateChange = king.userChanged(newNodeInfo.getName(), online);
            king.refreshUi(userStateChange);

            for (CommunicationLink node : king.getKingLogic().getAllNodes())
            {
                currentNodeInfo = node.getInfo();
                node.sendMessage(new Message(newNodeInfo.getName(), currentNodeInfo, myInfo, GREETINGS));
                if (node.readMessage().getMessageType() != CONFIRM) continue;
                node.sendUserChange(userStateChange);
            }
        } catch (CommunicationException e)
        {
            king.handleNodeDeath(currentNodeInfo);
            king.notifyUiAboutException("There was an error while updating data about other nodes. You might need to refresh it manually later.");
        } catch (OperationException | IOException e)
        {
            king.notifyUiAboutException("There was an error while updating data about other nodes. You might need to refresh it manually later.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleChatMessage(Message message, CommunicationLink messageSender)
    {
        king.addNewMessage(message);
        messageSender.sendMessage(new Message("", messageSender.getInfo(), myInfo, CONFIRM));
        king.getKingLogic().forwardToAll(message.getContent(), message.getSender());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleMapTopologyMessage(Message message, CommunicationLink messageSender)
    {
        try
        {
            String builder = "";
            for (TopologyInfo info : king.getTopologyInfo())
            {
                builder = builder.concat(messageSender.serializeTopologyInfo(info) + " ");
            }
            messageSender.sendMessage(new Message(builder.trim(), messageSender.getInfo(), myInfo, CONFIRM));
        } catch (OperationException e)
        {
            messageSender.sendMessage(new Message("", messageSender.getInfo(), myInfo, REJECT));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleTopologyChangedMessage(Message message, CommunicationLink messageSender)
    {
        String deadNodeName = message.getContent();
        NodeInfo deadNodeInfo = king.getKingLogic().getNodeByName(deadNodeName).getInfo();
        KingLogic kingLogic = king.getKingLogic();

        kingLogic.handleNodeDeath(deadNodeInfo);
        king.shutDownListenerByName(deadNodeName);

        kingLogic.removeDeadNodeFromUi(deadNodeName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleOnlineUsersMessage(Message message, CommunicationLink messageSender)
    {
        messageSender.sendMessage(new Message(king.getKingLogic().getOnlineUsers(messageSender.getName()),
                messageSender.getInfo(), myInfo, ONLINE_USERS));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleKingIsDeadMessage(Message message, CommunicationLink messageSender)
    {
        //ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleLongLiveTheKingMessage(Message message, CommunicationLink messageSender)
    {
        //ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleNameMessage(Message message, CommunicationLink messageSender)
    {
        messageSender.sendMessage(new Message(king.getNodeName(), messageSender.getInfo(), myInfo, NAME));
    }
}
