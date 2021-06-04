import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class GameServer extends Thread{
    private static final int PLAYER_COUNT = 10;
    private final int port;

    private final ArrayList<ServerWorker> workers;
    private final ArrayList<ServerWorker> mafias;
    private final ArrayList<ServerWorker> citizens;

    public GameServer(int port){
        this.port = port;
        workers = new ArrayList<>();
        mafias = new ArrayList<>();
        citizens = new ArrayList<>();
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server Started.\nWating for clients to join...");

            for(int i = 0; i < PLAYER_COUNT; i++){
                Socket clientSocket = serverSocket.accept();
                ServerWorker worker = new ServerWorker(clientSocket);
                workers.add(worker);
                worker.start();
            }

            if(initGame())
                startGame();
            else
                System.err.println("Could not start game!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean initGame() {
        if(!giveRoles())
            return false;

        return true;
    }

    private boolean giveRoles() {
        if(workers.size() != 10)
            return false;

        // Mafias:
        workers.get(0).giveRole(Group.Mafia, Type.GodFather);
        workers.get(1).giveRole(Group.Mafia, Type.DrLector);
        workers.get(2).giveRole(Group.Mafia, Type.OrdMafia);

        mafias.add(workers.get(0));
        mafias.add(workers.get(1));
        mafias.add(workers.get(2));

        // Citizens:
        workers.get(3).giveRole(Group.Mafia, Type.GodFather);
        workers.get(4).giveRole(Group.Mafia, Type.GodFather);
        workers.get(0).giveRole(Group.Mafia, Type.GodFather);


        return true;
    }

    private void startGame() {
    }
}
