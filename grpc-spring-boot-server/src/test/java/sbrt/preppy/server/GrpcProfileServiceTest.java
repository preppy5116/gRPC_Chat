package sbrt.preppy.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "grpc.server.inProcessName=test",
        "grpc.server.port=9092",
        "grpc.client.petService.address=in-process:test"
})
@SpringJUnitConfig(classes = {GrpcSpringBootServerApplication.class})
class GrpcProfileServiceTest {

    @Test
    void chatServiceImpl() {
    }
}