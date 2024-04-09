package sbrt.preppy.grpc;

import io.grpc.ManagedChannel;
import sbrt.preppy.grpc.exceptions.DuplicateUsernameException;
import sbrt.preppy.grpc.exceptions.UserNotFoundException;

import java.io.IOException;

/**
 * @author preppy
 * A interface describing the work of the chat client.
 */
public interface GrpcProfileClient {
    void start() throws UserNotFoundException, InterruptedException;

    boolean connectUser(String username) throws IOException, DuplicateUsernameException, UserNotFoundException;

    boolean disconnectUser(ManagedChannel channel);

    void sendBroadcastMsg(String text) throws UserNotFoundException;

    void sendPrivateMsg(String text) throws UserNotFoundException;

    void syncMessages();

    void syncUserList();

}
