package virtual.machine;

import java.io.IOException;
import java.net.ServerSocket;

public class PortChecker {
    public static void main(String[] args) {
        int port = 6001; // Change to the port you want to check
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Port " + port + " is available.");
        } catch (IOException e) {
            System.out.println("Port " + port + " is already in use.");
        }
    }
}
