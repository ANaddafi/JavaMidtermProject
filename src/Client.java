import java.io.*;
import java.net.Socket;
import java.util.Scanner;

// TODO TELL PLAYER IT'S DAY/NIGHT

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

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        isSleep = true;
        isDead = false;
        isMute = false;

        // asking for port
        int port = 8181;

        // TODO UNCOMMENT
        /*
        String line = null;
        while(line == null || !isNumber(line)) {
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
                                System.err.println("\nYou're ASLEEP! You can't chat.");

                            } else if (GameServer.WAKEUP.equals(cmd)) {
                                isSleep = false;
                                System.err.println("\nYou're AWAKE! You can chat.");

                            } else if (GameServer.MSG.equals(cmd)) {
                                showMsg(line);

                            } else if (GameServer.ERR.equals(cmd)) {
                                if(isVoting)
                                    handleVoteResponse(line);
                                else
                                    System.err.println(line.split(" ", 2)[1]);

                            } else if (GameServer.VOTE.equals(cmd)){
                                handleVote(line);

                            } else if (GameServer.TIMEOUT.equals(cmd)) {
                                isVoting = false;
                                hasVoted = false;

                            } else if (GameServer.DEAD.equals(cmd)){
                                isDead = true;
                                System.err.println("You are DEAD!\nYou can still see other players chats,\nOr type 'EXIT' to exit the game.");

                            } else if (GameServer.MUTE.equals(cmd)){
                                isMute = true;
                                System.err.println("You are MUTE for today!");

                            } else if (GameServer.HISTORY.equals(cmd)){
                                handleHistory(line);

                            } else {
                                System.out.println("Unknown command <" + cmd + ">");
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
                System.out.println(line.split(" ", 2)[1]);
            else
                System.out.println(line);

            line = bufferedReader.readLine();

        } while (line == null || !line.equals(GameServer.HISTORY));

        System.out.println("---------Finished---------\n");


    }

    private static void handleVoteResponse(String line) {
        String body = line.split(" ", 2)[1];
        if("OK".equals(body)) {
            System.err.println("Your vote is received!");
            hasVoted = true;
        } else
            System.err.println(body);
    }

    // format: <VOTE> <body>::<options> <time>
    private static void handleVote(String line) {
        isVoting = true;
        hasVoted = false;
        String[] tokens = line.split("::");
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
            line = bufferedReader.readLine(); // wait for OK

        } while (line == null);


        // wait for 'START'
        System.out.println("All joined! Type '" + GameServer.START + "' to start game...");
        do{
            line = scanner.nextLine();

        } while (line != null && !line.equalsIgnoreCase(GameServer.START));

        sendMsg(line);

        System.out.println("Please wait for others to start...");


        do{
            line = bufferedReader.readLine(); // wait for STARTED

        } while (line == null);


        do{
            line = bufferedReader.readLine(); // wait for ROLE

        } while (line == null);


        System.out.println("Game is starting...");


        String[] tokens = line.split(" ");
        System.out.println("Your Group is : " + tokens[2]);
        System.out.println("Your Type  is : " + tokens[3]);
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
