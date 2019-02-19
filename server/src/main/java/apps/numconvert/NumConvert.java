package apps.numconvert;

import core.Shared;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

class NumConvert {
    static String APP_NAME = "NumConvert";
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

        Thread udpReceiver = new Thread(new NumConvertReceive(datagramSocket));
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
                    if ("STORE".equalsIgnoreCase(command.split(" ")[1])) {
                        NumConvertReceive.storeMapa(command.split(" ")[2], command.split(" ")[3]);
                        System.out.println("Store successful");

                    } else if ("LIST".equalsIgnoreCase(command.split(" ")[1])) {
                        NumConvertReceive.listMapa();

                    } else if ("FETCH".equalsIgnoreCase(command.split(" ")[1])) {
                        System.out.println(NumConvertReceive.fetchFromMap(command.split(" ")[2]));

                    } else if ("ERASE".equalsIgnoreCase(command.split(" ")[1])) {
                        NumConvertReceive.eraseFromMapa(command.split(" ")[2]);
                        System.out.println("Store successful");
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

class NumConvertReceive implements Runnable {
    private DatagramSocket datagramSocket;
    private String APP_NAME = "NumConvert";

    public NumConvertReceive(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
    }

    public static HashMap<String, String> mapa = new HashMap<String, String>();

    public static void storeMapa(String parts1, String parts2) {
        mapa.put(parts1, parts2);
        String result = "RESULT: The value " + parts1 + " was stored in the key " + parts2 + ".";
    }

    public static void listMapa() {
        for (Map.Entry<String, String> entry : mapa.entrySet()) {
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
        }
    }

    public static void eraseFromMapa(String part) {
        mapa.remove(part);
        String message = "The key " + part + " was erased.";
    }

    public static String fetchFromMap(String part) {
        return "Fetching key " + part + ": " + mapa.get(part);
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

                if ("LIST".equalsIgnoreCase(parts[1])) {
                    String result = "BIN / OCT / DEC / HEX";
                    udpPacket = new DatagramPacket(result.getBytes(), result.length(), datagramPacket.getAddress(), Shared.PORT);
                    Shared.sendToIP(datagramSocket, udpPacket, datagramPacket.getAddress());

                } else if ("CONVERT".equalsIgnoreCase(parts[1])) {

                    Integer number = Integer.parseInt(parts[2]);
                    String outputFormat = parts[3];

                    int radix = 10;

                    if (outputFormat.equals("BIN")) {
                        radix = 2;
                    } else if (outputFormat.equalsIgnoreCase("OCT")) {
                        radix = 8;
                    } else if (outputFormat.equalsIgnoreCase("HEX")) {
                        radix = 16;
                    }

                    String result = "Result of converting to " + outputFormat + ": " + Integer.toString(number, radix);

                    udpPacket = new DatagramPacket(result.getBytes(), result.length(), datagramPacket.getAddress(), Shared.PORT);
                    Shared.sendToIP(datagramSocket, udpPacket, datagramPacket.getAddress());


                } else {
                    System.out.println(command);
                }
            }

        }
    }
}