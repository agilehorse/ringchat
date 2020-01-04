package cz.cvut.fel.bulkodav.communication;

import java.io.Serializable;

/**
 * The {@link TopologyInfo} class contains base topology information about a node.
 */
public class TopologyInfo implements Serializable
{
    private String nodeName;
    private String leftName;
    private String rightName;

    public TopologyInfo()
    {
    }

    /**
     * The constructor for {@link cz.cvut.fel.bulkodav.communication.TopologyInfo} class.
     *
     * @param nodeName  The id of the node.
     * @param leftName  The id of the left node.
     * @param rightName the id of the right node.
     */
    public TopologyInfo(String nodeName, String leftName, String rightName)
    {
        this.nodeName = nodeName;
        this.leftName = leftName;
        this.rightName = rightName;
    }

    /**
     * Gets the name of the node who this info belongs to.
     *
     * @return The name of the node.
     */
    public String getNodeName()
    {
        return nodeName;
    }

    /**
     * Gets the name to the left of the node.
     *
     * @return The name of the left node.
     */
    public String getLeftName()
    {
        return leftName;
    }

    /**
     * Gets the name to the right of the node.
     *
     * @return The name of the right node.
     */
    public String getRightName()
    {
        return rightName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "Node{" +
                "id=" + nodeName +
                ", leftId=" + leftName +
                ", rightId=" + rightName +
                '}';
    }
}
