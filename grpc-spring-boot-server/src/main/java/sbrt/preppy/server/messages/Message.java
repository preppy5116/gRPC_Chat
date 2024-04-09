package sbrt.preppy.server.messages;

import lombok.Getter;
import lombok.Setter;
import sbrt.preppy.server.users.User;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

/**
 * The simplest message model used in a chat
 */
@Getter
@Setter
public class Message {

    public static final String PRIVATE_MESSAGE_ID = "PRIVATE";
    public static final String PUBLIC_MESSAGE_ID = "PUBLIC";
    public static final String MESSAGE_TYPE_REGEX = ": ";

    private User sender;
    private  MessageType messageType;
    private  String text;
    private String receiver;
    private  Timestamp timestamp;

    public Message() {}
    public Message(User sender, MessageType messageType, String text) {
        this.sender = sender;
        this.messageType = messageType;
        this.text = text;
        this.timestamp = new Timestamp(new Date().getTime());
    }
    public Message(User sender, MessageType messageType, String text, String receiver) {
        this.sender = sender;
        this.messageType = messageType;
        this.text = text;
        this.receiver = receiver;
        this.timestamp = new Timestamp(new Date().getTime());
    }

    public String getContent() {
        return switch (messageType) {
            case BROADCAST -> PUBLIC_MESSAGE_ID + MESSAGE_TYPE_REGEX + sender.getName() + MESSAGE_TYPE_REGEX + text;
            case PRIVATE -> PRIVATE_MESSAGE_ID + MESSAGE_TYPE_REGEX + sender.getName() + MESSAGE_TYPE_REGEX + text;
            default -> text;
        };
    }


    @Override
    public String toString() {
        return "Message{" +
                "sender=" + sender +
                ", messageType=" + messageType +
                ", receiver=" + receiver +
                ", timestamp=" + timestamp +
                ", text=" + text +
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
