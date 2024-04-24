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
            System.out.println("Routers and Links:");
            for (Router router : routers.values()) {
                System.out.println("Router: " + router.getName());
                for (String link : router.getLinks()) {
                    System.out.println("  Link: " + link);
                }
            }

            calculateDistances();

            // Debug: Print routing tables
            printRoutingTables();
        }
    }


    public static void parseRouters(JsonArray routersArray) {
        for (JsonElement element : routersArray) {
            JsonObject routerObj = element.getAsJsonObject();
            String name = routerObj.get("name").getAsString();
            Router router = new Router(name);
            JsonArray linksArray = routerObj.getAsJsonArray("links");
            for (JsonElement link : linksArray) {
                String linkName = link.getAsString();
                // Check if the link is a valid router or subnet
                if (routers.containsKey(linkName) || linkName.startsWith("r")) {
                    router.addLink(linkName);
                } else {
                    System.out.println("Invalid link for router " + name + ": " + linkName);
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


        static class Router {
            private final String name;
            private final Map<String, Integer> distances;
            private final Map<String, String> routes;

            public Router(String name) {
                this.name = name;
                this.distances = new HashMap<>();
                this.routes = new HashMap<>();
            }

            public String getName() {
                return name;
            }

            public Map<String, Integer> getDistances() {
                return distances;
            }

            public void addLink(String destination) {
                distances.put(destination, 1);
                routes.put(destination, destination);
            }

            public void updateDistance(String destination, int distance, String route) {
                distances.put(destination, distance);
                routes.put(destination, route);
            }

            public boolean hasRoute(String destination) {
                return routes.containsKey(destination);
            }

            public int getDistance(String destination) {
                return distances.getOrDefault(destination, Integer.MAX_VALUE);
            }

            public String getRoute(String destination) {
                return routes.get(destination);
            }

            public Iterable<String> getLinks() {
                return distances.keySet();
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
                updates.put(router, potentialUpdates);
            }

            // Step 2: Apply updates
            for (Map.Entry<Router, Map<String, Integer>> entry : updates.entrySet()) {
                Router router = entry.getKey();
                Map<String, Integer> potentialUpdates = entry.getValue();
                for (Map.Entry<String, Integer> update : potentialUpdates.entrySet()) {
                    String destination = update.getKey();
                    Router destinationRouter = routers.get(destination);
                    if (destinationRouter != null) { // Check if destination is a router
                        // Update distance with the correct destination router's name
                        String route = destinationRouter.getName();
                        router.updateDistance(destination, update.getValue(), route);
                    }
                }
            }
        } while (updated);
    }


    public static void printRoutingTables() {
        for (Router router : routers.values()) {
            System.out.println("Routing table for Router " + router.getName() + ":");
            System.out.println("+-------------+----------+--------+");
            System.out.println("| Destination | Distance |  Route(HOP) |");
            System.out.println("+-------------+----------+--------+");
            for (Map.Entry<String, Integer> entry : router.getDistances().entrySet()) {
                String destination = entry.getKey();
                int distance = entry.getValue();
                String route = router.getRoute(destination);
                // Replace the router name with its proper name if available
                String properRoute = routers.containsKey(route) ? routers.get(route).getName() : route;
                // Padding for alignment
                String paddedDestination = String.format("%-12s", destination);
                String paddedDistance = String.format("%-9d", distance);
                String paddedRoute = String.format("%-7s", properRoute != null ? properRoute : "Unknown");
                System.out.println("| " + paddedDestination + " | " + paddedDistance + " | " + paddedRoute + " |");
            }
            System.out.println("+-------------+----------+--------+\n");
        }
    }
}

