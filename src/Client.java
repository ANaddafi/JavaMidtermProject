import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static Socket connectionSocket;
    private static InputStream inputStream;
    private static OutputStream outputStream;

    public static void main(String[] args) {
        try {
            connectionSocket = new Socket("127.0.0.1", 8181);
            System.out.println("Connected to server.");

            inputStream = connectionSocket.getInputStream();
            outputStream = connectionSocket.getOutputStream();

            System.out.println("Welcome to the game of MAFIA!");
            System.out.print("Enter your name: ");
            String userName = new Scanner(System.in).nextLine();

            // TODO CHECK IF NAME IS DUPLICATE

            outputStream.write((userName + "\n").getBytes());

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while((line = reader.readLine()) != null){
                System.out.println("Server: " + line);
            }

        } catch (IOException e) {
            System.err.println("Could not connect to server.");
            e.printStackTrace();
        }
    }
}
