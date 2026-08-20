package server;
import models.GameState;
import models.Team;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
public class RoomManager {
    private final Map<String, GameRoom> rooms;
    private final Map<String, RoomConfig> roomConfigs;
    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running;
    public RoomManager(int port) {
        this.rooms = new HashMap<>();
        this.roomConfigs = new HashMap<>();
        this.port = port;
        this.running = false;
    }
    public synchronized boolean createRoom(String roomCode, int numTeams, int playersPerTeam) {
        if (rooms.containsKey(roomCode)) {
            return false;
        }
        String[] teamCodes = new String[numTeams];
        for (int i = 0; i < numTeams; i++) {
            teamCodes[i] = "TEAM_" + (char) ('A' + i);
        }
        GameRoom room = new GameRoom(roomCode, teamCodes, playersPerTeam);
        RoomConfig config = new RoomConfig(roomCode, teamCodes, playersPerTeam);
        rooms.put(roomCode, room);
        roomConfigs.put(roomCode, config);
        System.out.println("\n[RoomManager] Created room: " + roomCode);
        return true;
    }
    public synchronized GameRoom getRoom(String roomCode) {
        return rooms.get(roomCode);
    }
    public synchronized boolean hasRoom(String roomCode) {
        return rooms.containsKey(roomCode);
    }
    public synchronized String[] listRooms() {
        if (rooms.isEmpty()) {
            return new String[0];
        }
        String[] roomList = new String[rooms.size()];
        int index = 0;
        for (Map.Entry<String, RoomConfig> entry : roomConfigs.entrySet()) {
            String roomCode = entry.getKey();
            RoomConfig config = entry.getValue();
            GameRoom room = rooms.get(roomCode);
            int connectedPlayers = room.getGameState().getConnectedPlayerCount();
            int totalPlayers = config.numTeams * config.playersPerTeam;
            String phase = room.getGameState().getPhase().toString();
            roomList[index++] = String.format("  [%s] %d/%d players | Phase: %s",
                    roomCode, connectedPlayers, totalPlayers, phase);
        }
        return roomList;
    }
    public synchronized String getRoomStatus(String roomCode) {
        if (!rooms.containsKey(roomCode)) {
            return null;
        }
        GameRoom room = rooms.get(roomCode);
        RoomConfig config = roomConfigs.get(roomCode);
        GameState state = room.getGameState();
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔════════════════════════════════════════╗\n");
        sb.append("║         Room Status: ").append(String.format("%-15s", roomCode)).append("║\n");
        sb.append("╚════════════════════════════════════════╝\n");
        sb.append("\nConfiguration:\n");
        sb.append("  Teams: ").append(config.numTeams).append("\n");
        sb.append("  Players per team: ").append(config.playersPerTeam).append("\n");
        sb.append("  Total capacity: ").append(config.numTeams * config.playersPerTeam).append("\n");
        sb.append("\nCurrent State:\n");
        sb.append("  Phase: ").append(state.getPhase()).append("\n");
        sb.append("  Connected players: ").append(state.getConnectedPlayerCount()).append("\n");
        sb.append("\nTeams:\n");
        for (String teamCode : config.teamCodes) {
            Team team = state.getTeam(teamCode);
            if (team != null) {
                sb.append("  ").append(teamCode).append(": ")
                        .append(team.getPlayerCount()).append("/").append(team.getMaxPlayers())
                        .append(" players");
                if (team.isFull()) {
                    sb.append(" [FULL]");
                }
                sb.append("\n");
            }
        }
        if (state.getPhase() == GameState.GamePhase.IN_PROGRESS) {
            sb.append("\nGame Progress:\n");
            sb.append("  Question: ").append(state.getCurrentQuestionNumber())
                    .append("/").append(state.getTotalQuestions()).append("\n");
        }
        return sb.toString();
    }
    public synchronized boolean restartRoom(String roomCode) {
        if (!rooms.containsKey(roomCode)) {
            return false;
        }
        GameRoom room = rooms.get(roomCode);
        try {
            room.restartGame();
            System.out.println("[RoomManager] Game restarted in room: " + roomCode);
            return true;
        } catch (Exception e) {
            System.err.println("[RoomManager] Failed to restart room " + roomCode + ": " + e.getMessage());
            return false;
        }
    }
    public void startAcceptingConnections() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("\nProjeto Kahoot");
        System.out.println("Port: " + port);
        System.out.println("Commands: create <room> <teams> <players>, exit\n");
        Thread acceptThread = new Thread(() -> acceptConnectionLoop());
        acceptThread.setDaemon(false);
        acceptThread.start();
    }
    private void acceptConnectionLoop() {
        int connectionCount = 0;
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                connectionCount++;
                System.out.println("\n[" + connectionCount + "] New connection from: " +
                        clientSocket.getRemoteSocketAddress());
                PlayerHandler handler = new PlayerHandler(clientSocket, this);
                handler.start();
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        }
    }
    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
        rooms.clear();
        roomConfigs.clear();
        System.out.println("RoomManager shut down.");
    }
    private static class RoomConfig {
        final String roomCode;
        final String[] teamCodes;
        final int numTeams;
        final int playersPerTeam;
        RoomConfig(String roomCode, String[] teamCodes, int playersPerTeam) {
            this.roomCode = roomCode;
            this.teamCodes = teamCodes;
            this.numTeams = teamCodes.length;
            this.playersPerTeam = playersPerTeam;
        }
    }
}
