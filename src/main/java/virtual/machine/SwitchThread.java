package virtual.machine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

class SwitchThread extends Thread {
    private final Socket clientSocket;
    private final Switch parentSwitch;

    public SwitchThread(Socket clientSocket, Switch parentSwitch) {
        this.clientSocket = clientSocket;
        this.parentSwitch = parentSwitch;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String neighborName = in.readLine();
            parentSwitch.addNeighbor(neighborName, clientSocket);
            System.out.println("Connected with neighbor: " + neighborName); // Print a message indicating successful connection

            // Continue to read frames as long as the connection is valid
            while (!clientSocket.isClosed()) {
                String frame = in.readLine();
                if (frame != null) {
                    System.out.println("Received frame: " + frame);
                    // Process the frame and perform Ethernet learning
                    // Extract source and destination MAC addresses
                    String[] frameData = frame.split("\\|");
                    String sourceMAC = frameData[1];
                    String destinationMAC = frameData[2];

                    // Check if destination MAC is a PC or a router
                    if (destinationMAC.startsWith("PC")) {
                        // Forward the frame to the PC
                        parentSwitch.forwardFrameToPC(frame, destinationMAC);
                    } else if (destinationMAC.startsWith("R")) {
                        // Forward the frame to the router
                        parentSwitch.forwardFrameToRouter(frame, destinationMAC);
                    } else {
                        System.err.println("Invalid destination MAC: " + destinationMAC);
                    }
                    System.out.println("Forwarding frame: " + frame);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

