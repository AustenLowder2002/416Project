package virtual.machine;

import java.util.HashMap;
import java.util.Map;

import static virtual.machine.DistanceVector.routers;

public class DistanceCalculator {

    public static DistanceResult calculateDistance(Router sourceRouter, String destinationRouter) {
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> nextHops = new HashMap<>();

        // Initialize distances and nextHops with default values
        for (Router router : routers.values()) {
            distances.put(router.getName(), Integer.MAX_VALUE);
            nextHops.put(router.getName(), null);
        }

        // Set the distance to itself to 0
        distances.put(sourceRouter.getName(), 0);

        // Update distances and nextHops based on the constant hop count of one
        for (Router router : routers.values()) {
            for (String neighbor : router.getLinks()) {
                if (routers.containsKey(neighbor)) {
                    Router neighborRouter = routers.get(neighbor);
                    int distanceToNeighbor = 1; // Constant hop count of one
                    int totalDistance = distances.get(router.getName()) + distanceToNeighbor;
                    if (totalDistance < distances.get(neighborRouter.getName())) {
                        distances.put(neighborRouter.getName(), totalDistance);
                        nextHops.put(neighborRouter.getName(), router.getName());
                    }
                    // Bidirectional link handling
                    int reverseTotalDistance = distances.get(neighborRouter.getName()) + distanceToNeighbor;
                    if (reverseTotalDistance < distances.get(router.getName())) {
                        distances.put(router.getName(), reverseTotalDistance);
                        nextHops.put(router.getName(), neighborRouter.getName());
                    }
                }
            }
        }

        // Print distances and nextHops for debugging
        System.out.println("Distances: " + distances);
        System.out.println("Next Hops: " + nextHops);

        // Return the calculated distances and nextHops
        return new DistanceResult(distances, nextHops);
    }
}
