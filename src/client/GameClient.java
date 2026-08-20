package client;
import models.Question;
import network.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
public class GameClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private MessageListener messageListener;
    private Thread receiverThread;
    private boolean connected;
    private String username; 
    public interface MessageListener {
        void onConnectionResponse(ConnectionResponse response);
        void onQuestionReceived(Question question, int questionNumber, int totalQuestions);
        void onScoreboardReceived(ScoreboardMessage scoreboard); 
        void onConnectionLost(String reason);
    }
    public GameClient() {
        this.connected = false;
    }
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }
    public ConnectionResponse connect(String serverIP, int port, String sala, String equipa, String username)
            throws IOException, ClassNotFoundException {
        System.out.println("Connecting to server: " + serverIP + ":" + port);
        socket = new Socket(serverIP, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        System.out.println("Connected to server. Sending connection request...");
        this.username = username; 
        ConnectionRequest request = new ConnectionRequest(sala, equipa, username);
        out.writeObject(request);
        out.flush();
        Object response = in.readObject();
        if (!(response instanceof ConnectionResponse)) {
            throw new IOException("Unexpected response type: " + response.getClass());
        }
        ConnectionResponse connResponse = (ConnectionResponse) response;
        if (connResponse.isAccepted()) {
            connected = true;
            System.out.println("Connection accepted!");
            System.out.println("  Team: " + connResponse.getAssignedTeam());
            System.out.println("  Team players: " + connResponse.getTeamPlayerCount());
            System.out.println("  Total players: " + connResponse.getTotalPlayerCount());
            startReceiverThread();
            if (messageListener != null) {
                messageListener.onConnectionResponse(connResponse);
            }
        } else {
            System.out.println("Connection rejected: " + connResponse.getReason());
            disconnect();
        }
        return connResponse;
    }
    private void startReceiverThread() {
        receiverThread = new Thread(() -> {
            try {
                receiveMessages();
            } catch (Exception e) {
                if (connected) {
                    System.err.println("Receiver thread error: " + e.getMessage());
                    if (messageListener != null) {
                        messageListener.onConnectionLost(e.getMessage());
                    }
                }
            }
        }, "ReceiverThread");
        receiverThread.setDaemon(true);
        receiverThread.start();
        System.out.println("Receiver thread started");
    }
    private void receiveMessages() throws IOException, ClassNotFoundException {
        while (connected && !socket.isClosed()) {
            Object obj = in.readObject();
            if (obj instanceof Message) {
                handleMessage((Message) obj);
            } else {
                System.err.println("Received unknown object type: " + obj.getClass());
            }
        }
    }
    private void handleMessage(Message message) {
        System.out.println("Received message: " + message.getType());
        switch (message.getType()) {
            case QUESTION:
                handleQuestionMessage((QuestionMessage) message);
                break;
            case CONNECTION_RESPONSE:
                handleConnectionResponse((ConnectionResponse) message);
                break;
            case GAME_START:
                System.out.println("Game is starting!");
                break;
            case SCOREBOARD:
                handleScoreboardMessage((ScoreboardMessage) message);
                break;
            case GAME_END:
                System.out.println("Game has ended!");
                break;
            default:
                System.out.println("Unhandled message type: " + message.getType());
        }
    }
    private void handleQuestionMessage(QuestionMessage message) {
        System.out.println("Question received: " + message.getQuestionNumber() +
                "/" + message.getTotalQuestions());
        if (messageListener != null) {
            messageListener.onQuestionReceived(
                    message.getQuestion(),
                    message.getQuestionNumber(),
                    message.getTotalQuestions());
        }
    }
    private void handleConnectionResponse(ConnectionResponse response) {
        if (messageListener != null) {
            messageListener.onConnectionResponse(response);
        }
    }
    private void handleScoreboardMessage(ScoreboardMessage message) {
        System.out.println("Scoreboard received for round " +
                message.getCurrentRound() + "/" + message.getTotalRounds());
        if (messageListener != null) {
            messageListener.onScoreboardReceived(message);
        }
    }
    public void sendAnswer(int selectedAnswer, int questionNumber) throws IOException {
        if (!connected || out == null) {
            throw new IOException("Not connected to server");
        }
        if (username == null) {
            throw new IOException("Username not set");
        }
        AnswerMessage answer = new AnswerMessage(username, questionNumber, selectedAnswer);
        out.writeObject(answer);
        out.flush();
        System.out.println("Answer sent: option " + selectedAnswer);
    }
    public void sendRestartRequest() throws IOException {
        if (!connected || out == null) {
            throw new IOException("Not connected to server");
        }
        if (username == null) {
            throw new IOException("Username not set");
        }
        RestartRequest request = new RestartRequest(username);
        out.writeObject(request);
        out.flush();
        System.out.println("Restart request sent");
    }
    public void disconnect() {
        connected = false;
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Disconnected from server");
        } catch (IOException e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        }
    }
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}
