import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

// TODO: MESSAGE HANDLER CLASS? (probably static)
//  WORKER HANDLER CLASS?
//  BECAUSE THERE ARE TOO MUCH METHODS FOR THAT!


public class GameServer extends Thread{
    public static final int PLAYER_COUNT = 2;
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

    private final ArrayList<ServerWorker> workers;
    private final ArrayList<ServerWorker> mafias;
    private final ArrayList<ServerWorker> citizens;
    private final Voter voter;

    public GameServer(int port){
        this.port = port;
        workers = new ArrayList<>();
        mafias = new ArrayList<>();
        citizens = new ArrayList<>();
        voter = new Voter(this);

        drLectorSelfHeal = 0;
        doctorSelfHeal = 0;
        strongQuery = 0;
        strongSurvived = 0;
    }

    public boolean hasUserName(String userName) {
        boolean hasUserName = false;

        for(ServerWorker worker : workers)
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
                workers.add(worker);
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
        // TODO ONLY ONE THING?
        //  IF YES, SO USE ONLY ONE METHOD!
        return giveRoles();
    }

    private boolean giveRoles() {
        if(workers.size() != PLAYER_COUNT)
            return false;

        // Mafias:
        workers.get(0).giveRole(Group.Mafia, Type.GodFather);
        mafias.add(workers.get(0));

        /*workers.get(1).giveRole(Group.Mafia, Type.DrLector);
        mafias.add(workers.get(1));*/

        workers.get(1).giveRole(Group.City, Type.Mayor);
        citizens.add(workers.get(1));

        /*
        workers.get(2).giveRole(Group.Mafia, Type.OrdMafia);

        mafias.add(workers.get(2));

        // Citizens:
        workers.get(3).giveRole(Group.City, Type.Doctor);
        workers.get(4).giveRole(Group.City, Type.Inspector);
        workers.get(5).giveRole(Group.City, Type.Sniper);
        workers.get(6).giveRole(Group.City, Type.Mayor);
        workers.get(7).giveRole(Group.City, Type.Psycho);
        workers.get(8).giveRole(Group.City, Type.Strong);
        workers.get(9).giveRole(Group.City, Type.OrdCity);

        citizens.add(workers.get(3));
        citizens.add(workers.get(4));
        citizens.add(workers.get(5));
        citizens.add(workers.get(6));
        citizens.add(workers.get(7));
        citizens.add(workers.get(8));
        citizens.add(workers.get(9));

         */

        return true;
    }

    private void startGame() throws IOException, InterruptedException {
        waitForLogin();

        // Intro (Night #1)
        System.out.println("INTRO BEGINS");
        for(ServerWorker worker : workers){
            worker.sendRole();
        }

        // The day cycle begins
        System.out.println("DAY CYCLE BEGINS");

        while(!gameIsFinished() || DEBUG){
            handleDay();

            Thread.sleep(3000);

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
        prepareNight();

        Thread.sleep(10 * 1000);
    }

    private void prepareNight() throws IOException {
        for(ServerWorker worker : workers)
            if(!worker.isDead())
                worker.NightReset();
    }

    private void handleDay() throws IOException, InterruptedException {
        // DAY
        System.out.println("IT'S DAY");

        // preparations
        wakeUpAll();

        // discussion
        for(int i = 0; i < DAY_TIME/TIME_TICK; i++){
            Thread.sleep(TIME_TICK);
            if(allReady())
                break;
        }

        // voting
        System.out.println("DAY VOTE");

        ServerWorker dayVoteWinner = voter.dayVote();

        // Mayor vote
        System.out.println("MAYOR VOTE");

        if(dayVoteWinner != null)
            sendMsgToAllAwake(serverMsgFromString("Waiting for Mayors decision..."), findWorker(Group.City, Type.Mayor));

        boolean doTheKill = dayVoteWinner != null && voter.mayorVote(dayVoteWinner.getUserName());

        if(doTheKill){
            System.err.println("MAYOR SAID TO DO THE KILL!");

            sendMsgToAllAwake(serverMsgFromString("Today " + dayVoteWinner.getUserName() + " is going to die!"));

            // DO THE KILL!
            dayVoteWinner.kill();

        } else {
            System.err.println("MAYOR SAID DONT DO THE KILL!");

            sendMsgToAllAwake(serverMsgFromString("No one is going to die today!"));
        }

        System.out.println("DAY FINISHED");
    }

    private boolean allReady() {
        boolean ready = true;
        for(ServerWorker worker : workers)
            ready &= worker.isDead() || worker.isMute() || worker.isReady();

        return ready;
    }

    /*private void goSleepAll() throws IOException {
        for(ServerWorker worker : workers)
            worker.goSleep();
    }*/

    private void wakeUpAll() throws IOException {
        for(ServerWorker worker : workers)
            if(!worker.isDead())
                worker.wakeUp();
    }

    private boolean gameIsFinished() {
        return mafias.isEmpty() || mafias.size() >= citizens.size();
    }

    private void waitForLogin() throws InterruptedException {
        boolean allLoggedIn;
        do{
            allLoggedIn = true;
            for(ServerWorker worker : workers)
                if(worker.getUserName() == null)
                    allLoggedIn = false;

            Thread.sleep(500);
        } while (!allLoggedIn);
    }

    public ArrayList<ServerWorker> getWorkers() {
        return workers;
    }

    public void sendMsgToAllAwake(String toSend) throws IOException {
        for(ServerWorker worker : workers)
            if(!worker.isSleep()){
                worker.sendMsg(toSend);
            }
    }
    public void sendMsgToAllAwake(String toSend, ServerWorker except) throws IOException {
        for(ServerWorker worker : workers)
            if(worker != except && !worker.isSleep()){
                worker.sendMsg(toSend);
            }
    }

    // when playing with 10 players, there is exactly on of each type
    public ServerWorker findWorker(Group group, Type type) {
        for(ServerWorker worker : workers)
            if(worker.getGroup() == group && worker.getType() == type)
                return worker;

        return null;
    }

    public String serverMsgFromString(String body){
        return GameServer.MSG + " " + GameServer.SERVER_NAME + " " + body + "\n";
    }

    public String msgFromString(String sender, String body){
        return GameServer.MSG + " " + sender + " " + body + "\n";
    }
}

