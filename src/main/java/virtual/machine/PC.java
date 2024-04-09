package virtual.machine;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class PC {
    private final String name;
    private final String ip;
    private final String mac;
    private final String port;
    private Router router;

    public PC(String name, String ip, String port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.mac = generateMacAddress();
    }

    public void connectToRouter(Router router) {
        this.router = router;
    }

    public void start() {
        try {
            Socket socket = new Socket(InetAddress.getLocalHost(), Integer.parseInt(port));
            System.out.println("Connected to router");

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(name);

            new PCReceiverThread(socket, mac).start();

            new Thread(() -> handleUserInput(socket)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleUserInput(Socket socket) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter a message: ");
            String message = scanner.nextLine();

            System.out.print("Enter the destination MAC address: ");
            String destinationMAC = scanner.nextLine();

            String frame = message + "|" + mac + "|" + destinationMAC + "|" + port;
            System.out.println("Sending frame: " + frame);

            if (router != null) {
                router.forwardFrameToPC(frame);
            } else {
                System.err.println("Router not connected!");
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Syntax: PC <ServerIP> <ServerPort> <PCName>");
            return;
        }
        String serverIp = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String pcName = args[2];

        PC pc = new PC(pcName, "127.0.0.1", String.valueOf(serverPort));
        Router router = new Router("Router1");
        pc.connectToRouter(router);
        pc.start();
    }


}
