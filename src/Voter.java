import com.sun.istack.internal.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;

public class Voter {
    private GameServer server;

    public Voter(GameServer server){
        this.server = server;
    }

    public ServerWorker dayVote() throws InterruptedException, IOException {
        ArrayList<ServerWorker> options = new ArrayList<>();
        ArrayList<String> optionsString = new ArrayList<>();

        for(ServerWorker worker : server.getWorkers())
            if(!worker.isDead()) {
                options.add(worker);
                optionsString.add(worker.getUserName());
            }


        ArrayBlockingQueue<Integer> preResults = new ArrayBlockingQueue<>(GameServer.PLAYER_COUNT);

        String voteBody = "Choose the one you think is Mafia";

        ArrayList<ServerWorker> voters = new ArrayList<>();
        for(ServerWorker worker : server.getWorkers())
            if(!worker.isDead() && !worker.isMute())
                voters.add(worker);

        // starting votes
        for(ServerWorker worker : voters)
            worker.getVote(voteBody, GameServer.DAY_VOTE_TIME, optionsString, preResults);

        // waiting to vote...
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
    public boolean mayorVote(String userNameToKill) throws IOException, InterruptedException {
        if(userNameToKill == null)
            return false;

        ServerWorker mayor = server.findWorker(Group.City, Type.Mayor);
        if(mayor == null)
            return false;

        // preparing vote
        ArrayList<String> options = new ArrayList<>();
        options.add("Yes");
        options.add("No");

        ArrayBlockingQueue<Integer> preResults = new ArrayBlockingQueue<Integer>(1);

        String voteBody = "Players voted to kill " + userNameToKill + ", as the mayor, do you agree?";

        // voting
        mayor.getVote(voteBody, GameServer.MAYOR_VOTE_TIME, options, preResults);

        // waiting to vote...
        for(int i = 0; i < GameServer.MAYOR_VOTE_TIME/GameServer.TIME_TICK; i++){
            Thread.sleep(GameServer.TIME_TICK);
            if(mayor.hasVoted())
                break;
        }

        // closing the vote
        mayor.closeVote();

        try{
            int result = preResults.take();
            return result == 1;
        } catch (InterruptedException e){
            e.printStackTrace();
            return false;
        }
    }

    private boolean hasAllVoted(ArrayList<ServerWorker> voters) {
        boolean hasVoted = true;
        for(ServerWorker worker : voters)
            hasVoted &= worker.hasVoted();

        return hasVoted;
    }
}
