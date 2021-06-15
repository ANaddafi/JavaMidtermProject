package Server;

import Enums.Group;
import Enums.Type;

import java.io.IOException;
import java.util.ArrayList;

class WorkerHandler {
    private final ArrayList<ServerWorker> workers;

    public WorkerHandler(){
        workers = new ArrayList<>();
    }

    public ArrayList<ServerWorker> getWorkers(){
        return workers;
    }

    public void addWorker(ServerWorker worker){
        workers.add(worker);
    }

    public int count(){
        return workers.size();
    }


    public ArrayList<ServerWorker> getMafias(){
        ArrayList<ServerWorker> mafias = new ArrayList<>();
        for(ServerWorker worker : workers)
            if(worker.getGroup() == Group.Mafia)
                mafias.add(worker);

        return mafias;
    }

    public ArrayList<ServerWorker> getCity(){
        ArrayList<ServerWorker> city = new ArrayList<>();
        for(ServerWorker worker : workers)
            if(worker.getGroup() == Group.City)
                city.add(worker);

        return city;
    }

    public int mafiaCount(){
        int mafias = 0;
        for(ServerWorker worker : getMafias())
            if(worker.isOnline() && !worker.isDead())
                mafias++;

        return mafias;
    }

    public int cityCount(){
        int cities = 0;
        for(ServerWorker worker : getCity())
            if(worker.isOnline() && !worker.isDead())
                cities++;

        return cities;
    }

    public boolean allLoggedIn() {
        for(ServerWorker worker : workers)
            if (worker.getUserName() == null)
                return false;

        return true;
    }


    public boolean allStarted() {
        for(ServerWorker worker : workers)
            if (!worker.isStart())
                return false;

        return true;
    }

    public void wakeUpAll() throws IOException {
        for(ServerWorker worker : workers)
                    worker.wakeUp(); // dead is handled in 'wakeUp' method
    }

    public void wakeUpList(ArrayList<ServerWorker> wakeUpList) throws IOException {
        for(ServerWorker worker : wakeUpList)
                worker.wakeUp(); // dead is handled in 'wakeUp' method
    }

    public void wakeUpWorker(ServerWorker worker) throws IOException {
        worker.wakeUp();
    }

    public void sleepList(ArrayList<ServerWorker> sleepList) throws IOException {
        for(ServerWorker worker : sleepList)
                worker.nightReset(); // dead is handled in 'nightReset' method
    }

    public void sleepWorker(ServerWorker worker) throws IOException {
        worker.nightReset();
    }

    public boolean allReady() {
        boolean ready = true;
        for(ServerWorker worker : workers)
            ready &= worker.isDead() || worker.isSleep() || worker.isMute() || worker.isReady();

        return ready;
    }

    public void prepareNight() throws IOException {
        for(ServerWorker worker : workers)
            if(!worker.isDead())
                worker.nightReset();
    }

    // when playing with 10 players, there is exactly on of each type
    public ServerWorker findWorker(Group group, Type type) {
        for(ServerWorker worker : workers)
            if(worker.getGroup() == group && worker.getType() == type)
                return worker;

        return null;
    }

    public void lineBreakAll() throws IOException {
        for(ServerWorker worker : workers)
            if(!worker.isSleep() && worker.isOnline())
                worker.lineBreak();
    }

    public void msgToAllAwake(String toSend) throws IOException {
        for(ServerWorker worker : workers)
            if(!worker.isSleep() && worker.isOnline()){
                worker.sendMsgToClient(toSend);
            }
    }

    public void msgToAllAwake(String toSend, ServerWorker except) throws IOException {
        for(ServerWorker worker : workers)
            if(worker != except && !worker.isSleep() && worker.isOnline()){
                worker.sendMsgToClient(toSend);
            }
    }

    public void msgToAll(String toSend) throws IOException {
        for(ServerWorker worker : workers)
            if(worker.isOnline())
                worker.sendMsgToClient(toSend);
    }

    public void tellTime(String time) throws IOException {
        for (ServerWorker worker : workers)
            if(worker.isOnline())
                worker.tellTime(time);
    }

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
