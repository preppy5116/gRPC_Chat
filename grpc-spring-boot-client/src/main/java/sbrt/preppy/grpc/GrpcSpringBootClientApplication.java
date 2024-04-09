package sbrt.preppy.grpc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import sbrt.preppy.grpc.exceptions.UserNotFoundException;

@SpringBootApplication
public class GrpcSpringBootClientApplication {

    public static void main(String[] args) throws UserNotFoundException, InterruptedException {
        ApplicationContext context = SpringApplication.run(GrpcSpringBootClientApplication.class, args);
        GrpcProfileClient client = context.getBean(GrpcProfileClientImpl.class);
        client.start();
    }
}
