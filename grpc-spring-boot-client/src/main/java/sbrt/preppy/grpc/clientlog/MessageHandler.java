package sbrt.preppy.grpc.clientlog;

import io.grpc.ManagedChannel;

/**
 * @author preppy
 * A class containing the main system messages for the client.
 * So using logging in the client module is unnecessary. The creation of messages is placed in this class
 */
public class MessageHandler implements MessageCreator{

    /**
     * Output of any message
     * @param message text of message
     */
    @Override
    public void printMessage(String message) {
        System.out.println(message);
    }

    /**
     * Output of a message about incorrect character input for the client's name
     * @see sbrt.preppy.grpc.GrpcProfileClientImpl#connectUser(String)
     */
    @Override
    public void printWrongSymbol() {
        System.out.println(" !Неверный символ: нельзя использовать @");
    }

    /**
     * The output of the message is a hint.
     * It is shown when trying to send an empty message to the chat
     */
    @Override
    public void printWrongMessageBody() {
        System.out.println("-----Введите сообщение для отправки всем----");
        System.out.println("-----Для приватного сообщения в конце текста укажите получателя " +
                "Пример: text @username -----");
        System.out.println("-----Для выведения списка on-line пользователей введите команду #online----");
    }

    /**
     * Displaying a message about the successful disconnection of the client to the server
     * @see sbrt.preppy.grpc.GrpcProfileClientImpl#disconnectUser(ManagedChannel)
     */
    @Override
    public void printSuccessFullyDisconnect() {
        System.out.println("Successfully disconnected from server.");
    }

    /**
     * Displaying a message about a failed client disconnection to the server
     * @see sbrt.preppy.grpc.GrpcProfileClientImpl#disconnectUser(ManagedChannel)
     */
    @Override
    public void printFailedDisconnect() {
        System.out.println("Failed to disconnect from server");
    }

    /**
     * Displaying a message about an unsuccessful attempt to send a message
     */
    @Override
    public void printFailedMessage() {
        System.out.println("Could not connect with server. Try again.");
    }

}
