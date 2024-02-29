package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Switch {
    private String name;
    private String ip;
    private int port;
    private Map<String, Socket> neighbors = new HashMap<>();

    public Switch(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    public void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Switch " + name + " is running on port " + port);

            while (true) {
                System.out.println("Waiting for a connection...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from: " + clientSocket.getInetAddress());
                new SwitchThread(clientSocket, this).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public synchronized void addNeighbor(String neighborName, Socket socket) {
        neighbors.put(neighborName, socket);
    }

    public synchronized void broadcastFrame(String frame, String sourceMAC, String destinationMAC) {
        for (Socket neighborSocket : neighbors.values()) {
            try {
                PrintWriter out = new PrintWriter(neighborSocket.getOutputStream(), true);
                out.println(frame + "|" + sourceMAC + "|" + destinationMAC);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        // Read the configuration file
        JsonObject config = readConfigFile("C:\\Users\\denni\\OneDrive\\Documents\\GitHub\\416Project\\src\\main\\java\\file.json");

        // Create Switch objects based on config file
        JsonArray switchesArray = config.getAsJsonArray("switches");
        for (int i = 0; i < switchesArray.size(); i++) {
            JsonObject switchObject = switchesArray.get(i).getAsJsonObject();
            String switchName = switchObject.get("name").getAsString();
            String switchIp = switchObject.get("ip").getAsString();
            int switchPort = switchObject.get("port").getAsInt();

            Switch currentSwitch = new Switch(switchName, switchIp, switchPort);
            new Thread(currentSwitch::start).start();
        }
    }

    private static JsonObject readConfigFile(String filename) {
        try (FileReader reader = new FileReader(filename)) {
            JsonParser parser = new JsonParser();
            return parser.parse(reader).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

class SwitchThread extends Thread {
    private Socket clientSocket;
    private Switch parentSwitch;

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

            while (true) {
                String frame = in.readLine();
                if (frame != null) {
                    System.out.println("Received frame: " + frame); // Print received frame for debugging
                    // Process the frame and perform Ethernet learning
                    // Extract source and destination MAC addresses
                    String[] frameData = frame.split("\\|");
                    String sourceMAC = frameData[1];
                    String destinationMAC = frameData[2];

                    // Broadcast the frame to other neighbors
                    parentSwitch.broadcastFrame(frame, sourceMAC, destinationMAC);
                    System.out.println("Broadcasted frame: " + frame); // Print broadcasted frame for debugging
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
