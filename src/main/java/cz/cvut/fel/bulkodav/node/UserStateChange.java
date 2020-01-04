package cz.cvut.fel.bulkodav.node;

import cz.cvut.fel.bulkodav.communication.TopologyInfo;

import java.util.List;

/**
 * The {@link UserStateChange} class represents a changes of state of a user.
 */
public class UserStateChange
{
    private String userName;
    private ConnectionState connectionState;
    private List<TopologyInfo> currentTopologyInfos;

    @Deprecated
    public UserStateChange()
    {
    }

    /**
     * The constructor for {@link cz.cvut.fel.bulkodav.node.UserStateChange} class.
     *
     * @param userName             The name of the user.
     * @param connectionState      The connection state of the user.
     * @param currentTopologyInfos The info about the current topology of every node after the change of user state.
     */
    UserStateChange(String userName, ConnectionState connectionState, List<TopologyInfo> currentTopologyInfos)
    {
        this.userName = userName;
        this.connectionState = connectionState;
        this.currentTopologyInfos = currentTopologyInfos;
    }

    /**
     * Gets the name of the user.
     *
     * @return the name of the user.
     */
    public String getUserName()
    {
        return userName;
    }

    /**
     * Gets the connection state of the user.
     *
     * @return the connection state of the user.
     */
    public ConnectionState getConnectionState()
    {
        return connectionState;
    }

    /**
     * Gets the current topology info after the user change.
     *
     * @return The list of topology information about every node in the network.
     */
    public List<TopologyInfo> getCurrentTopologyInfos()
    {
        return currentTopologyInfos;
    }
}
