package sbrt.preppy.grpc.exceptions;

public class UserNotFoundException extends Exception {
    public UserNotFoundException(String msg) {
        super(msg);
        System.out.println(" Не удается найти имя :" + msg);
    }
}