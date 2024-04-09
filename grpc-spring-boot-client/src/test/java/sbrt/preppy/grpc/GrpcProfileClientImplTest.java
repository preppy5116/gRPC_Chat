package sbrt.preppy.grpc;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sbrt.preppy.ProfileDescriptorOuterClass;
import sbrt.preppy.ProfileServiceGrpc;
import sbrt.preppy.grpc.exceptions.DuplicateUsernameException;
import sbrt.preppy.grpc.exceptions.UserNotFoundException;

import java.io.IOException;


import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;


@RunWith(MockitoJUnitRunner.class)
class GrpcProfileClientImplTest {

    ProfileServiceGrpc.ProfileServiceBlockingStub blockingStub = mock(ProfileServiceGrpc.ProfileServiceBlockingStub.class);
    GrpcProfileClientImpl grpcProfileClient = new GrpcProfileClientImpl();

    @Test
    void connectUser() throws IOException {
        grpcProfileClient.setBlockingStub(blockingStub);

        when(blockingStub.connectUser(any(ProfileDescriptorOuterClass.UserInfo.class)))
                .thenReturn(ProfileDescriptorOuterClass.ConnectMessage.newBuilder().setIsConnected(true).build());

        assertTrue(grpcProfileClient.connectUser("testUser"));

        verify(blockingStub, times(1)).connectUser(any(ProfileDescriptorOuterClass.UserInfo.class));
    }

    @Test
    @Disabled
    void connectUserDuplicateName(){
        GrpcProfileClientImpl grpcProfileClient = new GrpcProfileClientImpl();
        grpcProfileClient.setBlockingStub(blockingStub);
        when(blockingStub.connectUser(any(ProfileDescriptorOuterClass.UserInfo.class)))
                .thenReturn(ProfileDescriptorOuterClass.ConnectMessage.newBuilder().setIsConnected(false).build());
        assertThrows(DuplicateUsernameException.class, () -> grpcProfileClient.connectUser("duplicateUsername"));
    }

    @Test
    void disconnectUser() throws IOException {
        grpcProfileClient.setBlockingStub(blockingStub);

        when(blockingStub.connectUser(any(ProfileDescriptorOuterClass.UserInfo.class)))
                .thenReturn(ProfileDescriptorOuterClass.ConnectMessage.newBuilder().setIsConnected(true).build());

        assertTrue(grpcProfileClient.connectUser("testUser"));
        verify(blockingStub, times(1)).connectUser(any(ProfileDescriptorOuterClass.UserInfo.class));


        when(blockingStub.disconnectUser(any(ProfileDescriptorOuterClass.UserInfo.class)))
                .thenReturn(ProfileDescriptorOuterClass.DisconnectMessage.newBuilder().setIsDisconnected(true).build());

        assertTrue(grpcProfileClient.disconnectUser(grpcProfileClient.channel));
    }

    @Test
    public void testSendBroadcastMsg0() throws UserNotFoundException{
        String text = "Test message";
        grpcProfileClient.setBlockingStub(blockingStub);
        // Выполняем метод
        grpcProfileClient.sendBroadcastMsg(text);
        verify(blockingStub, times(1)).sendBroadcastMsg(any(ProfileDescriptorOuterClass.MessageText.class));
        verifyNoMoreInteractions(blockingStub);
    }

    @Test
    public void testSendPrivateMsg() throws UserNotFoundException {
        String text = "Some private message @receiver";
        grpcProfileClient.setBlockingStub(blockingStub);
        grpcProfileClient.sendPrivateMsg(text);
        verify(blockingStub).sendPrivateMsg(any(ProfileDescriptorOuterClass.PrivateMessageText.class));
    }
}
