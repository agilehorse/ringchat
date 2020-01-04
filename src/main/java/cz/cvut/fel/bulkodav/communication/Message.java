package cz.cvut.fel.bulkodav.communication;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

/**
 * The {@link Message} class is used to transport information between nodes.
 */
public class Message implements Serializable, Comparable<Message>
{
    private final Timestamp timestamp;
    private final String content;
    private final MessageType messageType;
    private final NodeInfo recipient;
    private final NodeInfo sender;

    @Deprecated
    public Message()
    {
        this.timestamp = new Timestamp(new Date().getTime());
        content = null;
        messageType = null;
        recipient = null;
        sender = null;
    }

    /**
     * The constructor for {@link cz.cvut.fel.bulkodav.communication.Message} class.
     *
     * @param content     The message content.
     * @param recipient   The info about the recipient of the message.
     * @param sender      The info about the sender of the message
     * @param messageType The type of the message.
     */
    public Message(String content, NodeInfo recipient, NodeInfo sender, MessageType messageType)
    {
        this.content = content;
        this.recipient = recipient;
        this.sender = sender;
        this.messageType = messageType;
        this.timestamp = new Timestamp(new Date().getTime());
    }

    /**
     * Gets the content of the message
     *
     * @return The content of the message.
     */
    public String getContent()
    {
        return content;
    }

    /**
     * Gets the info about the message recipient.
     *
     * @return The message recipient.
     */
    public NodeInfo getRecipient()
    {
        return recipient;
    }

    /**
     * Gets the name of the message recipient.
     *
     * @return The name of message recipient.
     */
    @JsonIgnore
    public String getRecipientName()
    {
        return recipient.getName();
    }

    /**
     * Gets the info about the message sender.
     *
     * @return the info about the message sender.
     */
    public NodeInfo getSender()
    {
        return sender;
    }

    /**
     * Gets the name of the message sender.
     *
     * @return The message sender name.
     */
    @JsonIgnore
    public String getSenderName()
    {
        return sender.getName();
    }

    /**
     * Gets the type of the message.
     *
     * @return The type of the message.
     */
    public MessageType getMessageType()
    {
        return messageType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "Message{" +
                "content='" + content + '\'' +
                '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Message o)
    {
        return this.timestamp.compareTo(o.timestamp);
    }
}
