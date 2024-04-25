package virtual.machine;

import java.util.HashMap;
import java.util.Map;

public class Router {
    private final String name;
    private final Map<String, Integer> distances;
    private final Map<String, String> routes;
    private final Map<String, String> routeUpdates = new HashMap<>();

    public Router(String name) {
        this.name = name;
        this.distances = new HashMap<>();
        this.routes = new HashMap<>();
    }

    public void updateRoutes(Map<String, String> routeUpdates) {
        for (Map.Entry<String, String> entry : routeUpdates.entrySet()) {
            String destination = entry.getKey();
            String nextHop = entry.getValue();
            routes.put(destination, nextHop);
        }
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
    public Map<String, String> getRouteUpdates() {
        return routeUpdates;
    }
    public void clearRouteUpdates() {
        routeUpdates.clear();
    }
}