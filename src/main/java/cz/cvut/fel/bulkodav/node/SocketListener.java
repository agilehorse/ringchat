package cz.cvut.fel.bulkodav.node;

import cz.cvut.fel.bulkodav.exceptions.CommunicationException;
import cz.cvut.fel.bulkodav.communication.CommunicationLink;
import cz.cvut.fel.bulkodav.communication.Message;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.Socket;

/**
 * The {@link SocketListener} class represents a listener which delegates an incoming messages
 * from one specific node to a concrete handler based on the type of receiving node.
 */
public class SocketListener extends Thread
{
    private final Node node;
    private CommunicationLink messageSender;
    private final MessageHandler messageHandler;
    private final static Logger logger = Logger.getLogger(SocketListener.class);
    private volatile boolean finished;

    /**
     * The constructor for {@link cz.cvut.fel.bulkodav.node.SocketListener} class.
     *
     * @param messageSender The socket to which we listen.
     * @param node          The node which build this socket listener.
     */
    SocketListener(Socket messageSender, Node node, String address)
    {
        this.messageSender = new CommunicationLink(messageSender, address);
        this.node = node;
        messageHandler = node.isKing() ? new KingHandler(node) : new PeasantHandler(node);
    }

    /**
     * Starts running and listening to incoming messages from one particular sender which this SocketListener thread is dedicated to.
     */
    @Override
    public void run()
    {
        while (!finished)
        {
            try
            {
                if (messageSender.readyToRead())
                {
                    Message message;
                    try
                    {
                        message = messageSender.readMessage();
                        if (messageSender.getPort() == 0)
                        {
                            messageSender.setName(message.getSenderName());
                            messageSender.setPort(message.getSender().getPort());
                        }
                    } catch (CommunicationException e)
                    {
                        int sender = messageSender.getPort();
                        logger.error("Node + " + node.getPort() + "\n - failed to receive message from" +
                                (sender == 0 ? "unknown sender" : sender), e);
                        node.handleNodeDeath(messageSender.getInfo());
                        break;
                    }
                    handleReceivedMessage(message, messageSender);
                }
            } catch (IOException e)
            {
                if (!node.isLoggedIn())
                {
                    String sender = messageSender.getName();
                    logger.error("Node " + node.getNodeName() + "\n - failed to receive message from " +
                            (sender.equals("") ? "unknown sender" : sender) + ".");
                }
                finished = true;
            }
            node.receiveAllMessages();
        }
        messageSender.close();
    }

    /**
     * Delegates the handling of the message to concrete implementation depending on whether this listener belongs to
     * a king node or a peasant node.
     *
     * @param message       The message to be handled.
     * @param messageSender The sender of the message
     */
    private void handleReceivedMessage(Message message, CommunicationLink messageSender)
    {
        switch (message.getMessageType())
        {
            case GREETINGS:
                messageHandler.handleGreetingsMessage(message, messageSender);
                break;
            case CHAT:
                messageHandler.handleChatMessage(message, messageSender);
                break;
            case TOPOLOGY_CHANGED:
                messageHandler.handleTopologyChangedMessage(message, messageSender);
                break;
            case ONLINE_USERS:
                messageHandler.handleOnlineUsersMessage(message, messageSender);
                break;
            case MAP_TOPOLOGY:
                messageHandler.handleMapTopologyMessage(message, messageSender);
                break;
            case KING_IS_DEAD:
                messageHandler.handleKingIsDeadMessage(message, messageSender);
                break;
            case LONG_LIVE_THE_KING:
                messageHandler.handleLongLiveTheKingMessage(message, messageSender);
                break;
            case NAME:
                messageHandler.handleNameMessage(message, messageSender);
                break;
        }
    }

    /**
     * Gets the name of the node who's messages this listeners listens to.
     *
     * @return The name of the node who's messages this listeners listens to.
     */
    String getListenedName()
    {
        return this.messageSender.getName();
    }

    /**
     * Stops listening to the messages from the socket.
     */
    void end()
    {
        this.finished = true;
    }
}
