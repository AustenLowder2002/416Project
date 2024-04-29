package virtual.machine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Random;
import static virtual.machine.JsonObject.readConfigFile;
import java.util.HashMap;
import java.util.Map;

public class DistanceVector {

    static final Map<String, Router> routers = new HashMap<>();
    private static final Map<String, String> subnets = new HashMap<>();
    static JsonObject config = readConfigFile("file.json");

    public static void bingBong(String sourceRouter, String destinationRouter) {


        parseRouters(config.getAsJsonArray("routers"));
        parseSubnets(config.getAsJsonObject("subnets"));


        calculateDistances();
        getRoutingTablesAsFrame(sourceRouter, destinationRouter);
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
        // Reset distances to infinity for each router
        for (Router router : routers.values()) {
            router.resetDistances();
        }

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

            // Apply updates
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

        } while (updated);
    }



    public static String getRoutingTablesAsFrame(String sourceRouter, String destinationRouter) {
        JsonObject frame = new JsonObject();
        frame.addProperty("sourceRouter", sourceRouter);
        frame.addProperty("destinationRouter", destinationRouter);

        JsonArray routingTable = new JsonArray();
        for (Router router : routers.values()) {
            if (router.getName().equals(sourceRouter) || router.getName().equals(destinationRouter)) {
                for (Map.Entry<String, Integer> entry : router.getDistances().entrySet()) {
                    String destination = entry.getKey();
                    int distance = entry.getValue();
                    String nextHop = router.getRoute(destination);
                    if (destination.equals(destinationRouter)) { // Only include entries for the destination router
                        JsonObject routeEntry = new JsonObject();
                        routeEntry.addProperty("destination", destination);
                        routeEntry.addProperty("distance", distance);
                        routeEntry.addProperty("nextHop", nextHop);
                        routingTable.add(routeEntry);
                    }
                }
            }
        }
        frame.add("routingTable", routingTable);
        return frame.toString();
    }
}

