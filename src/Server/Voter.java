package Server;

import Enums.Group;
import Enums.Type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Voter {
    private final GameServer server;

    public Voter(GameServer server) {
        this.server = server;
    }

    public ServerWorker dayVote() throws InterruptedException, IOException {
        ArrayList<ServerWorker> options = new ArrayList<>();
        ArrayList<String> optionsString = new ArrayList<>();

        for (ServerWorker worker : server.getWorkers())
            if (!worker.isDead()) {
                options.add(worker);
                optionsString.add(worker.getUserName());
            }

        // skip option
        options.add(null);
        optionsString.add("Skip");


        String voteBody = "Choose the one you think is Mafia";

        ArrayList<ServerWorker> voters = new ArrayList<>();
        for (ServerWorker worker : server.getWorkers())
            if (!worker.isDead())
                voters.add(worker);

        // starting votes
        for (ServerWorker worker : voters) {
            // one can not vote himself
            int indexOfUserName = optionsString.indexOf(worker.getUserName());
            if (indexOfUserName != -1)
                optionsString.remove(indexOfUserName);

            worker.getVote(voteBody, GameServer.DAY_VOTE_TIME, optionsString);

            if (indexOfUserName != -1)
                optionsString.add(indexOfUserName, worker.getUserName());
        }

        // waiting to vote...
        for (int i = 0; i < GameServer.DAY_VOTE_TIME / GameServer.TIME_TICK /*&& !hasAllVoted(voters)*/; i++)
            Thread.sleep(GameServer.TIME_TICK);                         // now everyone can change vote!

        //closing votes
        for (ServerWorker worker : voters)
            worker.closeVote();

        ServerWorker winner = null;
        int mostVotes = 0;
        int skipped = 0;

        HashMap<ServerWorker, Integer> resultCount = new HashMap<>();
        for (ServerWorker worker : options)
            if (worker != null)
                resultCount.put(worker, 0);

        for (ServerWorker worker : voters) {
            // one can not vote himself
            int indexOfUserName = options.indexOf(worker);
            if (indexOfUserName != -1)
                options.remove(indexOfUserName);

            int vote = worker.catchVote();

            if (vote != 0 && vote != options.size()) {

                ServerWorker voted = options.get(vote - 1);
                resultCount.replace(voted, resultCount.get(voted) + 1);

                server.getWorkerHandler().msgToAllAwake(
                        GameServer.serverMsgFromString(worker.getUserName() + " voted to " + voted.getUserName())
                        /*,worker*/
                );

            } else {

                skipped++;
                server.getWorkerHandler().msgToAllAwake(
                        GameServer.serverMsgFromString(worker.getUserName() + " skipped voting")
                        /*,worker*/
                );
            }

            if (indexOfUserName != -1)
                options.add(indexOfUserName, worker);
        }

        for (ServerWorker worker : options)
            if (worker != null && mostVotes < resultCount.get(worker)) {
                mostVotes = resultCount.get(worker);
                winner = worker;
            }

        int winnerCount = 0;
        if (mostVotes != 0) {
            for (ServerWorker worker : options)
                if (worker != null && resultCount.get(worker) == mostVotes)
                    winnerCount++;
        }

        if (mostVotes <= skipped)
            winner = null;

        return winnerCount != 1 ? null : winner;
    }

    // true -> remove dayVote
    public boolean mayorVote(String userNameToKill) throws IOException, InterruptedException {
        if (userNameToKill == null)
            return false;

        ServerWorker mayor = server.getWorkerHandler().findWorker(Group.City, Type.Mayor);
        if (mayor == null || mayor.isDead())
            return true;

        // preparing vote
        ArrayList<String> options = new ArrayList<>();
        options.add("Yes");
        options.add("No");

        String voteBody = "Players voted to kill " + userNameToKill + ", as the mayor, do you agree?";

        // voting
        mayor.getVote(voteBody, GameServer.MAYOR_VOTE_TIME, options);

        // waiting to vote...
        for (int i = 0; i < GameServer.MAYOR_VOTE_TIME / GameServer.TIME_TICK && !mayor.hasVoted(); i++)
            Thread.sleep(GameServer.TIME_TICK);

        // closing the vote
        mayor.closeVote();

        // getting vote
        int result = mayor.catchVote();
        return result != 2; // always will kill, except when mayor says NO; if said nothing, do the kill!
    }


    public ServerWorker mafiaVote() throws IOException, InterruptedException {
        ArrayList<ServerWorker> options = new ArrayList<>();
        ArrayList<String> optionsString = new ArrayList<>();

        for (ServerWorker worker : server.getWorkerHandler().getCity())
            if (!worker.isDead()) {
                options.add(worker);
                optionsString.add(worker.getUserName());
            }

        String voteBody = "Choose the citizen you want to shoot tonight";

        ServerWorker voter = server.getWorkerHandler().findWorker(Group.Mafia, Type.GodFather);
        if (voter == null || voter.isDead()) {
            voter = server.getWorkerHandler().findWorker(Group.Mafia, Type.DrLector);
            if (voter == null || voter.isDead())
                voter = server.getWorkerHandler().findWorker(Group.Mafia, Type.OrdMafia);
        }

        if (voter == null || voter.isDead())
            return null;


        // starting votes
        voter.getVote(voteBody, GameServer.MAFIA_KILL_TIME, optionsString);

        // waiting to vote...
        for (int i = 0; i < GameServer.MAFIA_KILL_TIME / GameServer.TIME_TICK && !voter.hasVoted(); i++)
            Thread.sleep(GameServer.TIME_TICK);


        //closing votes
        voter.closeVote();

        try {
            int index = voter.catchVote();
            return options.get(index - 1);

        } catch (IndexOutOfBoundsException e) {
            // e.printStackTrace();
            return null;
        }
    }


    public ServerWorker lectorVote(boolean canHealSelf) throws IOException, InterruptedException {
        ArrayList<ServerWorker> options = new ArrayList<>();
        ArrayList<String> optionsString = new ArrayList<>();

        for (ServerWorker worker : server.getWorkerHandler().getMafias())
            if (!worker.isDead()) {
                options.add(worker);
                optionsString.add(worker.getUserName());
            }

        ServerWorker voter = server.getWorkerHandler().findWorker(Group.Mafia, Type.DrLector);
        if (voter == null || voter.isDead())
            return null;

        if (!canHealSelf) {
            options.remove(voter);
            optionsString.remove(voter.getUserName());
        }

        if (options.isEmpty()) {
            System.err.println("NO OPTIONS FOR DR.LECTOR");
            return null;
        }

        // skip option
        options.add(null);
        optionsString.add("Skip");


        String voteBody = "Choose the mafia you want to heal tonight";


        // starting votes
        voter.getVote(voteBody, GameServer.MAFIA_HEAL_TIME, optionsString);

        // waiting to vote...
        for (int i = 0; i < GameServer.MAFIA_HEAL_TIME / GameServer.TIME_TICK && !voter.hasVoted(); i++)
            Thread.sleep(GameServer.TIME_TICK);

        //closing votes
        voter.closeVote();

        try {
            int index = voter.catchVote();
            return options.get(index - 1);

        } catch (IndexOutOfBoundsException e) {
            // e.printStackTrace();
            return null;
        }
    }


    public ServerWorker doctorVote(boolean canHealSelf) throws IOException, InterruptedException {
        ArrayList<ServerWorker> options = new ArrayList<>();
        ArrayList<String> optionsString = new ArrayList<>();

        for (ServerWorker worker : server.getWorkerHandler().getWorkers())
            if (!worker.isDead()) {
                options.add(worker);
                optionsString.add(worker.getUserName());
            }


        ServerWorker voter = server.getWorkerHandler().findWorker(Group.City, Type.Doctor);
        if (voter == null || voter.isDead())
            return null;

        if (!canHealSelf) {
            options.remove(voter);
            optionsString.remove(voter.getUserName());
        }

        if (options.isEmpty()) {
            System.err.println("NO OPTIONS FOR DOCTOR");
            return null;
        }

        // skip option
        options.add(null);
        optionsString.add("Skip");


        String voteBody = "Choose the person you want to heal tonight";


        // starting votes
        voter.getVote(voteBody, GameServer.DR_HEAL_TIME, optionsString);


        // waiting to vote...
        for (int i = 0; i < GameServer.DR_HEAL_TIME / GameServer.TIME_TICK && !voter.hasVoted(); i++)
            Thread.sleep(GameServer.TIME_TICK);


        //closing votes
        voter.closeVote();


        try {
            int index = voter.catchVote();
            return options.get(index - 1);

        } catch (IndexOutOfBoundsException e) {
            // e.printStackTrace();
            return null;
        }
    }


    public ServerWorker inspectorVote() throws IOException, InterruptedException {
        ArrayList<ServerWorker> options = new ArrayList<>();
        ArrayList<String> optionsString = new ArrayList<>();

        ServerWorker voter = server.getWorkerHandler().findWorker(Group.City, Type.Inspector);
        if (voter == null || voter.isDead())
            return null;

        for (ServerWorker worker : server.getWorkerHandler().getWorkers())
            if (!worker.isDead() && worker != voter) {
                options.add(worker);
                optionsString.add(worker.getUserName());
            }

        String voteBody = "Choose the person you want to inspect tonight";


        // starting votes
        voter.getVote(voteBody, GameServer.INSPECT_TIME, optionsString);


        // waiting to vote...
        for (int i = 0; i < GameServer.INSPECT_TIME / GameServer.TIME_TICK && !voter.hasVoted(); i++)
            Thread.sleep(GameServer.TIME_TICK);


        //closing votes
        voter.closeVote();


        try {
            int index = voter.catchVote();
            return options.get(index - 1);

        } catch (IndexOutOfBoundsException e) {
            // e.printStackTrace();
            return null;
        }
    }


    public ServerWorker sniperVote() throws IOException, InterruptedException {
        ArrayList<ServerWorker> options = new ArrayList<>();
        ArrayList<String> optionsString = new ArrayList<>();

        ServerWorker voter = server.getWorkerHandler().findWorker(Group.City, Type.Sniper);
        if (voter == null || voter.isDead())
            return null;

        for (ServerWorker worker : server.getWorkerHandler().getWorkers())
            if (!worker.isDead() && worker != voter) {
                options.add(worker);
                optionsString.add(worker.getUserName());
            }

        // skip option
        options.add(null);
        optionsString.add("Skip");

        String voteBody = "Choose the person you want to snipe tonight";


        // starting votes
        voter.getVote(voteBody, GameServer.INSPECT_TIME, optionsString);


        // waiting to vote...
        for (int i = 0; i < GameServer.SNIPE_TIME / GameServer.TIME_TICK && !voter.hasVoted(); i++)
            Thread.sleep(GameServer.TIME_TICK);


        //closing votes
        voter.closeVote();


        try {
            int index = voter.catchVote();
            return options.get(index - 1);

        } catch (IndexOutOfBoundsException e) {
            // e.printStackTrace();
            return null;
        }
    }


    public ServerWorker psychoVote() throws IOException, InterruptedException {
        ArrayList<ServerWorker> options = new ArrayList<>();
        ArrayList<String> optionsString = new ArrayList<>();

        ServerWorker voter = server.getWorkerHandler().findWorker(Group.City, Type.Psycho);
        if (voter == null || voter.isDead())
            return null;

        for (ServerWorker worker : server.getWorkerHandler().getWorkers())
            if (!worker.isDead() && worker != voter) {
                options.add(worker);
                optionsString.add(worker.getUserName());
            }

        // skip option
        options.add(null);
        optionsString.add("Skip");

        String voteBody = "Choose the person you want to mute for next day";


        // starting votes
        voter.getVote(voteBody, GameServer.PSYCHO_TIME, optionsString);


        // waiting to vote...
        for (int i = 0; i < GameServer.PSYCHO_TIME / GameServer.TIME_TICK && !voter.hasVoted(); i++)
            Thread.sleep(GameServer.TIME_TICK);


        //closing votes
        voter.closeVote();


        try {
            int index = voter.catchVote();
            return options.get(index - 1);

        } catch (IndexOutOfBoundsException e) {
            // e.printStackTrace();
            return null;
        }
    }

    public boolean strongVote() throws IOException, InterruptedException {

        ServerWorker voter = server.getWorkerHandler().findWorker(Group.City, Type.Strong);
        if (voter == null || voter.isDead())
            return true;

        // preparing vote
        ArrayList<String> options = new ArrayList<>();
        options.add("Yes");
        options.add("No");

        String voteBody = "Do you want to get the dead log? (you can do it 2 times at all)";

        // voting
        voter.getVote(voteBody, GameServer.STRONG_TIME, options);

        // waiting to vote...
        for (int i = 0; i < GameServer.STRONG_TIME / GameServer.TIME_TICK && !voter.hasVoted(); i++)
            Thread.sleep(GameServer.TIME_TICK);

        // closing the vote
        voter.closeVote();

        // getting vote
        int result = voter.catchVote();
        return result == 1; // never get log, except when he says YES

    }

    private boolean hasAllVoted(ArrayList<ServerWorker> voters) {
        for (ServerWorker worker : voters)
            if (!worker.hasVoted())
                return false;

        return true;
    }
}
