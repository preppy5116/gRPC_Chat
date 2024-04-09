package sbrt.preppy.grpc.clientlog;

/**
 * @author preppy
 * A interface containing the main system messages for the client.
 */
public interface MessageCreator {
     void printMessage(String message);
     void printWrongSymbol();
     void printWrongMessageBody();
     void printSuccessFullyDisconnect();
     void printFailedDisconnect();
     void printFailedMessage();
}
