package sbrt.preppy.server;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import sbrt.preppy.ProfileDescriptorOuterClass;
import sbrt.preppy.ProfileServiceGrpc;
import sbrt.preppy.server.database.DBController;
import sbrt.preppy.server.exception.DuplicateUsernameException;
import sbrt.preppy.server.exception.UserNotFoundException;
import sbrt.preppy.server.messages.Message;
import sbrt.preppy.server.messages.MessageType;
import sbrt.preppy.server.users.User;
import sbrt.preppy.server.users.UserManager;

import java.sql.SQLException;
import java.util.List;

@Slf4j
@GrpcService
public class GrpcProfileService extends ProfileServiceGrpc.ProfileServiceImplBase {
    private static UserManager userManager;

    //TODO найти как избавиться от mutex ConcurentMap?
    private static final Object MSG_MUTEX = new Object();
    private static final Object LEAVE_USER_MUTEX = new Object();
    private static boolean isRunning;
    static DBController base;

    @Autowired
    public void ChatServiceImpl(UserManager user) {
        GrpcProfileService.userManager = user;
        isRunning = true;
        if (base == null) {
            base = new DBController();
            base.createTableMessages();
        }
    }

    /**
     * Подключить нового клиента, записать в мапу он-лайн клиентов    *
     *  @param userInfo
     * @param responseObserver
     */
    @Override
    public void connectUser(ProfileDescriptorOuterClass.UserInfo userInfo,
                            StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.ConnectMessage> responseObserver) {
        try {
            log.info("connectUser: " + userInfo.getName() + " is connecting to server.");
            userManager.connectUser(userInfo.getName());
            responseObserver.onNext(ProfileDescriptorOuterClass.ConnectMessage.newBuilder().setUsername(userInfo.getName()).setIsConnected(true).build());
            responseObserver.onCompleted();
            log.info("connectUser: " + userInfo.getName() + " is connected to server.");
            log.info(Thread.currentThread().getName());
            //TODO Здесь отправка всех клиентов для всех(Создать текстовое поле в прото и записывать туда?)
        } catch (DuplicateUsernameException e) {
            responseObserver.onNext(ProfileDescriptorOuterClass.ConnectMessage.newBuilder().setIsConnected(false).build());
            log.info("connectUser: " + userInfo.getName() + " failed to connect to server.");
            responseObserver.onCompleted();
        }
    }

    @Override
    public void disconnectUser(ProfileDescriptorOuterClass.UserInfo userInfo,
                               StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.DisconnectMessage> responseObserver) {
        try {
            log.info(userInfo.getName() + " is disconnecting from server.");
            log.info(userInfo.getName() + "server");
            userManager.disconnectUser(userInfo.getName(), LEAVE_USER_MUTEX);

            responseObserver.onNext(ProfileDescriptorOuterClass.DisconnectMessage.newBuilder().setUsername(userInfo.getName()).setIsDisconnected(true).build());
            responseObserver.onCompleted();
            log.info(userInfo.getName() + " is disconnected from server.");
        } catch (UserNotFoundException e) {
            responseObserver.onNext(ProfileDescriptorOuterClass.DisconnectMessage.newBuilder().setIsDisconnected(false).build());
            log.info(userInfo.getName() + " not found.");
            responseObserver.onCompleted();
        }
    }

    @Override
    public void sendBroadcastMsg(ProfileDescriptorOuterClass.MessageText messageText,
                                 StreamObserver<ProfileDescriptorOuterClass.Empty> responseObserver) {
        try {
            System.out.println("sendBroadcastMsg:" + messageText.getText());
            User sender = userManager.findUserByName(messageText.getSender());
            //MESSAGE
            Message msg = new Message(sender, MessageType.BROADCAST, messageText.getText());
            userManager.addToMessages(msg, MSG_MUTEX);
            log.info("sendBroadcastMsg: " + msg);
            //RESPONSE OBSERVER
            responseObserver.onNext(ProfileDescriptorOuterClass.Empty.newBuilder().build());
            responseObserver.onCompleted();
            base.loadNewMessage(msg);
            base.getLastMessages();
        } catch (UserNotFoundException e) {
            responseObserver.onCompleted();
        } catch (SQLException e) {
            log.info("Неверный запрос");
        }
    }

    @Override
    public void sendPrivateMsg(ProfileDescriptorOuterClass.PrivateMessageText privateMessageText,
                               StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.Empty> responseObserver) {
        synchronized (MSG_MUTEX) {
            try {
                //GATHERING INFO
                ProfileDescriptorOuterClass.MessageText mt = privateMessageText.getMessageText();
                User sender = userManager.findUserByName(mt.getSender());
                User uReceiver = userManager.findUserByName(privateMessageText.getReceiver());
                String sReceiver = uReceiver.toString();
                //MESSAGE
                Message msg = new Message(sender, MessageType.PRIVATE, mt.getText(), sReceiver);
                userManager.addToMessages(msg, MSG_MUTEX);
                log.info(msg.toString());

                //RESPONSE OBSERVER
                responseObserver.onNext(ProfileDescriptorOuterClass.Empty.newBuilder().build());
                responseObserver.onCompleted();
                base.loadNewMessage(msg);
                base.getLastMessages();
            } catch (UserNotFoundException e) {
                responseObserver.onCompleted();
            } catch (SQLException e) {
                log.info("Неверный запрос");
            }
        }
    }

    @Override
    public void syncMessages(ProfileDescriptorOuterClass.UserInfo userInfo,
                             StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.MessageText> responseObserver) {
        while (isRunning) {
            synchronized (MSG_MUTEX) {
                try {
                    MSG_MUTEX.wait();
                } catch (Exception e) {
//                    e.printStackTrace();
                    responseObserver.onCompleted();
                }
            }
            // check if their is a message that belongs to the user
            Message msg = userManager.getLastMessage(userInfo.getName());

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

    @Override
    public void syncUserList(ProfileDescriptorOuterClass.Empty request,
                             StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.UserInfo> responseObserver) {
        List<String> onlineUsers = userManager.getOnlineUsers();
        StringBuilder userString = new StringBuilder();
        for (String s : onlineUsers) {
            userString.append(s).append(" ");
        }
        System.out.println(userString);
        responseObserver.onNext(ProfileDescriptorOuterClass.UserInfo.newBuilder().setName(userString.toString()).build());
        responseObserver.onCompleted();
    }
}
