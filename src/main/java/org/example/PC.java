package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class PC {

    private String name;
    private String ip;
    private String mac;
    private int port;



    public PC(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.mac = generateMacAddress();
    }

    public void start(InetAddress sIp, int sPort) {

        try {
            Socket socket = new Socket(sIp, sPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(name);  // Send PC name to the switch

            new PCReceiverThread(socket).start();

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Enter a message: ");
                String message = scanner.nextLine();

                System.out.print("Enter the destination MAC address: ");
                String destinationMAC = scanner.nextLine();

                String frame = message + "|" + name + "|" + destinationMAC;
                out.println(frame);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        if (args.length != 2) {
            System.out.println("Syntax: TCPFileServiceClient <ServerIP> <ServerPort>");
            return;
        }
        int sPort = Integer.parseInt(args[1]);
        InetAddress sIp = InetAddress.getByName(args[0]);


        // Read the configuration file
        JsonObject config = readConfigFile("C:/Users/auste/IdeaProjects/help/src/main/java//file.json");

        // Create PC objects based on config
        JsonArray devicesArray = config.getAsJsonArray("devices");
        for (int i = 0; i < devicesArray.size(); i++) {
            JsonObject deviceObject = devicesArray.get(i).getAsJsonObject();
            String pcName = deviceObject.get("name").getAsString();
            String pcIp = deviceObject.get("ip").getAsString();
            int pcPort = deviceObject.get("port").getAsInt();

            PC currentPC = new PC(pcName, pcIp, pcPort);
            new Thread(() -> currentPC.start(sIp, sPort)).start();
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
    private String generateMacAddress() {
        String namePart = name.substring(0, Math.min(name.length(), 6));
        String ipPart = ipToMacFormat(ip);

        System.out.println(namePart + ipPart);
        return namePart + ipPart;
    }

    private String ipToMacFormat(String ip) {

        String[] ipParts = ip.split("\\.");
        StringBuilder macBuilder = new StringBuilder();

        for (String part : ipParts) {
            int decimal = Integer.parseInt(part);
            String hex = Integer.toHexString(decimal).toUpperCase();
            macBuilder.append(hex.length() == 1 ? "0" + hex : hex);
        }

        return macBuilder.toString();
    }
}


class PCReceiverThread extends Thread {

    private Socket socket;

    public PCReceiverThread(Socket socket) {
        this.socket = socket;
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

                if (destinationMAC.equals("PC's Virtual MAC")) {
                    System.out.println("Received message from " + sourceMAC + ": " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

