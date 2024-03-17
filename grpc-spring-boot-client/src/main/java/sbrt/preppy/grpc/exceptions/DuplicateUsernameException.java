package sbrt.preppy.grpc.exceptions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DuplicateUsernameException extends Exception {
    public DuplicateUsernameException(String message) {
        super(message);
        log.info("Duplicate username: ( " + message + " ) is already entered");
    }
}
