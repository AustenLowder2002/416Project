package virtual.machine;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import static virtual.machine.JsonObject.readConfigFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DistanceVectorAlgorithm {
    private static final String CONFIG_FILE = "config.json";
    private static Map<String, Integer> distanceVector = new HashMap<>();
    private static Map<String, String> nextHop = new HashMap<>();
    private static Random random = new Random();

    public static void main(String[] args) {
        JsonObject config = readConfigFile(CONFIG_FILE);
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
            JsonArray links = entry.getValue().getAsJsonArray("links");
            for (JsonElement element : links) {
                String connectedTo = element.getAsString();
                distanceVector.put(connectedTo, Integer.MAX_VALUE); // Initialize with maximum value
                nextHop.put(connectedTo, null); // Initialize with null
            }
        }
        // Set distance to self as 0
        distanceVector.put(config.get("self").getAsString(), 0);
    }

    private static void runDistanceVectorAlgorithm() {
        boolean updated;
        do {
            updated = false;
            for (Map.Entry<String, Integer> entry : distanceVector.entrySet()) {
                String destination = entry.getKey();
                int distance = entry.getValue();

                for (Map.Entry<String, JsonElement> routerEntry : routers.entrySet()) {
                    String routerName = routerEntry.getKey();
                    JsonArray links = routerEntry.getValue().getAsJsonArray("links");
                    for (JsonElement element : links) {
                        String neighbor = element.getAsString();
                        int neighborDistance = distanceVector.get(neighbor);
                        if (distanceVector.containsKey(destination) && distanceVector.containsKey(neighbor)) {
                            int newDistance = neighborDistance + distanceVector.get(routerName);
                            if (newDistance < distanceVector.get(destination)) {
                                distanceVector.put(destination, newDistance);
                                nextHop.put(destination, routerName);
                                updated = true;
                            } else if (newDistance == distanceVector.get(destination)) {
                                // Randomly choose one path if distances are equal
                                if (random.nextBoolean()) {
                                    nextHop.put(destination, routerName);
                                }
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
