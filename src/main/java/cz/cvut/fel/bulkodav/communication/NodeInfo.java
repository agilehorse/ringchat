package cz.cvut.fel.bulkodav.communication;

import java.io.Serializable;
import java.util.Objects;

/**
 * The {@link NodeInfo} class contains atomic information about a node.
 */
public class NodeInfo implements Serializable
{
    private String address;
    private String name;
    private int port;

    @Deprecated
    public NodeInfo()
    {
    }

    /**
     * The constructor for {@link NodeInfo} class.
     *
     * @param address The ip address in string.
     * @param port    The server port of the node.
     * @param name    The name of the node.
     */
    public NodeInfo(String address, int port, String name)
    {
        this.address = address;
        this.port = port;
        this.name = name;
    }

    /**
     * Gets the name of the node.
     *
     * @return The name of the node.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the name of the node.
     *
     * @param name The name of the node.
     */
    void setName(String name)
    {
        this.name = name;
    }

    /**
     * Gets the server port of the node.
     *
     * @return The server port of the node.
     */
    public int getPort()
    {
        return port;
    }

    /**
     * Sets the server port of the node.
     *
     * @param port The server port of the node.
     */
    void setPort(int port)
    {
        this.port = port;
    }

    /**
     * Gets the address of the node.
     *
     * @return The address of the node.
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return name + "_" + address + "_" + port;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof NodeInfo)) return false;
        NodeInfo that = (NodeInfo) o;
        return port == that.port &&
                address.equals(that.address) &&
                name.equals(that.name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(name);
    }
}
