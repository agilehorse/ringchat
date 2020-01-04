package cz.cvut.fel.bulkodav.node;

import cz.cvut.fel.bulkodav.communication.CommunicationLink;
import cz.cvut.fel.bulkodav.communication.Message;
import cz.cvut.fel.bulkodav.communication.NodeInfo;
import cz.cvut.fel.bulkodav.communication.TopologyInfo;
import cz.cvut.fel.bulkodav.exceptions.ConnectingException;
import cz.cvut.fel.bulkodav.exceptions.OperationException;
import cz.cvut.fel.bulkodav.view.ChatController;
import javafx.application.Platform;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import static cz.cvut.fel.bulkodav.communication.MessageType.*;
import static cz.cvut.fel.bulkodav.node.Direction.LEFT;
import static cz.cvut.fel.bulkodav.node.Direction.RIGHT;

/**
 * The {@link Node} class provides all the data and functionality a node needs to function.
 */
public class Node extends Thread
{
    private NodeInfo myInfo;
    private PeasantLogic peasantLogic;
    private KingLogic kingLogic;
    private volatile boolean isLoggedIn;
    private ServerSocket server;
    private List<SocketListener> threadPool = new ArrayList<>();
    private final static Logger logger = Logger.getLogger(Node.class);
    private ChatController chatController;
    private PriorityBlockingQueue<Message> messageQueue = new PriorityBlockingQueue<>();

    /**
     * Tries to start a new network as a leader.
     *
     * @param myInfo The info containing connection parameters of the node.
     * @throws ConnectingException if the creation of the node fails.
     */
    public void startNewNetwork(NodeInfo myInfo) throws ConnectingException
    {
        this.myInfo = myInfo;
        try
        {
            server = new ServerSocket();
            server.bind(new InetSocketAddress(myInfo.getAddress(), myInfo.getPort()));
            kingLogic = new KingLogic(this);
            isLoggedIn = true;
            if (hasUi()) refreshUi(userChanged(null, null));
        } catch (BindException e)
        {
            throw new ConnectingException("Cannot connect with these parameters. " +
                    "Other node or service might already be running using them.");
        } catch (IOException e)
        {
            throw new ConnectingException("Unknown exception while starting new network.");
        } catch (OperationException e)
        {
            notifyUiAboutException(e.getMessage());
        }
    }

    /**
     * Tries to connect to a remote king node.
     *
     * @param kingsInfo The info about the king.
     * @param myInfo    The info about this node.
     * @throws ConnectingException if the connection was unsuccessful.
     */
    public void connectToRemoteKing(NodeInfo kingsInfo, NodeInfo myInfo) throws ConnectingException
    {
        String errorText = "Unknown error while establishing connection. " +
                "You might want to check the connection parameters of the existing node.";
        CommunicationLink kingsPigeon = null;
        try
        {
            this.myInfo = myInfo;
            kingsPigeon = new CommunicationLink(kingsInfo);
            server = new ServerSocket();
            server.bind(new InetSocketAddress(myInfo.getAddress(), myInfo.getPort()));
            kingsPigeon.sendMessage(new Message(kingsPigeon.serializeId(myInfo), kingsInfo, myInfo, GREETINGS));
            Message kingsResponse = kingsPigeon.readMessage();
            kingsPigeon.setName(kingsResponse.getSenderName());

            if (kingsResponse.getMessageType() == GREETINGS)
            {
                NodeInfo idOfTheRightNode = kingsPigeon.deserializeId(kingsResponse.getContent());

                peasantLogic = new PeasantLogic(this, kingsPigeon, new CommunicationLink(kingsPigeon.getInfo()),
                        new CommunicationLink(idOfTheRightNode));

                if (!peasantLogic.isNodeDead(peasantLogic.getRightNode()))
                {
                    kingsPigeon.sendMessage(new Message("", kingsResponse.getSender(), myInfo, CONFIRM));
                    isLoggedIn = true;
                    String onlineUsers = kingsPigeon.readMessage().getContent();
                    logger.info("Node " + myInfo + "\n - Online users are: " + onlineUsers + "\n");
                    return;
                }
            }
            if (kingsResponse.getContent().equals("name"))
                errorText = "The name \"" + myInfo.getName() + "\" is already used. Aborting.\n";
            throw new IOException();
        } catch (BindException e)
        {
            if (kingsPigeon != null) kingsPigeon.close();
            throw new ConnectingException("Cannot connect, the local address with the port you have provided " +
                    "might be incorrect, or already in use.");
        } catch (IOException e)
        {
            if (kingsPigeon != null) kingsPigeon.close();
            die();
            throw new ConnectingException(errorText);
        }
    }

    /**
     * Node runs and accepts incoming connections which will then be handled.
     */
    @Override
    public void run()
    {
        while (isLoggedIn)
        {
            try
            {
                Socket socket = server.accept();
                SocketListener thread = new SocketListener(socket, this,
                        socket.getInetAddress().getHostAddress());
                thread.setDaemon(true);
                thread.start();
                threadPool.add(thread);
            } catch (IOException ignored)
            {
            }
        }
    }

    /**
     * Promotes the peasant node to a king node.
     */
    void promoteToKing()
    {
        assert kingLogic == null;
        logger.info("Node " + myInfo + "\n - " + "promoting.\n");
        kingLogic = new KingLogic(this);
        getNewPeasants();
        logger.info("Node " + myInfo + " - " + " added all peasants, closing redundant connections.\n");
        String deadKingName = peasantLogic.getKingsName();
        peasantLogic.closeCommunicationWithNodes();
        peasantLogic = null;
        try
        {
            Thread.sleep(2000);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        kingLogic.removeDeadNodeFromUi(deadKingName);
    }

    /**
     * Collects data about all the nodes in the ring for the king. The nodes also correct their
     */
    private void getNewPeasants()
    {
        logger.info("Node " + myInfo + "\n - " + "getting new peasants.\n");
        CommunicationLink left = peasantLogic.getLeftNode();
        CommunicationLink right = peasantLogic.getRightNode();

        CommunicationLink leftMost = mapTopologyOneWay(left, LEFT);
        CommunicationLink rightMost = mapTopologyOneWay(right, RIGHT);
        NodeInfo oldKingInfo = peasantLogic.getKing().getInfo();

        if (leftMost != null)
        {
            NodeInfo rightInfo = rightMost == null ? myInfo : rightMost.getInfo();
            leftMost.sendMessage(new Message(leftMost.serializeId(oldKingInfo) + " " +
                    leftMost.serializeId(rightInfo), leftMost.getInfo(), myInfo, TOPOLOGY_CHANGED));
        }

        if (rightMost != null)
        {
            NodeInfo leftInfo = leftMost == null ? myInfo : leftMost.getInfo();
            rightMost.sendMessage(new Message(rightMost.serializeId(oldKingInfo) + " " +
                    rightMost.serializeId(leftInfo), rightMost.getInfo(), myInfo, TOPOLOGY_CHANGED));
        }
    }

    /**
     * Maps the topology one way from the starting node up to the end where there used to be the old king.
     *
     * @param startingNode The first node that starts forwarding the mapping message to other nodes.
     * @param direction    The direction of the mapping process in the ring.
     */
    private CommunicationLink mapTopologyOneWay(CommunicationLink startingNode, Direction direction)
    {
        try
        {
            if (startingNode.getName().equals(peasantLogic.getKingsName())) throw new IOException();
            startingNode.sendMessage(new Message("", startingNode.getInfo(), myInfo, LONG_LIVE_THE_KING));
            logger.info("Node " + myInfo + "\n - " + "sending " + direction + " " + startingNode.getPort() + "\n");

            String messageContent = startingNode.readMessage().getContent();
            List<CommunicationLink> nodes = addNodesFromString(messageContent.split(" "), startingNode);
            CommunicationLink lastNode = nodes.get(nodes.size() - 1);
            if (direction.equals(RIGHT)) Collections.reverse(nodes);
            for (CommunicationLink node : nodes)
            {
                kingLogic.addNode(node);
            }

            logger.info("Node " + myInfo + "\n - " + "got " + direction + "\n");
            return lastNode;
        } catch (IOException e)
        {
            // there is no left so my rightest right is my left
            logger.info("Node " + myInfo + "\n - " + "no " + direction + "\n");
            startingNode.close();
            return null;
        }
    }

    /**
     * Deserializes the ids of the nodes from string and adds them into the king's collection of all known nodes.
     *
     * @param stringIds    The array of ids serialized in string.
     * @param deserializer The object which handles deserialization.
     */
    private List<CommunicationLink> addNodesFromString(String[] stringIds, CommunicationLink deserializer)
    {
        List<CommunicationLink> nodes = new ArrayList<>();
        for (String str : stringIds)
        {
            NodeInfo nodeId = deserializer.deserializeId(str);
            try
            {
                nodes.add(new CommunicationLink(nodeId));
            } catch (IOException e)
            {
                // we ignore the wrong ids
                logger.info("Node " + myInfo + "\n - " + "error creating new peasant with id: " + nodeId.getPort() + "\n");
            }
        }
        return nodes;
    }

    /**
     * Handles a death of a node.
     *
     * @param deadNodeInfo The info of the dead node.
     */
    void handleNodeDeath(NodeInfo deadNodeInfo)
    {
        String deadNodeName;
        if (isKing())
        {
            deadNodeName = kingLogic.handleNodeDeath(deadNodeInfo);
        } else
        {
            deadNodeName = peasantLogic.handleNodeDeath(deadNodeInfo);
        }
        shutDownListenerByName(deadNodeName);
    }

    /**
     * Kills the node and closes all the connections with other nodes.
     */
    public void die()
    {
        try
        {
            isLoggedIn = false;
            if (server != null && !server.isClosed()) server.close();
            endAllListeners();
            if (kingLogic != null)
            {
                kingLogic.tryToFindNextKing();
                kingLogic.closeCommunicationWithNodes();
            } else if (peasantLogic != null)
            {
                peasantLogic.handleNodeDeath(myInfo);
                peasantLogic.closeCommunicationWithNodes();
                peasantLogic = null;
            }
        } catch (IOException e)
        {
            logger.error(e);
            isLoggedIn = false;
        }
    }

    /**
     * Gets a value which says whether the node is logged in.
     *
     * @return true if the node is logged in, otherwise false.
     */
    public boolean isLoggedIn()
    {
        return isLoggedIn;
    }

    /**
     * Closes all the threads that handle open communications to this node.
     */
    private void endAllListeners()
    {
        for (SocketListener thread : threadPool)
        {
            thread.end();
        }
    }

    /**
     * Prints to the console the {@link cz.cvut.fel.bulkodav.communication.TopologyInfo} of every node in the topology.
     */
    public void handleTopologyInfoPrinting()
    {
        try
        {
            for (TopologyInfo topologyInfo : getTopologyInfo())
            {
                logger.info("Node " + myInfo + " - " + topologyInfo.toString());
            }
        } catch (OperationException e)
        {
            logger.error("Node " + myInfo + " - " + e.getMessage());
        }
    }

    /**
     * Shuts down the listener to the node with provided name.
     *
     * @param nodeName The name of the node.
     */
    void shutDownListenerByName(String nodeName)
    {
        for (SocketListener thread : threadPool)
        {
            if (thread.getListenedName().equals(nodeName))
            {
                thread.end();
                return;
            }
        }
    }

    /**
     * Gets the topology info of every connected node.
     *
     * @return the list of topology info of every connected node.
     * @throws OperationException if the operation fails.
     */
    public List<TopologyInfo> getTopologyInfo() throws OperationException
    {
        if (isKing())
        {
            return kingLogic.getTopologyInfo();
        } else
        {
            return peasantLogic.getTopologyInfo();
        }
    }

    /**
     * Gets the names of online users separated by comas.
     *
     * @return the names of online users separated by comas.
     * @throws OperationException if the operation failed.
     */
    public String getOnlineUsers() throws OperationException
    {
        if (isKing())
        {
            return kingLogic.getOnlineUsers(myInfo.getName());
        } else
        {
            return peasantLogic.getOnlineUsers();
        }
    }

    /**
     * Sends a new message to the chat.
     *
     * @param text the content of the message.
     * @throws OperationException if the operation failed.
     */
    public void sendMessage(String text) throws OperationException
    {
        if (isKing())
        {
            kingLogic.sendMessage(text);
        } else
        {
            peasantLogic.sendMessage(text);
        }
        displayNewChatMessage(myInfo.getName(), text);
    }

    /**
     * Displays the new chat message to the commandline, and also on the UI if it's present.
     *
     * @param userName The name of the sender.
     * @param text     The content of the chat message.
     */
    private void displayNewChatMessage(String userName, String text)
    {
        String content = userName + ": " + text + "\n";
        logger.info(content);
        if (chatController != null)
            chatController.displayNewChatMessage(content);
    }

    /**
     * Gets the connection parameter's of this node's king. They are an ip address and the port separated by a ":".
     *
     * @return the connection parameter's of this node's king. They are an ip address and the port separated by a ":".
     */
    public String getKingsConnectionParams()
    {
        if (isKing())
        {
            return myInfo.getAddress() + ":" + myInfo.getPort();
        } else
        {
            CommunicationLink king = peasantLogic.getKing();
            return king.getAddress() + ":" + king.getPort();
        }
    }

    /**
     * Receives all the chat messages held in the queue.
     */
    void receiveAllMessages()
    {
        for (int i = 0; i < messageQueue.size(); i++)
        {
            Message message = messageQueue.poll();
            if (message != null) displayNewChatMessage(message.getSenderName(), message.getContent());
        }
    }

    /**
     * Refreshes the UI after a change of a state of a user.
     *
     * @param userStateChange a change of a state of a user.
     */
    void refreshUi(UserStateChange userStateChange)
    {
        if (hasUi())
            Platform.runLater(() -> chatController.refreshData(userStateChange));
    }

    /**
     * Creates a user state change class from given parameters and collected topology info.
     * @param name The name of the user which changed a state.
     * @param state The new state of the user.
     * @return The ser state change class.
     * @throws OperationException if the operation fails.
     */
    UserStateChange userChanged(String name, ConnectionState state) throws OperationException
    {
        return new UserStateChange(name, state, getTopologyInfo());
    }

    /**
     * Gets a value which represents whether the node has a UI or it's a command line app.
     * @return a value which represents whether the node has a UI or it's a command line app.
     */
    boolean hasUi()
    {
        return chatController != null;
    }

    /**
     * Notifies the UI about the exception which was raised during some operation.
     *
     * @param errorMessage The content of the error message.
     */
    void notifyUiAboutException(String errorMessage)
    {
        if (chatController != null)
            chatController.exceptionRaised(errorMessage);
    }

    /**
     * Gets the name of this node.
     *
     * @return The name of this node
     */
    public String getNodeName()
    {
        return myInfo.getName();
    }

    /**
     * Gets the info about this node.
     *
     * @return The info about this node.
     */
    public NodeInfo getNodeInfo()
    {
        return myInfo;
    }

    /**
     * Gets the server port of this node.
     *
     * @return The server port of this node.
     */
    int getPort()
    {
        return myInfo.getPort();
    }

    /**
     * Gets the value which represents whether this node is a leader node.
     *
     * @return The value which represents whether this node is a leader node.
     */
    public boolean isKing()
    {
        return kingLogic != null;
    }

    /**
     * Gets the logic of the basic node. Null if the nodes is a leader node.
     *
     * @return The logic of the basic node.
     */
    PeasantLogic getPeasantLogic()
    {
        return peasantLogic;
    }

    /**
     * Gets the logic of the leader node. Null if the node is a basic node.
     *
     * @return The logic of the leader node.
     */
    KingLogic getKingLogic()
    {
        return kingLogic;
    }

    /**
     * Sets a controller of the ui.
     *
     * @param chatController the controller of the ui.
     */
    public void setUiController(ChatController chatController)
    {
        this.chatController = chatController;
    }

    /**
     * Adds a new chat message to the queue.
     *
     * @param message the chat message to be added.
     */
    void addNewMessage(Message message)
    {
        messageQueue.add(message);
    }
}
