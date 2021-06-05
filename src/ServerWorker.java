import java.io.*;
import java.net.Socket;

public class ServerWorker extends Thread{
    private Socket connectionSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BufferedReader bufferedReader;
    private GameServer server;

    private String userName; // surely just one word
    private Group group;
    private Type type;

    private boolean isDead;
    private boolean isSleep;
    private boolean isMute;

    public ServerWorker(Socket connectionSocket, GameServer server){
        this.connectionSocket = connectionSocket;
        this.server = server;
        isDead = false;
        isSleep = true;
        isMute = false;
    }

    public void giveRole(Group group, Type type){
        this.group = group;
        this.type = type;
    }

    public boolean isDead() {
        return isDead;
    }

    @Override
    public void run() {
        System.out.println("Server Worker Started.");
        try {
            inputStream = connectionSocket.getInputStream();
            outputStream = connectionSocket.getOutputStream();

            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            do {
                line = bufferedReader.readLine();

                if(line.contains(" ") || server.hasUserName(line)) {
                    sendErr("NO");
                    line = null;
                }
            } while (line == null);

            sendErr("OK");

            userName = line;
            System.out.println("User " + userName + " joined!");

            handleClientInput();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClientInput() throws IOException {
        String line;
        while( (line = bufferedReader.readLine()) != null){
            String[] tokens = line.split(" ");

            if(tokens.length > 0){
                String cmd = tokens[0];

                if (GameServer.MSG.equals(cmd)){
                    if(isSleep)
                        sendErr("You are currently ASLEEP!");
                    else if(isMute)
                        sendErr("You are currently MUTE!");
                    else
                        server.sendMsgToAllAwake(line);

                } else if (cmd.equals(GameServer.VOTE)){
                    System.out.println("<VOTE>");

                } else {
                    System.err.println("Unknown command from " + userName + " <" + cmd + ">");
                }
            }
        }
    }

    private void sendErr(String error) throws IOException {
        String toSend = GameServer.ERR + " " + error + "\n";
        outputStream.write(toSend.getBytes());
    }

    public void sendRole() throws IOException {
        String body = this.group.name() + " " + this.type;
        String toSend = GameServer.MSG + " " + GameServer.SERVER_NAME + " " + body + "\n";
        outputStream.write(toSend.getBytes());
    }

    public String getUserName() {
        return userName;
    }

    public void wakeUp() throws IOException {
        isSleep = false;
        outputStream.write((GameServer.WAKEUP + "\n").getBytes());

        System.out.println(userName + " is now awake.");
    }

    public void goSleep() throws IOException {
        isSleep = true;
        outputStream.write((GameServer.SLEEP + "\n").getBytes());

        System.out.println(userName + " is now asleep.");
    }

    public void sendMsg(String toSend) throws IOException {
        if(!toSend.endsWith("\n"))
            toSend += "\n";
        outputStream.write(toSend.getBytes());
    }
}
