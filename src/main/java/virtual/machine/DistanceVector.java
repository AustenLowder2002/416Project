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


        calculateDistances(sourceRouter, destinationRouter);
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


    public static void calculateDistances(String sourceRouter, String destinationRouter) {
        Random random = new Random();
        boolean updated;

        do {
            updated = false;
            Map<Router, Map<String, Integer>> updates = new HashMap<>();

            // Step 1: Calculate potential updates
            for (Router router : routers.values()) {
                Map<String, Integer> potentialUpdates = new HashMap<>();
                if (router.getName().equals(sourceRouter) || router.getName().equals(destinationRouter)) {
                    for (String neighbor : router.getLinks()) {
                        if (routers.containsKey(neighbor)) { // Check if neighbor is a router
                            Router neighborRouter = routers.get(neighbor);
                            for (Map.Entry<String, Integer> entry : neighborRouter.getDistances().entrySet()) {
                                String destination = entry.getKey();
                                int distance = entry.getValue() + 1;
                                if (!router.hasRoute(destination) || distance < router.getDistance(destination)) {
                                    // If there's already a route with the same distance, randomly choose one
                                    if (router.hasRoute(destination) && distance == router.getDistance(destination)) {
                                        if (random.nextBoolean()) {
                                            potentialUpdates.put(destination, distance);
                                            updated = true;
                                        }
                                    } else {
                                        potentialUpdates.put(destination, distance);
                                        updated = true;
                                    }
                                }
                            }
                        }
                    }
                }
                updates.put(router, potentialUpdates);
            }

            // Step 2: Apply updates
            for (Map.Entry<Router, Map<String, Integer>> entry : updates.entrySet()) {
                Router router = entry.getKey();
                Map<String, Integer> potentialUpdates = entry.getValue();
                for (Map.Entry<String, Integer> update : potentialUpdates.entrySet()) {
                    String destination = update.getKey();
                    // String destinationRouter = routers.get(destination); // No need to retrieve router object
                    // if (destinationRouter != null) { // Check if destination is a router
                    // Update distance with the correct destination router's name
                    String route = destination; // Since destination is already the name of the router
                    router.updateDistance(destination, update.getValue(), route);
                    // }
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
                    int distance;
                    if (sourceRouter.equals(destinationRouter)) {
                        distance = 0;
                    } else {
                        distance = entry.getValue();
                    }
                    String nextHop = router.getRoute(destination);

                    if (destination.equals(destinationRouter)) {
                        // Add the routing entry for the destination router
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

