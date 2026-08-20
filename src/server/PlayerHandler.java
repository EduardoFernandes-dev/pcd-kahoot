package server;
import models.Player;
import network.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
public class PlayerHandler extends Thread {
    private final Socket clientSocket;
    private GameRoom gameRoom; 
    private final RoomManager roomManager; 
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Player player;
    public PlayerHandler(Socket clientSocket, GameRoom gameRoom) {
        this.clientSocket = clientSocket;
        this.gameRoom = gameRoom;
        this.roomManager = null;
        this.player = null;
    }
    public PlayerHandler(Socket clientSocket, RoomManager roomManager) {
        this.clientSocket = clientSocket;
        this.gameRoom = null; 
        this.roomManager = roomManager;
        this.player = null;
    }
    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(clientSocket.getInputStream());
            System.out.println("PlayerHandler started for " + clientSocket.getRemoteSocketAddress());
            if (!handleConnection()) {
                return; 
            }
            waitForGameStart();
            boolean keepPlaying = true;
            while (keepPlaying) {
                synchronized (gameRoom) {
                    while (!gameRoom.isGameInProgress()) {
                        try {
                            gameRoom.wait(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            keepPlaying = false;
                            break;
                        }
                    }
                }
                if (!keepPlaying)
                    break;
                System.out.println("Player " + player.getUsername() + " entered game loop");
                while (gameRoom.isGameInProgress()) {
                    try {
                        collectAnswer();
                    } catch (InterruptedException e) {
                        System.out.println("Player " + player.getUsername() + " interrupted");
                        Thread.currentThread().interrupt();
                        keepPlaying = false;
                        break;
                    }
                }
                System.out.println("Player " + player.getUsername() + " game loop ended - waiting for restart");
                if (clientSocket.isClosed() || !clientSocket.isConnected()) {
                    keepPlaying = false;
                }
            }
        } catch (IOException e) {
            System.err.println("PlayerHandler error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Invalid message received: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    private boolean handleConnection() throws IOException, ClassNotFoundException {
        Object obj = in.readObject();
        if (!(obj instanceof ConnectionRequest)) {
            System.err.println("First message was not ConnectionRequest: " + obj.getClass());
            sendResponse(ConnectionResponse.rejected("Invalid initial message"));
            return false;
        }
        ConnectionRequest request = (ConnectionRequest) obj;
        System.out.println("Connection request: " + request);
        if (roomManager != null) {
            String requestedRoom = request.getSalaCode();
            if (!roomManager.hasRoom(requestedRoom)) {
                String msg = "Room '" + requestedRoom + "' does not exist";
                System.out.println("Rejecting: " + msg);
                sendResponse(ConnectionResponse.rejected(msg));
                return false;
            }
            gameRoom = roomManager.getRoom(requestedRoom);
        }
        if (!request.getSalaCode().equals(gameRoom.getGameState().getRoomCode())) {
            String msg = "Invalid room code: " + request.getSalaCode();
            System.out.println("Rejecting: " + msg);
            sendResponse(ConnectionResponse.rejected(msg));
            return false;
        }
        GameRoom.ValidationResult result = gameRoom.validateAndAddPlayer(
                request.getUsername(),
                request.getEquipaCode());
        if (!result.isSuccess()) {
            System.out.println("Rejecting: " + result.getMessage());
            sendResponse(ConnectionResponse.rejected(result.getMessage()));
            return false;
        }
        this.player = result.getPlayer();
        player.setOutputStream(out);
        ConnectionResponse response = ConnectionResponse.accepted(
                player.getTeamCode(),
                result.getTeamPlayerCount(),
                gameRoom.getConnectedPlayerCount());
        sendResponse(response);
        System.out.println("Connection accepted: " + player.getUsername());
        return true;
    }
    private void waitForGameStart() {
        System.out.println("Player " + player.getUsername() + " waiting for game to start...");
        synchronized (gameRoom) {
            if (gameRoom.isReady()) {
                System.out.println("All players connected! Starting game...");
                try {
                    String[] questionsPaths = {
                            "../resources/questions.json",
                            "resources/questions.json",
                            "Project/resources/questions.json"
                    };
                    boolean loaded = false;
                    for (String path : questionsPaths) {
                        java.io.File f = new java.io.File(path);
                        if (f.exists()) {
                            System.out.println("Loading questions from: " + f.getAbsolutePath());
                            gameRoom.loadQuestions(path);
                            loaded = true;
                            break;
                        }
                    }
                    if (!loaded) {
                        throw new java.io.IOException("Could not find questions.json in any expected location");
                    }
                    gameRoom.startGame();
                    gameRoom.broadcastCurrentQuestion();
                    gameRoom.startQuestionRound();
                } catch (IOException e) {
                    System.err.println("Failed to load questions: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("Waiting for more players... (" +
                        gameRoom.getConnectedPlayerCount() + " connected)");
            }
        }
    }
    private void sendResponse(ConnectionResponse response) throws IOException {
        out.writeObject(response);
        out.flush();
    }
    private void collectAnswer() throws IOException, ClassNotFoundException, InterruptedException {
        Object obj = in.readObject();
        if (obj instanceof AnswerMessage) {
            AnswerMessage answer = (AnswerMessage) obj;
            System.out.println("Received answer from " + player.getUsername() +
                    ": option " + answer.getSelectedAnswer());
            gameRoom.submitAnswer(answer, player);
        } else if (obj instanceof RestartRequest) {
            RestartRequest request = (RestartRequest) obj;
            System.out.println("Restart vote from " + request.getUsername());
            try {
                GameRoom.VoteResult result = gameRoom.voteForRestart(request.getUsername());
                if (!result.restarted) {
                    System.out.println("Waiting for more votes: " + result.currentVotes + "/" + result.totalPlayers);
                }
            } catch (IOException e) {
                System.err.println("Failed to process restart vote: " + e.getMessage());
            }
        } else {
            System.err.println("Expected AnswerMessage but got: " + obj.getClass());
        }
    }
    private void cleanup() {
        System.out.println("Cleaning up PlayerHandler" +
                (player != null ? " for " + player.getUsername() : ""));
        
        if (gameRoom != null && player != null) {
            gameRoom.playerDisconnected(player.getUsername(), player.getTeamCode());
        }
        
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}
