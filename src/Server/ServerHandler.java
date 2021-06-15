package Server;

/**
 * ServerHandler class.
 * Starts game servers.
 */
public class ServerHandler {

    /**
     * Main method
     * @param args args
     */
    public static void main(String[] args) {
        // starting servers:
        new GameServer(8181).start();
    }
}
