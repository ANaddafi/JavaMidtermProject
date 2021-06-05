import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class GameServer extends Thread{
    public static final int PLAYER_COUNT = 2;
    public static final String SERVER_NAME = "SERVER";
    public static final String SLEEP = "SLEEP";
    public static final String WAKEUP = "WAKEUP";
    public static final String VOTE = "VOTE";
    public static final String MSG = "MSG";
    public static final String ERR = "ERR";
    public static final String TIMEOUT = "TIMEOUT";


    // time in milli second
    public static final int TIME_TICK = 1000;
    public static final int DAY_TIME = 20 * 1000; // should be 5 mins
    public static final int DAY_VOTE_TIME = 20 * 1000; // should be 30 secs
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

        workers.get(1).giveRole(Group.Mafia, Type.DrLector);
        mafias.add(workers.get(1));


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
            // DAY
            System.out.println("IT'S DAY");
            wakeUpAll();

            for(int i = 0; i < DAY_TIME/TIME_TICK; i++){
                Thread.sleep(TIME_TICK);
                // TODO HANDLE READY IN WORKER & CLIENT
                if(allReady())
                    break;
            }

            System.out.println("DAY VOTE");
            ServerWorker dayVoteWinner = voter.dayVote();

            if(dayVoteWinner == null){
                sendMsgToAllAwake(GameServer.MSG + " " + GameServer.SERVER_NAME + " " +
                        "No one is going to die today!" + "\n");
            } else {
                sendMsgToAllAwake(GameServer.MSG + " " + GameServer.SERVER_NAME + " " +
                        "Today " + dayVoteWinner.getUserName() + " is going to die!" + "\n");
            }

            // NIGHT
            System.out.println("ITS NIGHT");
            goSleepAll();
            Thread.sleep(20 * 1000);

        }

        System.out.println("WAITING...");
        while (true)
            Thread.sleep(1000);

    }

    private boolean allReady() {
        boolean ready = true;
        for(ServerWorker worker : workers)
            ready &= worker.isReady();

        return ready;
    }

    private void goSleepAll() throws IOException {
        for(ServerWorker worker : workers)
            worker.goSleep();
    }

    private void wakeUpAll() throws IOException {
        for(ServerWorker worker : workers)
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
            if(!worker.isDead() && !worker.isSleep()){
                worker.sendMsg(toSend);
            }
    }
}
