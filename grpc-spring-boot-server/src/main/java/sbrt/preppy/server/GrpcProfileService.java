package sbrt.preppy.server;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import sbrt.preppy.ProfileDescriptorOuterClass;
import sbrt.preppy.ProfileServiceGrpc;
import sbrt.preppy.server.database.MessageBD;
import sbrt.preppy.server.database.MessageRepository;
import sbrt.preppy.server.exception.DuplicateUsernameException;
import sbrt.preppy.server.exception.UserNotFoundException;
import sbrt.preppy.server.messages.Message;
import sbrt.preppy.server.messages.MessageType;
import sbrt.preppy.server.users.User;
import sbrt.preppy.server.users.UserManager;


import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author preppy
 * Service for connecting and running the gRPC server
 */
@Slf4j
@GrpcService
public class GrpcProfileService extends ProfileServiceGrpc.ProfileServiceImplBase {
    private static UserManager userManager;
    private static final Object MSG_MUTEX = new Object();
    private static final Object USER_MUTEX = new Object();
    private static boolean isRunning;
    /**
     * Database repository
     */
    final MessageRepository repository;

    public GrpcProfileService(MessageRepository repository) {
        this.repository = repository;
        if (userManager == null)
            userManager = new UserManager();
        isRunning = true;
    }


    /**
     * Connecting a new client to the server, creating a response to the client with the connection status.
     * Upon successful connection, a broadcast message is sent to everyone.
     *
     * @param userInfo         connection request with the client's name
     * @param responseObserver response with client connection status
     */
    @Override
    public void connectUser(ProfileDescriptorOuterClass.UserInfo userInfo,
                            StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.ConnectMessage> responseObserver) {
        try {
            log.info("connectUser: " + userInfo.getName() + " is connecting to server.");
            userManager.connectUser(userInfo.getName());

            synchronized (USER_MUTEX) {
                USER_MUTEX.notifyAll();
            }

            responseObserver.onNext(ProfileDescriptorOuterClass.ConnectMessage.newBuilder().setUsername(userInfo.getName()).setIsConnected(true).build());
            responseObserver.onCompleted();
            log.info("ConnectUser: " + userInfo.getName() + " is connected to server.");

            ProfileDescriptorOuterClass.MessageText msg = ProfileDescriptorOuterClass.MessageText.newBuilder().setText(userInfo.getName() + " has entered the chat").setSender("Server").build();
            sendBroadcastMsgFromServer(msg);



        } catch (DuplicateUsernameException e) {
            responseObserver.onNext(ProfileDescriptorOuterClass.ConnectMessage.newBuilder().setIsConnected(false).build());
            log.info("connectUser: " + userInfo.getName() + " failed to connect to server.");
            responseObserver.onCompleted();
        }
    }

    /**
     * Disconnecting the client from the server,
     * creating a response message with the shutdown status.
     *
     * @param userInfo         request for disconnection with the name of the client
     * @param responseObserver response with client shutdown status
     */
    @Override
    public void disconnectUser(ProfileDescriptorOuterClass.UserInfo userInfo,
                               StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.DisconnectMessage> responseObserver) {
        try {
            log.info(userInfo.getName() + " is disconnecting from server.");
            userManager.disconnectUser(userInfo.getName());
            synchronized (USER_MUTEX) {
                USER_MUTEX.notifyAll();
            }
            ProfileDescriptorOuterClass.MessageText msg = ProfileDescriptorOuterClass.MessageText.newBuilder().setText(userInfo.getName() + " has left the chat").setSender("Server").build();
            sendBroadcastMsgFromServer(msg);

            responseObserver.onNext(ProfileDescriptorOuterClass.DisconnectMessage.newBuilder().setUsername(userInfo.getName()).setIsDisconnected(true).build());
            responseObserver.onCompleted();
            log.info(userInfo.getName() + " is disconnected from server.");
        } catch (UserNotFoundException e) {
            responseObserver.onNext(ProfileDescriptorOuterClass.DisconnectMessage.newBuilder().setIsDisconnected(false).build());
            log.info(userInfo.getName() + " not found.");
            responseObserver.onCompleted();
        }
    }

    /**
     * Sending a broadcast message to clients,
     * writing the message to the database.
     *
     * @param messageText      request with the client's message and the sender's name
     * @param responseObserver an empty response from the server
     */
    @Override
    public void sendBroadcastMsg(ProfileDescriptorOuterClass.MessageText messageText,
                                 StreamObserver<ProfileDescriptorOuterClass.Empty> responseObserver) {
        User sender;
        try {
            if(messageText.getText().equals("#online")) {
                ProfileDescriptorOuterClass.MessageText msg = ProfileDescriptorOuterClass.MessageText.newBuilder().setText("Online users : " + getUsersOnlineString()+"@"+messageText.getSender()).setSender("Server").build();
                ProfileDescriptorOuterClass.PrivateMessageText prt = ProfileDescriptorOuterClass.PrivateMessageText.newBuilder().setMessageText(msg).setReceiver(messageText.getSender()).build();
                sendPrivateMsgFromServer(prt);
            }
            else {
                sender = userManager.findUserByName(messageText.getSender());
                Message msg = new Message(sender, MessageType.BROADCAST, messageText.getText());

                MessageBD messageBD = new MessageBD(msg.getTimestamp(), String.valueOf(msg.getSender()),
                        String.valueOf(msg.getReceiver()), String.valueOf(msg.getMessageType()), msg.getText());

                synchronized (MSG_MUTEX) {
                    repository.save(messageBD);
                    MSG_MUTEX.notifyAll();
                }
                log.info("sendBroadcastMsg: " + msg);
         
            }
                   responseObserver.onNext(ProfileDescriptorOuterClass.Empty.newBuilder().build());
                responseObserver.onCompleted();
        } catch (UserNotFoundException e) {
            responseObserver.onCompleted();
        }
    }

    /**
     * Sending a private message to clients,
     * writing the message to the database.
     *
     * @param privateMessageText request with the client's message and the receiver's name
     * @param responseObserver   an empty response from the server
     */
    @Override
    public void sendPrivateMsg(ProfileDescriptorOuterClass.PrivateMessageText privateMessageText,
                               StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.Empty> responseObserver) {
        try {
            ProfileDescriptorOuterClass.MessageText mt = privateMessageText.getMessageText();
            User sender = userManager.findUserByName(mt.getSender());
            User uReceiver = userManager.findUserByName(privateMessageText.getReceiver());
            String sReceiver = uReceiver.toString();
            Message msg = new Message(sender, MessageType.PRIVATE, mt.getText(), sReceiver);

            MessageBD messageBD = new MessageBD(msg.getTimestamp(), String.valueOf(msg.getSender()),
                    String.valueOf(msg.getReceiver()), String.valueOf(msg.getMessageType()), msg.getText());

            synchronized (MSG_MUTEX) {
                repository.save(messageBD);
                MSG_MUTEX.notifyAll();
            }
            log.info(msg.toString());
            responseObserver.onNext(ProfileDescriptorOuterClass.Empty.newBuilder().build());
            responseObserver.onCompleted();

        } catch (UserNotFoundException e) {
            responseObserver.onCompleted();
        }
    }

    /**
     * A stream for synchronizing messages for the client
     *
     * @param userInfo         the request
     * @param responseObserver a response observer, which is a special interface for the server to call with its response.
     */
    @Override
    public void syncMessages(ProfileDescriptorOuterClass.UserInfo userInfo,
                             StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.MessageText> responseObserver) {
        while (isRunning) {
            synchronized (MSG_MUTEX) {
                try {
                    MSG_MUTEX.wait();
                } catch (Exception e) {
                    responseObserver.onCompleted();
                }
            }

            MessageBD messageBD = findLastMessage();
            Message msg = getLastMessage(messageBD, userInfo.getName());

            if (msg != null) {
                log.info("syncMessages: " + "Synchronize... : " + msg);

                responseObserver.onNext(
                        ProfileDescriptorOuterClass.MessageText
                                .newBuilder()
                                .setSender(msg.getSender().getName())
                                .setText(msg.getContent()).build());

            }
        }
    }

    /**
     * The steam of sending a list of online clients
     *
     * @param request          an empty request from the client
     * @param responseObserver response with a list of online clients
     */
    @Override
    public void syncUserList(ProfileDescriptorOuterClass.Empty request,
                             StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.UserInfo> responseObserver) {
        while (isRunning) {
            synchronized (USER_MUTEX) {
                try {
                    USER_MUTEX.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                    responseObserver.onCompleted();
                }
            }

            List<String> onlineUsers = userManager.getOnlineUsers();
            StringBuilder userString = new StringBuilder();

            for (String s : onlineUsers) {
                if (!s.equals("Server")) {
                    userString.append(s).append(" ");
                }
            }
            responseObserver.onNext(ProfileDescriptorOuterClass.UserInfo.newBuilder().setName(userString.toString()).build());
        }
    }

    /**
     * Sending a broadcast message from the server.
     * It is used to notify when the client connects and disconnects to the chat.
     *
     * @param messageText a message about connecting and disconnecting the client to the chat.
     */
    private void sendBroadcastMsgFromServer(ProfileDescriptorOuterClass.MessageText messageText) {
        sendBroadcastMsg(messageText, new StreamObserver<>() {
            @Override
            public void onNext(ProfileDescriptorOuterClass.Empty empty) {
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onCompleted() {
            }
        });
    }

    /**
     * Sending a private message from the server.
     * It is used to notify when the client needs online users.
     *
     * @param messageText a message with list of online.
     */
    private void sendPrivateMsgFromServer(ProfileDescriptorOuterClass.PrivateMessageText messageText) {
        sendPrivateMsg(messageText, new StreamObserver<>() {
            @Override
            public void onNext(ProfileDescriptorOuterClass.Empty empty) {
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onCompleted() {
            }
        });
    }

    /**
     * Search for the last message in the in-memory database
     *
     * @return message
     */
    public MessageBD findLastMessage() {
        return repository.findTopByOrderByDateDesc();
    }

    /**
     * Getting and converting an extreme message from a database
     *
     * @param userName client's name
     * @return message or null
     */
    public Message getLastMessage(MessageBD messageBD, String userName) {
        Message msg = new Message();
        try {
            msg.setSender(userManager.findUserByName(messageBD.getAuthor()));
            msg.setReceiver(messageBD.getReceiver());
            msg.setMessageType(MessageType.valueOf(messageBD.getMessageType()));
            msg.setTimestamp(messageBD.getDate());
            msg.setText(messageBD.getMessageText());
        } catch (UserNotFoundException e) {
            throw new RuntimeException(e);
        }
        boolean isBroadcast = msg.getMessageType() == MessageType.BROADCAST;
        if (isBroadcast) {
            return msg;
        } else {
            boolean isCorrectUser = msg.getReceiver().contains(userName) || msg.getSender().toString().contains(userName);
            if (isCorrectUser) {
                return msg;
            }
        }
        return null;
    }

    /**
     * Getting string of actual online users
     */
    public String getUsersOnlineString() {
        List<String> onlineUsers = userManager.getOnlineUsers();
        StringBuilder userString = new StringBuilder();

        for (String s : onlineUsers) {
            if (!s.equals("Server")) {
                userString.append(s).append(" ");
            }
        }
        return String.valueOf(userString);
    }
}
