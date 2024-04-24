package virtual.machine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static virtual.machine.JsonObject.readConfigFile;

public class DistanceVectorAlgorithm {
    private static final String CONFIG_FILE = "config.json";
    private static Map<String, Integer> distanceVector = new HashMap<>();
    private static Map<String, String> nextHop = new HashMap<>();
    private static Random random = new Random();
    private static JsonObject config = readConfigFile(CONFIG_FILE);
    public static void main(String[] args) {
        if (config != null) {
            initializeDistanceVector(config);
            runDistanceVectorAlgorithm();
            printRoutingTable();
        }
    }

    private static void initializeDistanceVector(JsonObject config) {
        JsonObject routers = config.getAsJsonObject("routers");
        for (Map.Entry<String, JsonElement> entry : routers.entrySet()) {
            String routerName = entry.getKey();
            distanceVector.put(routerName, Integer.MAX_VALUE); // Initialize with maximum value
            nextHop.put(routerName, null); // Initialize with null
        }
        // Set distance to self as 0
        distanceVector.put(config.get("self").getAsString(), 0);
    }

    private static void runDistanceVectorAlgorithm() {
        boolean updated;
        do {
            updated = false;
            for (Map.Entry<String, Integer> entry : distanceVector.entrySet()) {
                String routerName = entry.getKey();
                int distance = entry.getValue();

                JsonObject routers = config.getAsJsonObject("routers");
                JsonArray links = routers.getAsJsonArray(routerName);
                for (JsonElement element : links) {
                    String neighbor = element.getAsString();
                    int neighborDistance = distanceVector.get(neighbor);
                    if (distanceVector.containsKey(routerName) && distanceVector.containsKey(neighbor)) {
                        int newDistance = neighborDistance + distanceVector.get(routerName);
                        if (newDistance < distanceVector.get(neighbor)) {
                            distanceVector.put(neighbor, newDistance);
                            nextHop.put(neighbor, routerName);
                            updated = true;
                        } else if (newDistance == distanceVector.get(neighbor)) {
                            // Randomly choose one path if distances are equal
                            if (random.nextBoolean()) {
                                nextHop.put(neighbor, routerName);
                            }
                        }
                    }
                }
            }
        } while (updated);
    }

    private static void printRoutingTable() {
        System.out.println("Routing Table:");
        System.out.println("Destination\t\tDistance\tNext Hop");
        for (Map.Entry<String, Integer> entry : distanceVector.entrySet()) {
            String destination = entry.getKey();
            int distance = entry.getValue();
            String next = nextHop.get(destination);
            System.out.println(destination + "\t\t\t" + distance + "\t\t" + next);
        }
    }
}
