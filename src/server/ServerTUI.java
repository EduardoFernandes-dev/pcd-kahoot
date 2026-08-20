package server;
import java.util.Scanner;
public class ServerTUI implements Runnable {
    private final RoomManager roomManager;
    private final Scanner scanner;
    private volatile boolean running;
    public ServerTUI(RoomManager roomManager) {
        this.roomManager = roomManager;
        this.scanner = new Scanner(System.in);
        this.running = false;
    }
    @Override
    public void run() {
        running = true;
        while (running) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }
            processCommand(input);
        }
    }
    private void processCommand(String input) {
        String[] parts = input.split("\\s+");
        String command = parts[0].toLowerCase();
        try {
            switch (command) {
                case "create":
                    handleCreate(parts);
                    break;
                case "exit":
                case "quit":
                    handleExit();
                    break;
                default:
                    System.out.println("Unknown command: " + command);
                    System.out.println("Available commands: create, exit");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    private void handleCreate(String[] parts) {
        if (parts.length < 4) {
            System.out.println("Usage: create <room_code> <num_teams> <players_per_team>");
            System.out.println("Example: create ROOM1 2 2");
            return;
        }
        String roomCode = parts[1].toUpperCase();
        int numTeams;
        int playersPerTeam;
        try {
            numTeams = Integer.parseInt(parts[2]);
            playersPerTeam = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            System.out.println("Error: Teams and players must be numbers.");
            return;
        }
        if (numTeams < 1 || numTeams > 10) {
            System.out.println("Error: Number of teams must be between 1 and 10.");
            return;
        }
        if (playersPerTeam < 1 || playersPerTeam > 10) {
            System.out.println("Error: Players per team must be between 1 and 10.");
            return;
        }
        boolean success = roomManager.createRoom(roomCode, numTeams, playersPerTeam);
        if (success) {
            System.out.println("Room created: " + roomCode);
            System.out.println("  Teams: " + numTeams + ", Players per team: " + playersPerTeam);
        } else {
            System.out.println("Failed to create room. Room code might already exist.");
        }
    }
    private void handleExit() {
        System.out.println("\nShutting down server...");
        running = false;
        roomManager.shutdown();
        System.exit(0);
    }
    private void printWelcome() {
        System.out.println("\n=== IsKahoot Server ===");
        System.out.println("Commands: create <room> <teams> <players_per_team>, exit");
    }
    public void stop() {
        running = false;
    }
}
