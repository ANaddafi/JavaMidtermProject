package Server;

import Enums.Group;
import Enums.Type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Voter class.
 * Contains methods for voting.
 */
public class Voter {
    private final GameServer server;

    /**
     * Voter constructor.
     *
     * @param server the server
     */
    public Voter(GameServer server) {
        this.server = server;
    }


    /**
     * Day vote.
     * All ALIVE player can vote
     * winner may be null if there are more than one winner!
     *
     * @return vote winner, or null if there are more than one winner
     * @throws InterruptedException If there is any unhandled exceptions while sleeping
     * @throws IOException          If there is any unhandled exceptions while sending/reading messages
     */
    public ServerWorker dayVote() throws InterruptedException, IOException {
        // options
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

        // voters
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

        // processing results
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


    /**
     * Mayor vote.
     * Result is 'YES' or 'NO'
     * Always do the kill, except for when mayor says 'NO'
     *
     * @param userNameToKill the player to kill
     * @return true, if mayor says to kill, or says nothing
     * @throws IOException          If there is any unhandled exceptions while sending/reading messages
     * @throws InterruptedException If there is any unhandled exceptions while sleeping
     */
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


    /**
     * Mafia vote.
     * Only godfather can vote.
     * If he's dead, drLector,
     * If drLector is dead, ordMafia.
     * Only citizens can be shot
     *
     * @return the citizen to shoot
     * @throws IOException          If there is any unhandled exceptions while sending/reading messages
     * @throws InterruptedException If there is any unhandled exceptions while sleeping
     */
    public ServerWorker mafiaVote() throws IOException, InterruptedException {
        // options
        ArrayList<ServerWorker> options = new ArrayList<>();
        ArrayList<String> optionsString = new ArrayList<>();

        for (ServerWorker worker : server.getWorkerHandler().getCity())
            if (!worker.isDead()) {
                options.add(worker);
                optionsString.add(worker.getUserName());
            }

        String voteBody = "Choose the citizen you want to shoot tonight";

        // choosing voter
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


    /**
     * DrLector vote.
     * Only can heal mafias.
     * Can heal self only once.
     * Can skip.
     *
     * @param canHealSelf whether he can heal himself
     * @return mafia to heal
     * @throws IOException          If there is any unhandled exceptions while sending/reading messages
     * @throws InterruptedException If there is any unhandled exceptions while sleeping
     */
    public ServerWorker lectorVote(boolean canHealSelf) throws IOException, InterruptedException {
        // options
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


    /**
     * Doctor vote.
     * Can heal both mafias or citizens.
     * Can heal self only once.
     * Can skip.
     *
     * @param canHealSelf whether he can heal himself
     * @return player to heal
     * @throws IOException          If there is any unhandled exceptions while sending/reading messages
     * @throws InterruptedException If there is any unhandled exceptions while sleeping
     */
    public ServerWorker doctorVote(boolean canHealSelf) throws IOException, InterruptedException {
        // options
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


    /**
     * Inspector vote.
     * Chooses someone to ask.
     *
     * @return the player he wants to inspect
     * @throws IOException          If there is any unhandled exceptions while sending/reading messages
     * @throws InterruptedException If there is any unhandled exceptions while sleeping
     */
    public ServerWorker inspectorVote() throws IOException, InterruptedException {
        // options
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


    /**
     * Sniper vote.
     * Chooses one to snipe.
     * If snipes a city, GameServer will kill him instead.
     * Can skip.
     *
     * @return the player to snipe
     * @throws IOException          If there is any unhandled exceptions while sending/reading messages
     * @throws InterruptedException If there is any unhandled exceptions while sleeping
     */
    public ServerWorker sniperVote() throws IOException, InterruptedException {
        // options
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


    /**
     * Psycho vote.
     * Chooses one to make mute for one day.
     * Can skip.
     *
     * @return the player to make mute
     * @throws IOException          If there is any unhandled exceptions while sending/reading messages
     * @throws InterruptedException If there is any unhandled exceptions while sleeping
     */
    public ServerWorker psychoVote() throws IOException, InterruptedException {
        // options
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


    /**
     * Strong vote.
     * He can choose to get query of dead players.
     * He can only get query twice.
     *
     * @return true, if he wants to get query
     * @throws IOException          If there is any unhandled exceptions while sending/reading messages
     * @throws InterruptedException If there is any unhandled exceptions while sleeping
     */
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


    /**
     * Checks whether everyone in voters list hase voted
     * @param voters list of voters
     * @return true, if all have voted
     */
    private boolean hasAllVoted(ArrayList<ServerWorker> voters) {
        for (ServerWorker worker : voters)
            if (!worker.hasVoted())
                return false;

        return true;
    }
}
