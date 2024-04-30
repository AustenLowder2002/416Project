package virtual.machine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Router {
    private String name;
    private String ip;
    private int port;
    private String[] links;
    private String[] subnets;
    private Map<String, Map<String, Integer>> distanceVector;

    public Router(String name, String ip, int port, String[] links, String[] subnets) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.links = links;
        this.subnets = subnets != null ? subnets : new String[0];  // Ensure subnets are not null
        this.distanceVector = new HashMap<>();
        for (String subnet : this.subnets) {
            Map<String, Integer> entry = new HashMap<>();
            // Initialize the distance to itself as 0
            entry.put(name, 0);
            // Initialize the distance to connected subnets as 0
            for (String linkedRouter : links) {
                entry.put(linkedRouter, 0);
            }
            distanceVector.put(subnet, entry);
        }
    }

    public void sendDistanceVector(String neighborName) {
        JsonObject neighborInfo = getNeighborInfo(neighborName);
        if (neighborInfo == null) {
            System.err.println("Neighbor information not found for: " + neighborName);
            return;
        }
        String neighborIp = neighborInfo.get("ip").getAsString();
        int neighborPort = neighborInfo.get("port").getAsInt();

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress neighborAddress = InetAddress.getByName(neighborIp);
            JsonObject data = new JsonObject();
            for (Map.Entry<String, Map<String, Integer>> subnetEntry : distanceVector.entrySet()) {
                JsonObject entryObject = new JsonObject();
                for (Map.Entry<String, Integer> neighborEntry : subnetEntry.getValue().entrySet()) {
                    entryObject.addProperty(cleanString(neighborEntry.getKey()), neighborEntry.getValue());
                }
                data.add(cleanString(subnetEntry.getKey()), entryObject);
            }
            byte[] sendData = data.toString().getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, neighborAddress, neighborPort);
            socket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveDistanceVector() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] receiveData = new byte[1024];
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                String receivedData = new String(receivePacket.getData(), 0, receivePacket.getLength());
                // Check if the received data is a distance vector or a frame
                if (receivedData.contains("|")) {
                    // Parse the frame data
                    String[] frameParts = receivedData.split("\\|");
                    String frame = frameParts[0];
                    String sourceMAC = frameParts[1];
                    String destinationMAC = frameParts[2];
                    String path = frameParts[3];
                    // Handle the frame based on its source
                    if (path.startsWith("pc")) {
                        // Frame received from a PC
                        handleFrameFromPC(frame, sourceMAC, destinationMAC, path);
                    } else if (path.startsWith("switch")) {
                        // Frame received from a switch
                        handleFrameFromSwitch(frame, sourceMAC, destinationMAC, path);
                    } else {
                        // Unknown source, ignore the frame
                        System.err.println("Received frame with unknown source: " + path);
                    }
                } else {
                    // Parse the received distance vector and update routing table
                    JsonObject receivedVector = new JsonParser().parse(receivedData).getAsJsonObject();
                    updateDistanceVector(receivedVector);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void updateDistanceVector(JsonObject receivedVector) {
        for (Map.Entry<String, JsonElement> subnetEntry : receivedVector.entrySet()) {
            String subnet = cleanString(subnetEntry.getKey());
            JsonObject neighborInfo = subnetEntry.getValue().getAsJsonObject();
            if (neighborInfo.isJsonObject()) {
                for (Map.Entry<String, JsonElement> neighborEntry : neighborInfo.entrySet()) {
                    String neighbor = cleanString(neighborEntry.getKey());
                    int cost = neighborEntry.getValue().getAsInt();
                    if (distanceVector.containsKey(subnet) && distanceVector.get(subnet).containsKey(neighbor)) {
                        int currentCost = distanceVector.get(subnet).get(neighbor);
                        distanceVector.get(subnet).put(neighbor, Math.min(currentCost, cost + 1));
                    }
                }
            }
        }
    }

    private static String cleanString(String str) {
        return str.replaceAll("[\"\\[\\]]", "");
    }

    private JsonObject getNeighborInfo(String neighborName) {
        JsonObject config = readConfigFile("file.json");
        JsonObject neighborInfo = null;
        neighborName = cleanString(neighborName);
        for (int i = 0; i < Objects.requireNonNull(config).getAsJsonArray("routers").size(); i++) {
            JsonObject router = config.getAsJsonArray("routers").get(i).getAsJsonObject();
            String routerName = cleanString(router.get("name").getAsString());
            if (routerName.equals(neighborName)) {
                neighborInfo = router;
                break;
            }
        }
        return neighborInfo;
    }

    public synchronized void forwardFrame(String frame, String sourceMAC, String destinationMAC, String path) {
        // Extract the next hop from the path
        String[] pathNodes = path.split(" -> ");
        String nextHop = null;
        for (int i = 0; i < pathNodes.length; i++) {
            if (pathNodes[i].equals(name)) {
                if (i < pathNodes.length - 1) {
                    nextHop = pathNodes[i + 1];
                    break;
                }
            }
        }

        if (nextHop != null) {
            // Find the socket corresponding to the next hop router
            JsonObject nextHopInfo = getNeighborInfo(nextHop);
            if (nextHopInfo != null) {
                String nextHopIp = nextHopInfo.get("ip").getAsString();
                int nextHopPort = nextHopInfo.get("port").getAsInt();
                try (Socket nextHopSocket = new Socket(nextHopIp, nextHopPort)) {
                    PrintWriter out = new PrintWriter(nextHopSocket.getOutputStream(), true);
                    // Append the path to the frame
                    String frameWithPath = frame + "|" + sourceMAC + "|" + destinationMAC + "|" + path;
                    out.println(frameWithPath);
                    // Log the path in the terminal
                    System.out.println("Frame forwarded to " + nextHop + " with path: " + path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Next hop information not found.");
            }
        } else {
            System.out.println("Next hop not found.");
        }
    }


    public synchronized void handleFrameFromPC(String frame, String sourceMAC, String destinationMAC, String path) {
        // Forward the frame to the next hop router
        forwardFrame(frame, sourceMAC, destinationMAC, path);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please specify the router name as a command line argument.");
            return;
        }
        String routerName = args[0];
        JsonObject config = readConfigFile("file.json");
        JsonObject routerConfig = null;
        for (JsonElement element : Objects.requireNonNull(config.getAsJsonArray("routers"))) {
            JsonObject router = element.getAsJsonObject();
            if (router.get("name").getAsString().equals(routerName)) {
                routerConfig = router;
                break;
            }
        }
        if (routerConfig == null) {
            System.out.println("Router not found in the config.");
            return;
        }

        String[] links = getStringArray(String.valueOf(routerConfig.get("links")));
        String[] subnets = getStringArray(String.valueOf(routerConfig.get("subnets")));

        // Create an instance of the Router class with the retrieved links and subnets arrays
        Router router = new Router(cleanString(routerConfig.get("name").getAsString()),
                cleanString(routerConfig.get("ip").getAsString()),
                routerConfig.get("port").getAsInt(),
                links,
                subnets);

        // Start threads for sending and receiving distance vectors
        Thread receiveThread = new Thread(router::receiveDistanceVector);
        receiveThread.start();
        for (String neighborName : router.links) {
            // Remove brackets and quotes from neighborName
            neighborName = cleanString(neighborName);
            router.sendDistanceVector(neighborName);
        }
        while (true) {
            try {
                Thread.sleep(5000);
                for (String neighborName : router.links) {
                    router.sendDistanceVector(neighborName);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void handleFrameFromSwitch(String frame, String sourceMAC, String destinationMAC, String path) {
        // Forward the frame to the next hop router or PC
        forwardFrame(frame, sourceMAC, destinationMAC, path);
    }

    public static JsonObject readConfigFile(String filename) {
        try (FileReader reader = new FileReader(filename)) {
            JsonParser parser = new JsonParser();
            return parser.parse(reader).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String[] getStringArray(String str) {
        if (str == null) {
            return new String[0];
        }
        return str.split(",");
    }
}
