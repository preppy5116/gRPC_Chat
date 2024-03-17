package sbrt.preppy.grpc;

import io.grpc.ManagedChannel;
import sbrt.preppy.grpc.exceptions.UserNotFoundException;

import java.io.IOException;

public interface GrpcProfileClient {
    boolean connectUser(String username) throws IOException;
    void disconnectUser(ManagedChannel channel);
    void sendBroadcastMsg(String text) throws UserNotFoundException;
    void sendPrivateMsg (String text) throws UserNotFoundException;
    void syncMessages();
    void syncUserList();


    void start() throws UserNotFoundException, InterruptedException;
}
