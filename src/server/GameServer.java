package server;
import java.io.IOException;
public class GameServer {
    private static final int DEFAULT_PORT = 8080;
    private final RoomManager roomManager;
    private final ServerTUI tui;
    public GameServer(int port) {
        this.roomManager = new RoomManager(port);
        this.tui = new ServerTUI(roomManager);
    }
    public void start() throws IOException {
        roomManager.startAcceptingConnections();
        tui.run();
    }
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                if (port < 1024 || port > 65535) {
                    System.err.println("Port must be between 1024 and 65535. Using default: " + DEFAULT_PORT);
                    port = DEFAULT_PORT;
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: " + DEFAULT_PORT);
            }
        }
        GameServer server = new GameServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutdown signal received...");
            server.roomManager.shutdown();
        }));
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
