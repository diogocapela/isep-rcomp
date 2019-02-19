package core;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class Shared {
    // Private

    private static HashMap<InetAddress, String> peers = new HashMap<InetAddress, String>();

    // Public

    public static final String BROADCAST_STRING = "255.255.255.255";
    public static final int PORT = 9999;

    public static synchronized void addIP(InetAddress ip, String appName) {
        peers.put(ip, appName);
    }

    public static synchronized void removeIP(InetAddress ip) {
        peers.remove(ip);
    }

    public static synchronized InetAddress getIPFromApp(String appName) {
        for (Map.Entry<InetAddress, String> entry : peers.entrySet()) {
            if(entry.getValue().equalsIgnoreCase(appName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static synchronized void printPeers() {
        System.out.println("List of Connected Peers IP Addresses:");
        int i = 1;
        for (Map.Entry<InetAddress, String> entry : peers.entrySet()) {
            System.out.println("#" + i + " " + entry.getKey() + " - " + entry.getValue());
            i = i + 1;
        }
        System.out.println();
    }

    public static synchronized void sendToAll(DatagramSocket datagramSocket, DatagramPacket datagramPacket) {
        try {
            for (Map.Entry<InetAddress, String> entry : peers.entrySet()) {
                datagramPacket.setAddress(entry.getKey());
                datagramSocket.send(datagramPacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void sendToIP(DatagramSocket datagramSocket, DatagramPacket datagramPacket, InetAddress ip) {
        try {
            datagramPacket.setAddress(ip);
            datagramSocket.send(datagramPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
