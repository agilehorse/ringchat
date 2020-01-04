package cz.cvut.fel.bulkodav.node;

import cz.cvut.fel.bulkodav.communication.CommunicationLink;
import cz.cvut.fel.bulkodav.communication.Message;
import cz.cvut.fel.bulkodav.communication.NodeInfo;
import cz.cvut.fel.bulkodav.communication.TopologyInfo;
import cz.cvut.fel.bulkodav.exceptions.CommunicationException;
import org.apache.log4j.Logger;

import static cz.cvut.fel.bulkodav.communication.MessageType.*;
import static cz.cvut.fel.bulkodav.node.Direction.LEFT;

/**
 * The {@link PeasantHandler} handles incoming messages sent from a specific node to a basic node.
 */
public class PeasantHandler implements MessageHandler
{
    private final NodeInfo myInfo;
    private final Node node;
    private final static Logger logger = Logger.getLogger(PeasantHandler.class);

    /**
     * The constructor for {@link cz.cvut.fel.bulkodav.node.PeasantHandler} class.
     *
     * @param node The node who uses an instance of this class to handle messages from one other node connected to it.
     */
    PeasantHandler(Node node)
    {
        this.myInfo = node.getNodeInfo();
        this.node = node;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleGreetingsMessage(Message message, CommunicationLink messageSender)
    {
        NodeInfo messageSenderInfo = messageSender.getInfo();
        messageSender.sendMessage(new Message("", messageSenderInfo, myInfo, CONFIRM));
        try
        {
            UserStateChange userStateChange = messageSender.readUserChange();
            if (node.hasUi())
            {
                if (userStateChange.getUserName().equals(myInfo.getName()))
                    userStateChange = new UserStateChange(null, null, userStateChange.getCurrentTopologyInfos());
                node.refreshUi(userStateChange);
            }
        } catch (CommunicationException e)
        {
            node.handleNodeDeath(messageSenderInfo);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleChatMessage(Message message, CommunicationLink messageSender)
    {
        node.addNewMessage(message);
        messageSender.sendMessage(new Message("", messageSender.getInfo(), myInfo, CONFIRM));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleMapTopologyMessage(Message message, CommunicationLink messageSender)
    {
        PeasantLogic peasantLogic = node.getPeasantLogic();
        messageSender.sendTopologyInfo(new TopologyInfo(myInfo.getName(), peasantLogic.getLeftNode().getName(), peasantLogic.getRightNode().getName()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleTopologyChangedMessage(Message message, CommunicationLink messageSender)
    {
        PeasantLogic peasantLogic = node.getPeasantLogic();
        String[] ids = message.getContent().split(" ");
        NodeInfo firstId = messageSender.deserializeId(ids[0]);

        if (ids.length == 2)
        {
            NodeInfo secondId = messageSender.deserializeId(ids[1]);
            peasantLogic.correctTopologyAfterNodeDeath(firstId, secondId);
        } else
        {
            boolean success = peasantLogic.createNewNodeInDirection(LEFT, firstId);
            if (!success) node.shutDownListenerByName(firstId.getName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleOnlineUsersMessage(Message message, CommunicationLink messageSender)
    {
        String messageContent = message.getContent();
        boolean hasUi = node.hasUi();
        if (messageContent.contains("off "))
        {
            try
            {
                UserStateChange userStateChange = messageSender.readUserChange();
                logger.info("Node " + myInfo + " - " + userStateChange.getUserName() + " logged off.\n");
                if (hasUi) node.refreshUi(userStateChange);
            } catch (CommunicationException e)
            {
                NodeInfo king = node.getPeasantLogic().getKing().getInfo();
                node.handleNodeDeath(king);
                node.notifyUiAboutException("Leader node " + king.getName() + " disconnected unexpectedly when," +
                        "refreshing " + (hasUi ? "topology." : "online users."));
            }
        } else logger.info("Node " + myInfo + "\n - Online users are: " + messageContent + "\n");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleKingIsDeadMessage(Message message, CommunicationLink messageSender)
    {
        PeasantLogic peasantLogic = node.getPeasantLogic();
        String kingName = peasantLogic.getKingsName();
        if (!peasantLogic.isElectionParticipant())
        {
            logger.info("Node " + myInfo + "\n - " + kingName + " disconnected unexpectedly.\n");
        }
        String senderName = message.getSenderName();
        peasantLogic.handleElection(Integer.parseInt(message.getContent()), senderName, senderName.equals(kingName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleLongLiveTheKingMessage(Message message, CommunicationLink messageSender)
    {
        PeasantLogic peasantLogic = node.getPeasantLogic();
        String senderName = messageSender.getName();
        String deadKingName = peasantLogic.getKingsName();
        CommunicationLink nextHop = peasantLogic.getNextHop(senderName);
        String messageContent = message.getContent() + messageSender.serializeId(myInfo) + " ";
        NodeInfo newKingInfo = message.getSender();

        boolean success = peasantLogic.createNewNodeInDirection(Direction.KING, newKingInfo);
        if (!success) node.shutDownListenerByName(newKingInfo.getName());
        peasantLogic.electionIsOver();

        if (!nextHop.getName().equals(deadKingName) && !senderName.equals(nextHop.getName()))
        {
            try
            {
                nextHop.sendMessage(new Message(messageContent, nextHop.getInfo(), newKingInfo, LONG_LIVE_THE_KING));
                messageContent = nextHop.readMessage().getContent();
            } catch (Exception e)
            {
                logger.error("Node " + myInfo + " error getting neighbours of " + nextHop.getName(), e);
            }
        }
        messageSender.sendMessage(new Message(messageContent.trim(), message.getSender(), myInfo, CONFIRM));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleNameMessage(Message message, CommunicationLink messageSender)
    {
        messageSender.sendMessage(new Message(myInfo.getName(), message.getSender(), myInfo, NAME));
    }
}
