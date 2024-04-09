package sbrt.preppy.server.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * @author preppy
 * Database table for storing chat messages
 */
@Entity
@Table
@Setter
@Getter
public class MessageBD {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Timestamp date;
    private String author;
    private String receiver;
    private String messageType;
    private String messageText;

    public MessageBD() {    }
    public MessageBD(Timestamp date, String author, String receiver, String messageType, String messageText) {
        this.date = date;
        this.author = author;
        this.receiver = receiver;
        this.messageType = messageType;
        this.messageText = messageText;
    }

    @Override
    public String toString() {
        return "Messages{ " +
                "id = " + id +
                ", date = " + date +
                ", author = " + author +
                ", receiver = " + receiver +
                ", Type = " + messageType +
                ", message text = " + messageText + "}";
    }
}
