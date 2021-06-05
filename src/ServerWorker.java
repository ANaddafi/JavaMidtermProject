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
                // TODO CHECK DUPLICATE USERNAME
            } while (line == null);

            userName = line;
            System.out.println("User " + userName + " joined!");

        } catch (IOException e) {
            e.printStackTrace();
        }
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

        System.out.println("Getting input from " + userName + "...");

        Thread readerThread  = new Thread() {

            @Override
            public void run() {
                String line;
                try {
                    while ((line = bufferedReader.readLine()) != null) {
                        if (isSleep)
                            break;
                        if (isMute)
                            continue;

                        // line: MSG <sender> <body>
                        server.sendMsgToAllAwake(line + "\n");
                    }
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        };

        readerThread.start();
    }

    public void goSleep() throws IOException {
        isSleep = true;
        outputStream.write((GameServer.SLEEP + "\n").getBytes());
    }

    public void sendMsg(String toSend) throws IOException {
        outputStream.write(toSend.getBytes());
    }
}
