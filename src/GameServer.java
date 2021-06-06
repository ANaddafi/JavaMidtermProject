import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class GameServer extends Thread{
    public static final int PLAYER_COUNT = 3;
    public static final String SERVER_NAME = "GOD";
    public static final String SLEEP = "SLEEP";
    public static final String WAKEUP = "WAKEUP";
    public static final String VOTE = "VOTE";
    public static final String MSG = "MSG";
    public static final String ERR = "ERR";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String READY = "READY";
    public static final String DEAD = "DEAD";


    // time in milli second
    public static final int TIME_TICK = 1000;
    public static final int DAY_TIME = 5 * 60 * 1000; // should be 5 mins
    public static final int DAY_VOTE_TIME = 30 * 1000; // should be 30 secs
    public static final int MAYOR_VOTE_TIME = 10 * 1000;
    public static final int MAFIA_TALK_TIME = 30 * 1000;
    public static final int MAFIA_KILL_TIME = 10 * 1000;
    public static final int MAFIA_HEAL_TIME = 10 * 1000;
    public static final int DR_HEAL_TIME = 10 * 1000;
    public static final int INSPECT_TIME = 10 * 1000;
    public static final int SNIPE_TIME = 10 * 1000;
    public static final int PSYCHO_TIME = 10 * 1000;
    public static final int STRONG_TIME = 10 * 100;


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

    public GameServer(int port){
        this.port = port;
        workers = new WorkerHandler();
        voter = new Voter(this);
        nightNews = new ArrayList<>();

        drLectorSelfHeal = 0;
        doctorSelfHeal = 0;
        strongQuery = 0;
        strongSurvived = 0;
    }


    public boolean hasUserName(String userName) {
        boolean hasUserName = false;

        for(ServerWorker worker : workers.getWorkers())
            if(worker.getUserName() != null)
                hasUserName |= worker.getUserName().equals(userName);

        return hasUserName;
    }


    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server Started.\nWating for clients to join...");

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
        return giveRoles();
    }


    private boolean giveRoles() {
        if(workers.count() != PLAYER_COUNT)
            return false;

        workers.getWorkers().get(0).giveRole(Group.Mafia, Type.GodFather);
        workers.getWorkers().get(1).giveRole(Group.Mafia, Type.DrLector);

        //workers.getWorkers().get(2).giveRole(Group.City, Type.Mayor);
        workers.getWorkers().get(2).giveRole(Group.City, Type.Doctor);

        /*
        workers.getWorkers().get(2).giveRole(Group.Mafia, Type.OrdMafia);

        workers.getWorkers().get(4).giveRole(Group.City, Type.Inspector);
        workers.getWorkers().get(5).giveRole(Group.City, Type.Sniper);
        workers.getWorkers().get(6).giveRole(Group.City, Type.Mayor);
        workers.getWorkers().get(7).giveRole(Group.City, Type.Psycho);
        workers.getWorkers().get(8).giveRole(Group.City, Type.Strong);
        workers.getWorkers().get(9).giveRole(Group.City, Type.OrdCity);
         */

        return true;
    }


    private void startGame() throws IOException, InterruptedException {
        waitForLogin();

        // Intro (Night #1)
        System.out.println("INTRO BEGINS");
        for(ServerWorker worker : workers.getWorkers()){
            worker.sendRole();
        }

        // The day cycle begins
        System.out.println("DAY CYCLE BEGINS");

        while(!gameIsFinished() || DEBUG){
            handleDay();

            Thread.sleep(3000);
            if(gameIsFinished() && !DEBUG)
                break;

            handleNight();

        }

        System.out.println("WAITING...");
        while (true)
            Thread.sleep(1000);

    }


    private void handleNight() throws IOException, InterruptedException {
        // NIGHT
        System.out.println("ITS NIGHT");

        // preparations
        workers.prepareNight();
        firstNight = false;
        ServerWorker mafiaShoot = null;
        ServerWorker mafiaHeal = null;
        ServerWorker doctorHeal = null;
        ServerWorker sniperShoot = null;
        ServerWorker psychoMute = null;
        boolean strongQuery = false;

        // wakeup Mafias
        workers.wakeUpList(workers.getMafias());

        for(int i = 0; i < MAFIA_TALK_TIME/TIME_TICK && !workers.allReady(); i++)
            Thread.sleep(TIME_TICK);

        mafiaShoot = voter.mafiaVote();
        workers.sleepList(workers.getMafias());

        // DEBUG LOG
        if(mafiaShoot == null)
            System.err.println("NO MAFIA KILL TONIGHT");
        else
            System.err.println("MAFIA KILL: " + mafiaShoot.getUserName());
        /////////////

        Thread.sleep(1500);

        // wakeup drLector
        ServerWorker drLector = workers.findWorker(Group.Mafia, Type.DrLector);
        if(drLector != null && !drLector.isDead()) {
            workers.wakeUpWorker(drLector);

            mafiaHeal = voter.lectorVote(drLectorSelfHeal < 1);
            if(mafiaHeal == drLector)
                drLectorSelfHeal++;

            workers.sleepWorker(drLector);
        }

        // DEBUG LOG
        if(mafiaHeal == null)
            System.err.println("NO MAFIA HEAL TONIGHT");
        else
            System.err.println("MAFIA HEAL: " + mafiaHeal.getUserName());
        /////////////

        Thread.sleep(1500);

        // wakeup Doctor
        ServerWorker doctor = workers.findWorker(Group.City, Type.Doctor);
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


        // affecting night votes:
        if(mafiaShoot != null && mafiaShoot != doctorHeal){
            mafiaShoot.kill();
            nightNews.add(mafiaShoot.getUserName() + " was killed last night.");
        }
    }


    private void handleDay() throws IOException, InterruptedException {
        // DAY
        System.out.println("IT'S DAY");

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
            workers.msgToAllAwake(serverMsgFromString("He/She was " + dayVoteWinner.getRoleString()));

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
}

