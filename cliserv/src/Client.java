import java.io.*;
import java.net.*;

class Client {
    private static InetAddress serverIP;

    public static void main(String args[]) throws Exception {

        // Configure server address and port

        String SERVER_ADDRESS = null;
        int SERVER_PORT = 9999;

        if (args.length > 1) {
            SERVER_ADDRESS = args[0];
            SERVER_PORT = Integer.parseInt(args[1]);
        }
        System.out.println("Server IP address/DNS name: " + SERVER_ADDRESS);
        System.out.println("Server port: " + SERVER_PORT);

        try {
            serverIP = InetAddress.getByName(SERVER_ADDRESS);
        } catch (UnknownHostException ex) {
            System.out.println("Invalid server IP address/DNS supplied: " + SERVER_ADDRESS);
            System.exit(1);
        }

        // Create socket connection and UDP packet

        byte[] data = new byte[300];
        DatagramSocket sock = new DatagramSocket();
        int TIMEOUT = 3;
        sock.setSoTimeout(1000 * TIMEOUT);
        DatagramPacket udpPacket = new DatagramPacket(data, data.length, serverIP, SERVER_PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        String message;

        while (true) {
            System.out.print("Message (\"exit\" to quit): ");
            message = in.readLine();
            if (message.compareTo("exit") == 0) break;

            // Send Packet to Server
            udpPacket.setData(message.getBytes());
            udpPacket.setLength(message.length());
            sock.send(udpPacket);

            // Get Packet From Server
            udpPacket.setData(data);
            udpPacket.setLength(data.length);

            try {
                sock.receive(udpPacket);
            } catch (SocketTimeoutException ex) {
                System.out.println("No reply from server.");
            }

            String socketMessage = new String(udpPacket.getData(), 0, udpPacket.getLength());
            String socketAddress = udpPacket.getAddress().getHostAddress();
            int socketPort = udpPacket.getPort();
            System.out.println(socketAddress + ":" + socketPort + " - " + socketMessage);
        }

        sock.close();
    }
}