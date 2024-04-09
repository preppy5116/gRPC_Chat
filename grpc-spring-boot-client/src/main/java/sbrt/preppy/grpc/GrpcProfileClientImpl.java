package sbrt.preppy.grpc;

import io.grpc.*;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;
import sbrt.preppy.ProfileDescriptorOuterClass;
import sbrt.preppy.ProfileServiceGrpc;

import jline.console.ConsoleReader;
import sbrt.preppy.grpc.clientlog.MessageCreator;
import sbrt.preppy.grpc.clientlog.MessageHandler;
import sbrt.preppy.grpc.exceptions.DuplicateUsernameException;
import sbrt.preppy.grpc.exceptions.UserNotFoundException;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author preppy
 * A class describing the work of the chat client.
 */
@Component
public class GrpcProfileClientImpl implements GrpcProfileClient {
    /**
     * The field is the client's host
     */
    private static final String HOST = "localhost";

    /**
     * The field is the client's port
     */
    private static final int PORT = 9090;
    /**
     *
     */
    private static final String TARGET = HOST + ":" + PORT;
    /**
     *
     */
    public static final String PRIVATE_MESSAGE_ID = "PRIVATE";
    /**
     *
     */
    public static final String PUBLIC_MESSAGE_ID = "PUBLIC";
    /**
     *
     */
    public static final String MESSAGE_TYPE_REGEX = ": ";

    /**
     * Blocking stub for calling service methods
     */
    private ProfileServiceGrpc.ProfileServiceBlockingStub blockingStub;

    /**
     * Non-blocking stub for calling service methods
     */
    private final ProfileServiceGrpc.ProfileServiceStub asyncStub;

    /**
     * gRPC channel for connection stubs
     */
    ManagedChannel channel;
    /**
     *
     */
    private static User user;
    /**
     * Creating messages/log for the client
     */
    MessageCreator messageCreator;

    /**
     *A constructor in which a channel is created for the client's work.
     * A blocking and asynchronous stub for the channel are also being created
     */
    public GrpcProfileClientImpl() {
        channel = ManagedChannelBuilder.forTarget(HOST + ":" + PORT)
                .usePlaintext()
                .build();
        blockingStub = ProfileServiceGrpc.newBlockingStub(channel);
        asyncStub = ProfileServiceGrpc.newStub(channel);
        messageCreator = new MessageHandler();
    }

    /**
     * The method that starts the client's work
     */
    public void start() {
        try (ConsoleReader console = new ConsoleReader()) {
            String author;
            String message;

            author = console.readLine(" Set your name: ");
            boolean running = connectUser(author);

            syncUserList();
            syncMessages();

            while (running) {
                message = console.readLine(">");
                if (message.contains("@")) {
                    sendPrivateMsg(message);
                } else if (message.equals("exit")) {
                    running = false;
                } else if (message.isEmpty()) {
                    messageCreator.printWrongMessageBody();
                } else {
                    sendBroadcastMsg(message);
                }
            }
            if (disconnectUser(channel)) {
                console.getTerminal().restore();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sending a request to the server to connect the client by name.
     * It is not allowed to use @ and server in the name.
     * @param username the entered name of the client
     * @return connection status
     */
    @Override
    public boolean connectUser(String username) throws IOException {

        if (username.contains("@") | username.contains("Server")) {
            messageCreator.printWrongSymbol();
            try (ConsoleReader console = new ConsoleReader()) {
                String author = console.readLine(" Set you name: ");
                return connectUser(author);
            }
        }
        ProfileDescriptorOuterClass.UserInfo userInfo = ProfileDescriptorOuterClass.UserInfo.newBuilder().setName(username).build();
        ProfileDescriptorOuterClass.ConnectMessage response;
        try {
            response = blockingStub.connectUser(userInfo);
            if (response.getIsConnected()) {
                user = new User(username);
                return true;
            } else {
                throw new DuplicateUsernameException(username);
            }
        } catch (StatusRuntimeException e) {
            System.out.println("Exception" + e.getMessage());
        } catch (DuplicateUsernameException e) {
            try (ConsoleReader console = new ConsoleReader()) {
                String author = console.readLine("Set you name: ");
                return connectUser(author);
            }
        }
        return false;
    }

    /**
     * Sending a request to disconnect the client from the server.
     * If it is impossible to disconnect, a negative response will be returned.
     * @param channel client channel waiting to be disconnected
     * @return disconnection status
     */
    @Override
    public boolean disconnectUser(ManagedChannel channel) {
        ProfileDescriptorOuterClass.UserInfo userInfo = ProfileDescriptorOuterClass.UserInfo.newBuilder().setName(user.getName()).build();
        ProfileDescriptorOuterClass.DisconnectMessage response;
        try {
            response = blockingStub.disconnectUser(userInfo);
            if (response.getIsDisconnected()) {
                messageCreator.printSuccessFullyDisconnect();
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                return true;
            } else {
                messageCreator.printFailedDisconnect();
            }
        } catch (StatusRuntimeException e) {
            System.out.println("Exception" + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return false;
    }


    /**
     * Sending a broadcast message from the client to the server
     * @param text the text of the message
     * @throws UserNotFoundException  the chat client was not created
     */
    @Override
    public void sendBroadcastMsg(String text) throws UserNotFoundException {
        if (user != null) {
            ProfileDescriptorOuterClass.MessageText messageText = ProfileDescriptorOuterClass.MessageText.newBuilder().setText(text).setSender(user.getName()).build();
            try {
                blockingStub.sendBroadcastMsg(messageText);
            } catch (StatusRuntimeException e) {
                messageCreator.printFailedMessage();
            }
        } else {
            throw new UserNotFoundException("Could not find user");
        }
    }

    /**
     * Sending a private message from the client to the server
     * @param text the text of the message
     * @throws UserNotFoundException the chat client was not created
     */
    @Override
    public void sendPrivateMsg(String text) throws UserNotFoundException {
        int client_count = text.indexOf("@");
        String toClientName = text.substring(client_count + 1);
        if (user != null) {
            ProfileDescriptorOuterClass.MessageText messageText = ProfileDescriptorOuterClass.MessageText.newBuilder().setText(text).setSender(user.getName()).build();
            ProfileDescriptorOuterClass.PrivateMessageText privateMessageText = ProfileDescriptorOuterClass.PrivateMessageText.newBuilder().setMessageText(messageText).setReceiver(toClientName).build();
            try {
                blockingStub.sendPrivateMsg(privateMessageText);
            } catch (StatusRuntimeException e) {
                messageCreator.printFailedMessage();
            }
        } else {
            throw new UserNotFoundException("Could not find user");
        }
    }

    /**
     * Opening a stream to synchronize messages received from the server
     */
    @Override
    public void syncMessages() {
        StreamObserver<ProfileDescriptorOuterClass.MessageText> observer = new StreamObserver<>() {
            @Override
            public void onNext(ProfileDescriptorOuterClass.MessageText messageText) {
                try {
                    placeInRightMessageList(messageText.getText(), messageText.getSender());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            @Override
            public void onError(Throwable t) {
                messageCreator.printMessage(" Server-side error.");
                System.exit(1);
            }

            @Override
            public void onCompleted() {      }
        };
        try {
            asyncStub.syncMessages(ProfileDescriptorOuterClass.UserInfo.newBuilder().setName(user.getName()).build(), observer);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Distribution of the received message for display as private or broadcast
     * @param text the text of the message
     * @param sender sender of message
     */
    private void placeInRightMessageList(String text, String sender) throws InterruptedException {

        String[] split = text.split(MESSAGE_TYPE_REGEX);
        String MESSAGE_ID = split[0];
        String send = split[1];
        String content = split[2];

        switch (MESSAGE_ID) {
            case PRIVATE_MESSAGE_ID:
                String message = content.substring(0, content.indexOf("@"));
                if (!sender.equals(user.getName())) {
                    messageCreator.printMessage("[Private message] " + send + ":" + message);
                }
                if (sender.equals("Server")) {
                    messageCreator.printMessage(message);
                }
                break;
            case PUBLIC_MESSAGE_ID:
                if (!sender.equals(user.getName())) {
                    messageCreator.printMessage(" " + send + ":" + content);
                }
                break;
        }
    }

    /**
     * Stream for displaying the current list of on-line clients
     */
    @Override
    public void syncUserList() {
        StreamObserver<ProfileDescriptorOuterClass.UserInfo> observer = new StreamObserver<>() {
            @Override
            public void onNext(ProfileDescriptorOuterClass.UserInfo value) {
                System.out.println("Online users : " + value.getName());
            }

            @Override
            public void onError(Throwable t) {
                System.out.println(" Server error.");
            }

            @Override
            public void onCompleted() {
            }
        };
        try {
            asyncStub.syncUserList(ProfileDescriptorOuterClass.Empty.newBuilder().build(), observer);
        } catch (Exception e) {
            messageCreator.printMessage(e.getMessage());
        }
    }

    /**
     * Installing a blocking plug. It is necessary for testing
     * @param blockingStub
     */
    public void setBlockingStub(ProfileServiceGrpc.ProfileServiceBlockingStub blockingStub) {
        this.blockingStub = blockingStub;
        user = new User("test");
    }
}
