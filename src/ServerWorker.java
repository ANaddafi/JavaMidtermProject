import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

public class ServerWorker extends Thread{
    private Socket connectionSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BufferedReader bufferedReader;
    private GameServer server;

    private ArrayBlockingQueue<Integer> voteResults;
    private int voteSize;
    private boolean hasVoted;
    private boolean isVoting;

    private String userName; // surely just one word
    private Group group;
    private Type type;

    private boolean isDead;
    private boolean isSleep;
    private boolean isMute;
    private boolean isReady;

    public ServerWorker(Socket connectionSocket, GameServer server){
        this.connectionSocket = connectionSocket;
        this.server = server;
        isDead = false;
        isSleep = true;
        isMute = false;
        isReady = false;
        isVoting = false;
        hasVoted = false;
    }

    public void giveRole(Group group, Type type){
        this.group = group;
        this.type = type;
    }

    public boolean isDead() {
        return isDead;
    }

    public boolean isReady() {
        return isReady;
    }

    public boolean isSleep() {
        return isSleep;
    }

    public boolean isMute() {
        return isMute;
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
                        sendMsgToAllAwake(line);

                } else if (GameServer.VOTE.equals(cmd)){
                    if(!isVoting || hasVoted || isMute){
                        sendErr("You can't vote at the moment");
                        // TODO TELL WHY
                    }else if(tokens.length != 2 || !isNumber(tokens[1]))
                        sendErr("Enter a valid number");
                    else
                        checkVote(tokens[1]);

                } else {
                    System.err.println("Unknown command from " + userName + " <" + cmd + ">");
                }
            }
        }
    }

    private void checkVote(String vote) throws IOException {
        int voteIndex = Integer.parseInt(vote);
        if(voteIndex > 0 && voteIndex <= voteSize && !hasVoted) {
            hasVoted = true;
            voteResults.add(voteIndex);
            sendErr("OK");
        } else
            sendErr("Enter a valid number");
    }

    private boolean isNumber(String str) {
        boolean isNum = true;
        for(char c : str.toCharArray())
            isNum &= '0' <= c && c <= '9';

        return isNum;
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

    public void sendMsgToAllAwake(String toSend) throws IOException {
        /*for(ServerWorker worker : server.getWorkers())
            if(!worker.isDead() && !worker.isSleep()){
                worker.sendMsg(toSend);
            }*/

        server.sendMsgToAllAwake(toSend);
    }

    public void getVote(String voteBody, int voteTime, ArrayList<ServerWorker> options,
                        ArrayBlockingQueue<Integer> results) throws IOException {

        String optionBody = "";
        int cnt = 1;
        for(ServerWorker worker : options)
            optionBody += worker.getUserName() + " ";

        String toSend = GameServer.VOTE + " " + voteBody + "::" + optionBody + voteTime + "\n";
        outputStream.write(toSend.getBytes());

        isVoting = true;
        hasVoted = false;
        voteResults = results;
        voteSize = options.size();
    }

    public void closeVote() throws IOException {
        isVoting = false;
        hasVoted = false;
        voteResults = null;
        voteSize = 0;

        String toSend = GameServer.TIMEOUT + "\n";
        outputStream.write(toSend.getBytes());
    }
}
