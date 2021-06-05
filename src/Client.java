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
            handleGame();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleGame() throws IOException {

        // READER THREAD
        Thread readerThread = new Thread() {
            @Override
            public void run() {
                String line;

                try {
                    while ((line = bufferedReader.readLine()) != null) {

                        String[] tokens = line.split(" ");

                        if (tokens.length > 0) {
                            String cmd = tokens[0];

                            if (cmd.equals(GameServer.SLEEP)) {
                                isSleep = true;
                                System.out.println("\nYou are now ASLEEP! You can't speak or listen!");

                            } else if (cmd.equals(GameServer.WAKEUP)) {
                                isSleep = false;
                                System.out.println("\nYou are now AWAKE! You can speak or listen!");

                            } else if (cmd.equals(GameServer.MSG)) {
                                showMsg(line);

                            } else if (cmd.equals(GameServer.ERR)) {
                                System.err.println(line.split(" ", 2)[1]);

                            } else{
                                System.out.println("Unknown command <" + cmd + ">");
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        readerThread.start();

        // WRITER THREAD
        Thread writerThread = new Thread() {
            @Override
            public void run() {
                String line;
                while ((line = scanner.nextLine()) != null) {
                    try {
                        sendMsg(line);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        writerThread.start();
    }


    // format : MSG <sender> <body>
    private static void showMsg(String line) {
        String[] tokens = line.split(" ", 3);
        if(tokens.length < 3)
            return;

        String sender = tokens[1];
        String body = tokens[2];

        if(body != null && body.length() > 0)
            System.out.println(sender + ": " + body);
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
            outputStream.write((login + "\n").getBytes());

            String response = bufferedReader.readLine();

            if("NO".equals(response.split(" ")[1])) {
                System.out.print("Invalid or duplicate name!");

                if(login.contains(" "))
                    System.out.println(" Name must be ONE word!");

                System.out.print("Try again: ");

                login = null;
            }

        } while (login == null);

        userName = login;

        System.out.println("Please wait for other players to join...");

        String line;
        do{
            line = bufferedReader.readLine();
        } while (line == null);

        String[] tokens = line.split(" ");
        System.out.println("Your Group is : " + tokens[2]);
        System.out.println("Your Type  is : " + tokens[3]);
    }
}
