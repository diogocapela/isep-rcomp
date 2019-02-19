package apps.stack;

import core.Shared;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

class StackApp {

    static String APP_NAME = "StackApp";
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

        Thread udpReceiver = new Thread(new StackReceive(datagramSocket));
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
                    if ("PUSH".equalsIgnoreCase(command.split(" ")[1])) {
                        StackReceive.pushMapa(command.split(" ")[2], command.split(" "));
                        System.out.println("Store successful");

                    } else if ("LIST".equalsIgnoreCase(command.split(" ")[1])) {
                        StackReceive.listMapa();

                    } else if ("POP".equalsIgnoreCase(command.split(" ")[1])) {
                        String message = StackReceive.popMapa(command.split(" ")[2]);
                        System.out.println(message);
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

class StackReceive implements Runnable {

    private DatagramSocket datagramSocket;
    private String APP_NAME = "StackApp";

    public StackReceive(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
    }

    public static HashMap<String, Stack<String>> mapaStack = new HashMap<String, Stack<String>>();

    public static String pushMapa(String identifier, String[] content) {
        if (mapaStack.containsKey(identifier)) {
            for (int i = 3; i < content.length; i++) {
                mapaStack.get(identifier).add(content[i]);
            }
            return "Number of elements are: " + mapaStack.get(identifier).size();
        } else {
            Stack<String> nova = new Stack<String>();
            for (int i = 3; i < content.length; i++) {
                nova.add(content[i]);
            }
            mapaStack.put(identifier, nova);
            return "Number of elements are: " + mapaStack.get(identifier).size();
        }
    }

    public static void listMapa() {
        int i = 1;
        if (!mapaStack.isEmpty()) {
            for (Map.Entry<String, Stack<String>> mapa : mapaStack.entrySet()) {

                System.out.println("Stack " + i + " - identifier : " + mapa.getKey() + " and have " + mapa.getValue().size() + " elements : " + mapa.getValue().toString());
                i++;
            }
        } else {
            System.out.println("There isnt data");
        }
    }

    public static String popMapa(String part) {
        int index = 0;
        if (mapaStack.containsKey(part)) {
            //elemento do topo da pilha  removido.
            index = mapaStack.get(part).size();
            String partErased = mapaStack.get(part).get(index - 1);
            mapaStack.get(part).remove(index - 1);
            //verifica se a pilha ficou vazia
            sizeStack(part);
            return "The key " + partErased + " was erased.";

        } else {
            return "There ins't data";
        }
    }

    public static void sizeStack(String part) {

        if (mapaStack.get(part).isEmpty()) {
            mapaStack.remove(part);
        }
    }

    public static String fetchFromMap(String part) {
        return "Fetching key " + part + ": " + mapaStack.get(part);
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
            } else if (command.startsWith("@LEAVE")) { //peer exit
                Shared.removeIP(datagramPacket.getAddress());
            } else if (command.startsWith("@APP")) {
                String splitCommand[] = command.split(" ");
                Shared.addIP(datagramPacket.getAddress(), splitCommand[1]);
            } else {
                String[] parts = command.split(" ");
                // Show command
                System.out.println(currPeerAddress + " " + command);
                if ("PUSH".equalsIgnoreCase(parts[1])) {

                    String result = pushMapa(parts[2], parts);
                    udpPacket = new DatagramPacket(result.getBytes(), result.length(), datagramPacket.getAddress(), Shared.PORT);
                    Shared.sendToIP(datagramSocket, udpPacket, datagramPacket.getAddress());

                } else if ("POP".equalsIgnoreCase(parts[1])) {

                    String message = popMapa(parts[2]);
                    udpPacket = new DatagramPacket(message.getBytes(), message.length(), datagramPacket.getAddress(), Shared.PORT);
                    Shared.sendToIP(datagramSocket, udpPacket, datagramPacket.getAddress());

                } else if ("LIST".equalsIgnoreCase(parts[1]) || "L".equalsIgnoreCase(command.split(" ")[1])) {
                    String message;
                    int i = 1;
                    if (!mapaStack.isEmpty()) {
                        for (Map.Entry<String, Stack<String>> mapa : mapaStack.entrySet()) {
                            message = "Identifier " + i + " is " + mapa.getKey() + " and have " + mapa.getValue().size() + " elements";
                            udpPacket = new DatagramPacket(message.getBytes(), message.length(), datagramPacket.getAddress(), Shared.PORT);
                            Shared.sendToIP(datagramSocket, udpPacket, datagramPacket.getAddress());
                            i++;
                        }
                    } else {
                        message = "There isn't data";
                        udpPacket = new DatagramPacket(message.getBytes(), message.length(), datagramPacket.getAddress(), Shared.PORT);
                        Shared.sendToIP(datagramSocket, udpPacket, datagramPacket.getAddress());
                    }
                } else {
                    System.out.println(command);
                }
            }
        }
    }
}
