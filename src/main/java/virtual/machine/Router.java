package virtual.machine;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Router {
    private final String name;
    private final Map<String, Integer> routingTable; // Stores subnet and distance
    private final Map<String, String> nextHop; // Stores next hop for each subnet
    private final Map<String, Socket> connectedPCs; // Stores connected PCs
    private final Map<String, Router> connectedRouters; // Stores connected routers

    public Router(String name) {
        this.name = name;
        this.routingTable = new HashMap<>();
        this.nextHop = new HashMap<>();
        this.connectedPCs = new HashMap<>();
        this.connectedRouters = new HashMap<>();
    }

    public synchronized void connectPC(String pcName, Socket socket) {
        connectedPCs.put(pcName, socket);
    }

    public synchronized void connectRouter(String routerName, Router router) {
        connectedRouters.put(routerName, router);
    }

    public synchronized void updateRoutingTable(String subnet, int distance, String nextHopRouter) {
        routingTable.put(subnet, distance);
        nextHop.put(subnet, nextHopRouter);
    }

    public synchronized void forwardFrameToPC(String frame) {
        // Extract destination MAC address from the frame
        String[] frameParts = frame.split("\\|");
        String destinationMAC = frameParts[2];

        // Find the connected PC with the matching MAC address
        Socket pcSocket = connectedPCs.get(destinationMAC);
        if (pcSocket != null) {
            try {
                PrintWriter out = new PrintWriter(pcSocket.getOutputStream(), true);
                out.println(frame);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Destination PC not found!");
        }
    }

    public synchronized void forwardFrameToRouter(String frame, String nextHop) {
        // Extract destination MAC address from the frame
        String[] frameParts = frame.split("\\|");
        String destinationMAC = frameParts[2];

        // Find the connected router with the matching MAC address
        Router router = connectedRouters.get(destinationMAC);
        if (router != null) {
            router.receiveFrame(frame);
        } else {
            System.err.println("Destination router not found!");
        }
    }

    public synchronized void receiveFrame(String frame) {
        // Extract destination MAC address from the frame
        String[] frameParts = frame.split("\\|");
        String destinationMAC = frameParts[2];
        String nextHopRouter = " ";

        // Look up the next hop router or directly connected PC in the routing table
        nextHopRouter = nextHop.get(destinationMAC);

        if (nextHopRouter != null) {
            if (nextHopRouter.startsWith("r")) {
                // Forward the frame to the next hop router
                forwardFrameToRouter(frame, nextHopRouter);
            } else {
                // Forward the frame to the directly connected PC
                forwardFrameToPC(frame);
            }
        } else {
            System.err.println("Destination not found in routing table!");
        }
    }

    public synchronized void printRoutingTable() {
        System.out.println("Routing table for router " + name + ":");
        for (Map.Entry<String, Integer> entry : routingTable.entrySet()) {
            String subnet = entry.getKey();
            int distance = entry.getValue();
            String nextHopRouter = nextHop.get(subnet);
            System.out.println("Subnet: " + subnet + ", Distance: " + distance + ", Next Hop: " + nextHopRouter);
        }
    }
}

