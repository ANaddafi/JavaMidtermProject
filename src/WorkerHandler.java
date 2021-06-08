import java.io.IOException;
import java.util.ArrayList;

public class WorkerHandler {
    private final ArrayList<ServerWorker> workers;

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

    public void initRoles(){
        godFather = findWorker(Group.Mafia, Type.GodFather);
        drLector = findWorker(Group.Mafia, Type.DrLector);
        ordMafia = findWorker(Group.Mafia, Type.OrdMafia);

        mayor = findWorker(Group.City, Type.Mayor);
        doctor = findWorker(Group.City, Type.Doctor);
        inspector = findWorker(Group.City, Type.Inspector);
        sniper = findWorker(Group.City, Type.Sniper);
        psycho = findWorker(Group.City, Type.Psycho);
        strong = findWorker(Group.City, Type.Strong);
        ordCity = findWorker(Group.City, Type.OrdCity);
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
        return getMafias().size();
    }

    public int cityCount(){
        return getCity().size();
    }

    public boolean allLoggedIn() {
        for(ServerWorker worker : workers)
            if (worker.getUserName() == null)
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

    public void wakeUpType(Group group, Type type) throws IOException {
        findWorker(group, type).wakeUp(); // dead is handled in 'wakeUp' method
    }

    public void wakeUpWorker(ServerWorker worker) throws IOException {
        worker.wakeUp();
    }

    public void sleepList(ArrayList<ServerWorker> sleepList) throws IOException {
        for(ServerWorker worker : sleepList)
                worker.nightReset(); // dead is handled in 'nightReset' method
    }

    public void sleepType(Group group, Type type) throws IOException {
        findWorker(group, type).nightReset(); // dead is handled in 'wakeUp' method
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

    public void msgToAllAwake(String toSend) throws IOException {
        for(ServerWorker worker : workers)
            if(!worker.isSleep()){
                worker.sendMsgToClient(toSend);
            }
    }

    public void msgToAllAwake(String toSend, ServerWorker except) throws IOException {
        for(ServerWorker worker : workers)
            if(worker != except && !worker.isSleep()){
                worker.sendMsgToClient(toSend);
            }
    }

    public ServerWorker getGodFather() {
        return godFather;
    }

    public ServerWorker getDrLector() {
        return drLector;
    }

    public ServerWorker getOrdMafia() {
        return ordMafia;
    }

    public ServerWorker getMayor() {
        return mayor;
    }

    public ServerWorker getDoctor() {
        return doctor;
    }

    public ServerWorker getInspector() {
        return inspector;
    }

    public ServerWorker getSniper() {
        return sniper;
    }

    public ServerWorker getPsycho() {
        return psycho;
    }

    public ServerWorker getStrong() {
        return strong;
    }

    public ServerWorker getOrdCity() {
        return ordCity;
    }
}
