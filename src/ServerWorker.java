import java.io.*;
import java.net.Socket;

public class ServerWorker extends Thread{
    private Socket connectionSocket;
    private OutputStream outputStream;
    private InputStream inputStream;

    private Group group;
    private Type type;

    private boolean isAlive;
    private boolean isSleep;
    private boolean isMute;

    public ServerWorker(Socket connectionSocket){
        this.connectionSocket = connectionSocket;
        isAlive = true;
        isSleep = true;
        isMute = false;
    }

    public void giveRole(Group group, Type type){
        this.group = group;
        this.type = type;
    }

    @Override
    public void run() {
        System.out.println("Server Worker Started.");
        try {
            inputStream = connectionSocket.getInputStream();
            outputStream = connectionSocket.getOutputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while((line = reader.readLine()) != null){
                System.out.println("Client Says: " + line);
                outputStream.write("Got your Message!\n".getBytes());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
