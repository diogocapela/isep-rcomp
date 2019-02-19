package apps.hash;

import core.Shared;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

class Hash {

    static String APP_NAME = "Hash";
    static InetAddress broadcastAddress;
    static DatagramSocket datagramSocket;

    public static void main(String args[]) throws Exception {
        String command;
        DatagramPacket udpPacket;

        try {
            datagramSocket = new DatagramSocket(Shared.PORT);
        } catch (IOException ex) {
            System.out.println("Failed to open local port");
            System.exit(1);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        broadcastAddress = InetAddress.getByName(Shared.BROADCAST_STRING);

        System.out.println("APP " + APP_NAME + " is now running...");

        // Broadcast to all (START)
        datagramSocket.setBroadcast(true);
        String START_APP = "@START " + APP_NAME;
        udpPacket = new DatagramPacket(START_APP.getBytes(), START_APP.length(), broadcastAddress, Shared.PORT);
        datagramSocket.send(udpPacket);

        Thread udpReceiver = new Thread(new HashReceive(datagramSocket));
        udpReceiver.start();
        while (true) {
            command = in.readLine();
            if (command.compareTo("EXIT") == 0) {
                break;
            } else if (command.compareTo("LIST") == 0) {
                Shared.printPeers();
            } else {
                String appToComunicate = command.split(" ")[0];

                System.out.println("Searching for app, name is " + appToComunicate);

                if (appToComunicate.equalsIgnoreCase(APP_NAME)) {
                    System.out.println("Doing operations locally");
                    if ("DIGEST".equalsIgnoreCase(command.split(" ")[1])|| "D".equalsIgnoreCase(command.split(" ")[1])) {
                        HashReceive.createMessage(command.split(" ")[2], command.split(" ")[3]);
                        
                        

                        System.out.println("Store successful");

                    } else if ("LIST".equalsIgnoreCase(command.split(" ")[1])) {
                        HashReceive.listMapa();

                    }
                } else {
                    InetAddress appAddress = Shared.getIPFromApp(appToComunicate);

                    if (appAddress != null) {

                        udpPacket.setData(command.getBytes());
                        udpPacket.setLength(command.length());
                        Shared.sendToIP(datagramSocket, udpPacket, appAddress);
                    } else {
                        // if there was no app at the start of the string
                        System.out.println("ERROR: App was not found!");
                    }
                }
            }
        }
        // Broadcast to all (LEAVE)
        String LEAVE_APP = "@LEAVE " + APP_NAME;
        udpPacket.setData(LEAVE_APP.getBytes());
        udpPacket.setLength(LEAVE_APP.length());
        Shared.sendToAll(datagramSocket, udpPacket);

        datagramSocket.close();
        udpReceiver.join();
    }
}

class HashReceive implements Runnable {

    private DatagramSocket datagramSocket;
    private String APP_NAME = "Hash";

    public HashReceive(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
    }

    public static HashMap<String, String> mapa = new HashMap<String, String>();

    public static byte[] inputToBytes(String input) {
        byte[] byteInput = input.getBytes();
        return byteInput;
    }

    public static MessageDigest createMessage(String hashAlgorithm, String byteInput) throws NoSuchAlgorithmException  {
        MessageDigest messageDigest = MessageDigest.getInstance(hashAlgorithm);
        
        byte[] digest = messageDigest.digest(byteInput.getBytes());

        return messageDigest;
    }

    public static void listMapa() {
        for (Map.Entry<String, String> entry : mapa.entrySet()) {
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
        }
    }

    public void run() {
        DatagramPacket udpPacket;
        byte[] data = new byte[300];
        String command;
        DatagramPacket datagramPacket;
        InetAddress currPeerAddress;
        datagramPacket = new DatagramPacket(data, data.length);
        while (true) {
            datagramPacket.setData(data);
            datagramPacket.setLength(data.length);
            try {
                datagramSocket.receive(datagramPacket);

            } catch (IOException e) {
                return;
            }
            currPeerAddress = datagramPacket.getAddress();
            // COMMAND
            command = new String(datagramPacket.getData(), 0, datagramPacket.getLength());

            if (command.startsWith("@START")) {
                Shared.addIP(currPeerAddress, command.split(" ")[1]);
                try {

                    String identifier = "@APP " + APP_NAME;
                    datagramPacket.setData(identifier.getBytes());
                    datagramPacket.setLength(identifier.length());
                    datagramSocket.send(datagramPacket);
                } catch (IOException ex) {
                    return;
                }
            } else if (command.startsWith("@LEAVE")) { // peer exit
                Shared.removeIP(datagramPacket.getAddress());
            } else if (command.startsWith("@APP")) {
                String splitCommand[] = command.split(" ");
                Shared.addIP(datagramPacket.getAddress(), splitCommand[1]);
            } else {
                String[] parts = command.split(" ");
                // Show command
                System.out.println(currPeerAddress + " " + command);

                if ("DIGEST".equalsIgnoreCase(parts[1])) {
                    
                    try {
                        MessageDigest result = createMessage(parts[2], parts[3]);
                        udpPacket = new DatagramPacket(result.digest(), result.getDigestLength(), datagramPacket.getAddress(), Shared.PORT);
                        Shared.sendToIP(datagramSocket, udpPacket, datagramPacket.getAddress());
                    } catch (NoSuchAlgorithmException ex) {
                        Logger.getLogger(HashReceive.class.getName()).log(Level.SEVERE, null, ex);
                    }

                } else if ("LIST".equalsIgnoreCase(parts[1])) {

                    String message;
                    int i = 1;
                    if (!mapa.isEmpty()) {
                        for (Map.Entry<String, String> mapa : mapa.entrySet()) {
                            message = "Identifier " + i + " is " + mapa.getKey() + " and have " + mapa.getKey().length() + " elements";
                            udpPacket = new DatagramPacket(message.getBytes(), message.length(), datagramPacket.getAddress(), Shared.PORT);
                            Shared.sendToIP(datagramSocket, udpPacket, datagramPacket.getAddress());
                            i++;

                    }
                    } else {
                        message = "There isnt data";
                        udpPacket = new DatagramPacket(message.getBytes(), message.length(), datagramPacket.getAddress(), Shared.PORT);
                        Shared.sendToIP(datagramSocket, udpPacket, datagramPacket.getAddress());
                    }
                } else System.out.println(command);
            }
        }
    }
}

