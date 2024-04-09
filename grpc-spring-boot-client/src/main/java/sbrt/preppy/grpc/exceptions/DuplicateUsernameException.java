package sbrt.preppy.grpc.exceptions;

import lombok.extern.slf4j.Slf4j;

/**
 * The exception is about using a name that is already on the server
 */
@Slf4j
public class DuplicateUsernameException extends Exception {
    /**
     * The exception is about name duplication. It can be obtained if the server responds negatively
     * to the connection of a new client
     * @param message duplicate client's name
     */
    public DuplicateUsernameException(String message) {
        System.out.println(" Повтор имени: ( " + message + " ) уже используется");
    }

}
