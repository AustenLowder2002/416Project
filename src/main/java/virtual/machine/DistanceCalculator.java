package virtual.machine;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static virtual.machine.DistanceVector.routers;

public class DistanceCalculator {

    public static DistanceResult calculateDistance(Router sourceRouter, String destinationRouter) {
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> nextHops = new HashMap<>();

        boolean updated;
        Random random = new Random();

        do {
            updated = false;
            Map<Router, Map<String, Integer>> updates = new HashMap<>();

            for (Router router : routers.values()) {
                Map<String, Integer> potentialUpdates = new HashMap<>();
                Map<String, String> routeUpdates = new HashMap<>();

                for (String neighbor : router.getLinks()) {
                    if (routers.containsKey(neighbor)) {
                        Router neighborRouter = routers.get(neighbor);
                        for (Map.Entry<String, Integer> entry : neighborRouter.getDistances().entrySet()) {
                            String destination = entry.getKey();
                            int distance = entry.getValue() + 1;

                            int parentToNeighborDistance = router.getDistance(neighbor);
                            int totalDistance = parentToNeighborDistance + distance;

                            if (!router.hasRoute(destination) || totalDistance < router.getDistance(destination)) {
                                if (router.hasRoute(destination) && totalDistance == router.getDistance(destination)) {
                                    if (random.nextBoolean()) {
                                        potentialUpdates.put(destination, totalDistance);
                                        routeUpdates.put(destination, neighborRouter.getName());
                                        updated = true;
                                    }
                                } else {
                                    potentialUpdates.put(destination, totalDistance);
                                    routeUpdates.put(destination, neighborRouter.getName());
                                    updated = true;
                                }
                            }
                        }
                    }
                }
                for (Map.Entry<String, Integer> update : potentialUpdates.entrySet()) {
                    String destination = update.getKey();
                    int distance = update.getValue();
                    router.updateDistance(destination, distance, routeUpdates.get(destination));
                }
                updates.put(router, potentialUpdates);
            }

            for (Map.Entry<Router, Map<String, Integer>> entry : updates.entrySet()) {
                Router router = entry.getKey();
                Map<String, Integer> potentialUpdates = entry.getValue();
                for (Map.Entry<String, Integer> update : potentialUpdates.entrySet()) {
                    String destination = update.getKey();
                    int distance = update.getValue();
                    String nextHopRouterName = router.getRoute(destination);
                    router.updateDistance(destination, distance, nextHopRouterName);
                }
            }

            for (Router router : routers.values()) {
                router.clearRouteUpdates();
            }

        } while (updated);

        // Populate distances and nextHops maps with the calculated results
        for (Router router : routers.values()) {
            distances.put(router.getName(), router.getDistance(destinationRouter));
            nextHops.put(router.getName(), router.getRoute(destinationRouter));
        }
        System.out.println(distances);
        System.out.println(nextHops);
        return new DistanceResult(distances, nextHops);
    }
}
