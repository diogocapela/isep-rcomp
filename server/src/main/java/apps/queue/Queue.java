package apps.queue;

import core.Shared;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

class Queue {

    static String APP_NAME = "QUEUE";
    static InetAddress broadcastAddress;
    static DatagramSocket datagramSocket;


    public static void main(String[] args) throws Exception {
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

        //Broadcast to all (Start)
        datagramSocket.setBroadcast(true);
        String START_APP = "@START " + APP_NAME;
        udpPacket = new DatagramPacket(START_APP.getBytes(), START_APP.length(), broadcastAddress, Shared.PORT);
        datagramSocket.send(udpPacket);

        Thread udpReceiver = new Thread(new QueueReceive(datagramSocket));
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
                    if ("ENQUEUE".equalsIgnoreCase(command.split(" ")[1]) || "E".equalsIgnoreCase(command.split(" ")[1])) {

                        String message = QueueReceive.storeQueue(command.split(" ")[2], command.split(" "));
                        System.out.println(message);
                        System.out.println("Store successful");


                    } else if ("DEQUEUE".equalsIgnoreCase(command.split(" ")[1]) || "D".equalsIgnoreCase(command.split(" ")[1])) {

                        String message = QueueReceive.oldestElementQueue(command.split(" ")[2]);
                        System.out.println(message);


                    } else if ("REMOVE".equalsIgnoreCase(command.split(" ")[1]) || "R".equalsIgnoreCase(command.split(" ")[1])) {

                        String message = QueueReceive.removeAll(command.split(" ")[2]);
                        System.out.println(message);


                    } else if ("LIST".equalsIgnoreCase(command.split(" ")[1]) || "L".equalsIgnoreCase(command.split(" ")[1])) {
                        QueueReceive.list();
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
        //BroadCast to all (LEAVE)
        String LEAVE_APP = "@LEAVE " + APP_NAME;
        udpPacket.setData(LEAVE_APP.getBytes());
        udpPacket.setLength(LEAVE_APP.length());
        Shared.sendToAll(datagramSocket, udpPacket);

        datagramSocket.close();
        udpReceiver.join();

    }
}

 class QueueReceive implements Runnable {

    private DatagramSocket datagramSocket;
    private String APP_NAME = "QUEUE";

    public QueueReceive(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
    }

    public static HashMap<String, java.util.Queue<String>> mapaQueues = new HashMap<String, java.util.Queue<String>>();

    /*pesquisa o identficador da queue se exister guarda os elementos da queue
    se nao exister cria uma queue nova com os elementos
    */
    public static String storeQueue(String identifier, String[] elementos) {
        if (mapaQueues.containsKey(identifier)) {
            for (int i = 3; i < elementos.length; i++) {
                mapaQueues.get(identifier).add(elementos[i]);
            }
            return "Number of elements are: " + mapaQueues.get(identifier).size();
        } else {
            java.util.Queue<String> nova = new LinkedList<String>();
            for (int i = 3; i < elementos.length; i++) {
                nova.add(elementos[i]);
            }
            mapaQueues.put(identifier, nova);
            return "Number of elements are: " + mapaQueues.get(identifier).size();
        }
    }

    /*
     Pesquisa o identificador e se existir retorna como mensagem o elemento mais antigo(head) e apaga-o da queue
     Se nao encontrar o identificador manda mensagem de erro
      */
    public static String oldestElementQueue(String identifier) {
        if (mapaQueues.containsKey(identifier)) {
            if (mapaQueues.get(identifier).isEmpty()) {
                return "Queue is empty";
            } else {
                String head = mapaQueues.get(identifier).poll();
                return "The oldest element was: " + head + " and was erased";
            }
        } else return "The identifier don´t exist";
    }

    /*
    Procura o identificador e se encontrar apaga tudo
    Se nao encontrar o identificador da mensagem de erro
     */
    public static String removeAll(String identifier) {
        if (mapaQueues.containsKey(identifier)) {
            mapaQueues.remove(identifier);
            return "All elements of " + identifier + " was erased";
        } return "The identifier don´t exist";
    }

    public static void list() {
        int i = 1;
        if (!mapaQueues.isEmpty()) {
            for (Map.Entry<String, java.util.Queue<String>> mapa : mapaQueues.entrySet()) {
                System.out.println("Identifier " + i + " is " + mapa.getKey() + " and have " + mapa.getValue().size() + " elements");
                i++;
            }
        } else System.out.println("There isn´t data");
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

                if ("ENQUEUE".equalsIgnoreCase(parts[1]) || "E".equalsIgnoreCase(command.split(" ")[1])) {

                    String result = storeQueue(parts[2], parts);
                    udpPacket = new DatagramPacket(result.getBytes(), result.length(), datagramPacket.getAddress(), Shared.PORT);
                    Shared.sendToIP(datagramSocket, udpPacket, datagramPacket.getAddress());

                } else if ("DEQUEUE".equalsIgnoreCase(parts[1]) || "D".equalsIgnoreCase(command.split(" ")[1]) ) {

                    String message = oldestElementQueue(parts[2]);
                    udpPacket = new DatagramPacket(message.getBytes(), message.length(), datagramPacket.getAddress(), Shared.PORT);
                    Shared.sendToIP(datagramSocket, udpPacket, datagramPacket.getAddress());

                } else if ("REMOVE".equalsIgnoreCase(parts[1]) || "R".equalsIgnoreCase(command.split(" ")[1])) {

                    String message = removeAll(parts[2]);
                    udpPacket = new DatagramPacket(message.getBytes(), message.length(), datagramPacket.getAddress(), Shared.PORT);
                    Shared.sendToIP(datagramSocket, udpPacket, datagramPacket.getAddress());

                } else if ("LIST".equalsIgnoreCase(parts[1]) || "L".equalsIgnoreCase(command.split(" ")[1])) {
                    String message;
                    int i = 1;
                    if (!mapaQueues.isEmpty()) {
                        for (Map.Entry<String, java.util.Queue<String>> mapa : mapaQueues.entrySet()) {
                            message = "Identifier " + i + " is " + mapa.getKey() + " and have " + mapa.getValue().size() + " elements";
                            udpPacket = new DatagramPacket(message.getBytes(), message.length(), datagramPacket.getAddress(), Shared.PORT);
                            Shared.sendToIP(datagramSocket, udpPacket, datagramPacket.getAddress());
                            i++;
                        }
                    } else {
                        message = "There isn´t data";
                        udpPacket = new DatagramPacket(message.getBytes(), message.length(), datagramPacket.getAddress(), Shared.PORT);
                        Shared.sendToIP(datagramSocket, udpPacket, datagramPacket.getAddress());
                    }
                } else System.out.println(command);
            }
        }
    }
}

