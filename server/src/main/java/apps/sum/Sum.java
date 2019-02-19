package apps.sum;


import core.Shared;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


class Sum {

    static String APP_NAME = "SUM";
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

        Thread udpReceiver = new Thread(new SumReceive(datagramSocket));
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
                    if (command.split(" ")[1].equalsIgnoreCase("SUM")) {
                        int num1 = Integer.parseInt(command.split(" ")[2]);
                        int num2 = Integer.parseInt(command.split(" ")[3]);
                        int num3 = num1 + num2;
                        System.out.println("SUM of " + num1 + num2 + "is " + num3);
                    }
                } else {
                    InetAddress appAddress = Shared.getIPFromApp(appToComunicate);

                    if (appAddress != null) {
                        System.out.println("found ip address");
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

class SumReceive implements Runnable {
    private DatagramSocket datagramSocket;
    private String APP_NAME = "SUM";

    public SumReceive(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
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
            } else if (command.startsWith("SUM")) {
                String[] parts = command.split(" ");
                // Show command
                System.out.println(currPeerAddress + " " + command);
                if (command.split(" ")[1].equalsIgnoreCase("SUM")) {
                    int num1 = Integer.parseInt(command.split(" ")[2]);
                    int num2 = Integer.parseInt(command.split(" ")[3]);
                    int num3 = num1 + num2;
                    String Snum3 = "" + num3;
                    udpPacket = new DatagramPacket(Snum3.getBytes(), Snum3.length(), datagramPacket.getAddress(), Shared.PORT);
                    Shared.sendToIP(datagramSocket, udpPacket, datagramPacket.getAddress());

                }
            } else {
                System.out.println(command);
            }
        }
    }
}

