package cz.cvut.fel.bulkodav.node;

import cz.cvut.fel.bulkodav.communication.CommunicationLink;
import cz.cvut.fel.bulkodav.communication.Message;


/**
 * The {@link MessageHandler} interface provides methods a node should implement
 * to handle the most important messages depending on their type.
 */
public interface MessageHandler
{
    /**
     * Handles a particular message sent during a process of registration of a new node.
     *
     * @param message       The message.
     * @param messageSender The sender of the message.
     */
    void handleGreetingsMessage(Message message, CommunicationLink messageSender);

    /**
     * Handles a chat message
     *
     * @param message       The {@link cz.cvut.fel.bulkodav.communication.MessageType#CHAT} message.
     * @param messageSender The sender of the message.
     */
    void handleChatMessage(Message message, CommunicationLink messageSender);

    /**
     * Handles a particular message sent when a topology is being mapped.
     *
     * @param message       The {@link cz.cvut.fel.bulkodav.communication.MessageType#MAP_TOPOLOGY} message.
     * @param messageSender The sender of the message.
     */
    void handleMapTopologyMessage(Message message, CommunicationLink messageSender);

    /**
     * Handles a particular message sent when a topology changes in some way.
     *
     * @param message       The {@link cz.cvut.fel.bulkodav.communication.MessageType#TOPOLOGY_CHANGED} message.
     * @param messageSender The sender of the message.
     */
    void handleTopologyChangedMessage(Message message, CommunicationLink messageSender);

    /**
     * Handles a particular message sent in a process of discovering all online users ot their change.
     *
     * @param message       The {@link cz.cvut.fel.bulkodav.communication.MessageType#ONLINE_USERS} message.
     * @param messageSender The sender of the message.
     */
    void handleOnlineUsersMessage(Message message, CommunicationLink messageSender);

    /**
     * Handles a particular message sent a ring leader dies.
     *
     * @param message       The {@link cz.cvut.fel.bulkodav.communication.MessageType#KING_IS_DEAD} message.
     * @param messageSender The sender of the message.
     */
    void handleKingIsDeadMessage(Message message, CommunicationLink messageSender);

    /**
     * Handles a particular message sent when a new leader is elected.
     *
     * @param message       The {@link cz.cvut.fel.bulkodav.communication.MessageType#LONG_LIVE_THE_KING} message.
     * @param messageSender The sender of the message.
     */
    void handleLongLiveTheKingMessage(Message message, CommunicationLink messageSender);

    /**
     * Handles a particular message sent in a process of discovering a name of a particular user.
     *
     * @param message       The {@link cz.cvut.fel.bulkodav.communication.MessageType#NAME} message.
     * @param messageSender The sender of the message.
     */
    void handleNameMessage(Message message, CommunicationLink messageSender);
}
