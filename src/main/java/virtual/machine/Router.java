package virtual.machine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static virtual.machine.JsonObject.readConfigFile;

public class Router {
    private final String name;
    private final int port;
    private final String routerIp;

    public Router(String name, int port, String routerIp) {
        this.name = name;
        this.port = port;
        this.routerIp = routerIp;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Syntax: Router <RouterName>");
            return;
        }
        String routerName = args[0];

        // Read the configuration file
        JsonObject config = readConfigFile("Router.json");

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




    private String generateInitialDistanceVectorTable(com.google.gson.JsonObject config) {
        // Create a StringBuilder to construct the table
        StringBuilder tableBuilder = new StringBuilder();

        // Append header row
        tableBuilder.append(String.format("%-10s %-10s %-10s%n", "Subnet", "Next Hop", "Distance"));

        // Retrieve the router configuration by name
        com.google.gson.JsonArray routersArray = config.getAsJsonArray("routers");
        com.google.gson.JsonObject routerConfig = null;
        for (com.google.gson.JsonElement routerElement : routersArray) {
            com.google.gson.JsonObject routerObject = routerElement.getAsJsonObject();
            if (routerObject.get("name").getAsString().equals(name)) {
                routerConfig = routerObject;
                break;
            }
        }

        // If the router configuration is found, retrieve its subnets and next hops
        if (routerConfig != null) {
            // Retrieve the subnets and next hops as JsonArrays
            JsonArray subnetsArray = routerConfig.getAsJsonArray("subnets");
            JsonArray nextHopsArray = routerConfig.getAsJsonArray("links");

            // Check if both arrays are non-null and have the same length
            if (subnetsArray != null && nextHopsArray != null && subnetsArray.size() == nextHopsArray.size()) {
                // Populate the table with subnet, next hop, and distance
                for (int i = 0; i < subnetsArray.size(); i++) {
                    String subnet = subnetsArray.get(i).getAsString();
                    String nextHop = nextHopsArray.get(i).getAsString();
                    tableBuilder.append(String.format("%-10s %-10s %-10s%n", subnet, nextHop, 0)); // Initialize distance to zero
                }
            } else {
                System.out.println("Subnets array or next hops array is null or has different lengths.");
            }
        } else {
            System.out.println("Router configuration not found.");
        }

        return tableBuilder.toString();
    }




    public synchronized void connectToOtherRouters() {
        JsonObject config = readConfigFile("Router.json");
        JsonArray routersArray = Objects.requireNonNull(config).getAsJsonArray("routers");

        try {
            // Create a UDP socket for sending and receiving data
            DatagramSocket socket = new DatagramSocket(port);

            // Generate the initial distance vector table as a string
            String initialDistanceVectorTable = generateInitialDistanceVectorTable(config);

            // Convert the table string to bytes
            byte[] sendData = initialDistanceVectorTable.getBytes();

            // Store the initial distance vector table received from neighbors
            Map<String, String> receivedTables = new HashMap<>();

            // Iterate through each router configuration
            for (int i = 0; i < routersArray.size(); i++) {
                JsonObject routerObject = routersArray.get(i).getAsJsonObject();
                String neighborName = routerObject.get("name").getAsString();
                int neighborPort = routerObject.get("port").getAsInt();
                JsonArray linksArray = routerObject.getAsJsonArray("links");

                // If the router is a neighbor and not itself, send the initial distance vector table
                if (!neighborName.equals(name) && linksArray.contains(new JsonPrimitive(name))) {
                    System.out.println("Connecting to router: " + neighborName + " at port: " + neighborPort);

                    // Create a UDP packet with the table data
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(), neighborPort);

                    // Send the UDP packet
                    socket.send(sendPacket);

                    System.out.println("Sent initial distance vector table to router: " + neighborName);
                }
            }

            // Continuously receive packets
            while (true) {
                // Prepare to receive data
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                // Receive UDP packet
                socket.receive(receivePacket);

                // Process received data
                String receivedData = new String(receivePacket.getData(), 0, receivePacket.getLength());
                String neighborName = getNeighborName(receivePacket.getPort(), config);
                System.out.println("Received data from router " + neighborName + ": " + receivedData);

                // Split the received data into rows using newline character as the delimiter
                String[] rows = receivedData.split("\n");

                // Initialize a map to store subnet distances for the current neighbor router
                Map<String, Integer> distances = new HashMap<>();

                for (String row : rows) {
                    // Split the row into columns using tab character as the delimiter
                    String[] columns = row.trim().split("\\s+");

                    // Check if the row contains the expected number of columns
                    if (columns.length >= 3) {
                        // Extract subnet, next hop, and distance from the columns
                        String subnet = columns[0].trim();
                        String nextHop = columns[1].trim();
                        // Combine remaining columns as the distance value
                        StringBuilder distanceBuilder = new StringBuilder();
                        for (int j = 2; j < columns.length; j++) {
                            distanceBuilder.append(columns[j].trim());
                            if (j < columns.length - 1) {
                                distanceBuilder.append(" "); // Add space delimiter between values
                            }
                        }
                        System.out.println("Subnet: " + subnet + ", Next Hop: " + nextHop + ", Distance: " + distanceBuilder.toString());
                        try {
                            int distance = Integer.parseInt(distanceBuilder.toString());
                            // Store the subnet and distance in the distances map
                            distances.put(subnet, distance);
                        } catch (NumberFormatException e) {
                            // Log an error for invalid distance format
                            System.out.println("Invalid distance format: " + distanceBuilder.toString());
                        }
                    } else {
                        // Log an error for rows with invalid format
                        System.out.println("Invalid format in received table row: " + row);
                    }
                }



                // Store the calculated distances for the current neighbor router
                receivedTables.put(neighborName, receivedData);

                // If received tables from all neighbors, calculate distances to other subnets
                if (receivedTables.size() == routersArray.size() - 1) {
                    // Process received tables to calculate distances to other subnets
                    Map<String, Map<String, Integer>> subnetDistances = calculateSubnetDistances(receivedTables);

                    // Display the complete table
                    displayCompleteTable(subnetDistances);
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to connect to routers.");
            e.printStackTrace();
        }
    }



    // Method to calculate distances to other subnets
    private Map<String, Map<String, Integer>> calculateSubnetDistances(Map<String, String> receivedTables) {
        // Initialize the map to store subnet distances for each neighbor router
        Map<String, Map<String, Integer>> subnetDistances = new HashMap<>();

        // Iterate through each neighbor's table
        for (Map.Entry<String, String> entry : receivedTables.entrySet()) {
            String neighborName = entry.getKey();
            String table = entry.getValue();

            // Parse the table to extract subnet distances
            Map<String, Integer> distances = new HashMap<>();
            String[] rows = table.split("\n"); // Split rows by newline character
            for (String row : rows) {
                String[] columns = row.split("\\s+"); // Split each row using one or more spaces or tabs
                // Ensure the row contains the expected number of columns
                if (columns.length == 3) {
                    String subnet = columns[0].trim();
                    // Skip the next hop column (columns[1])
                    int distance = Integer.parseInt(columns[2].trim()); // The distance is at index 2
                    distances.put(subnet, distance);
                } else {
                    // Handle invalid row format
                    System.out.println("Invalid format in received table row: " + row);
                }
            }

            // Store the calculated distances for the current neighbor router
            subnetDistances.put(neighborName, distances);
        }

        return subnetDistances;
    }



    // Method to display the complete table
    private void displayCompleteTable(Map<String, Map<String, Integer>> subnetDistances) {
        // Print header
        System.out.println("Complete Distance Vector Table:");
        System.out.printf("%-10s %-10s %-10s%n", "Router", "Subnet", "Distance");

        // Iterate through each neighbor router
        for (Map.Entry<String, Map<String, Integer>> entry : subnetDistances.entrySet()) {
            String neighborName = entry.getKey();
            Map<String, Integer> distances = entry.getValue();

            // Print subnet distances for the current neighbor router
            for (Map.Entry<String, Integer> distanceEntry : distances.entrySet()) {
                String subnet = distanceEntry.getKey();
                int distance = distanceEntry.getValue();
                System.out.printf("%-10s %-10s %-10s%n", neighborName, subnet, distance);
            }
        }
    }


    // Helper method to get neighbor name based on port number
    private String getNeighborName(int port, JsonObject config) {
        JsonArray routersArray = config.getAsJsonArray("routers");
        for (JsonElement element : routersArray) {
            JsonObject routerObject = element.getAsJsonObject();
            if (routerObject.get("port").getAsInt() == port) {
                return routerObject.get("name").getAsString();
            }
        }
        return null;
    }


    public void start() {
        connectToOtherRouters();

    }

    }
