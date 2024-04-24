package virtual.machine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

class PCReceiverThread extends Thread {
    private final Socket socket;
    private final String mac;

    public PCReceiverThread(Socket socket, String mac) {
        this.socket = socket;
        this.mac = mac;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (true) {
                String receivedFrame = in.readLine();
                // Process the received frame and print the message if the destination MAC matches
                // Extract source and destination MAC addresses
                String[] frameData = receivedFrame.split("\\|");
                String sourceMAC = frameData[1];
                String destinationMAC = frameData[2];
                String message = frameData[0];

                if (destinationMAC.equals(mac)) {
                    System.out.println("Received message from " + sourceMAC + ": " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}