package Server;

import Enums.Group;
import Enums.Type;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Game server class.
 */
public class GameServer extends Thread{
    // number of players
    public static final int PLAYER_COUNT = 10;
    // commands
    public static final String SERVER_NAME = "<GOD>";
    public static final String SLEEP = "SLEEP";
    public static final String WAKEUP = "WAKEUP";
    public static final String VOTE = "VOTE";
    public static final String MSG = "MSG";
    public static final String ERR = "ERR";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String READY = "READY";
    public static final String DEAD = "DEAD";
    public static final String MUTE = "MUTE";
    public static final String START = "START";
    public static final String HISTORY = "HISTORY";
    public static final String EXIT = "EXIT";
    public static final String GAME_OVER = "GAME_OVER";
    public static final String BREAK = "LINEBREAK";


    // times, in milli second
    public static final int TIME_TICK = 1000;
    public static final int DAY_TIME = 5 * 60 * 1000;
    public static final int DAY_VOTE_TIME = 30 * 1000;
    public static final int NIGHT_SPLIT_TIME = 2 * 1000;
    public static final int MAYOR_VOTE_TIME = 20 * 1000;
    public static final int MAFIA_TALK_TIME = 30 * 1000;
    public static final int MAFIA_KILL_TIME = 20 * 1000;
    public static final int MAFIA_HEAL_TIME = 20 * 1000;
    public static final int DR_HEAL_TIME = 20 * 1000;
    public static final int INSPECT_TIME = 20 * 1000;
    public static final int SNIPE_TIME = 20 * 1000;
    public static final int PSYCHO_TIME = 20 * 1000;
    public static final int STRONG_TIME = 20 * 1000;

    // workers for each role
    private ServerWorker godFather;
    private ServerWorker drLector;

    private ServerWorker mayor;
    private ServerWorker doctor;
    private ServerWorker inspector;
    private ServerWorker sniper;
    private ServerWorker psycho;
    private ServerWorker strong;

    // chat history
    private final StringBuilder chatHistory;

    // number of times drLector healed himself
    public int drLectorSelfHeal;
    // number of times doctor healed himself
    public int doctorSelfHeal;
    // number of times strong got query
    public int strongQuery;
    // number of times strong survived mafia shot
    public int strongSurvived;

    // connection port
    private final int port;

    // worker handler for the game
    private final WorkerHandler workers;
    // voter for handling votes
    private final Voter voter;
    // stores what happened in the night
    private final ArrayList<String> nightNews;

    private boolean firstNight;
    private boolean firstDay;
    private boolean isDay;

    /**
     * Game server constructor.
     * Initializes some fields.
     *
     * @param port the port
     */
    public GameServer(int port){
        this.port = port;
        workers = new WorkerHandler();
        voter = new Voter(this);
        nightNews = new ArrayList<>();

        drLectorSelfHeal = 0;
        doctorSelfHeal = 0;
        strongQuery = 0;
        strongSurvived = 0;

        chatHistory = new StringBuilder();
    }


    /**
     * Check whether the given userName is already logged in.
     *
     * @param userName the userName
     * @return true, if the userName is duplicate
     */
    public boolean hasUserName(String userName) {
        if(userName == null)
            return true;

        if(SERVER_NAME.equalsIgnoreCase(userName))
            return true;

        for(ServerWorker worker : workers.getWorkers())
            if(worker.getUserName() != null && worker.getUserName().equals(userName))
                    return true;

        return false;
    }


    /**
     * Main method of GameServer
     */
    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server Started. (port: " + port + ")\nWaiting for clients to join...");

            // waiting for clients to connect
            for(int i = 0; i < PLAYER_COUNT; i++){
                Socket clientSocket = serverSocket.accept();
                ServerWorker worker = new ServerWorker(clientSocket, this);
                workers.addWorker(worker);
                worker.start();
            }

            if(initGame())
                startGame();
            else
                System.err.println("Could not start game!");

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Initializes some fields,
     * also gives roles to clients.
     *
     * @return true, if noting goes wrong
     */
    private boolean initGame() {
        firstNight = true;
        firstDay = true;
        isDay = false;

        if(giveRoles()){
            initRoles();
            return true;
        }

        return false;
    }


    /**
     * Distributes the game roles between players, randomly.
     *
     * @return true, if nothing goes wrong
     */
    private boolean giveRoles() {
        if(workers.count() != PLAYER_COUNT)
            return false;

        ArrayList<Integer> index = new ArrayList<>();
        for(int i = 0; i < PLAYER_COUNT; i++)
            index.add(i);
        Collections.shuffle(index); // randomizing

        // giving mafia roles
        workers.getWorkers().get(index.get(0)).giveRole(Group.Mafia, Type.GodFather);
        workers.getWorkers().get(index.get(1)).giveRole(Group.Mafia, Type.DrLector);
        workers.getWorkers().get(index.get(2)).giveRole(Group.Mafia, Type.OrdMafia);

        // giving city roles
         workers.getWorkers().get(index.get(3)).giveRole(Group.City, Type.Mayor);
         workers.getWorkers().get(index.get(4)).giveRole(Group.City, Type.Psycho);
         workers.getWorkers().get(index.get(5)).giveRole(Group.City, Type.Doctor);
         workers.getWorkers().get(index.get(6)).giveRole(Group.City, Type.Inspector);
         workers.getWorkers().get(index.get(7)).giveRole(Group.City, Type.Sniper);
         workers.getWorkers().get(index.get(8)).giveRole(Group.City, Type.Strong);
         workers.getWorkers().get(index.get(9)).giveRole(Group.City, Type.OrdCity);

        return true;
    }


    /**
     * Initializes "role fields", after roles are given
     */
    private void initRoles(){
        godFather = workers.findWorker(Group.Mafia, Type.GodFather);
        drLector = workers.findWorker(Group.Mafia, Type.DrLector);

        mayor = workers.findWorker(Group.City, Type.Mayor);
        doctor = workers.findWorker(Group.City, Type.Doctor);
        inspector = workers.findWorker(Group.City, Type.Inspector);
        sniper = workers.findWorker(Group.City, Type.Sniper);
        psycho = workers.findWorker(Group.City, Type.Psycho);
        strong = workers.findWorker(Group.City, Type.Strong);
    }


    /**
     * Starts the game, after some preparations.
     * first, performs "Intro night", then the day-night cycle begins.
     * Also handles if game is finished.
     *
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     * @throws InterruptedException If there is any unhandled exceptions while sleeping
     */
    private void startGame() throws IOException, InterruptedException {
        // preperations
        waitForLogin();
        System.err.println("ALL LOGGED IN");

        workers.msgToAll(serverMsgFromString("OK"));

        waitForStart();
        System.err.println("ALL START");

        // Intro (Night #1)
        System.out.println("INTRO BEGINS");
        for(ServerWorker worker : workers.getWorkers())
            worker.sendRole();

        // tell mayor about doctor
        if(mayor != null && doctor != null)
            mayor.sendMsgToClient(
                    serverMsgFromString("-> City Doctor is " + doctor.getUserName())
            );

        // tell mafias about each other
        for(ServerWorker mafia : workers.getMafias())
            for(ServerWorker otherMafia : workers.getMafias())
                if(mafia != otherMafia)
                    otherMafia.sendMsgToClient(
                            serverMsgFromString(mafia.getUserName() + " is " + mafia.getType())
                    );


        // The day cycle begins
        System.out.println("DAY CYCLE BEGINS");

        while(!gameIsFinished()){
            handleDay();

            processChat(serverMsgFromString("--Day is finished here--"));

            Thread.sleep(3000);
            if(gameIsFinished())
                break;

            handleNight();

        }

        // game is finished
        System.err.println("\nGAME IS FINISHED!\n");

        if(workers.mafiaCount() == 0){ // CITY WINS!
            System.err.println("CITY WINS!");
            workers.finishGame(Group.City);

        } else if(workers.cityCount() <= workers.mafiaCount()){ // MAFIA WINS!
            System.err.println("MAFIA WINS!");
            workers.finishGame(Group.Mafia);

        } else {
            System.err.println("UNKNOWN REASON OF GAME FINISH!");
        }

    }


    /**
     * Get query, in case String has requested.
     * @return Query, showing how many citizens/mafias are dead.
     */
    private String getQuery() {
        int cityDead = 0, mafiaDead = 0;

        for(ServerWorker worker : workers.getWorkers())
            if(worker.isDead()){
                if(worker.getGroup() == Group.City)
                    cityDead ++;
                else
                    mafiaDead ++;
            }

        return "We have lost " + cityDead + " citizens and " + mafiaDead + " mafias so far.";
    }


    /**
     * Handles everything that happens in the day,
     * wakes up all, and gets votes
     *
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     * @throws InterruptedException If there is any unhandled exceptions while sleeping
     */
    private void handleDay() throws IOException, InterruptedException {
        // DAY
        System.err.println("DAY");
        isDay = true;
        workers.tellTime("day");
        Thread.sleep(500);

        // preparations
        workers.wakeUpAll();
        Thread.sleep(500);

        if(!firstNight)
            sendNightNews();

        if(!firstDay)
            workers.tellWhoIsAlive();

        firstDay = false;

        // discussion
        for(int i = 0; i < DAY_TIME/TIME_TICK; i++){
            Thread.sleep(TIME_TICK);
            if(workers.allReady())
                break;
        }

        // voting
        System.out.println("DAY VOTE");

        ServerWorker dayVoteWinner = voter.dayVote();

        // Mayor vote
        boolean doTheKill = dayVoteWinner != null;

        if(dayVoteWinner != null && mayor != null && !mayor.isDead()) {
            System.out.println("MAYOR VOTE");

            workers.msgToAllAwake(serverMsgFromString("Waiting for Mayors decision..."), mayor);

            doTheKill = voter.mayorVote(dayVoteWinner.getUserName());
        }

        if(doTheKill){
            workers.msgToAllAwake(serverMsgFromString("Today " + dayVoteWinner.getUserName() + " is going to die!"));

            // DO THE KILL!
            dayVoteWinner.kill();

        } else {
            workers.msgToAllAwake(serverMsgFromString("No one is going to die today!"));
        }

        System.out.println("DAY FINISHED");
    }


    /**
     * Handles everything that happens in night
     * sleeps all, and performs votes, one by one,
     * at the end performs the kills and heals.
     * The log is saved in 'nightNews'
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     * @throws InterruptedException If there is any unhandled exceptions while sleeping
     */
    private void handleNight() throws IOException, InterruptedException {
        // NIGHT
        System.err.println("NIGHT");
        isDay = false;
        workers.tellTime("night");
        Thread.sleep(500);

        // preparations
        workers.prepareNight();
        nightNews.clear();
        firstNight = false;
        ServerWorker mafiaShoot;
        ServerWorker mafiaHeal = null;
        ServerWorker doctorHeal = null;
        ServerWorker sniperShoot = null;
        ServerWorker psychoMute = null;
        boolean strongGetQuery = false;

        // wakeup Mafias
        workers.wakeUpList(workers.getMafias());

        // tell mafias about each other
        for(ServerWorker mafia : workers.getMafias()) if(mafia.isOnline() && !mafia.isDead())
            for(ServerWorker otherMafia : workers.getMafias()) if(otherMafia.isOnline() && !otherMafia.isDead())
                if(mafia != otherMafia)
                    otherMafia.sendMsgToClient(
                            serverMsgFromString(mafia.getUserName() + " is " + mafia.getType())
                    );

        // discussing
        for(int i = 0; i < MAFIA_TALK_TIME/TIME_TICK && !workers.allReady(); i++)
            Thread.sleep(TIME_TICK);

        // voting
        mafiaShoot = voter.mafiaVote();

        String tellMafia = (mafiaShoot == null ? "No one" : mafiaShoot.getUserName()) + " is shot by mafia tonight.";
        for(ServerWorker mafia : workers.getMafias())
            mafia.sendMsgToClient(serverMsgFromString(tellMafia));

        workers.sleepList(workers.getMafias());

        // SERVER LOG
        if(mafiaShoot == null)
            System.err.println("NO MAFIA KILL TONIGHT");
        else
            System.err.println("MAFIA KILL: " + mafiaShoot.getUserName());
        /////////////

        Thread.sleep(NIGHT_SPLIT_TIME);

        // wakeup drLector
        if(drLector != null && !drLector.isDead()) {
            workers.wakeUpWorker(drLector);

            // voting
            mafiaHeal = voter.lectorVote(drLectorSelfHeal < 1);
            if(mafiaHeal == drLector)
                drLectorSelfHeal++;


            tellMafia = (mafiaHeal == null ? "No one" : mafiaHeal.getUserName()) + " is healed by drLector tonight.";
            for(ServerWorker mafia : workers.getMafias())
                mafia.sendMsgToClient(serverMsgFromString(tellMafia));

            workers.sleepWorker(drLector);
        }

        // SERVER LOG
        if(mafiaHeal == null)
            System.err.println("NO MAFIA HEAL TONIGHT");
        else
            System.err.println("MAFIA HEAL: " + mafiaHeal.getUserName());
        /////////////

        Thread.sleep(NIGHT_SPLIT_TIME);

        // wakeup Doctor
        if(doctor != null && !doctor.isDead()){
            workers.wakeUpWorker(doctor);

            // voting
            doctorHeal = voter.doctorVote(doctorSelfHeal < 1);

            if(doctorHeal == doctor)
                doctorSelfHeal++;

            workers.sleepWorker(doctor);
        }

        // SERVER LOG
        if(doctorHeal == null)
            System.err.println("NO DOCTOR HEAL TONIGHT");
        else
            System.err.println("DOCTOR HEAL: " + doctorHeal.getUserName());
        /////////////

        Thread.sleep(NIGHT_SPLIT_TIME);

        // wakeup Inspector
        if(inspector != null && !inspector.isDead()){
            workers.wakeUpWorker(inspector);

            // voting
            ServerWorker inspected = voter.inspectorVote();

            // answering inspector
            if(inspected == null){
                System.err.println("NO ONE TO INSPECT");

            } else if(inspected.getType() == Type.GodFather || inspected.getGroup() == Group.City){
                inspector.sendMsgToClient(serverMsgFromString("inspected is CITY"));

            } else {
                inspector.sendMsgToClient(serverMsgFromString("inspected is MAFIA"));

            }

            workers.sleepWorker(inspector);
        }

        Thread.sleep(NIGHT_SPLIT_TIME);

        // wakeup Sniper
        if(sniper != null && !sniper.isDead()){
            workers.wakeUpWorker(sniper);

            // voting
            sniperShoot = voter.sniperVote();

            if(sniperShoot != null && sniperShoot.getGroup() == Group.City)
                sniperShoot = sniper;

            workers.sleepWorker(sniper);
        }

        // SERVER LOG
        if(sniperShoot == null)
            System.err.println("NO SNIPER SHOOT TONIGHT");
        else
            System.err.println("SNIPER SHOOT: " + sniperShoot.getUserName());
        /////////////

        Thread.sleep(NIGHT_SPLIT_TIME);

        // wakeup Psycho
        if(psycho != null && !psycho.isDead()){
            workers.wakeUpWorker(psycho);

            // voting
            psychoMute = voter.psychoVote();
            nightNews.add((psychoMute == null ? "No one" : psychoMute.getUserName()) + " is mute today.");

            workers.sleepWorker(psycho);
        }

        // SERVER LOG
        if(psychoMute == null)
            System.err.println("NO PSYCHO MUTE TONIGHT");
        else
            System.err.println("PSYCHO MUTE: " + psychoMute.getUserName());
        /////////////

        Thread.sleep(NIGHT_SPLIT_TIME);

        // wakeup Strong
        if(strongQuery < 2) {

            if (strong != null && !strong.isDead()) {
                workers.wakeUpWorker(strong);

                // voting
                strongGetQuery = voter.strongVote();

                if(strongGetQuery)
                    strongQuery++;

                workers.sleepWorker(strong);
            }
        }

        // SERVER LOG
        if(!strongGetQuery)
            System.err.println("NO STRONG QUERY TONIGHT");
        else
            System.err.println("STRONG QUERY: " + strongQuery);
        /////////////

        Thread.sleep(NIGHT_SPLIT_TIME);


        // affecting night votes:
        if(mafiaShoot != null && mafiaShoot != doctorHeal){

            if(mafiaShoot == strong && strongSurvived < 1){
                strongSurvived ++;
            } else {
                mafiaShoot.kill();
                nightNews.add(mafiaShoot.getUserName() + " was killed.");
            }

        }

        if(sniperShoot != null && sniperShoot != doctorHeal && sniperShoot != mafiaHeal){
            sniperShoot.kill();
            nightNews.add(sniperShoot.getUserName() + " was killed.");
        }

        if(strongGetQuery)
            nightNews.add(getQuery());

        if(psychoMute != null)
            psychoMute.makeMute();

        System.out.println("NIGHT FINISHED");
    }


    /**
     * Sends what happened the last night, to players
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    private void sendNightNews() throws IOException {
        if(nightNews.isEmpty())
            nightNews.add("No one is killed last night.");
        else
            nightNews.add(0, "Here is what happened last night:");

        nightNews.add(0, "-------------------");
        nightNews.add(   "-------------------");

        for(String news : nightNews)
            workers.msgToAllAwake(serverMsgFromString(news));
    }


    /**
     * Checks whether the game is finished or not.
     * The game is finished if whether all mafias are dead, or
     * remaining citizens are not more than remaining mafias.
     * @return true, if the game is finished, false otherwise
     */
    private boolean gameIsFinished() {
        return workers.mafiaCount() == 0 || workers.mafiaCount() >= workers.cityCount();
    }


    /**
     * Waits for all clients to log in,
     * after they have connected to the server
     * @throws InterruptedException If there is any unhandled exceptions while sleeping
     */
    private void waitForLogin() throws InterruptedException {
        while (!workers.allLoggedIn())
            Thread.sleep(500);
    }


    /**
     * Waits for all clients to type 'start',
     * after they have connected to the server
     * @throws InterruptedException If there is any unhandled exceptions while sleeping
     */
    private void waitForStart() throws InterruptedException {
        while (!workers.allStarted())
            Thread.sleep(500);
    }


    /**
     * Gets workers.
     *
     * @return the workers
     */
    public ArrayList<ServerWorker> getWorkers() {
        return workers.getWorkers();
    }


    /**
     * Get worker handler.
     *
     * @return the worker handler
     */
    public WorkerHandler getWorkerHandler(){
        return workers;
    }


    /**
     * Makes a message string whose sender is SERVER_NAME.
     *
     * @param body the body of message
     * @return The message string
     */
    public static String serverMsgFromString(String body){
        return msgFromString(SERVER_NAME, body);
    }


    /**
     * Makes a message string with the given sender and body.
     *
     * @param sender the sender of message
     * @param body   the body of message
     * @return The message string
     */
    public static String msgFromString(String sender, String body){
        return GameServer.MSG + " " + sender + " " + body + "\n";
    }

    /**
     * Adds a chat message to the chatHistory.
     *
     * @param msg The chat message
     */
    public void processChat(String msg) {
        if(!isDay)
            return;

        String[] tokens = msg.split(" ", 3);
        if(tokens.length != 3)
            return;

        String sender = tokens[1], body = tokens[2];
        chatHistory.append(sender + ": " + body + "\n");
    }


    /**
     * Gets chat history as a string.
     *
     * @return chat history, as a string
     */
    public String getHistory() {
        return chatHistory.toString();
    }


    /**
     * Tells all online players, that userName is now offline.
     *
     * @param userName The userName
     */
    public void tellOffline(String userName) {
        try {
            workers.msgToAll(serverMsgFromString(userName + " left the game."));
        } catch (IOException e) {
            // e.printStackTrace();
            System.err.println("Couldn't tell others that " + userName + " is offline.");
        }
    }
}

