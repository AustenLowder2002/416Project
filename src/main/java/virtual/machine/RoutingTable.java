package virtual.machine;

import java.util.HashMap;
import java.util.Map;

public class RoutingTable {
    private final Map<String, Integer> subnetDistances;

    public RoutingTable() {
        this.subnetDistances = new HashMap<>();
        initializeRoutingTable();
    }

    private void initializeRoutingTable() {
        // Add subnet distances to the routing table
        subnetDistances.put("192.168.1.0", 1);
        subnetDistances.put("192.168.2.0", 2);
        subnetDistances.put("192.168.3.0", 3);
        // Add more subnets as needed
    }

    public int getDistance(String subnet) {
        // Get the distance for the given subnet from the routing table
        Integer distance = subnetDistances.get(subnet);
        return (distance != null) ? distance : -1; // Return -1 if subnet not found
    }
}
