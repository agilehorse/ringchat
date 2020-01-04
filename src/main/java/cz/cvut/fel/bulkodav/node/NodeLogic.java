package cz.cvut.fel.bulkodav.node;

import cz.cvut.fel.bulkodav.communication.CommunicationLink;
import cz.cvut.fel.bulkodav.communication.NodeInfo;
import cz.cvut.fel.bulkodav.communication.TopologyInfo;
import cz.cvut.fel.bulkodav.exceptions.OperationException;

import java.util.List;

/**
 * The {@link NodeLogic} interface provides functionality a node needs to function inside the network.
 */
public interface NodeLogic
{
    /**
     * Handles the death of another node.
     *
     * @param deadNodeInfo The info about the dead node.
     */
    String handleNodeDeath(NodeInfo deadNodeInfo);

    /**
     * Closes the communication with all other nodes.
     */
    void closeCommunicationWithNodes();

    /**
     * Gets the communication link with the right node. Closes communication with the previous right node.
     *
     * @return The object which communicates with the right node.
     */
    CommunicationLink getRightNode();

    /**
     * Gets the object which communicates with the left node.
     *
     * @return The object which communicates with the left node.
     */
    CommunicationLink getLeftNode();

    /**
     * Gets the topology info of every connected node.
     *
     * @return the list of topology info of every connected node.
     */
    List<TopologyInfo> getTopologyInfo() throws OperationException;

    /**
     * Sends a message with the text to the chat.
     *
     * @param text The text of the message to be send.
     * @throws OperationException if the message sending failed.
     */
    void sendMessage(String text) throws OperationException;
}
