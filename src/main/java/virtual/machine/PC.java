package virtual.machine;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.Scanner;

import static virtual.machine.JsonObject.readConfigFile;

public class PC {
    private final String name;
    private final String ip;
    private final String mac;
    private final String port;
    public PC(String name, String ip, String port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.mac = generateMacAddress();
    }

    public void start(InetAddress sIp, int sPort) {
        try {
            Socket socket = new Socket(sIp, sPort);
            System.out.println("Connected to Switch at " + sIp + ":" + sPort); // Print a message indicating successful connection
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(name);  // Send PC name to the switch

            new PCReceiverThread(socket, mac).start();

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

                System.out.print("Enter the destination MAC address: ");
                String destinationMAC = scanner.nextLine();

                // Constructing the frame with proper format including pcPort
                String frame = message + "|" + mac + "|" + destinationMAC + "|" + port;
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


    // needs flood frame first time sending across switch
    //s1 needs to connect to s2
    // c needs send to s2, s2 to s1, s1 to a/b
    public static void main(String[] args) throws UnknownHostException {
        if (args.length != 3) {
            System.out.println("Syntax: PC <ServerIP> <ServerPort> <PCName>");
            return;
        }
        String serverIp = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String pcName = args[2];

        // Read the configuration file
        JsonObject config = readConfigFile("file.json");

        // Find the PC configuration based on the provided PC name
        String pcIp = null;
        String pcPort = null;
        JsonArray devicesArray = Objects.requireNonNull(config).getAsJsonArray("devices");
        for (int i = 0; i < devicesArray.size(); i++) {
            JsonObject deviceObject = devicesArray.get(i).getAsJsonObject();
            String name = deviceObject.get("name").getAsString();
            if (name.equals(pcName)) {
                pcIp = deviceObject.get("ip").getAsString();
                pcPort = deviceObject.get("port").getAsString();
                break;
            }
        }

        if (pcIp == null || pcPort == null) {
            System.out.println("PC with name '" + pcName + "' not found in the configuration.");
            return;
        }

        InetAddress sIp = InetAddress.getByName(serverIp);

        PC currentPC = new PC(pcName, pcIp, pcPort);
        currentPC.start(sIp, serverPort);
    }


    private String generateMacAddress() {
        String namePart = name.substring(0, Math.min(name.length(), 6));
        String ipPart = ipToMacFormat(ip);
        String portPart = port.length() >= 4 ? port.substring(0, 4) : port;
        System.out.println(name + " " + namePart + ipPart + portPart);
        return namePart + ipPart + portPart;
    }

    private String ipToMacFormat(String ip) {
        String[] ipParts = ip.split("\\.");
        StringBuilder macBuilder = new StringBuilder();

        for (String part : ipParts) {
            int decimal = Integer.parseInt(part);
            String hex = Integer.toHexString(decimal).toUpperCase();
            macBuilder.append(hex.length() == 1 ? "0" + hex : hex);
        }

        return macBuilder.toString();
    }

}