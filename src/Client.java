import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static Socket connectionSocket;
    private static InputStream inputStream;
    private static OutputStream outputStream;
    private static BufferedReader bufferedReader;

    private static String userName;

    private static boolean isSleep;
    private static boolean isDead;
    private static boolean isMute;

    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        isSleep = true;
        isDead = false;
        isMute = false;

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
                    isSleep = true;
                    System.out.println("You are now ASLEEP! You can't speak or listen!");

                } else if(cmd.equals(GameServer.WAKEUP)){
                    isSleep = false;
                    System.out.println("You are now AWAKE! You can speak or listen!");

                    Thread readerThread = new Thread(){
                        @Override
                        public void run() {
                            String input;
                            while((input = scanner.nextLine()) != null){
                                if(isSleep) {
                                    // System.out.println("You're ASLEEP!");
                                    break;
                                }
                                if(isMute) {
                                    System.out.println("You're MUTE!");
                                    continue;
                                }

                                try {
                                    sendMsg(input);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                    };

                    readerThread.start();

                } else if(cmd.equals(GameServer.MSG)){
                    showMsg(line);

                } else {
                    System.out.println("Unknown command <" + cmd + ">");
                }
            }
        }
    }

    // format : MSG <sender> <body>
    private static void showMsg(String line) {
        String[] tokens = line.split(" ", 3);
        String sender = tokens[1];
        String body = tokens[2];

        System.out.println(sender + " says: " + body);
    }

    private static void sendMsg(String body) throws IOException {
        String toSend = GameServer.MSG + " " + userName + " " + body + "\n";
        outputStream.write(toSend.getBytes());
    }

    private static void handleLogin() throws IOException {
        System.out.print("Enter your name: ");
        String login;
        do{
            login = scanner.nextLine();
            if(login.contains(" "))
                System.out.print("Your user name must be ONE word!\nTry again: ");
        } while (login.contains(" "));

        // TODO CHECK IF NAME IS DUPLICATE

        outputStream.write((login + "\n").getBytes());
        userName = login;

        String line;
        do{
            line = bufferedReader.readLine();
        } while (line == null);

        String[] tokens = line.split(" ");
        System.out.println("Your Group is : " + tokens[2]);
        System.out.println("Your Type  is : " + tokens[3]);
    }
}
