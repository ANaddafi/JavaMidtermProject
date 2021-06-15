package Client;

import Server.GameServer;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static Socket connectionSocket;
    private static InputStream inputStream;
    private static OutputStream outputStream;
    private static BufferedReader bufferedReader;

    private static String userName;
    private static boolean isVoting;

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {

        // asking for port
        int port = 8181;

        // TODO UNCOMMENT
        /*
        String line = null;
        while(!isNumber(line)) {
            System.out.print("Enter your game port: ");
            line = scanner.nextLine();
        }

        port = Integer.parseInt(line);
        */

        try {
            connectionSocket = new Socket("127.0.0.1", port);
            System.out.println("Connected to server.");

            inputStream = connectionSocket.getInputStream();
            outputStream = connectionSocket.getOutputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            System.out.println("Welcome to the game of MAFIA!");

            handleLogin();
            handleGame(); // after starting IO threads, the function returns, and try block finishes.

        } catch (IOException e) {
            // e.printStackTrace();
            System.err.println("Something went wrong while connecting to the server.");
        }
    }

    private static void handleGame() {

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

                            if(cmd.equals(GameServer.GAME_OVER))
                                exitGame();

                            switch (cmd) {
                                case GameServer.SLEEP:
                                    System.err.println("# You're ASLEEP! You can't chat.");

                                    break;
                                case GameServer.WAKEUP:
                                    System.err.println("# You're AWAKE! You can chat.");

                                    break;
                                case GameServer.MSG:
                                    showMsg(line);

                                    break;
                                case GameServer.ERR:
                                    if (isVoting)
                                        handleVoteResponse(line);
                                    else if (line.split(" ", 2)[1].equals(GameServer.BREAK))
                                        System.err.println();
                                    else
                                        System.err.println("> " + line.split(" ", 2)[1]);

                                    break;
                                case GameServer.VOTE:
                                    handleVote(line);

                                    break;
                                case GameServer.TIMEOUT:
                                    isVoting = false;
                                    System.out.println();

                                    break;
                                case GameServer.DEAD:
                                    System.err.println("\n>> You are DEAD!\nYou can still see other players chats," +
                                                                "\nOr type 'EXIT' to exit the game.");

                                    break;
                                case GameServer.MUTE:
                                    System.err.println("\n>> You are MUTE for today!");

                                    break;
                                case GameServer.HISTORY:
                                    handleHistory(line);

                                    break;
                                default:
                                    System.out.println("!Unknown command <" + cmd + ">");
                                    break;
                            }
                        }
                    }
                } catch (IOException e) {
                    // e.printStackTrace();
                    exitGame("Couldn't read message from server.");
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
                        if(line.equalsIgnoreCase(GameServer.EXIT))
                            handleExit();
                        else if(isVoting)
                            sendVote(line);
                        else
                            sendMsg(line);

                    }
                } catch (IOException e) {
                    // e.printStackTrace();
                    exitGame("Couldn't send your message.");
                }
            }
        };

        writerThread.start();
    }

    private static void exitGame() {
        System.err.println("\n>>> HAVE A GOOD DAY! <<<");
        System.exit(0);
    }

    private static void exitGame(String error) {
        System.err.println("\n!" + error);
        System.exit(0);
    }

    private static void handleExit() throws IOException {
        System.out.println("Are you sure you want to exit the game? (Y/N)");

        String line;
        do{
            System.out.print("> ");
            line = scanner.nextLine();

        } while(line == null || !line.equalsIgnoreCase("Y") && !line.equalsIgnoreCase("N"));

        if(line.equalsIgnoreCase("Y")){
            outputStream.write(GameServer.EXIT.getBytes());
            exitGame();
        }
    }

    private static void handleHistory(String line) throws IOException {
        System.out.println("\n-------Chat History-------");

        String[] tokens = line.split(" ", 2);
        if(tokens.length == 2)
            line = tokens[1];

        do {
            // check if GOD message
            if(line != null && line.contains(" ") && line.split(" ")[0].equals(GameServer.SERVER_NAME + ":"))
                System.out.println("\n" + line.split(" ", 2)[1]);
            else
                System.out.println(line);

            line = bufferedReader.readLine();

        } while (line == null || !line.equals(GameServer.HISTORY));

        System.out.println("---------Finished---------\n");


    }

    private static void handleVoteResponse(String line) {
        String body = line.split(" ", 2)[1];
        if("OK".equals(body)) {
            System.err.println("> Your vote is received!");
        } else {
            System.err.println("> " + body);
        }
    }

    // format: <VOTE> <body>::<options> <time>
    private static void handleVote(String line) {
        isVoting = true;
        String[] tokens = line.split("::", 2);
        String[] options = tokens[1].split(" "); // last one is <time>

        String body = tokens[0].split(" ", 2)[1];
        int voteTime = Integer.parseInt(options[options.length-1]) / 1000;

        System.out.println("\n- You are voting! Vote in " + voteTime + " seconds! -");

        System.out.println(body);

        int cnt = options.length - 1; // one of them is <time>
        for(int i = 1; i <= cnt; i++)
            System.out.print("" + i + ")" + options[i-1] + "\t");

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

        if(body != null && body.length() > 0) {
            System.out.println(sender + ": " + body);
        }
    }

    private static void sendMsg(String body) throws IOException {
        String toSend = GameServer.MSG + " " + userName + " " + body + "\n";
        outputStream.write(toSend.getBytes());
    }

    private static void handleLogin() throws IOException {
        System.out.print("\nEnter your name: ");
        String login;
        do{
            login = scanner.nextLine();
            outputStream.write((login + "\n").getBytes());

            String response = bufferedReader.readLine();

            if("NO".equals(response.split(" ")[1])) {
                System.out.print("Invalid or duplicate name!");

                if(login.contains(" "))
                    System.out.println(" Name must be ONE word!");

                System.out.print(" Try again: ");

                login = null;
            }

        } while (login == null);

        userName = login;


        System.out.println("-----------\n> Please wait for other players to join...");

        String line;
        do{
            line = bufferedReader.readLine(); // wait for OK

        } while (line == null);


        // wait for 'START'
        System.out.println("-----------\n> All joined! Type '" + GameServer.START + "' to start game...");
        do{
            System.out.print("> ");
            line = scanner.nextLine();

        } while (line != null && !line.equalsIgnoreCase(GameServer.START));

        sendMsg(line);

        System.out.println("-----------\n> Please wait for others to start...\n");


        do{
            line = bufferedReader.readLine(); // wait for STARTED

        } while (line == null);


        do{
            line = bufferedReader.readLine(); // wait for ROLE

        } while (line == null);


        System.out.println("> Game is starting...\n");


        String[] tokens = line.split(" ");
        System.out.println(">> Your Group is : " + tokens[2]);
        System.out.println(">> Your Type  is : " + tokens[3]);

        System.out.println("\n---------------------------------\n");
    }

    private static boolean isNumber(String str) {
        if(str == null || str.isEmpty())
            return false;

        for(char c : str.toCharArray())
            if('0' > c || c > '9')
                return false;

        return true;
    }
}
