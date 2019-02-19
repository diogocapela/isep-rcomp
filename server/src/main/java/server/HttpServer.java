package server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer implements Runnable {
    static private final String BASE_FOLDER = "www";
    static private ServerSocket sock;

    public static void main(String args[]) throws Exception {

    }


    @Override
    public void run() {
        Socket cliSock;

        int PORT = 3000;


        try {
            sock = new ServerSocket(PORT);
        } catch (IOException ex) {
            System.out.println("Server failed to open local port " + PORT);
            System.exit(1);
        }
        while (true) {
            try {
                cliSock = sock.accept();
                HttpRequest req = new HttpRequest(cliSock, BASE_FOLDER);
                req.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
