package virtual.machine;

import com.google.gson.JsonArray;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

import static virtual.machine.JsonObject.readConfigFile;

public class Router {
    private final String name;
    private final Map<String, Integer> distances;
    private final Map<String, String> routes;
    private final Map<String, String> routeUpdates = new HashMap<>();
    private final int port;
    private final String routerIp;
    private final Map<String, Socket> neighbors = new HashMap<>();

    public Router(String name, int port, String routerIp) {
        this.name = name;
        this.distances = new HashMap<>();
        this.routes = new HashMap<>();
        this.port = port;
        this.routerIp = routerIp;
    }

    public void start() {
        // Connect to other routers
        connectToOtherRouters();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Router " + name + " is running on port " + port + " with ip " + routerIp);

            while (true) {
                System.out.println("Waiting for a connection on port " + port + "...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from: " + clientSocket.getInetAddress());
                new RouterThread(clientSocket, this).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addNeighbor(String neighborName, Socket socket) {
        neighbors.put(neighborName, socket);
    }

    public synchronized void broadcastFrame(String frame, String sourceMAC, String destinationMAC) {
        for (Socket neighborSocket : neighbors.values()) {
            try {
                PrintWriter out = new PrintWriter(neighborSocket.getOutputStream(), true);
                out.println(frame + "|" + sourceMAC + "|" + destinationMAC);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Syntax: Router <RouterName>");
            return;
        }
        String routerName = args[0];

        // Read the configuration file
        com.google.gson.JsonObject config = readConfigFile("C:\\Users\\Austen Lowder\\Documents\\GitHub\\416Project\\src\\Router.json");

        // Find the router configuration based on the provided router name
        int routerPort = 0;
        JsonArray routersArray = config.getAsJsonArray("routers");
        String routerIp = null;
        for (int i = 0; i < routersArray.size(); i++) {
            com.google.gson.JsonObject routerObject = routersArray.get(i).getAsJsonObject();
            String name = routerObject.get("name").getAsString();
            routerIp = routerObject.get("ip").getAsString();
            if (name.equals(routerName)) {
                routerPort = routerObject.get("port").getAsInt();
                break;
            }
        }

        if (routerPort == 0) {
            System.out.println("Router with name '" + routerName + "' not found in the configuration.");
            return;
        }

        Router currentRouter = new Router(routerName, routerPort, routerIp);
        currentRouter.start();
    }

    private void connectToOtherRouters() {
        com.google.gson.JsonObject config = readConfigFile("C:\\Users\\Austen Lowder\\Documents\\GitHub\\416Project\\src\\Router.json");
        JsonArray routersArray = config.getAsJsonArray("routers");
        boolean connected = false;

        while (!connected) {
            for (int i = 0; i < routersArray.size(); i++) {
                com.google.gson.JsonObject routerObject = routersArray.get(i).getAsJsonObject();
                String routerName = routerObject.get("name").getAsString();
                int routerPort = routerObject.get("port").getAsInt();
                String routerIp = routerObject.get("ip").getAsString();

                if (!routerName.equals(name)) {
                    try {
                        System.out.println("Attempting to connect to router: " + routerName + " at IP: " + routerIp + " and port: " + routerPort);
                        Socket socket = new Socket(routerIp, routerPort); // Connect to the specified IP address
                        addNeighbor(routerName, socket);
                        System.out.println("Connected to router: " + routerName);
                        connected = true;
                        break; // Exit loop once a connection is established
                    } catch (IOException e) {
                        System.out.println("Failed to connect to router: " + routerName + " at IP: " + routerIp + " and port: " + routerPort);
                        e.printStackTrace();
                        // Sleep for a short duration before attempting to connect again
                        try {
                            Thread.sleep(5000); // Sleep for 5 seconds before retrying
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }

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