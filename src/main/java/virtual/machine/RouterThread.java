package virtual.machine;

import java.net.*;

public class RouterThread extends Thread {
    private final Router parentRouter;

    public RouterThread(Router parentRouter) {
        this.parentRouter = parentRouter;
    }

    public void run() {
        // Place your UDP packet sending and receiving logic here
        // You can use DatagramSocket to send and receive UDP packets
        // Example:
        try (DatagramSocket socket = new DatagramSocket()) {
            // Send UDP packet
            InetAddress destinationAddress = InetAddress.getByName("destination_ip");
            int destinationPort = 1234;
            byte[] sendData = "Hello, world!".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, destinationAddress, destinationPort);
            socket.send(sendPacket);

            // Receive UDP packet
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Received message: " + receivedMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Router router = new Router("RouterName", 1234);
        RouterThread routerThread = new RouterThread(router);
        routerThread.start();
    }
}

