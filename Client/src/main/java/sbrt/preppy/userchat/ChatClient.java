package sbrt.preppy.userchat;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import jline.console.ConsoleReader;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

//java -jar Client-1.0-SNAPSHOT.jar

public class ChatClient {
    public static final String PRIVATE_MESSAGE_ID = "PRIVATE";
    public static final String PUBLIC_MESSAGE_ID = "PUBLIC";
    public static final String MESSAGE_TYPE_REGEX = ": ";
    private static final int PORT = 9999;
    private static final Logger logger = Logger.getLogger(ChatClient.class.getName());
    private static ChatGrpc.ChatStub asyncStub;
    private static ChatGrpc.ChatBlockingStub blockingStub;
    private static User user;

    public static void main(String[] args) throws Exception {

        logger.log(Level.INFO, "Client started");
        logger.setLevel(Level.FINE);
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", PORT)
                .usePlaintext(true)
                .build();
        blockingStub = ChatGrpc.newBlockingStub(channel);
        asyncStub = ChatGrpc.newStub(channel);


        ConsoleReader console = new ConsoleReader();
        String author = console.readLine("Set you name: ");
        connectUser(author);
        syncMessages(); // TODO last message
        syncUserList();

        String message;
        while ((message = console.readLine(author + " > ")) != null) {

            if (message.contains("@")) {
                sendPrivateMsg(message);
            } else if (message.equals("exit")) {
                disconnectUser(channel);
            } else {
                sendBroadcastMsg(message);
            }
        }

        disconnectUser(channel);
        console.getTerminal().restore();
    }

    public static boolean connectUser(String username) {
        ChatProto.UserInfo userInfo = ChatProto.UserInfo.newBuilder().setName(username).build();
        ChatProto.ConnectMessage response;
        try {
            response = blockingStub.connectUser(userInfo);
            if (response.getIsConnected()) {
                user = new User(username);
                sendBroadcastMsg(username + " has entered the chat");
                return true;
            } else {
                logger.log(Level.WARNING, "Duplicate username (" + username + ") entered");
            }
        } catch (StatusRuntimeException | UserNotFoundException e) {
            logger.log(Level.SEVERE, "Exception" + e.getMessage());
        }
        return false;
    }

    private static void sendBroadcastMsg(String text) throws UserNotFoundException {
        if (user != null) {
            ChatProto.MessageText messageText = ChatProto.MessageText.newBuilder().setText(text).setSender(user.getName()).build();
            try {
                blockingStub.sendBroadcastMsg(messageText);
            } catch (StatusRuntimeException e) {
                error(e.getMessage());
                System.out.println("Could not connect with server. Try again.");
            }
        } else {
            throw new UserNotFoundException("Could not find user");
        }
    }

    private static void sendPrivateMsg(String text) throws UserNotFoundException {
        int client_count = text.indexOf("@");
        String toClientName = text.substring(client_count + 1);

        if (user != null) {
            // make standard message
            ChatProto.MessageText messageText = ChatProto.MessageText.newBuilder().setText(text).setSender(user.getName()).build();
            // make private message intended for receiver
            ChatProto.PrivateMessageText privateMessageText = ChatProto.PrivateMessageText.newBuilder().setMessageText(messageText).setReceiver(toClientName).build();
            try {
//                info("Send private message...");
                blockingStub.sendPrivateMsg(privateMessageText);
            } catch (StatusRuntimeException e) {
                error(e.getMessage());
                System.out.println("Could not connect with server. Try again.");
            }
        } else {
            throw new UserNotFoundException("Could not find user");
        }
    }

    public static void disconnectUser(ManagedChannel channel) throws InterruptedException {
        ChatProto.UserInfo userInfo = ChatProto.UserInfo.newBuilder().setName(user.getName()).build();
        ChatProto.DisconnectMessage response;
        try {
            response = blockingStub.disconnectUser(userInfo);
            if (response.getIsDisconnected()) {
                logger.log(Level.INFO, "Successfully disconnected from server.");
                sendBroadcastMsg(user.getName() + " has left the chat");
                channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);

            } else {
                logger.log(Level.WARNING, "Failed to disconnect from server");
            }
        } catch (StatusRuntimeException | UserNotFoundException e) {
            logger.log(Level.SEVERE, "Exception" + e.getMessage());
        }
    }

    public static void syncMessages() {
        StreamObserver<ChatProto.MessageText> observer = new StreamObserver<ChatProto.MessageText>() {
            @Override
            public void onNext(ChatProto.MessageText value) {
                placeInRightMessageList(value.getText(), value.getSender());
            }

            @Override
            public void onError(Throwable t) {
                error("Server-side error.");
                System.out.println("Server-side error.");
                System.exit(1);
            }

            @Override
            public void onCompleted() {
            }
        };
        try {
            asyncStub.syncMessages(ChatProto.UserInfo.newBuilder().setName(user.getName()).build(), observer);
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    public static void placeInRightMessageList(String text, String sender) {

        String[] split = text.split(MESSAGE_TYPE_REGEX);
        String MESSAGE_ID = split[0];
        String send = split[1];
        String content = split[2];

        switch (MESSAGE_ID) {
            case PRIVATE_MESSAGE_ID:
                String message = content.substring(0, content.indexOf("@"));
                if (!sender.equals(user.getName())) {
                    System.out.println("[Private message] " + send + ":" + message);
                }
                break;
            case PUBLIC_MESSAGE_ID:
                if (text.contains(" has entered the chat")) {
                    syncUserList();
                }
                if (!sender.equals(user.getName())) {
                    System.out.println(send + ":" + content);
                }
                break;
        }
    }

    public static void syncUserList() {
        StreamObserver<ChatProto.UserInfo> observer = new StreamObserver<ChatProto.UserInfo>() {
            @Override
            public void onNext(ChatProto.UserInfo value) {
                logger.log(Level.INFO, "On-Line users : " + value.getName());
            }
            @Override
            public void onError(Throwable t) {
                error("Server error.");
            }
            @Override
            public void onCompleted() {
            }
        };
        try {
            asyncStub.syncUserList(ChatProto.Empty.newBuilder().build(), observer);

        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    private static void info(String msg, @Nullable Object... params) {
        logger.log(Level.INFO, msg, params);
    }

    private static void error(String msg, @Nullable Object... params) {
        logger.log(Level.WARNING, msg, params);
    }
}
