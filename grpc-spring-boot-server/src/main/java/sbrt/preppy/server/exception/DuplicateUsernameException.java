package sbrt.preppy.server.exception;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * When you enter an existing client name on the server,
 * this exception occurs
 */
public class DuplicateUsernameException extends Exception {
    private static final Logger logger = Logger.getLogger(DuplicateUsernameException.class.getName());
    public DuplicateUsernameException(String message) {
        super(message);
        logger.log(Level.SEVERE, "Duplicate username entered: " + message);
    }
}
