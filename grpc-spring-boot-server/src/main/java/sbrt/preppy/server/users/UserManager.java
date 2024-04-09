package sbrt.preppy.server.users;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sbrt.preppy.server.database.MessageBD;
import sbrt.preppy.server.exception.DuplicateUsernameException;
import sbrt.preppy.server.exception.UserNotFoundException;
import sbrt.preppy.server.messages.Message;
import sbrt.preppy.server.messages.MessageType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class for working with clients
 */
@Slf4j
@Component
public class UserManager {
    /**
     * A field for storing all online server clients
     */
    private final Map<String, User> onlineUsers;

    /**
     * A constructor that creates a map for clients.
     * The first client is a server for sending broadcast messages about connection and disconnection
     */
    public UserManager() {
        onlineUsers = new ConcurrentHashMap<>();
        onlineUsers.put("Server", new User("Server"));

    }

    /**
     * Checking the name for repetition, if the name is not repeated, then recording a new client name
     * @param username client's name
     * @throws DuplicateUsernameException when trying to connect with a duplicate name
     */
    public void connectUser(String username) throws DuplicateUsernameException {
        if (onlineUsers.containsKey(username)) {
            throw new DuplicateUsernameException(username);
        } else {
            onlineUsers.put(username, new User(username));
        }
    }

    /**
     * Checking the content of the client's name. If successfully found,
     * deleting the name from the list of online clients
     * @param username client's name
     * @throws UserNotFoundException the name could not be found in the list of online clients
     */
    public void disconnectUser(String username) throws UserNotFoundException {
        if (onlineUsers.containsKey(username)) {
            onlineUsers.remove(username);
        } else {
            throw new UserNotFoundException("Could not find user: " + username);
        }
    }

    /**
     * Search by name in the list of online clients
     * @param username client's name
     * @return client
     * @throws UserNotFoundException the name could not be found in the list of online clients
     */
    public User findUserByName(String username) throws UserNotFoundException {
        User u = onlineUsers.get(username);
        if (u != null) {
            return u;
        } else {
            throw new UserNotFoundException(username);
        }
    }
    /**
     * Getting a list of online clients
     * @return list of online clients
     */
    public List<String> getOnlineUsers() {
        Set<String> set = onlineUsers.keySet();
        return new ArrayList<>(set);
    }

}
