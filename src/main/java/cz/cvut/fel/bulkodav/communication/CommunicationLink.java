package cz.cvut.fel.bulkodav.communication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.fel.bulkodav.exceptions.CommunicationException;
import cz.cvut.fel.bulkodav.node.UserStateChange;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * The {@link CommunicationLink} class is a communication abstraction for a node.
 * It handles the TCP communication with {@link Message}s and serialization of data which can be sent throught messages.
 */
public class CommunicationLink
{
    private NodeInfo info;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ObjectMapper mapper;
    private final static Logger logger = Logger.getLogger(CommunicationLink.class);

    /**
     * The constructor for {@link CommunicationLink} class.
     *
     * @param nodeInfo The info about the node which contains connection parameters.
     * @throws IOException if the connection fails.
     */
    public CommunicationLink(NodeInfo nodeInfo) throws IOException
    {
        socket = new Socket();
        socket.connect(new InetSocketAddress(nodeInfo.getAddress(), nodeInfo.getPort()), 3000);
        info = nodeInfo;
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        mapper = new ObjectMapper();
    }

    /**
     * The constructor for {@link CommunicationLink} class.
     *
     * @param socket  The socket through which the communication happens.
     * @param address The address of the node to which this connection link points to.
     */
    public CommunicationLink(Socket socket, String address)
    {
        try
        {
            this.socket = socket;
            this.info = new NodeInfo(address, 0, "");
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            mapper = new ObjectMapper();
        } catch (IOException e)
        {
            logger.info(e);
        }
    }

    /**
     * Sends a message to the node with which this connection link is open.
     *
     * @param message The message to be sent.
     */
    public void sendMessage(Message message)
    {
        try
        {
            String json = mapper.writeValueAsString(message);
            out.println(json);
        } catch (JsonProcessingException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Gets whether the message is ready to be read.
     *
     * @return true if there is a new message, otherwise returns false.
     * @throws IOException if the communication was closed.
     */
    public boolean readyToRead() throws IOException
    {
        return in.ready();
    }

    /**
     * Reads a message from the connected node. This method blocks the thread,
     * until a message is ready to be read.
     *
     * @return Received message.
     * @throws IOException if there was an error receiving the message. Usually when the node
     *                     disconnected before sending a message.
     */
    public Message readMessage() throws IOException
    {
        try
        {
            String message = in.readLine();
            if (message == null) throw new CommunicationException(this.info.hashCode());
            return mapper.readValue(message, Message.class);
        } catch (SocketException e)
        {
            throw new CommunicationException(this.info.hashCode());
        }
    }

    /**
     * Sends topology info to a connected node.
     *
     * @param topologyInfo to be sent.
     */
    public void sendTopologyInfo(TopologyInfo topologyInfo)
    {
        try
        {
            String json = mapper.writeValueAsString(topologyInfo);
            out.println(json);
        } catch (JsonProcessingException e)
        {
            logger.info("Node " + this.info.hashCode(), e);
        }
    }

    /**
     * Reads a topology info from a connected node.
     *
     * @return {@link cz.cvut.fel.bulkodav.communication.TopologyInfo}
     * or null if there was an {@link IOException} thrown in the process of reading.
     * @throws CommunicationException if there was an error reading a message, most likely caused by
     *                                node disconnection.
     */
    public TopologyInfo readTopologyInfo() throws CommunicationException
    {
        try
        {
            String message = in.readLine();
            if (message == null) throw new CommunicationException(this.info.hashCode());
            return mapper.readValue(message, TopologyInfo.class);
        } catch (SocketException e)
        {
            throw new CommunicationException(this.info.hashCode());
        } catch (IOException e)
        {
            logger.info("Node " + this.info.hashCode(), e);
            return null;
        }
    }

    /**
     * Closes the communication with the connected node.
     */
    public void close()
    {
        try
        {
            out.close();
            in.close();
            socket.close();
            mapper = null;
        } catch (IOException e)
        {
            logger.info("Error closing communicator", e);
        }
    }

    /**
     * Serializes the provided info about the node into the json string.
     *
     * @param nodeInfo The info about the node.
     * @return The info about the node serialized in json string.
     */
    public String serializeId(NodeInfo nodeInfo)
    {
        try
        {
            return mapper.writeValueAsString(nodeInfo);
        } catch (JsonProcessingException e)
        {
            logger.info("Error serializing identification.", e);
            return null;
        }
    }

    /**
     * Deserializes the json string into the info about the node.
     *
     * @param json The json string.
     * @return Deserialized info about the node.
     */
    public NodeInfo deserializeId(String json)
    {
        try
        {
            return mapper.readValue(json, NodeInfo.class);
        } catch (IOException e)
        {
            logger.info("Error deserializing identification.", e);
            return null;
        }
    }


    /**
     * Serializes the provided information about a topology of a node to the json string.
     *
     * @param nodeInfo The information about a topology of a node.
     * @return The information about a topology of a node serialized in json format.
     */
    public String serializeTopologyInfo(TopologyInfo nodeInfo)
    {
        try
        {
            return mapper.writeValueAsString(nodeInfo);
        } catch (JsonProcessingException e)
        {
            logger.info("Error serializing identification.", e);
            return null;
        }
    }


    /**
     * Deserializes information about a topology of a node from a string.
     *
     * @param json information about a topology of a node serialized in json format.
     * @return the information about a topology of a node.
     */
    public TopologyInfo deserializeTopologyInfo(String json)
    {
        try
        {
            return mapper.readValue(json, TopologyInfo.class);
        } catch (IOException e)
        {
            logger.info("Error deserializing identification.", e);
            return null;
        }
    }

    /**
     * Sends info about a change of a user.
     *
     * @param userStateChange the user state change to be sent.
     */
    public void sendUserChange(UserStateChange userStateChange)
    {
        try
        {
            String json = mapper.writeValueAsString(userStateChange);
            out.println(json);
        } catch (JsonProcessingException e)
        {
            logger.info("Node " + this.info.hashCode(), e);
        }
    }

    /**
     * Reads info about a user change.
     *
     * @return the user state change.
     * or null if there was an {@link IOException} thrown in the process of reading.
     * @throws CommunicationException if there was an error reading a message, most likely caused by
     *                                node disconnection.
     */
    public UserStateChange readUserChange() throws CommunicationException
    {
        try
        {
            String message = in.readLine();
            if (message == null) throw new CommunicationException(this.info.hashCode());
            return mapper.readValue(message, UserStateChange.class);
        } catch (SocketException e)
        {
            throw new CommunicationException(this.info.hashCode());
        } catch (IOException e)
        {
            logger.info("Node " + this.info.hashCode(), e);
            return null;
        }
    }

    /**
     * Gets the server port of the node to which this connection link is bound.
     *
     * @return the server port of the node to which this connection link is bound.
     */
    public int getPort()
    {
        return info.getPort();
    }

    /**
     * * Sets the server port of the node to which this connection link is bound.
     *
     * @param port the server port of the node to which this connection link is bound.
     */
    public void setPort(int port)
    {
        this.info.setPort(port);
    }

    /**
     * Gets the info about the node this connection link is bound to.
     *
     * @return the info about the node this connection link is bound to.
     */
    public NodeInfo getInfo()
    {
        return info;
    }

    /**
     * Sets the name of the node this connection link is bound to.
     *
     * @param name the name of the node this connection link is bound to.
     */
    public void setName(String name)
    {
        this.info.setName(name);
    }

    /**
     * Gets the name of the node this connection link is bound to.
     *
     * @return the name of the node this connection link is bound to.
     */
    public String getName()
    {
        return this.info.getName();
    }

    /**
     * Gets the address of the node this connection link is bound to.
     *
     * @return the address of the node this connection link is bound to.
     */
    public String getAddress()
    {
        return this.info.getAddress();
    }
}
