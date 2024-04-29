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

    public synchronized void broadcastFrame(String frame) {
        for (Socket neighborSocket : neighbors.values()) {
            try {
                PrintWriter out = new PrintWriter(neighborSocket.getOutputStream(), true);
                out.println(frame + "|");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Syntax: Router <RouterName>");
            return;
        }
        String routerName = args[0];

        // Read the configuration file
        JsonObject config = readConfigFile("file.json");

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


    public synchronized void connectToOtherRouters() {
        JsonObject config = readConfigFile("file.json");
        JsonArray routersArray = Objects.requireNonNull(config).getAsJsonArray("routers");

        for (int i = 0; i < routersArray.size(); i++) {
            JsonObject routerObject = routersArray.get(i).getAsJsonObject();
            String neighborName = routerObject.get("name").getAsString();
            String neighborIp = routerObject.get("ip").getAsString();
            int neighborPort = routerObject.get("port").getAsInt();

            if (!neighborName.equals(name)) {
                try {
                    System.out.println("Connecting to router: " + neighborName + " at IP: " + neighborIp + " and port: " + neighborPort);
                    Socket socket = new Socket(neighborIp, neighborPort);
                    neighbors.put(neighborName, socket);
                    System.out.println("Connected to router: " + neighborName);
                    new RouterThread(socket, this);
                } catch (IOException e) {
                    System.out.println("Failed to connect to router: " + neighborName + " at IP: " + neighborIp + " and port: " + neighborPort);
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void startListening() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Router " + name + " is running on port " + port + " with IP " + routerIp);

            while (true) {
                System.out.println("Waiting for a connection on port " + port + "...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from: " + clientSocket.getInetAddress());
                new RouterThread(clientSocket, this).start();
            }
        } catch (IOException e) {
            System.out.println("Error starting router: " + e.getMessage());
        }
    }

    public void start() {
        if(name.equals("r1")){
            startListening();
        }else{
        connectToOtherRouters();
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
    public int getPort() {
        return port;
    }
    }
