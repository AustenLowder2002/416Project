package virtual.machine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class RouterThread extends Thread {
    private final Socket clientSocket;
    private final Router parentRouter;

    public RouterThread(Socket clientSocket, Router parentRouter) {
        this.clientSocket = clientSocket;
        this.parentRouter = parentRouter;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Read the name of the connected router
            String neighborName = in.readLine();
            parentRouter.addNeighbor(neighborName, clientSocket);
            System.out.println("Connected with neighbor: " + neighborName); // Print a message indicating successful connection

            // Handle user input
            handleUserInput(out);

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

                    // Broadcast the frame to other neighbors
                    parentRouter.broadcastFrame(frame, sourceMAC, destinationMAC);
                    System.out.println("Broadcasting frame: " + frame);
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
    private void handleUserInput(PrintWriter out) throws IOException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter the destination Router: ");
            String destinationRouter = scanner.nextLine();

            // Calculate the distance to the destination router
            int distanceToDestination = parentRouter.getDistance(destinationRouter);

            // Constructing the frame with proper format including distance
            String frame = distanceToDestination + "|" + destinationRouter + "|" + parentRouter.getPort();
            System.out.println("Sending frame: " + frame); // Print the frame for debugging

            // Send the frame to the connected router
            out.println(frame);
            out.flush(); // Ensure the message is sent immediately

            // Introduce a delay to give the user time to receive a message before new input is requested
            try {
                Thread.sleep(1000); // Sleep for 1000 milliseconds (1 second)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
