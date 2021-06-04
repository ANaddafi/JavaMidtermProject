import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static Socket connectionSocket;
    private static InputStream inputStream;
    private static OutputStream outputStream;
    private static BufferedReader bufferedReader;

    public static void main(String[] args) {
        try {
            connectionSocket = new Socket("127.0.0.1", 8181);
            System.out.println("Connected to server.");

            inputStream = connectionSocket.getInputStream();
            outputStream = connectionSocket.getOutputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            System.out.println("Welcome to the game of MAFIA!");

            handleLogin();
            gameLoop();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void gameLoop() throws IOException {
        String line;
        while ((line = bufferedReader.readLine()) != null){
            String[] tokens = line.split(" ");
            if(tokens.length > 0){
                String cmd = tokens[0];

                if(cmd.equals(GameServer.SLEEP)){
                    System.out.println("You are now ASLEEP! You can't speak or listen!");
                } else if(cmd.equals(GameServer.WAKEUP)){
                    System.out.println("You are now AWAKE! You can speak or listen!");
                } else {
                    System.out.println("Unknown command <" + cmd + ">");
                }
            }
        }
    }

    private static void handleLogin() throws IOException {
        System.out.print("Enter your name: ");
        String userName = new Scanner(System.in).nextLine();

        // TODO CHECK IF NAME IS DUPLICATE

        outputStream.write((userName + "\n").getBytes());

        String line;
        do{
            line = bufferedReader.readLine();
        } while (line == null);

        String[] tokens = line.split(" ");
        System.out.println("Your Group is : " + tokens[2]);
        System.out.println("Your Type  is : " + tokens[3]);
    }
}
