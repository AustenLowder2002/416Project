package virtual.machine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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

            // Debug: Print sent distance vector
            System.out.println("Sent Distance Vector to " + neighborName + ":");
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
                JsonObject receivedVector = new JsonParser().parse(receivedData).getAsJsonObject();
                updateDistanceVector(receivedVector);
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
                System.out.println("Updated distance vector for subnet: " + subnet);
                for (Map.Entry<String, JsonElement> neighborEntry : neighborInfo.entrySet()) {
                    String neighbor = cleanString(neighborEntry.getKey());
                    int cost = neighborEntry.getValue().getAsInt();
                    System.out.println("Cost: " + cost);
                    if (distanceVector.containsKey(subnet) && distanceVector.get(subnet).containsKey(neighbor)) {
                        System.out.println("NeighborInfo is a JSON object.");
                        System.out.println("Updating distance for subnet: " + subnet + ", neighbor: " + neighbor);
                        int currentCost = distanceVector.get(subnet).get(neighbor);
                        distanceVector.get(subnet).put(neighbor, Math.min(currentCost, cost + 1));
                        System.out.println("Next hop: " + neighbor + ", Distance: " + (cost + 1));
                    }
                }
            } else {
                System.err.println("Invalid neighbor information for subnet: " + subnet);
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

        // Debug: Print initial routing table
        System.out.println("Initial Routing Table:");
        for (Map.Entry<String, Map<String, Integer>> entry : router.distanceVector.entrySet()) {
            System.out.println("Subnet: " + cleanString(entry.getKey()));
            Map<String, Integer> neighborDistances = entry.getValue();
            for (Map.Entry<String, Integer> neighborEntry : neighborDistances.entrySet()) {
                System.out.println("- Neighbor: " + cleanString(neighborEntry.getKey()) + ", Distance: " + neighborEntry.getValue());
            }
        }

        // Start threads for sending and receiving distance vectors
        Thread receiveThread = new Thread(router::receiveDistanceVector);
        receiveThread.start();
        for (String neighborName : router.links) {
            // Remove brackets and quotes from neighborName
            neighborName = cleanString(neighborName);
            System.out.println("Neighbor name: " + neighborName); // Debugging print
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
