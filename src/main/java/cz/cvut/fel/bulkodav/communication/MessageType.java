package cz.cvut.fel.bulkodav.communication;

/**
 * The {@link MessageType} enum represents the type of the message.
 */
public enum MessageType
{
    /**
     * The message about the death of the leader.
     */
    KING_IS_DEAD,
    /**
     * The message about newly elected leader.
     */
    LONG_LIVE_THE_KING,
    /**
     * The message about the initiation of a connection.
     */
    GREETINGS,
    /**
     * The message about a change of topology.
     */
    TOPOLOGY_CHANGED,
    /**
     * The confirmation message.
     */
    CONFIRM,
    /**
     * The rejection message.
     */
    REJECT,
    /**
     * The chat message
     */
    CHAT,
    /**
     * The topology mapping message.
     */
    MAP_TOPOLOGY,
    /**
     * The message listing online users.
     */
    ONLINE_USERS,
    /**
     * The message containing the name of the node.
     */
    NAME,
}
