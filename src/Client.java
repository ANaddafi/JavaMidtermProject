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

    private static boolean isVoting;
    private static boolean hasVoted;

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

                            if (GameServer.SLEEP.equals(cmd)) {
                                isSleep = true;
                                System.out.println("\nYou are now ASLEEP! You can't speak or listen!");

                            } else if (GameServer.WAKEUP.equals(cmd)) {
                                isSleep = false;
                                System.out.println("\nYou are now AWAKE! You can speak or listen!");

                            } else if (GameServer.MSG.equals(cmd)) {
                                showMsg(line);

                            } else if (GameServer.ERR.equals(cmd)) {
                                if(isVoting)
                                    handleVoteResponse(line);
                                System.err.println(line.split(" ", 2)[1]);

                            } else if (GameServer.VOTE.equals(cmd)){
                                handleVote(line);

                            } else if (GameServer.TIMEOUT.equals(cmd)) {
                                isVoting = false;
                            } else {
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
                try {
                    String line;
                    while ((line = scanner.nextLine()) != null) {
                        if(isVoting)
                            sendVote(line);
                        else
                            sendMsg(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        writerThread.start();
    }

    private static void handleVoteResponse(String line) {
        String body = line.split(" ", 2)[1];
        if("OK".equals(body)) {
            hasVoted = true;
            System.err.println("Your vote is received!");
        } else
            System.err.println(body);
    }

    // format: <VOTE> <body>::<options> <time>
    private static void handleVote(String line) {
        isVoting = true;
        String[] tokens = line.split("::");
        String[] options = tokens[1].split(" "); // last one is <time>

        String body = tokens[0].split(" ", 2)[1];
        int voteTime = Integer.parseInt(options[options.length-1]) / 1000;

        System.out.println("You are voting! Vote in " + voteTime + " seconds!");

        System.out.println(body);

        int cnt = options.length - 1; // one of them is <time>
        for(int i = 1; i <= cnt; i++)
            System.out.print("" + i + ")" + options[i-1] + " ");

        System.out.println();
    }


    private static void sendVote(String line) throws IOException {
        String toSend = GameServer.VOTE + " " + line + "\n";
        outputStream.write(toSend.getBytes());
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
