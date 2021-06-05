import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

public class Voter {
    private GameServer server;

    public Voter(GameServer server){
        this.server = server;
    }

    public ServerWorker dayVote() throws InterruptedException, IOException {
        ArrayList<ServerWorker> options = new ArrayList<>();

        for(ServerWorker worker : server.getWorkers())
            if(!worker.isDead())
                options.add(worker);


        ArrayBlockingQueue<Integer> preResults = new ArrayBlockingQueue<>(GameServer.PLAYER_COUNT);

        String voteBody = "Choose the one you think is Mafia";

        ArrayList<ServerWorker> voters = new ArrayList<>();
        for(ServerWorker worker : server.getWorkers())
            if(!worker.isDead() && !worker.isMute())
                voters.add(worker);

        // starting votes
        for(ServerWorker worker : voters)
            worker.getVote(voteBody, GameServer.DAY_VOTE_TIME, options, preResults);

        // waiting to vote...
        //Thread.sleep(GameServer.DAY_VOTE_TIME);
        for(int i = 0; i < GameServer.DAY_VOTE_TIME/GameServer.TIME_TICK; i++){
            Thread.sleep(GameServer.TIME_TICK);
            if(hasAllVoted(voters))
                break;
        }

        //closing votes
        for(ServerWorker worker : voters)
            worker.closeVote();

        ServerWorker winner = null;
        int mostVotes = 0;

        HashMap<ServerWorker, Integer> resultCount = new HashMap<ServerWorker, Integer>();
        for(ServerWorker worker : options)
            resultCount.put(worker, 0);

        for(int index : preResults)
            if(index > 0 && index <= options.size()) {
                ServerWorker worker = options.get(index - 1);
                resultCount.replace(worker, resultCount.get(worker)+1);
            }

        for(ServerWorker worker : options)
            if(mostVotes < resultCount.get(worker)){
                mostVotes = resultCount.get(worker);
                winner = worker;
            }

        int winnerCount = 0;
        if(mostVotes != 0){
            for(ServerWorker worker : options)
                if(resultCount.get(worker) == mostVotes)
                    winnerCount++;
        }

        return winnerCount != 1 ? null : winner;
    }

    // true -> remove dayVote
    public boolean mayorVote(){
        return false;
    }

    private boolean hasAllVoted(ArrayList<ServerWorker> voters) {
        boolean hasVoted = true;
        for(ServerWorker worker : voters)
            hasVoted &= worker.hasVoted();

        return hasVoted;
    }
}
