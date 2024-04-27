package virtual.machine;

import com.google.gson.JsonArray;

import javax.xml.xpath.XPathEvaluationResult;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static virtual.machine.JsonObject.readConfigFile;

public class Router {
    private final String name;
    private final Map<String, Integer> distances;
    private final Map<String, String> routes;
    private final Map<String, String> routeUpdates = new HashMap<>();
    private final int port;
    private final Map<String, Socket> neighbors = new HashMap<>();

    public Router(String name, int port) {
        this.name = name;
        this.distances = new HashMap<>();
        this.routes = new HashMap<>();
        this.port = port;
    }



    public void start(InetAddress rIp, int rPort) {
        try {
            Socket socket = new Socket(rIp, rPort);
            System.out.println("Connected to router at " + rIp + ":" + rPort); // Print a message indicating successful connection
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(name);  // Send router name to another router

            new RouterThread(socket, this).start();

            // Separate thread for user input
            new Thread(() -> handleUserInput(socket)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void handleUserInput(Socket socket) {
        try {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Enter a message: ");
                String message = scanner.nextLine();

                System.out.print("Enter the destination Router address: ");
                String destinationRouter = scanner.nextLine();

                // Constructing the frame with proper format including pcPort
                String frame = message + "|" + "|" + destinationRouter + "|" + port;
                System.out.println("Sending frame: " + frame); // Print the frame for debugging
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(frame);
                out.flush(); // Ensure the message is sent immediately

                // Introduce a delay to give the user time to receive a message before new input is requested
                try {
                    Thread.sleep(1000); // Sleep for 1000 milliseconds (1 second)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws UnknownHostException {
        if (args.length != 2) {
            System.out.println("Syntax: Router <RouterName> <RouterPort>");
            return;
        }
        String routerName = args[0];

        // Read the configuration file
        com.google.gson.JsonObject config = readConfigFile("C:\\Users\\Austen Lowder\\Documents\\GitHub\\416Project\\src\\Router.json");

        // Find the switch configuration based on the provided switch name
        String routerIp = null;
        int routerPort = 0;
        assert config != null;
        JsonArray routersArray = config.getAsJsonArray("routers");
        for (int i = 0; i < routersArray.size(); i++) {
            com.google.gson.JsonObject routerObject = routersArray.get(i).getAsJsonObject();
            String name = routerObject.get("name").getAsString();
            if (name.equals(routerName)) {
                routerIp = routerObject.get("ip").getAsString();
                routerPort = routerObject.get("port").getAsInt();
                break;
            }
        }

        if (routerIp == null) {
            System.out.println("Router with name '" + routerName + "' not found in the configuration.");
            return;
        }

        Router currentRouter = new Router(routerName, routerPort);
        currentRouter.start(InetAddress.getByName(routerIp), routerPort);

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