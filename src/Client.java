import java.io.IOException;
import java.net.Socket;

public class Client {
    private static Socket connectionSocket;

    public static void main(String[] args) {
        try {
            connectionSocket = new Socket("127.0.0.1", 8181);
            System.out.println("Connected to server.");
        } catch (IOException e) {
            System.err.println("Could not connect to server.");
            e.printStackTrace();
        }
    }
}
