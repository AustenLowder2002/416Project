package virtual.machine;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class Router {
    private final String name;
    private final Map<String, Integer> distances;
    private final Map<String, String> routes;
    private final Map<String, String> routeUpdates = new HashMap<>();
    private final int port;
    private boolean isRunning;


    public Router(String name, int port) {
        this.name = name;
        this.distances = new HashMap<>();
        this.routes = new HashMap<>();
        this.port = port;
    }

    public void start() {
        isRunning = true;
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            System.out.println("Router " + name + " is running on port " + port);

            while (isRunning) {
                socket.receive(packet);
                String receivedData = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received packet on router " + name + ": " + receivedData);
                // Process the received packet as needed
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Syntax: Router <RouterName> <Port>");
            return;
        }
        String routerName = args[0];
        int port = Integer.parseInt(args[1]);

        Router router = new Router(routerName, port);
        router.start();
    }
    public void stop() {
        isRunning = false;
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
    public int getPort() {
        return port;
    }
}