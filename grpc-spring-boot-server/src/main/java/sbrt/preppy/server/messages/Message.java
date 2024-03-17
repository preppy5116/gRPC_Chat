package sbrt.preppy.server.messages;

import lombok.Getter;
import sbrt.preppy.server.users.User;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

/**
 * Модель сообщения
 */
public class Message {

    public static final String PRIVATE_MESSAGE_ID = "PRIVATE";
    public static final String PUBLIC_MESSAGE_ID = "PUBLIC";
    public static final String MESSAGE_TYPE_REGEX = ": ";
    /* ----------------------------- GETTERS ----------------------------- */
    @Getter
    private User sender;
    @Getter
    private final MessageType messageType;
    private final String content;
    @Getter
    private String receiver;
    @Getter
    private final Timestamp timestamp;
    @Getter
    private Set<String> activeUsers;

    /* ----------------------------- CONSTRUCTOR ----------------------------- */
    public Message(User sender, MessageType messageType, String text) {
        this.sender = sender;
        this.messageType = messageType;
        this.content = text;
        this.timestamp = new Timestamp(new Date().getTime());
    }

    /* CONNECT / DISCONNECT MESSAGE */
    public Message(MessageType messageType) {
        this.messageType = messageType;
        this.content = "I want to disconnect !!!";
        this.timestamp = new Timestamp(new Date().getTime());
    }

    public Message(User sender, MessageType messageType, String text, String receiver) {
        this.sender = sender;
        this.messageType = messageType;
        this.content = text;
        this.receiver = receiver;
        this.timestamp = new Timestamp(new Date().getTime());
    }

    public Message(MessageType messageType, String text) {
        this.messageType = messageType;
        this.content = text;
        this.timestamp = new Timestamp(new Date().getTime());
    }
    //TODO разобраться почему так
    public String getContent() {
        switch (messageType) {
            case BROADCAST:
                return PUBLIC_MESSAGE_ID + MESSAGE_TYPE_REGEX + sender.getName() + MESSAGE_TYPE_REGEX + content;
            case PRIVATE:
                return PRIVATE_MESSAGE_ID + MESSAGE_TYPE_REGEX + sender.getName() + MESSAGE_TYPE_REGEX + content;
            default:
                return content;
        }
    }

    /* ----------------------------- SETTERS ----------------------------- */
    public void setSender(User user) {
        this.sender = user;
    }

    public void setActiveUsers(Set<String> activeUsers) {
        this.activeUsers = activeUsers;
    }

    /* ----------------------------- OVERRIDE ----------------------------- */
    @Override
    public String toString() {
        return "Message{" +
                "sender=" + sender +
                ", messageType=" + messageType +
                ", receiver=" + receiver +
                ", timestamp=" + timestamp +
                ", content=" + content +
                '}';
    }

    /* ----------------------------- EQUALS/HASH ----------------------------- */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return sender.equals(message.sender) &&
                messageType == message.messageType &&
                timestamp.equals(message.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, messageType, timestamp);
    }
}
