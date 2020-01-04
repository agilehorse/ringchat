package cz.cvut.fel.bulkodav.exceptions;

import java.net.SocketException;


/**
 * The {@link CommunicationException} is raised when the communication with the node failed.
 */
public class CommunicationException extends SocketException
{
    private final int nodeId;

    /**
     * The constructor for {@link CommunicationException} class.
     *
     * @param nodeId The name of the node that caused the exception.
     */
    public CommunicationException(int nodeId)
    {
        this.nodeId = nodeId;
    }

    /**
     * The id of the node with which the communication failed.
     *
     * @return The error node id.
     */
    public int getNodeId()
    {
        return nodeId;
    }
}
