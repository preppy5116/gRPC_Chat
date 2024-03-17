package sbrt.preppy.grpc;

import io.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sbrt.preppy.ProfileDescriptorOuterClass;
import sbrt.preppy.ProfileServiceGrpc;

import jline.console.ConsoleReader;
import sbrt.preppy.grpc.exceptions.DuplicateUsernameException;
import sbrt.preppy.grpc.exceptions.UserNotFoundException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GrpcProfileClientImpl implements GrpcProfileClient {
    private static final String HOST = "localhost";
    private static final int PORT = 9090;
    private static final String TARGET = HOST + ":" + PORT;

    public static final String PRIVATE_MESSAGE_ID = "PRIVATE";
    public static final String PUBLIC_MESSAGE_ID = "PUBLIC";
    public static final String MESSAGE_TYPE_REGEX = ": ";


    private final ProfileServiceGrpc.ProfileServiceBlockingStub blockingStub;
    private final ProfileServiceGrpc.ProfileServiceStub asyncStub;
    ManagedChannel channel;
    private static User user;


    public GrpcProfileClientImpl() {
        channel = ManagedChannelBuilder.forTarget(TARGET)
                .usePlaintext()
                .build();
        blockingStub = ProfileServiceGrpc.newBlockingStub(channel);
        asyncStub = ProfileServiceGrpc.newStub(channel);
    }

    public void start() {
        //TODO запрос на выведение всех on-line клиентов(getOnline?),
        // выведение в консоль лист клиентов

        try (ConsoleReader console = new ConsoleReader()) {
            String author = console.readLine("Set you name: ");
            boolean running = connectUser(author);
            String message;

            while (running) {
                message = console.readLine(user.getName() + " > ");
                if (message.contains("@")) {
                    sendPrivateMsg(message);
                } else if (message.equals("exit")) {
                    running = false;
                } else if (message.isEmpty()) {
                    System.out.println("####Введите сообщение для отправки всем");
                    System.out.println("####Для приватного сообщения в конце текста укажите получателя @username" +
                            " text @username");
                } else {
                    sendBroadcastMsg(message);
                }
            }
            disconnectUser(channel);
            console.getTerminal().restore();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean connectUser(String username) throws IOException {
        ProfileDescriptorOuterClass.UserInfo userInfo = ProfileDescriptorOuterClass.UserInfo.newBuilder().setName(username).build();
        ProfileDescriptorOuterClass.ConnectMessage response;
        try {
            response = blockingStub.connectUser(userInfo);
            if (response.getIsConnected()) {
                user = new User(username);
                sendBroadcastMsg(username + " has entered the chat");
                syncMessages();
                syncUserList();
                return true;
            } else {
                throw new DuplicateUsernameException(username);
            }
        } catch (StatusRuntimeException | UserNotFoundException e) {
            System.out.println("Exception" + e.getMessage());
        } catch (DuplicateUsernameException e) {

            try(ConsoleReader console = new ConsoleReader()) {
                String author = console.readLine("Set you name: ");
                return connectUser(author);
            }
        }
        return false;
    }

    @Override
    public void disconnectUser(ManagedChannel channel) {
        ProfileDescriptorOuterClass.UserInfo userInfo = ProfileDescriptorOuterClass.UserInfo.newBuilder().setName(user.getName()).build();
        ProfileDescriptorOuterClass.DisconnectMessage response;
        try {
            sendBroadcastMsg(user.getName() + " has left the chat");
            response = blockingStub.disconnectUser(userInfo);
            if (response.getIsDisconnected()) {
                System.out.println("Successfully disconnected from server.");

                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } else {
                System.out.println("Failed to disconnect from server");
            }
        } catch (StatusRuntimeException e) {
            System.out.println("Exception" + e.getMessage());
        } catch (InterruptedException | UserNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendBroadcastMsg(String text) throws UserNotFoundException {
        if (user != null) {
            ProfileDescriptorOuterClass.MessageText messageText = ProfileDescriptorOuterClass.MessageText.newBuilder().setText(text).setSender(user.getName()).build();
            try {
                blockingStub.sendBroadcastMsg(messageText);
            } catch (StatusRuntimeException e) {
                log.info(e.getMessage());
                System.out.println("Could not connect with server. Try again.");
            }
        } else {
            throw new UserNotFoundException("Could not find user");
        }
    }

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
                log.info(e.getMessage());
                System.out.println("Could not connect with server. Try again.");
            }
        } else {
            throw new UserNotFoundException("Could not find user");
        }
    }

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
                log.info("Server-side error.");
                System.exit(1);
            }

            @Override
            public void onCompleted() {

            }
        };
        try {
            asyncStub.syncMessages(ProfileDescriptorOuterClass.UserInfo.newBuilder().setName(user.getName()).build(), observer);
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }

    private void placeInRightMessageList(String text, String sender) throws InterruptedException {
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


    @Override
    public void syncUserList() {
        StreamObserver<ProfileDescriptorOuterClass.UserInfo> observer = new StreamObserver<>() {
            @Override
            public void onNext(ProfileDescriptorOuterClass.UserInfo value) {
                System.out.println("On-Line users : " + value.getName());
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Server error.");
            }

            @Override
            public void onCompleted() {
            }
        };
        try {
            asyncStub.syncUserList(ProfileDescriptorOuterClass.Empty.newBuilder().build(), observer);
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }
}
