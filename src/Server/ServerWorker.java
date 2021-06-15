package Server;

import Enums.Group;
import Enums.Type;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Server worker class.
 * handles connection and I/O with client.
 */
class ServerWorker extends Thread{
    // some connection requirements
    private final Socket connectionSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BufferedReader bufferedReader;
    private final GameServer server;

    // to show exactly how is the voting process going
    private int theVote;        // vote
    private int voteSize;       // maximum index for vote
    private boolean hasVoted;   // whether the client has already voted
    private boolean isVoting;   // whether a vote is running at the moment

    // clients user name, surely just one word
    private String userName;

    // players roles
    private Group group;
    private Type type;

    // shows state of player
    private boolean isDead;
    private boolean isOnline;
    private boolean isSleep;
    private boolean isMute;
    private boolean isReady;
    private boolean isStart;

    /**
     * Server worker Constructor.
     *
     * @param connectionSocket the connection socket
     * @param server           the gameServer
     */
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


    /**
     * Give role.
     *
     * @param group the group
     * @param type  the type
     */
    public void giveRole(Group group, Type type){
        this.group = group;
        this.type = type;
    }


    /**
     * @return true if is dead
     */
    public boolean isDead() {
        return isDead;
    }


    /**
     * @return true if is online
     */
    public boolean isOnline() {
        return isOnline;
    }


    /**
     * @return true if is ready
     */
    public boolean isReady() {
        return isReady;
    }


    /**
     * @return true if is sleep
     */
    public boolean isSleep() {
        return isSleep;
    }


    /**
     * @return true if is mute
     */
    public boolean isMute() {
        return isMute;
    }


    /**
     * @return true if has started
     */
    public boolean isStart() {
        return isStart;
    }


    /**
     * @return true if has voted
     */
    public boolean hasVoted() {
        return hasVoted;
    }


    /**
     * @return the role group
     */
    public Group getGroup() {
        return group;
    }


    /**
     * @return the role type
     */
    public Type getType() {
        return type;
    }


    /**
     * Main method of ServerWorker
     * Starts connections and I/O
     */
    @Override
    public void run() {
        System.out.println("Server Worker Started.");
        try {
            inputStream = connectionSocket.getInputStream();
            outputStream = connectionSocket.getOutputStream();

            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            // getting username
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

            // starting client I/O process
            handleClientInput();

        } catch (IOException e) {
            // e.printStackTrace();
            offline();
        }
    }


    /**
     * If the connection is lost or client has exited,
     * some fields are set and some methods are called
     * so others will know that this player is offline.
     */
    private void offline() {
        System.err.println(userName + " LEFT");
        isDead = true;
        isOnline = false;

        server.tellOffline(userName);
    }


    /**
     * Input process.
     *
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    private void handleClientInput() throws IOException {
        String line;

        // input loop; worker is always listening for client input
        while( (line = bufferedReader.readLine()) != null){
            String[] tokens = line.split(" ");

            // handling some straight cases
            if(line.equalsIgnoreCase(GameServer.EXIT)){
                offline();
                return;
            }

            if(isDead){
                sendErr("You are DEAD!");

            } else if(tokens.length > 0){
                // handling commands
                String cmd = tokens[0];

                if (GameServer.MSG.equals(cmd)){    // messages
                    if(isStart && tokens.length == 3 && tokens[2].equalsIgnoreCase(GameServer.HISTORY)){
                        // show history
                        sendHistory();

                    } else if(isSleep && isStart) {
                        // check if player is asleep, or if has not started yet
                        sendErr("You are currently ASLEEP!");

                    } else if(tokens.length == 3 && tokens[2].equalsIgnoreCase(GameServer.READY)){
                        // if player is ready for voting
                        isReady = true;
                        sendErr("You're ready for voting!");

                    } else if(tokens.length == 3 && tokens[2].equalsIgnoreCase(GameServer.START)){
                        // 'start'
                        isStart = true;
                        sendErr("STARTED");

                    } else if(isMute)
                        // check if is mute
                        sendErr("You are currently MUTE!");

                     else {
                         // send message to chatroom
                        sendMsgToAllAwake(line);
                        server.processChat(line);
                    }

                } else if (GameServer.VOTE.equals(cmd)){    // voting
                    if(!isVoting){
                        sendErr("You can't vote at the moment");

                    }else if(tokens.length != 2 || !isNumber(tokens[1]))
                        // has sent an invalid vote
                        sendErr("Enter a valid number");
                    else
                        // has sent a valid vote
                        checkVote(tokens[1]);

                } else {
                    // unknown command
                    System.err.println("Unknown command from " + userName + " <" + cmd + ">");
                }
            }
        }
    }


    /**
     * Sends chat history to client
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
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


    /**
     * Checks whether the given string is a number
     * @param str string to check
     * @return true, if str is a number
     */
    private boolean isNumber(String str) {
        for(char c : str.toCharArray())
            if(!('0' <= c && c <= '9'))
                return false;

        return true;
    }


    /**
     * Send error.
     * Mainly used to send server messages to client.
     * Note that 'GOD' messages are different from
     * server messages.
     *
     * @param error the error / server message
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void sendErr(String error) throws IOException {
        if(!isOnline)
            return;

        String toSend = GameServer.ERR + " " + error + "\n";
        outputStream.write(toSend.getBytes());
    }


    /**
     * Send role to client.
     *
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void sendRole() throws IOException {
        if(!isOnline)
            return;

        String body = this.group.name() + " " + this.type;
        String toSend = GameServer.MSG + " " + GameServer.SERVER_NAME + " " + body + "\n";
        outputStream.write(toSend.getBytes());
    }


    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }


    /**
     * Wakes up player.
     * Also sends proper message to client.
     *
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void wakeUp() throws IOException {
        if(isDead || !isOnline)
            return;

        isSleep = false;
        outputStream.write((GameServer.WAKEUP + "\n").getBytes());

        System.out.println(userName + " is now awake.");
    }


    /**
     * Sleeps player.
     * Also sends proper message to client.
     *
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void goSleep() throws IOException {
        if(!isOnline)
            return;

        isSleep = true;
        outputStream.write((GameServer.SLEEP + "\n").getBytes());

        System.out.println(userName + " is now asleep.");
    }


    /**
     * Send message to client.
     *
     * @param toSend the message string
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void sendMsgToClient(String toSend) throws IOException {
        if(!isOnline)
            return;

        if(!toSend.endsWith("\n"))
            toSend += "\n";

        outputStream.write(toSend.getBytes());
    }


    /**
     * Send message to all AWAKE and ONLINE players.
     * using workerHandler methods.
     *
     * @param toSend message string
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void sendMsgToAllAwake(String toSend) throws IOException {
        if(!isDead && isOnline)
            server.getWorkerHandler().msgToAllAwake(toSend, this);
        else if(isOnline)
            sendErr("You can't chat because you are DEAD");
    }


    /**
     * Catch the vote.
     * If no vote is set by client, 0 is returned
     *
     * @return the vote
     */
    public int catchVote(){

        int voteToSend = theVote;
        theVote = 0;

        return voteToSend;
    }


    /**
     * Processes the vote client has sent.
     *
     * @param vote vote (string) sent by user
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    private void checkVote(String vote) throws IOException {
        int voteIndex = Integer.parseInt(vote);
        if(voteIndex > 0 && voteIndex <= voteSize) {
            hasVoted = true;
            theVote = voteIndex;

            sendErr("OK");
        } else
            sendErr("Enter a valid number");
    }


    /**
     * Processes a new voting.
     *
     * @param voteBody the vote body
     * @param voteTime the vote time
     * @param options  the options
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
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


    /**
     * Closes current voting.
     * Sends proper message to client.
     *
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void closeVote() throws IOException {
        if(!isOnline)
            return;

        isVoting = false;
        hasVoted = false;
        voteSize = 0;

        String toSend = GameServer.TIMEOUT + "\n";
        outputStream.write(toSend.getBytes());
    }


    /**
     * Night reset.
     * Sets some fields and put player to sleep.
     *
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void nightReset() throws IOException {
        if(isDead)
            return;

        hasVoted = false;
        isReady = false;
        isMute = false;
        goSleep();
    }


    /**
     * Kills player.
     * Sends proper messages to client
     *
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void kill() throws IOException {
        if(!isOnline)
            return;

        isDead = true;
        isSleep = false;
        outputStream.write((GameServer.DEAD + "\n").getBytes());
    }


    /**
     * @return the role as string
     */
    public String getRoleString() {
        return group.toString() + ":" + type.toString();
    }


    /**
     * Make player mute.
     * Sends proper messages to client.
     *
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void makeMute() throws IOException {
        if(!isOnline)
            return;

        isMute = true;
        sendMsgToClient(GameServer.MUTE);
    }


    /**
     * Tell time (DAY/NIGHT).
     *
     * @param time the time (DAY/NIGHT) string
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void tellTime(String time) throws IOException {
        if(!isOnline)
            return;

        lineBreak(); // sends line break
        sendErr("Now it's " + time);
        lineBreak();
    }


    /**
     * Sends a lineBreak.
     *
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void lineBreak() throws IOException {
        if(isOnline)
            sendErr(GameServer.BREAK);
    }


    /**
     * Close game for client, by sending GAME_OVER to client.
     * Also marks worker as offline.
     *
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void closeGame() throws IOException {
        outputStream.write((GameServer.GAME_OVER+"\n").getBytes());
        isOnline = false;
    }
}
