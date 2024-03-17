package sbrt.preppy.server.users;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sbrt.preppy.server.exception.DuplicateUsernameException;
import sbrt.preppy.server.exception.UserNotFoundException;
import sbrt.preppy.server.messages.Message;
import sbrt.preppy.server.messages.MessageType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class UserManager {
    private final List<Message> messages;
    private final Map<String, User> users;


    public UserManager() {
        messages = new ArrayList<>();
//        users = new HashMap<>();
        users = new ConcurrentHashMap<>();
    }

    public void connectUser(String username) throws DuplicateUsernameException {
        if (users.containsKey(username)) {
            throw new DuplicateUsernameException(username);
        } else {
            User user = new User(username);
            users.put(username, user);
        }
    }

    public void disconnectUser(String username, Object mutex) throws UserNotFoundException {
        synchronized (mutex) {
            if (users.containsKey(username)) {
                users.remove(username);
            } else {
                throw new UserNotFoundException("Could not find user: " + username);
            }
        }
    }

    public User findUserByName(String username) throws UserNotFoundException {
        User u = users.get(username);
        if (u != null) {
            return u;
        } else {
            throw new UserNotFoundException(username);
        }
    }



    public Message getLastMessage(String userName) {
        Message msg;
        if (!messages.isEmpty()) {
            msg = messages.get(messages.size() - 1);
            boolean isBroadcast = msg.getMessageType() == MessageType.BROADCAST;
            boolean isPrivate = msg.getMessageType() == MessageType.PRIVATE;
            if (isBroadcast) {
                return msg;
            } else {
                boolean isCorrectUser = msg.getReceiver().contains(userName) || msg.getSender().toString().contains(userName);
                if (isCorrectUser) {
                    return msg;
                }
            }
        } else {
            return null;
        }
        return null;
    }

    public void addToMessages(Message message, Object mutex) {
        synchronized (mutex) {
            try {
                messages.add(message);
                mutex.notifyAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public List<String> getOnlineUsers() {
        Set<String> set = users.keySet();
        return new ArrayList<>(set);
    }

}
