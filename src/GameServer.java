import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

// TODO AT NIGHT IN MAFIA TALK, TELL THEM THE MAFIAS ID
// TODO IN START OF DAY, TELL WHO IS STILL ALIVE (MAYBE?)
// TODO USE FOLDERS!

public class GameServer extends Thread{
    public static final int PLAYER_COUNT = 2;
    public static final String SERVER_NAME = "*GOD*";
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


    // time in milli second
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

    private ServerWorker godFather;
    private ServerWorker drLector;
    private ServerWorker ordMafia;

    private ServerWorker mayor;
    private ServerWorker doctor;
    private ServerWorker inspector;
    private ServerWorker sniper;
    private ServerWorker psycho;
    private ServerWorker strong;
    private ServerWorker ordCity;

    private StringBuilder chatHistory;

    public int drLectorSelfHeal;
    public int doctorSelfHeal;
    public int strongQuery;
    public int strongSurvived;

    public static final boolean DEBUG = true;


    private final int port;

    private final WorkerHandler workers;
    private final Voter voter;
    private final ArrayList<String> nightNews;
    private boolean firstNight;
    private boolean isDay;

    public GameServer(int port){
        this.port = port;
        workers = new WorkerHandler();
        voter = new Voter(this);
        nightNews = new ArrayList<>();

        drLectorSelfHeal = 0;
        doctorSelfHeal = 0;
        strongQuery = 0;
        strongSurvived = 0;

        chatHistory = new StringBuilder("");
    }


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


    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server Started.\nWaiting for clients to join...");

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


    private boolean initGame() {
        firstNight = true;
        isDay = false;
        if(giveRoles()){
            initRoles();
            return true;
        }

        return false;
    }


    private boolean giveRoles() {
        if(workers.count() != PLAYER_COUNT)
            return false;

        workers.getWorkers().get(0).giveRole(Group.Mafia, Type.GodFather);
        workers.getWorkers().get(1).giveRole(Group.Mafia, Type.DrLector);
        //workers.getWorkers().get(2).giveRole(Group.Mafia, Type.OrdMafia);

        //workers.getWorkers().get(2).giveRole(Group.City, Type.Mayor);
        /*workers.getWorkers().get(4).giveRole(Group.City, Type.Doctor);
        workers.getWorkers().get(5).giveRole(Group.City, Type.Inspector);
        workers.getWorkers().get(6).giveRole(Group.City, Type.Sniper);
        workers.getWorkers().get(7).giveRole(Group.City, Type.Psycho);
        workers.getWorkers().get(8).giveRole(Group.City, Type.Strong);
        workers.getWorkers().get(9).giveRole(Group.City, Type.OrdCity);*/

        return true;
    }

    public void initRoles(){
        godFather = workers.findWorker(Group.Mafia, Type.GodFather);
        drLector = workers.findWorker(Group.Mafia, Type.DrLector);
        ordMafia = workers.findWorker(Group.Mafia, Type.OrdMafia);

        mayor = workers.findWorker(Group.City, Type.Mayor);
        doctor = workers.findWorker(Group.City, Type.Doctor);
        inspector = workers.findWorker(Group.City, Type.Inspector);
        sniper = workers.findWorker(Group.City, Type.Sniper);
        psycho = workers.findWorker(Group.City, Type.Psycho);
        strong = workers.findWorker(Group.City, Type.Strong);
        ordCity = workers.findWorker(Group.City, Type.OrdCity);
    }


    private void startGame() throws IOException, InterruptedException {
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
                            serverMsgFromString("-> " + mafia.getUserName() + " is " + mafia.getType())
                    );


        // The day cycle begins
        System.out.println("DAY CYCLE BEGINS");

        while(!gameIsFinished() || DEBUG){
            handleDay();

            processChat(serverMsgFromString("--Day is finished here--"));

            Thread.sleep(3000);
            if(gameIsFinished() && !DEBUG)
                break;

            handleNight();

        }

        // TODO TELL PLAYERS ABOUT THE RESULTS!
        System.out.println("WAITING...");
        while (true)
            Thread.sleep(1000);

    }

    private void handleNight() throws IOException, InterruptedException {
        // NIGHT
        System.err.println("NIGHT");
        isDay = false;

        // preparations
        workers.prepareNight();
        nightNews.clear();
        firstNight = false;
        ServerWorker mafiaShoot = null;
        ServerWorker mafiaHeal = null;
        ServerWorker doctorHeal = null;
        ServerWorker sniperShoot = null;
        ServerWorker psychoMute = null;
        boolean strongGetQuery = false;

        // wakeup Mafias
        workers.wakeUpList(workers.getMafias());

        for(int i = 0; i < MAFIA_TALK_TIME/TIME_TICK && !workers.allReady(); i++)
            Thread.sleep(TIME_TICK);

        mafiaShoot = voter.mafiaVote();

        String tellMafia = (mafiaShoot == null ? "No one" : mafiaShoot.getUserName()) + " is shot by mafia tonight.";
        for(ServerWorker mafia : workers.getMafias())
            mafia.sendMsgToClient(serverMsgFromString(tellMafia));

        workers.sleepList(workers.getMafias());

        // DEBUG LOG
        if(mafiaShoot == null)
            System.err.println("NO MAFIA KILL TONIGHT");
        else
            System.err.println("MAFIA KILL: " + mafiaShoot.getUserName());
        /////////////

        Thread.sleep(NIGHT_SPLIT_TIME);

        // wakeup drLector
        if(drLector != null && !drLector.isDead()) {
            workers.wakeUpWorker(drLector);

            mafiaHeal = voter.lectorVote(drLectorSelfHeal < 1);
            if(mafiaHeal == drLector)
                drLectorSelfHeal++;


            tellMafia = (mafiaHeal == null ? "No one" : mafiaHeal.getUserName()) + " is healed by drLector tonight.";
            for(ServerWorker mafia : workers.getMafias())
                mafia.sendMsgToClient(serverMsgFromString(tellMafia));

            workers.sleepWorker(drLector);
        }

        // DEBUG LOG
        if(mafiaHeal == null)
            System.err.println("NO MAFIA HEAL TONIGHT");
        else
            System.err.println("MAFIA HEAL: " + mafiaHeal.getUserName());
        /////////////

        Thread.sleep(NIGHT_SPLIT_TIME);

        // wakeup Doctor
        if(doctor != null && !doctor.isDead()){
            workers.wakeUpWorker(doctor);

            doctorHeal = voter.doctorVote(doctorSelfHeal < 1);

            if(doctorHeal == doctor)
                doctorSelfHeal++;

            workers.sleepWorker(doctor);
        }

        // DEBUG LOG
        if(doctorHeal == null)
            System.err.println("NO DOCTOR HEAL TONIGHT");
        else
            System.err.println("DOCTOR HEAL: " + doctorHeal.getUserName());
        /////////////

        Thread.sleep(NIGHT_SPLIT_TIME);

        // wakeup Inspector
        if(inspector != null && !inspector.isDead()){
            workers.wakeUpWorker(inspector);

            ServerWorker inspected = voter.inspectorVote();

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

            sniperShoot = voter.sniperVote();

            if(sniperShoot != null && sniperShoot.getGroup() == Group.City)
                    sniperShoot = sniper;

            workers.sleepWorker(sniper);
        }

        // DEBUG LOG
        if(sniperShoot == null)
            System.err.println("NO SNIPER SHOOT TONIGHT");
        else
            System.err.println("SNIPER SHOOT: " + sniperShoot.getUserName());
        /////////////

        Thread.sleep(NIGHT_SPLIT_TIME);

        // wakeup Psycho
        if(psycho != null && !psycho.isDead()){
            workers.wakeUpWorker(psycho);

            psychoMute = voter.psychoVote();
            nightNews.add((psychoMute == null ? "No one" : psychoMute.getUserName()) + " is mute today.");

            workers.sleepWorker(psycho);
        }

        // DEBUG LOG
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

                strongGetQuery = voter.strongVote();

                if(strongGetQuery)
                    strongQuery++;

                workers.sleepWorker(strong);
            }
        }

        // DEBUG LOG
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
                nightNews.add(mafiaShoot.getUserName() + " was killed last night.");
            }

        }

        if(sniperShoot != null && sniperShoot != doctorHeal && sniperShoot != mafiaHeal){
            sniperShoot.kill();
            nightNews.add(sniperShoot.getUserName() + " was killed last night.");
        }

        if(strongGetQuery)
            nightNews.add(getQuery());

        if(psychoMute != null)
            psychoMute.makeMute();

        System.out.println("NIGHT FINISHED");
    }

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


    private void handleDay() throws IOException, InterruptedException {
        // DAY
        System.err.println("DAY");
        isDay = true;

        // preparations
        workers.wakeUpAll();
        if(!firstNight)
            sendNightNews();

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
        ServerWorker mayor = workers.findWorker(Group.City, Type.Mayor);

        if(dayVoteWinner != null && mayor != null && !mayor.isDead()) {
            System.out.println("MAYOR VOTE");

            workers.msgToAllAwake(serverMsgFromString("Waiting for Mayors decision..."), mayor);

            doTheKill = voter.mayorVote(dayVoteWinner.getUserName());
        }

        if(doTheKill){
            workers.msgToAllAwake(serverMsgFromString("Today " + dayVoteWinner.getUserName() + " is going to die!"));
            // workers.msgToAllAwake(serverMsgFromString("He/She was " + dayVoteWinner.getRoleString()));

            // DO THE KILL!
            dayVoteWinner.kill();

        } else {
            workers.msgToAllAwake(serverMsgFromString("No one is going to die today!"));
        }

        System.out.println("DAY FINISHED");
    }


    private void sendNightNews() throws IOException {
        if(nightNews.isEmpty())
            nightNews.add("No one is killed last night.");
        else
            nightNews.add(0, "Here is what happened last night:");

        for(String news : nightNews)
            workers.msgToAllAwake(serverMsgFromString(news));
    }


    private boolean gameIsFinished() {
        return workers.mafiaCount() == 0 || workers.mafiaCount() >= workers.cityCount();
    }


    private void waitForLogin() throws InterruptedException {
        while (!workers.allLoggedIn())
            Thread.sleep(500);
    }


    private void waitForStart() throws InterruptedException {
        while (!workers.allStarted())
            Thread.sleep(500);
    }


    public ArrayList<ServerWorker> getWorkers() {
        return workers.getWorkers();
    }


    public WorkerHandler getWorkerHandler(){
        return workers;
    }


    public String serverMsgFromString(String body){
        return msgFromString(SERVER_NAME, body);
    }


    public String msgFromString(String sender, String body){
        return GameServer.MSG + " " + sender + " " + body + "\n";
    }

    public void processChat(String msg) {
        if(!isDay)
            return;

        String[] tokens = msg.split(" ", 3);
        if(tokens.length != 3)
            return;

        String sender = tokens[1], body = tokens[2];
        chatHistory.append(sender + ": " + body + "\n");
    }

    public String getHistory() {
        return chatHistory.toString();
    }

    public void tellOffline(String userName) {
        try {
            workers.msgToAll(serverMsgFromString(userName + " left the game."));
        } catch (IOException e) {
            // e.printStackTrace();
            System.err.println("Couldn't tell others that " + userName + " is offline.");
        }
    }
}

