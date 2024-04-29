package virtual.machine;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    public void start() throws IOException {
        connectToOtherRouters();

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Router " + name + " is running on port " + port + " with IP " + routerIp);

        while (true) {
            System.out.println("Waiting for a connection on port " + port + "...");
            Socket clientSocket = serverSocket.accept();
            System.out.println("Accepted connection from: " + clientSocket.getInetAddress());
            new RouterThread(clientSocket, this).start();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Syntax: Router <RouterName>");
            return;
        }
        String routerName = args[0];

        // Read the configuration file
        JsonObject config = readConfigFile("C:\\Users\\Austen Lowder\\Documents\\GitHub\\416Project\\src\\Router.json");

        // Find router configuration based on provided router name
        JsonArray routersArray = Objects.requireNonNull(config).getAsJsonArray("routers");
        String routerIp = null;
        int routerPort = 0;
        for (int i = 0; i < routersArray.size(); i++) {
            JsonObject routerObject = routersArray.get(i).getAsJsonObject();
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
        JsonArray routersArray = Objects.requireNonNull(config).getAsJsonArray("routers");
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
                        Socket socket = new Socket(routerIp, routerPort);
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