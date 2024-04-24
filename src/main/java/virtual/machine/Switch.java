package virtual.machine;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

import static virtual.machine.JsonObject.readConfigFile;

public class Switch {
    private final String name;
    private final int port;
    private final Map<String, Socket> neighbors = new HashMap<>();

    public Switch(String name, int port) {
        this.name = name;
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
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
        if (args.length != 1) {
            System.out.println("Syntax: Switch <SwitchName>");
            return;
        }
        String switchName = args[0];

        // Read the configuration file
        JsonObject config = readConfigFile("file.json");

        // Find the switch configuration based on the provided switch name
        String switchIp = null;
        int switchPort = 0;
        assert config != null;
        JsonArray switchesArray = config.getAsJsonArray("switches");
        for (int i = 0; i < switchesArray.size(); i++) {
            JsonObject switchObject = switchesArray.get(i).getAsJsonObject();
            String name = switchObject.get("name").getAsString();
            if (name.equals(switchName)) {
                switchIp = switchObject.get("ip").getAsString();
                switchPort = switchObject.get("port").getAsInt();
                break;
            }
        }

        if (switchIp == null) {
            System.out.println("Switch with name '" + switchName + "' not found in the configuration.");
            return;
        }

        Switch currentSwitch = new Switch(switchName, switchPort);
        currentSwitch.start();
    }
}