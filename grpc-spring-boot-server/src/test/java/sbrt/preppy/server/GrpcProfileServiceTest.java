package sbrt.preppy.server;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import org.mockito.Mock;
import org.mockito.Mockito;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import sbrt.preppy.ProfileDescriptorOuterClass;
import sbrt.preppy.server.database.MessageRepository;
import sbrt.preppy.server.exception.UserNotFoundException;
import sbrt.preppy.server.users.UserManager;

import java.util.Arrays;
import java.util.List;


@SpringBootTest(properties = {
        "grpc.server.inProcessName=test",
        "grpc.server.port=9092",
        "grpc.client.petService.address=in-process:test"
})
@SpringJUnitConfig(classes = {GrpcSpringBootServerApplication.class})
class GrpcProfileServiceTest {
    @Mock
    MessageRepository mockRepository;
    @Mock
    UserManager userManager;

    //Заглушки для StreamObserver разных видов
    @Mock
    StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.ConnectMessage> mockObserver;
    @Mock
    StreamObserver<ProfileDescriptorOuterClass.DisconnectMessage> mockObserverDisconnect;


    @Test
    public void testConnectUser() {
        // Создание объекта GrpcProfileService для тестирования
        GrpcProfileService service = new GrpcProfileService(mockRepository);
        // Вызов метода connectUser
        ProfileDescriptorOuterClass.UserInfo userInfo = ProfileDescriptorOuterClass.UserInfo.newBuilder().setName("testConnectUser").build();
        service.connectUser(userInfo, mockObserver);

        // Проверка, что методы onNext и onCompleted были вызваны у StreamObserver
        verify(mockObserver).onNext(ProfileDescriptorOuterClass.ConnectMessage.newBuilder().setUsername("testConnectUser").setIsConnected(true).build());
        verify(mockObserver).onCompleted();
    }

    @Test
    public void testConnectUserDuplicateUsername() {
//        StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.ConnectMessage> mockObserver =
//                mock(StreamObserver.class);
        StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.ConnectMessage> mockObserver2 =
                mock(StreamObserver.class);

        // Создание объекта GrpcProfileService для тестирования
        GrpcProfileService service = new GrpcProfileService(mockRepository);

        // Вызов метода connectUser
        ProfileDescriptorOuterClass.UserInfo userInfo = ProfileDescriptorOuterClass.UserInfo.newBuilder().setName("testUser").build();
        service.connectUser(userInfo, mockObserver);
        ProfileDescriptorOuterClass.UserInfo userInfo2 = ProfileDescriptorOuterClass.UserInfo.newBuilder().setName("testUser").build();
        service.connectUser(userInfo2, mockObserver2);

        // Проверка, что методы onNext и onCompleted были вызваны у StreamObserver
        verify(mockObserver2).onNext(ProfileDescriptorOuterClass.ConnectMessage.newBuilder().setUsername("").setIsConnected(false).build());
        verify(mockObserver2).onCompleted();
    }

    //Проверить, что при правильных данных метод отсоединяет пользователя и возвращает сообщение
    // об успешном отключении от сервера.
    @Test
    void testDisconnectUser() {
        GrpcProfileService service = new GrpcProfileService(mockRepository);

        // Вызов метода connectUser
        StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.ConnectMessage> mockObserver =
                mock(StreamObserver.class);
        ProfileDescriptorOuterClass.UserInfo userInfo = ProfileDescriptorOuterClass.UserInfo.newBuilder().setName("testConnectUser").build();
        service.connectUser(userInfo, mockObserver);

        // Создаем заглушки объектов
        StreamObserver<ProfileDescriptorOuterClass.DisconnectMessage> mockObserverDisconnect =
                mock(StreamObserver.class);

        ProfileDescriptorOuterClass.UserInfo userInfoDisconnect = ProfileDescriptorOuterClass.UserInfo.newBuilder().setName("testConnectUser").build();
        service.disconnectUser(userInfoDisconnect, mockObserverDisconnect);

        // Проверяем, что методы responseObserver были вызваны с корректными параметрами
        verify(mockObserverDisconnect).onNext(ProfileDescriptorOuterClass.DisconnectMessage.newBuilder().setUsername("testConnectUser").setIsDisconnected(true).build());
        verify(mockObserverDisconnect).onCompleted();
    }

    //Проверить, что при неправильных данных (например, неверное имя пользователя) метод
    //возвращает сообщение о неудачной попытке отключения от сервера.
    @Test
    void testDisconnectUserThrowsUserNotFound() {
        GrpcProfileService service = new GrpcProfileService(mockRepository);

        // Создаем заглушки объектов
        StreamObserver<ProfileDescriptorOuterClass.DisconnectMessage> mockObserverDisconnect =
                mock(StreamObserver.class);

        ProfileDescriptorOuterClass.UserInfo userInfoDisconnect = ProfileDescriptorOuterClass.UserInfo.newBuilder().setName("testUserDisconnectNotFound").build();
        service.disconnectUser(userInfoDisconnect, mockObserverDisconnect);

        // Проверяем, что метод onNext вызывается с isDisconnected установленным в false
        verify(mockObserverDisconnect, times(1)).onNext(
                ProfileDescriptorOuterClass.DisconnectMessage.newBuilder().setIsDisconnected(false).build());
        verify(mockObserverDisconnect).onCompleted();
    }

    //Перехватить сообщение "Hello from User1"
    @Test
    public void sendBroadcastMsgTest() {
        GrpcProfileService service = new GrpcProfileService(mockRepository);

        StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.ConnectMessage> mockObserver =
                mock(StreamObserver.class);

        // Вызов метода connectUser
        ProfileDescriptorOuterClass.UserInfo userInfo = ProfileDescriptorOuterClass.UserInfo.newBuilder().setName("User1").build();
        service.connectUser(userInfo, mockObserver);

        ProfileDescriptorOuterClass.MessageText messageText = ProfileDescriptorOuterClass.MessageText.newBuilder()
                .setSender("User1")
                .setText("Hello from User1")
                .build();

        StreamObserver<ProfileDescriptorOuterClass.Empty> responseObserver = mock(StreamObserver.class);

        service.sendBroadcastMsg(messageText, responseObserver);
        Mockito.verify(responseObserver, times(1)).onNext(ProfileDescriptorOuterClass.Empty.newBuilder().build());
        Mockito.verify(responseObserver, times(1)).onCompleted();
    }

    @Test
    public void sendPrivateMsgTest() {
        GrpcProfileService service = new GrpcProfileService(mockRepository);
        // Создание mock объектов
        ProfileDescriptorOuterClass.PrivateMessageText.Builder privateMsgBuilder = ProfileDescriptorOuterClass.PrivateMessageText.newBuilder();
        ProfileDescriptorOuterClass.MessageText.Builder msgBuilder = ProfileDescriptorOuterClass.MessageText.newBuilder();
        StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.ConnectMessage> mockObserver =
                mock(StreamObserver.class);

        ProfileDescriptorOuterClass.UserInfo userInfoSender = ProfileDescriptorOuterClass.UserInfo.newBuilder().setName("Sender").build();
        service.connectUser(userInfoSender, mockObserver);

        ProfileDescriptorOuterClass.UserInfo userInfoReceiver = ProfileDescriptorOuterClass.UserInfo.newBuilder().setName("Receiver").build();
        service.connectUser(userInfoReceiver, mockObserver);

        // Наполнение объектов данными
        msgBuilder.setSender("Sender");
        msgBuilder.setText("Hello!");
        privateMsgBuilder.setMessageText(msgBuilder.build());
        privateMsgBuilder.setReceiver("Receiver");

        // Создание mock объектов
        StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.Empty> responseObserver = Mockito.mock(StreamObserver.class);

        // Ожидаемый результат
        ProfileDescriptorOuterClass.Empty expectedResponse = ProfileDescriptorOuterClass.Empty.newBuilder().build();

        // Вызов метода
        service.sendPrivateMsg(privateMsgBuilder.build(), responseObserver);

        // Проверка, что метод onNext был вызван с ожидаемым результатом
        Mockito.verify(responseObserver).onNext(expectedResponse);

        // Проверка, что метод onCompleted был вызван
        Mockito.verify(responseObserver).onCompleted();
    }

    //Приватное сообщение неизвестному получателю, отредактировать точнее
    @Test
    public void sendPrivateMsgTestUserNotFound() throws UserNotFoundException {
        GrpcProfileService service = new GrpcProfileService(mockRepository);
        // Создание mock объектов
        ProfileDescriptorOuterClass.PrivateMessageText.Builder privateMsgBuilder = ProfileDescriptorOuterClass.PrivateMessageText.newBuilder();
        ProfileDescriptorOuterClass.MessageText.Builder msgBuilder = ProfileDescriptorOuterClass.MessageText.newBuilder();
        StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.ConnectMessage> mockObserver =
                mock(StreamObserver.class);

        ProfileDescriptorOuterClass.UserInfo userInfoSender = ProfileDescriptorOuterClass.UserInfo.newBuilder().setName("Sender").build();
        service.connectUser(userInfoSender, mockObserver);

        // Наполнение объектов данными
        msgBuilder.setSender("Sender");
        msgBuilder.setText("Hello!");
        privateMsgBuilder.setMessageText(msgBuilder.build());
        privateMsgBuilder.setReceiver("ReceiverNotFound");

        // Создание mock объектов
        StreamObserver<sbrt.preppy.ProfileDescriptorOuterClass.Empty> responseObserver = Mockito.mock(StreamObserver.class);
        // Mock объекты
        when(userManager.findUserByName(anyString())).thenThrow(new UserNotFoundException(""));
        // Вызов метода
        service.sendPrivateMsg(privateMsgBuilder.build(), responseObserver);
        // Проверки
        verify(responseObserver).onCompleted();
    }
}