package sbrt.preppy.server.database;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 *Repository for working with the chat message table
 */
@Repository
public interface MessageRepository extends CrudRepository<MessageBD, Integer> {
    /**
     * Receiving the last message in the database
     * @return Message
     */
    MessageBD findTopByOrderByDateDesc();

}
