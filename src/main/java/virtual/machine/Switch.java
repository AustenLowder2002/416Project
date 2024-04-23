package virtual.machine;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Switch {
    private final String name;
    private final int port;
    private final Map<String, Socket> neighbors = new HashMap<>();
    private final Map<String, Router> routers = new HashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public Switch(String name, int port) {
        this.name = name;
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Switch " + name + " is running on port " + port);

            while (true) {
                System.out.println("Waiting for a connection...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from: " + clientSocket.getInetAddress());
                executor.execute(new SwitchThread(clientSocket, this));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addNeighbor(String neighborName, Socket socket) {
        neighbors.put(neighborName, socket);
    }

    public synchronized void addRouter(String routerName, Router router) {
        routers.put(routerName, router);
    }

    public synchronized void sendRoutingUpdate(String routerName, String routingUpdate) {
        for (Socket neighborSocket : neighbors.values()) {
            try {
                PrintWriter out = new PrintWriter(neighborSocket.getOutputStream(), true);
                out.println("ROUTING_UPDATE|" + routerName + "|" + routingUpdate);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void forwardFrameToRouter(String routerName, String frame) {
        Router router = routers.get(routerName);
        if (router != null) {
            router.receiveFrame(frame);
        } else {
            System.err.println("Router " + routerName + " not found!");
        }
    }

    public synchronized void forwardFrameToPC(String routerName, String frame) {
        Router router = routers.get(routerName);
        if (router != null) {
            router.forwardFrameToPC(frame);
        } else {
            System.err.println("Router " + routerName + " not found!");
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}


