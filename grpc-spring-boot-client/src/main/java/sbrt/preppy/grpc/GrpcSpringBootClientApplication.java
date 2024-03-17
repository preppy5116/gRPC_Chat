package sbrt.preppy.grpc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import sbrt.preppy.grpc.exceptions.UserNotFoundException;

// java -jar grpc-spring-boot-client-0.0.1-SNAPSHOT.jar
@SpringBootApplication
public class GrpcSpringBootClientApplication {

    public static void main(String[] args) throws UserNotFoundException, InterruptedException {
        ApplicationContext context = SpringApplication.run(GrpcSpringBootClientApplication.class, args);
        GrpcProfileClient client = context.getBean(GrpcProfileClientImpl.class);
        client.start();
    }
}
