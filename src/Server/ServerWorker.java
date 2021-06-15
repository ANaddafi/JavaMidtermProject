package Server;

import Enums.Group;
import Enums.Type;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

class ServerWorker extends Thread{
    private final Socket connectionSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BufferedReader bufferedReader;
    private final GameServer server;

    private int theVote;
    private int voteSize;
    private boolean hasVoted;
    private boolean isVoting;

    private String userName; // surely just one word
    private Group group;
    private Type type;

    private boolean isDead;
    private boolean isOnline;
    private boolean isSleep;
    private boolean isMute;
    private boolean isReady;
    private boolean isStart;

    public ServerWorker(Socket connectionSocket, GameServer server){
        this.connectionSocket = connectionSocket;
        this.server = server;

        isOnline = true;
        isDead = false;
        isSleep = true;
        isMute = false;
        isReady = false;
        isStart = false;
        isVoting = false;
        hasVoted = false;
        theVote = 0;
    }

    public void giveRole(Group group, Type type){
        this.group = group;
        this.type = type;
    }

    public boolean isDead() {
        return isDead;
    }

    public boolean isOnline() {
        return isOnline;
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
            offline();
        }
    }

    private void offline() {
        System.err.println(userName + " LEFT");
        isDead = true;
        isOnline = false;

        server.tellOffline(userName);
    }

    private void handleClientInput() throws IOException {
        String line;
        while( (line = bufferedReader.readLine()) != null){
            String[] tokens = line.split(" ");

            if(line.equalsIgnoreCase(GameServer.EXIT)){
                offline();
                return;
            }

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

        if(isOnline)
            outputStream.write((GameServer.HISTORY + " " + chatHistory + GameServer.HISTORY + "\n").getBytes());
    }

    private boolean isNumber(String str) {
        boolean isNum = true;
        for(char c : str.toCharArray())
            isNum &= '0' <= c && c <= '9';

        return isNum;
    }

    public void sendErr(String error) throws IOException {
        if(!isOnline)
            return;

        String toSend = GameServer.ERR + " " + error + "\n";
        outputStream.write(toSend.getBytes());
    }

    public void sendRole() throws IOException {
        if(!isOnline)
            return;

        String body = this.group.name() + " " + this.type;
        String toSend = GameServer.MSG + " " + GameServer.SERVER_NAME + " " + body + "\n";
        outputStream.write(toSend.getBytes());
    }

    public String getUserName() {
        return userName;
    }

    public void wakeUp() throws IOException {
        if(isDead || !isOnline)
            return;

        isSleep = false;
        outputStream.write((GameServer.WAKEUP + "\n").getBytes());

        System.out.println(userName + " is now awake.");
    }

    public void goSleep() throws IOException {
        if(!isOnline)
            return;

        isSleep = true;
        outputStream.write((GameServer.SLEEP + "\n").getBytes());

        System.out.println(userName + " is now asleep.");
    }

    public void sendMsgToClient(String toSend) throws IOException {
        if(!isOnline)
            return;

        if(!toSend.endsWith("\n"))
            toSend += "\n";
        outputStream.write(toSend.getBytes());
    }

    public void sendMsgToAllAwake(String toSend) throws IOException {
        if(!isDead && isOnline)
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
        if(!isOnline)
            return;

        String optionBody = "";
        for(String workerUserName : options)
            optionBody += workerUserName + " ";

        String toSend = GameServer.VOTE + " " + voteBody + "::" + optionBody + voteTime + "\n";
        outputStream.write(toSend.getBytes());

        isVoting = true;
        hasVoted = false;
        theVote = 0;
        voteSize = options.size();
    }

    public void closeVote() throws IOException {
        if(!isOnline)
            return;

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
        isReady = false;
        isMute = false;
        goSleep();
    }

    public void kill() throws IOException {
        if(!isOnline)
            return;

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

    public void tellTime(String time) throws IOException {
        if(!isOnline)
            return;

        lineBreak(); // sends line break
        sendErr("Now it's " + time);
        lineBreak();
    }

    public void lineBreak() throws IOException {
        if(isOnline)
            sendErr(GameServer.BREAK);
    }

    public void closeGame() throws IOException {
        outputStream.write((GameServer.GAME_OVER+"\n").getBytes());
        isOnline = false;
    }
}
