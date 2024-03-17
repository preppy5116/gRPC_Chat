package sbrt.preppy.server.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserNotFoundException extends Exception {

    public UserNotFoundException(String msg) {
        super( "Could not find user: "+ msg);
        log.info("Could not find user: "+ msg);
    }
}
