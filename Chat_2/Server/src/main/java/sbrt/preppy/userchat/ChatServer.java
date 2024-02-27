package sbrt.preppy.userchat;

import com.salesforce.grpc.contrib.Servers;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import sbrt.preppy.userchat.exception.DuplicateUsernameException;
import sbrt.preppy.userchat.exception.UserNotFoundException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatServer {
    private static final Logger LOGGER = Logger.getLogger(ChatServer.class.getName());
    private static UserManager userManager;
    private static final Object MSG_MUTEX = new Object();
    private static final Object NEW_USER_MUTEX = new Object();
    private static final Object LEAVE_USER_MUTEX = new Object();
    private static int PORT = 9999;
    private static boolean isRunning;


    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder
                .forPort(PORT)
                .addService(new ChatServiceImpl())
                .build();

        Servers.shutdownWithJvm(server, 1000);
        server.start();
        isRunning = true;
        info("Server started, listening on " + PORT);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.log(Level.SEVERE, "gRPC server shutting down (JVM is shutting down)");
            isRunning = false;
            server.shutdown();
            LOGGER.log(Level.SEVERE, "gRPC server is shutdown");
        }));
        Thread.currentThread().join();
        server.awaitTermination();
    }

    private static void info(String msg, @Nullable Object... params) {
        LOGGER.log(Level.INFO, msg, params);
    }

    public static class ChatServiceImpl extends ChatGrpc.ChatImplBase {
//
//        private Observable messageObservable = new Observable() {
//            @Override
//            public void notifyObservers(Object arg) {
//                super.setChanged();
//                super.notifyObservers(arg);
//            }
//        };

        public ChatServiceImpl() {
            if (userManager == null) {
                userManager = new UserManager();
            }
        }

        @Override
        public void connectUser(ChatProto.UserInfo userInfo, StreamObserver<ChatProto.ConnectMessage> responseObserver) {
            try {
                LOGGER.log(Level.INFO, userInfo.getName() + " is connecting to server.");
                userManager.connectUser(userInfo.getName(), NEW_USER_MUTEX);

                responseObserver.onNext(ChatProto.ConnectMessage.newBuilder().setUsername(userInfo.getName()).setIsConnected(true).build());
                responseObserver.onCompleted();
                LOGGER.log(Level.INFO, userInfo.getName() + " is connected to server.");

            } catch (DuplicateUsernameException e) {
                responseObserver.onNext(ChatProto.ConnectMessage.newBuilder().setIsConnected(false).build());
                LOGGER.log(Level.WARNING, userInfo.getName() + " failed to connect to server.");
                responseObserver.onCompleted();
            }
        }

        @Override
        public void disconnectUser(ChatProto.UserInfo userInfo, StreamObserver<ChatProto.DisconnectMessage> responseObserver) {
            try {
                LOGGER.log(Level.INFO, userInfo.getName() + " is disconnecting from server.");
                userManager.disconnectUser(userInfo.getName(), LEAVE_USER_MUTEX);

                responseObserver.onNext(ChatProto.DisconnectMessage.newBuilder().setUsername(userInfo.getName()).setIsDisconnected(true).build());
                responseObserver.onCompleted();
                LOGGER.log(Level.INFO, userInfo.getName() + " is disconnected from server.");
            } catch (UserNotFoundException e) {
                responseObserver.onNext(ChatProto.DisconnectMessage.newBuilder().setIsDisconnected(false).build());
                LOGGER.log(Level.WARNING, userInfo.getName() + " not found.");
                responseObserver.onCompleted();
            }
        }

        /*  -------------------------------- SENDING MESSAGES -------------------------------- */
        // send a message to all users
        // put a message in the message list, that is accessible by all users, and notify the sync method
        @Override
        public void sendBroadcastMsg(ChatProto.MessageText messageText, StreamObserver<ChatProto.Empty> responseObserver) {
            synchronized (MSG_MUTEX) {
                try {
                    //GATHERING INFO
                    User sender = userManager.findUserByName(messageText.getSender());
                    //MESSAGE
                    Message msg = new Message(sender, MessageType.BROADCAST, messageText.getText());
                    userManager.addToMessages(msg, MSG_MUTEX);
                    LOGGER.log(Level.INFO, msg.toString());
                    //RESPONSE OBSERVER
                    responseObserver.onNext(ChatProto.Empty.newBuilder().build());
                    responseObserver.onCompleted();
                } catch (UserNotFoundException e) {
                    responseObserver.onCompleted();
                }
            }
        }

        @Override
        public void sendPrivateMsg(ChatProto.PrivateMessageText privateMessageText, StreamObserver<ChatProto.Empty> responseObserver) {
            synchronized (MSG_MUTEX) {
                try {
                    //GATHERING INFO
                    ChatProto.MessageText mt = privateMessageText.getMessageText();
                    User sender = userManager.findUserByName(mt.getSender());
                    User uReceiver = userManager.findUserByName(privateMessageText.getReceiver());
                    String sReceiver = uReceiver.toString();
                    //MESSAGE
                    Message msg = new Message(sender, MessageType.PRIVATE, mt.getText(), sReceiver);
                    userManager.addToMessages(msg, MSG_MUTEX);
                    LOGGER.log(Level.INFO, msg.toString());

                    //RESPONSE OBSERVER
                    responseObserver.onNext(ChatProto.Empty.newBuilder().build());
                    responseObserver.onCompleted();
                } catch (UserNotFoundException e) {
                    responseObserver.onCompleted();
                }
            }
        }

        /*  -------------------------------- GETTING MESSAGES -------------------------------- */
        // synchronize message list of all users, so that they receive the latest message
        // mutex will wait until a message is added to the list -- consume
        @Override
        public void syncMessages(ChatProto.UserInfo userInfo, StreamObserver<ChatProto.MessageText> responseObserver) {
            while (isRunning) {
                synchronized (MSG_MUTEX) {
                    try {
                        MSG_MUTEX.wait();
                    } catch (Exception e) {
                        e.printStackTrace();
                        responseObserver.onCompleted();
                    }
                }
                // check if their is a message that belongs to the user
                Message msg = userManager.getLastMessage(userInfo.getName());

                if (msg != null) {
                    info("Synchronize... : " + msg);

                    responseObserver.onNext(
                            ChatProto.MessageText
                                    .newBuilder()
                                    .setSender(msg.getSender().getName())
                                    .setText(msg.getContent()).build());

                }
            }
        }

        @Override
        public void syncUserList(ChatProto.Empty empty, StreamObserver<ChatProto.UserInfo> responseObserver) {
            List<String> onlineUsers = userManager.getOnlineUsers();
            StringBuilder userString = new StringBuilder();
            for (String s : onlineUsers) {
                userString.append(s).append(" ");
            }
            System.out.println(userString);
            for (String s : onlineUsers) {
                responseObserver.onNext(ChatProto.UserInfo.newBuilder().setName(userString.toString()).build());
            }

//            while (isRunning) {
//                synchronized (NEW_USER_MUTEX) {
//                    try {
//                        NEW_USER_MUTEX.wait();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        responseObserver.onCompleted();
//                    }
//
//                    onlineUsers = userManager.getOnlineUsers();
//                    for (String s : onlineUsers) {
//                        responseObserver.onNext(ChatProto.UserInfo.newBuilder().setName(userString.toString()).build());
//                    }
//                    responseObserver.onNext(ChatProto.UserInfo.newBuilder().setName(userString.toString()).build());
//
//                }
//            }
        }

    }
}
