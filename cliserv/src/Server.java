import java.net.*;

class Server {
    private static DatagramSocket socket;

    public static void main(String args[]) throws Exception {
        byte[] data = new byte[300];
        final int SERVER_PORT = 9999;

        try {
            socket = new DatagramSocket(SERVER_PORT);
        } catch (BindException e) {
            System.out.println("Bind to local port failed");
            System.exit(1);
        }

        DatagramPacket udpPacket = new DatagramPacket(data, data.length);
        System.out.println("Listening for UDP requests (IPv6/IPv4). CTRL+C to terminate");

        while (true) {
            // Set
            udpPacket.setData(data);
            udpPacket.setLength(data.length);

            // Get Packet from a Client
            socket.receive(udpPacket);

            // Print Client Message
            String socketMessage = new String(udpPacket.getData(), 0, udpPacket.getLength());
            String socketAddress = udpPacket.getAddress().getHostAddress();
            int socketPort = udpPacket.getPort();
            System.out.println(socketAddress + ":" + socketPort + " - " + socketMessage);

            // Send Packet to All Clients
            socket.send(udpPacket);
        }
    }
}