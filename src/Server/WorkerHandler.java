package Server;

import Enums.Group;
import Enums.Type;

import java.io.IOException;
import java.util.ArrayList;

/**
 * WorkerHandler class.
 * Handles server workers of the gameServer
 */
class WorkerHandler {
    private final ArrayList<ServerWorker> workers;

    /**
     * Worker handler Constructor.
     */
    public WorkerHandler(){
        workers = new ArrayList<>();
    }


    /**
     * Get workers list.
     *
     * @return workers list
     */
    public ArrayList<ServerWorker> getWorkers(){
        return workers;
    }


    /**
     * Add a worker to list.
     *
     * @param worker the worker
     */
    public void addWorker(ServerWorker worker){
        workers.add(worker);
    }


    /**
     * Counts the number of current workers.
     *
     * @return number of workers
     */
    public int count(){
        return workers.size();
    }


    /**
     * Get mafias list.
     * May be DEAD or OFFLINE.
     *
     * @return mafias list
     */
    public ArrayList<ServerWorker> getMafias(){
        ArrayList<ServerWorker> mafias = new ArrayList<>();
        for(ServerWorker worker : workers)
            if(worker.getGroup() == Group.Mafia)
                mafias.add(worker);

        return mafias;
    }


    /**
     * Get city list.
     * May be DEAD or OFFLINE.
     *
     * @return city list
     */
    public ArrayList<ServerWorker> getCity(){
        ArrayList<ServerWorker> city = new ArrayList<>();
        for(ServerWorker worker : workers)
            if(worker.getGroup() == Group.City)
                city.add(worker);

        return city;
    }


    /**
     * Number of ALIVE and ONLINE mafias.
     *
     * @return number of active mafias
     */
    public int mafiaCount(){
        int mafias = 0;
        for(ServerWorker worker : getMafias())
            if(worker.isOnline() && !worker.isDead())
                mafias++;

        return mafias;
    }


    /**
     * Number of ALIVE and ONLINE cities.
     *
     * @return number of active cities
     */
    public int cityCount(){
        int cities = 0;
        for(ServerWorker worker : getCity())
            if(worker.isOnline() && !worker.isDead())
                cities++;

        return cities;
    }


    /**
     * Checks whether every worker is logged in.
     *
     * @return true, if all are logged in
     */
    public boolean allLoggedIn() {
        for(ServerWorker worker : workers)
            if (worker.getUserName() == null)
                return false;

        return true;
    }


    /**
     * Checks whether every client has sent 'start'.
     *
     * @return true, if all have 'started'
     */
    public boolean allStarted() {
        for(ServerWorker worker : workers)
            if (!worker.isStart())
                return false;

        return true;
    }


    /**
     * Wake up all ALIVE and ONLINE players.
     *
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void wakeUpAll() throws IOException {
        for(ServerWorker worker : workers)
            worker.wakeUp(); // dead is handled in 'wakeUp' method
    }


    /**
     * Wake up all ALIVE and ONLINE players in the list.
     *
     * @param wakeUpList the wake up list
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void wakeUpList(ArrayList<ServerWorker> wakeUpList) throws IOException {
        for(ServerWorker worker : wakeUpList)
                worker.wakeUp(); // dead is handled in 'wakeUp' method
    }


    /**
     * Wake up a specific worker.
     * must be ALIVE and ONLINE,
     * otherwise nothing happens
     *
     * @param worker the worker
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void wakeUpWorker(ServerWorker worker) throws IOException {
        worker.wakeUp();
    }


    /**
     * Sleep all ALIVE and ONLINE players in the list.
     *
     * @param sleepList the sleep list
     * @throws IOException the io exception
     */
    public void sleepList(ArrayList<ServerWorker> sleepList) throws IOException {
        for(ServerWorker worker : sleepList)
                worker.nightReset(); // dead is handled in 'nightReset' method
    }


    /**
     * Sleep a specific ALIVE and ONLINE worker.
     * must be ALIVE and ONLINE,
     * otherwise nothing happens
     * @param worker the worker
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void sleepWorker(ServerWorker worker) throws IOException {
        worker.nightReset();
    }


    /**
     * Checks whether every ALIVE and ONLINE and UNMUTE players
     * are ready for voting
     *
     * @return true, if all are ready
     */
    public boolean allReady() {
        boolean ready = true;
        for(ServerWorker worker : workers)
            ready &= worker.isDead() || worker.isSleep() || worker.isMute() || worker.isReady();

        return ready;
    }


    /**
     * Prepares clients and workers for night.
     * Descriptions are in 'ServerWorker.nightReset' method
     *
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void prepareNight() throws IOException {
        for(ServerWorker worker : workers)
            if(!worker.isDead())
                worker.nightReset();
    }


    /**
     * Find the worker with given group and type.
     * Note that when playing with 10 players,
     * there is exactly on of each type
     * May be DEAD or OFFLINE.
     *
     * @param group the group
     * @param type  the type
     * @return the worker with given group and type
     */
    public ServerWorker findWorker(Group group, Type type) {
        for(ServerWorker worker : workers)
            if(worker.getGroup() == group && worker.getType() == type)
                return worker;

        return null;
    }


    /**
     * Send lineBreak for all ONLINE players.
     *
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void lineBreakAll() throws IOException {
        for(ServerWorker worker : workers)
            if(worker.isOnline())
                worker.lineBreak();
    }


    /**
     * Send message to all AWAKE and ONLINE players.
     *
     * @param toSend message body
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void msgToAllAwake(String toSend) throws IOException {
        for(ServerWorker worker : workers)
            if(!worker.isSleep() && worker.isOnline()){
                worker.sendMsgToClient(toSend);
            }
    }


    /**
     * Send message to all AWAKE and ONLINE players,
     * except for the 'except' worker.
     *
     * @param toSend message body
     * @param except except worker
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void msgToAllAwake(String toSend, ServerWorker except) throws IOException {
        for(ServerWorker worker : workers)
            if(worker != except && !worker.isSleep() && worker.isOnline()){
                worker.sendMsgToClient(toSend);
            }
    }


    /**
     * Send message to all ONLINE players.
     *
     * @param toSend message body
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void msgToAll(String toSend) throws IOException {
        for(ServerWorker worker : workers)
            if(worker.isOnline())
                worker.sendMsgToClient(toSend);
    }


    /**
     * Tell the time to all ONLINE players.
     *
     * @param time the time (DAY/NIGHT)
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void tellTime(String time) throws IOException {
        for (ServerWorker worker : workers)
            if(worker.isOnline())
                worker.tellTime(time);
    }


    /**
     * Tell who is ALIVE, to ONLINE and AWAKE users.
     *
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void tellWhoIsAlive() throws IOException {
        String alive = "";
        for(ServerWorker worker : workers)
            if(worker.isOnline() && !worker.isDead())
                alive += worker.getUserName() + ", ";

        msgToAllAwake(GameServer.serverMsgFromString("Alive players:"));
        msgToAllAwake(GameServer.serverMsgFromString(
                alive.substring(0, alive.lastIndexOf(",")))
        );
        msgToAllAwake(GameServer.serverMsgFromString("-------------------"));

        lineBreakAll();
    }


    /**
     * Finish the game.
     * Tells all ONLINE players which team wins,
     * and closes the connection and the game for them.
     *
     * @param winnerGroup the winner group
     * @throws IOException If there is any unhandled exceptions while sending/reading messages
     */
    public void finishGame(Group winnerGroup) throws IOException {
        for(ServerWorker worker : workers)
            if(worker.isOnline()){

                // notify them
                worker.lineBreak();
                worker.sendErr("Game is Finished!");
                worker.lineBreak();

                String winner = winnerGroup == Group.City ? "City" : "Mafia";
                String loser = winnerGroup == Group.City ? "Mafia" : "City";

                if(worker.getGroup() == winnerGroup)
                    worker.sendErr("Congrats! " + winner + " won!");
                else
                    worker.sendErr("Oops! " + loser + " lost!");

                worker.lineBreak();


                // close game
                worker.closeGame();
            }

    }
}
