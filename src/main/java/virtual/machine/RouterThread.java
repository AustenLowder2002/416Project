package virtual.machine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;

import static virtual.machine.DistanceCalculator.calculateDistance;

public class RouterThread extends Thread {
    private final Socket clientSocket;
    private final Router parentRouter;

    public RouterThread(Socket clientSocket, Router parentRouter) {
        this.clientSocket = clientSocket;
        this.parentRouter = parentRouter;
        start();
    }

    public void start() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Handle user input
            handleUserInput(in, out);

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


    private void handleUserInput(BufferedReader in, PrintWriter out) throws IOException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            // Check if there's any incoming frame
            if (in.ready()) {
                String receivedFrame = in.readLine();
                System.out.println("Received frame: " + receivedFrame);
                // Process the received frame as needed
            }

            // Prompt the user to enter the destination Router
            System.out.print("Enter the destination Router: ");
            String destinationRouter = scanner.nextLine();

            // Calculate the distance to the destination router
            DistanceResult distanceToDestination = DistanceCalculator.calculateDistance(parentRouter, destinationRouter);

            // Get the distances and next hops as strings
            Map<String, Integer> distances = distanceToDestination.getDistances();
            Map<String, String> nextHops = distanceToDestination.getNextHops();

            // Convert distances and next hops to strings
            StringBuilder distancesString = new StringBuilder();
            StringBuilder nextHopsString = new StringBuilder();
            for (Map.Entry<String, Integer> entry : distances.entrySet()) {
                distancesString.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
            }
            for (Map.Entry<String, String> entry : nextHops.entrySet()) {
                nextHopsString.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
            }

            // Constructing the frame with proper format including distance
            String frame = distancesString.toString() + "|" + nextHopsString.toString() + "|" + destinationRouter + "|" + parentRouter.getPort();
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