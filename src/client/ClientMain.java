package client;
import models.Question;
import network.ConnectionResponse;
import network.ScoreboardMessage;
import server.GameLogic;
import utils.QuestionLoader;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public class ClientMain {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Starting in standalone mode (no server)");
            startStandaloneMode();
        } else if (args.length == 5) {
            System.out.println("Starting in network mode");
            startNetworkMode(args);
        } else {
            printUsage();
            System.exit(1);
        }
    }
    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  Standalone mode: java ClientMain");
        System.err.println("  Network mode:    java ClientMain <IP> <PORT> <Sala> <Equipa> <Username>");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  java ClientMain");
        System.err.println("  java ClientMain localhost 8080 ROOM1 TEAM_A Alice");
        System.err.println("  java ClientMain 192.168.1.100 8080 ROOM1 TEAM_B Bob");
    }
    private static void startStandaloneMode() {
        SwingUtilities.invokeLater(() -> {
            ClientGUI gui = new ClientGUI();
            startQuiz(gui);
        });
    }
    private static void startNetworkMode(String[] args) {
        String serverIP = args[0];
        int port;
        String sala = args[2];
        String equipa = args[3];
        String username = args[4];
        try {
            port = Integer.parseInt(args[1]);
            if (port < 1024 || port > 65535) {
                System.err.println("Error: Port must be between 1024 and 65535");
                System.exit(1);
                return;
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid port number: " + args[1]);
            System.exit(1);
            return;
        }
        if (sala.trim().isEmpty() || equipa.trim().isEmpty() || username.trim().isEmpty()) {
            System.err.println("Error: Sala, Equipa, and Username cannot be empty");
            System.exit(1);
            return;
        }
        System.out.println("Connecting to server...");
        System.out.println("  Server: " + serverIP + ":" + port);
        System.out.println("  Sala: " + sala);
        System.out.println("  Equipa: " + equipa);
        System.out.println("  Username: " + username);
        SwingUtilities.invokeLater(() -> {
            ClientGUI gui = new ClientGUI();
            gui.open();
            gui.showWaitingForConnection();
            new Thread(() -> {
                try {
                    connectToServer(gui, serverIP, port, sala, equipa, username);
                } catch (Exception e) {
                    System.err.println("Connection error: " + e.getMessage());
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                null,
                                "Failed to connect to server:\n" + e.getMessage(),
                                "Connection Error",
                                JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    });
                }
            }, "ConnectionThread").start();
        });
    }
    private static void connectToServer(ClientGUI gui, String serverIP, int port,
            String sala, String equipa, String username) throws Exception {
        GameClient client = new GameClient();
        client.setMessageListener(new GameClient.MessageListener() {
            @Override
            public void onConnectionResponse(ConnectionResponse response) {
                if (!response.isAccepted()) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                null,
                                "Connection rejected:\n" + response.getReason(),
                                "Connection Rejected",
                                JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    });
                }
            }
            @Override
            public void onQuestionReceived(Question question, int questionNumber, int totalQuestions) {
                System.out.println("Question received: " + questionNumber + "/" + totalQuestions);
                SwingUtilities.invokeLater(() -> {
                    gui.displayQuestion(question, questionNumber, totalQuestions);
                    gui.setGameClient(client, questionNumber); 
                });
            }
            @Override
            public void onScoreboardReceived(ScoreboardMessage scoreboard) {
                System.out.println("Scoreboard received");
                SwingUtilities.invokeLater(() -> {
                    gui.displayScoreboard(scoreboard);
                });
            }
            @Override
            public void onConnectionLost(String reason) {
                System.err.println("Connection lost: " + reason);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            null,
                            "Connection to server lost:\n" + reason,
                            "Connection Lost",
                            JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                });
            }
        });
        ConnectionResponse response = client.connect(serverIP, port, sala, equipa, username);
        if (response.isAccepted()) {
            final String playerName = username;
            SwingUtilities.invokeLater(() -> {
                gui.setPlayerUsername(playerName);
                gui.showWaitingForPlayers(response.getTotalPlayerCount());
                gui.setRestartListener(() -> {
                    System.out.println("Restart requested - sending to server...");
                    try {
                        client.sendRestartRequest();
                        gui.showWaitingForPlayers(response.getTotalPlayerCount());
                    } catch (Exception e) {
                        System.err.println("Failed to send restart request: " + e.getMessage());
                        JOptionPane.showMessageDialog(null,
                                "Erro ao enviar pedido de restart: " + e.getMessage(),
                                "Erro",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
            });
            System.out.println("Waiting for game to start...");
        }
    }
    private static void startQuiz(ClientGUI gui) {
        List<Question> questions = createDemoQuestions();
        GameLogic game = new GameLogic(questions);
        gui.open();
        gui.setAnswerSelectionListener(answerIndex -> {
            new Thread(() -> handleAnswerSelection(gui, game, answerIndex), "AnswerHandler").start();
        });
        gui.setRestartListener(() -> {
            System.out.println("Restarting game...");
            List<Question> newQuestions = createDemoQuestions();
            game.resetWithNewQuestions(newQuestions);
            SwingUtilities.invokeLater(() -> displayCurrentQuestion(gui, game));
        });
        displayCurrentQuestion(gui, game);
    }
    private static void handleAnswerSelection(ClientGUI gui, GameLogic game, int answerIndex) {
        try {
            boolean correct = game.checkAnswer(answerIndex);
            int correctIndex = game.getCurrentQuestion().getCorrectAnswerIndex();
            SwingUtilities.invokeLater(() -> gui.showResult(correct, correctIndex));
            Thread.sleep(3000);
            if (game.hasNextQuestion()) {
                game.nextQuestion();
                SwingUtilities.invokeLater(() -> displayCurrentQuestion(gui, game));
            } else {
                SwingUtilities.invokeLater(() -> gui.showGameComplete());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Answer handling interrupted: " + e.getMessage());
        }
    }
    private static void displayCurrentQuestion(ClientGUI gui, GameLogic game) {
        Question current = game.getCurrentQuestion();
        if (current != null) {
            gui.displayQuestion(current, game.getCurrentQuestionNumber(), game.getTotalQuestions());
        }
    }
    private static List<Question> createDemoQuestions() {
        String[] paths = {
                "resources/questions.json",
                "../resources/questions.json",
                "Project/resources/questions.json",
                "src/../resources/questions.json"
        };
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) {
                try {
                    System.out.println("Loading questions from: " + f.getAbsolutePath());
                    List<Question> questions = QuestionLoader.loadAllQuestions(path);
                    System.out.println("Loaded " + questions.size() + " questions from JSON");
                    return questions;
                } catch (IOException e) {
                    System.err.println("Error loading questions from " + path + ": " + e.getMessage());
                }
            }
        }
        System.err.println("Could not find questions.json. Using fallback hardcoded questions.");
        List<Question> questions = new ArrayList<>();
        questions.add(new Question(
                1,
                "What is the capital of Portugal? (Fallback)",
                new String[] {
                        "Madrid",
                        "Lisbon",
                        "Porto",
                        "Barcelona"
                },
                1 
        ));
        questions.add(new Question(
                2,
                "Which year was ISCTE founded? (Fallback)",
                new String[] {
                        "1972",
                        "1980",
                        "1974",
                        "1990"
                },
                2 
        ));
        return questions;
    }
}