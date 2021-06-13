import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ServerWorker extends Thread{
    private Socket connectionSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BufferedReader bufferedReader;
    private GameServer server;

    private int theVote;
    private int voteSize;
    private boolean hasVoted;
    private boolean isVoting;
    private boolean recVote;

    private String userName; // surely just one word
    private Group group;
    private Type type;

    private boolean isDead;
    private boolean isSleep;
    private boolean isMute; // TODO TELL CLIENT THAT HES MUTE NOW!
    private boolean isReady;
    private boolean isStart;

    public ServerWorker(Socket connectionSocket, GameServer server){
        this.connectionSocket = connectionSocket;
        this.server = server;
        isDead = false;
        isSleep = true;
        isMute = false;
        isReady = false;
        isStart = false;
        isVoting = false;
        hasVoted = false;
        recVote = false;
        theVote = 0;
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

    public boolean isStart() {
        return isStart;
    }

    public boolean hasVoted() {
        return hasVoted;
    }

    public Group getGroup() {
        return group;
    }

    public Type getType() {
        return type;
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
            // e.printStackTrace();
            System.err.println(userName + " LEFT");
            isDead = true;
        }
    }

    private void handleClientInput() throws IOException {
        String line;
        while( (line = bufferedReader.readLine()) != null){
            String[] tokens = line.split(" ");

            if(isDead){
                sendErr("You are DEAD!");

            } else if(tokens.length > 0){
                String cmd = tokens[0];

                if (GameServer.MSG.equals(cmd)){
                    if(isStart && tokens.length == 3 && tokens[2].equalsIgnoreCase(GameServer.HISTORY)){
                        sendHistory();

                    } else if(isSleep && isStart) {
                        sendErr("You are currently ASLEEP!");

                    } else if(tokens.length == 3 && tokens[2].equalsIgnoreCase(GameServer.READY)){
                        isReady = true;
                        sendErr("You're ready for voting!");

                    } else if(tokens.length == 3 && tokens[2].equalsIgnoreCase(GameServer.START)){
                        isStart = true;
                        sendErr("STARTED");

                    } else if(isMute)
                        sendErr("You are currently MUTE!");
                     else {
                        sendMsgToAllAwake(line);
                        server.processChat(line);
                    }

                } else if (GameServer.VOTE.equals(cmd)){
                    if(!isVoting /*|| hasVoted*/){
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

    private void sendHistory() throws IOException {
        String chatHistory = server.getHistory();
        if(chatHistory.length() == 0){
            chatHistory = "Nothing to show!\n";
        } else {
            chatHistory = "# Chats until now:\n" + chatHistory;
        }

        outputStream.write((GameServer.HISTORY + " " + chatHistory + GameServer.HISTORY + "\n").getBytes());
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
        if(isDead)
            return;

        isSleep = false;
        outputStream.write((GameServer.WAKEUP + "\n").getBytes());

        System.out.println(userName + " is now awake.");
    }

    public void goSleep() throws IOException {
        isSleep = true;
        outputStream.write((GameServer.SLEEP + "\n").getBytes());

        System.out.println(userName + " is now asleep.");
    }

    public void sendMsgToClient(String toSend) throws IOException {
        if(!toSend.endsWith("\n"))
            toSend += "\n";
        outputStream.write(toSend.getBytes());
    }

    public void sendMsgToAllAwake(String toSend) throws IOException {
        if(!isDead)
            server.getWorkerHandler().msgToAllAwake(toSend, this);
        else
            sendErr("You can't chat because you are DEAD");
    }

    public int catchVote(){

        int voteToSend = theVote;
        theVote = 0;

        return voteToSend;
    }

    private void checkVote(String vote) throws IOException {
        int voteIndex = Integer.parseInt(vote);
        if(voteIndex > 0 && voteIndex <= voteSize /*&& !hasVoted*/) {
            hasVoted = true;
            theVote = voteIndex;

            sendErr("OK");
        } else
            sendErr("Enter a valid number");
    }

    public void getVote(String voteBody, int voteTime, ArrayList<String> options) throws IOException {

        String optionBody = "";
        int cnt = 1;
        for(String workerUserName : options)
            optionBody += workerUserName + " ";

        String toSend = GameServer.VOTE + " " + voteBody + "::" + optionBody + voteTime + "\n";
        outputStream.write(toSend.getBytes());

        isVoting = true;
        hasVoted = false;
        recVote = false;
        theVote = 0;
        voteSize = options.size();
    }

    public void closeVote() throws IOException {
        isVoting = false;
        hasVoted = false;
        voteSize = 0;

        String toSend = GameServer.TIMEOUT + "\n";
        outputStream.write(toSend.getBytes());
    }

    public void nightReset() throws IOException {
        if(isDead)
            return;

        hasVoted = false;
        recVote = false;
        isReady = false;
        isMute = false;
        goSleep();
    }

    public void kill() throws IOException {
        isDead = true;
        isSleep = false;
        outputStream.write((GameServer.DEAD + "\n").getBytes());
    }

    public String getRoleString() {
        return group.toString() + ":" + type.toString();
    }

    public void makeMute() throws IOException {
        isMute = true;
        sendMsgToClient(GameServer.MUTE);
    }
}
