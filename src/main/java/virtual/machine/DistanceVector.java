package virtual.machine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Random;
import static virtual.machine.JsonObject.readConfigFile;
import java.util.HashMap;
import java.util.Map;

public class DistanceVector {

    private static final Map<String, Router> routers = new HashMap<>();
    private static final Map<String, String> subnets = new HashMap<>();

    public static void main(String[] args) {
        JsonObject config = readConfigFile("C:\\Users\\auste\\Documents\\GitHub\\416Project\\src\\Router.json");
        if (config != null) {
            parseRouters(config.getAsJsonArray("routers"));
            parseSubnets(config.getAsJsonObject("subnets"));

            // Debug: Print routers and links
//            System.out.println("Routers and Links:");
//            for (Router router : routers.values()) {
//                System.out.println("Router: " + router.getName());
//                for (String link : router.getLinks()) {
//                    System.out.println("  Link: " + link);
//                }
//            }

            calculateDistances();

            // Debug: Print routing tables
           printRoutingTables();
        }
    }



    public static void parseRouters(JsonArray routersArray) {
        for (JsonElement element : routersArray) {
            JsonObject routerObj = element.getAsJsonObject();
            String name = routerObj.get("name").getAsString();
            int port = routerObj.get("port").getAsInt();
            String routerIp = routerObj.get("ip").getAsString();
            Router router = new Router(name, port, routerIp);
            JsonArray linksArray = routerObj.getAsJsonArray("links");
            for (JsonElement link : linksArray) {
                String linkName = link.getAsString();
                // Check if the link is a valid router or subnet
                if (routers.containsKey(linkName) || linkName.startsWith("r")) {
                    router.addLink(linkName);
                } else {
                    //System.out.println("Invalid link for router " + name + ": " + linkName);
                }
            }
            routers.put(name, router);
        }
    }

    public static void parseSubnets(JsonObject subnetsObj) {
        for (Map.Entry<String, JsonElement> entry : subnetsObj.entrySet()) {
            subnets.put(entry.getKey(), entry.getValue().getAsString());
        }
    }



    public static void calculateDistances() {
        Random random = new Random();
        boolean updated;

        do {
            updated = false;
            Map<Router, Map<String, Integer>> updates = new HashMap<>();

            // Step 1: Calculate potential updates
            for (Router router : routers.values()) {
                Map<String, Integer> potentialUpdates = new HashMap<>();
                Map<String, String> routeUpdates = new HashMap<>(); // Track the route updates
                for (String neighbor : router.getLinks()) {
                    if (routers.containsKey(neighbor)) { // Check if neighbor is a router
                        Router neighborRouter = routers.get(neighbor);
                        for (Map.Entry<String, Integer> entry : neighborRouter.getDistances().entrySet()) {
                            String destination = entry.getKey();
                            int distance = entry.getValue() + 1;

                            // Calculate distance from the parent router to the destination
                            int parentToNeighborDistance = router.getDistance(neighbor);
                            int totalDistance = parentToNeighborDistance + distance;

                            if (!router.hasRoute(destination) || totalDistance < router.getDistance(destination)) {
                                // If there's already a route with the same distance, randomly choose one
                                if (router.hasRoute(destination) && totalDistance == router.getDistance(destination)) {
                                    if (random.nextBoolean()) {
                                        potentialUpdates.put(destination, totalDistance);
                                        routeUpdates.put(destination, neighborRouter.getName()); // Track route update
                                        updated = true;
                                    }
                                } else {
                                    potentialUpdates.put(destination, totalDistance);
                                    routeUpdates.put(destination, neighborRouter.getName()); // Track route update
                                    updated = true;
                                }
                            }
                        }
                    }
                }
                // Merge potential updates into existing distances
                for (Map.Entry<String, Integer> update : potentialUpdates.entrySet()) {
                    String destination = update.getKey();
                    int distance = update.getValue();
                    router.updateDistance(destination, distance, routeUpdates.get(destination));
                }
                updates.put(router, potentialUpdates); // Store potential distance updates
            }

            // Debug: Print potential updates
            //System.out.println("Potential Updates:");
            // for (Map.Entry<Router, Map<String, Integer>> entry : updates.entrySet()) {
            //     Router router = entry.getKey();
            //     System.out.println("Router: " + router.getName());
            //     for (Map.Entry<String, Integer> update : entry.getValue().entrySet()) {
            //         System.out.println("Destination: " + update.getKey() + ", Distance: " + update.getValue());
            //     }
            // }

            // Step 2: Apply updates
            for (Map.Entry<Router, Map<String, Integer>> entry : updates.entrySet()) {
                Router router = entry.getKey();
                Map<String, Integer> potentialUpdates = entry.getValue();
                for (Map.Entry<String, Integer> update : potentialUpdates.entrySet()) {
                    String destination = update.getKey();
                    int distance = update.getValue();
                    String nextHopRouterName = router.getRoute(destination); // Retrieve the next hop router name
                    router.updateDistance(destination, distance, nextHopRouterName); // Update distance and next hop router
                }
            }

            // Clear route updates for the next iteration
            for (Router router : routers.values()) {
                router.clearRouteUpdates();
            }

            // Debug: Print route updates
        /*System.out.println("Route Updates:");
        for (Router router : routers.values()) {
            Map<String, String> routeUpdates = router.getRouteUpdates();
            System.out.println("Router: " + router.getName());
            for (Map.Entry<String, String> update : routeUpdates.entrySet()) {
                System.out.println("Destination: " + update.getKey() + ", Next Hop: " + update.getValue());
            }
        }*/

        } while (updated);
    }







    public static void printRoutingTables() {
        for (Router router : routers.values()) {
            System.out.println("Routing table for Router " + router.getName() + ":");
            System.out.println("+-------------+----------+----------------+-------------+");
            System.out.println("| Destination | Distance | Next Hop Router| Route       |");
            System.out.println("+-------------+----------+----------------+-------------+");
            for (Map.Entry<String, Integer> entry : router.getDistances().entrySet()) {
                String destination = entry.getKey();
                int distance = entry.getValue();
                String nextHop = router.getRoute(destination);
                String properNextHop = routers.containsKey(nextHop) ? routers.get(nextHop).getName() : nextHop;
                String paddedDestination = String.format("%-12s", destination);
                String paddedDistance = String.format("%-9d", distance);
                String paddedNextHop = String.format("%-15s", properNextHop != null ? properNextHop : "Unknown");
                System.out.println("| " + paddedDestination + " | " + paddedDistance + " | " + paddedNextHop + " | " + nextHop + " |");
            }
            System.out.println("+-------------+----------+----------------+-------------+\n");
        }
    }
}

