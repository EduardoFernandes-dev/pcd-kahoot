package client;
import models.Question;
import javax.swing.*;
import java.awt.*;
public class ClientGUI {
    private static final Color COR_FUNDO = new Color(240, 242, 245);
    private static final Color COR_HEADER = new Color(66, 103, 178);
    private static final Color COR_BOTAO = new Color(100, 149, 237);
    private static final Color COR_CORRETA = new Color(76, 175, 80);
    private static final Color COR_ERRADA = new Color(244, 67, 54);
    private JFrame frame;
    private JLabel questionNumLabel;
    private JTextArea questionText;
    private JButton[] answerButtons;
    private JLabel feedbackLabel;
    private int selectedAnswer = -1;
    private AnswerSelectionListener answerListener;
    private JPanel scoreboardPanel;
    private JLabel[] teamLabels;
    private JLabel[] teamScoreLabels;
    private static final int MAX_TEAMS = 10;
    private GameClient gameClient;
    private int currentQuestionNumber;
    private JButton restartButton;
    private RestartListener restartListener;
    private String playerUsername;
    public ClientGUI() {
        frame = new JFrame("IsKahoot - Simple Quiz");
        frame.setSize(900, 550);
        frame.setMinimumSize(new Dimension(800, 500));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(COR_FUNDO);
        createComponents();
        layoutComponents();
    }
    public void open() {
        frame.setVisible(true);
    }
    private void createComponents() {
        questionNumLabel = createLabel("Question 0/0", Font.BOLD, 20, Color.BLACK);
        questionNumLabel.setHorizontalAlignment(SwingConstants.CENTER);
        questionText = new JTextArea("Waiting for question...");
        questionText.setFont(new Font("Arial", Font.PLAIN, 18));
        questionText.setLineWrap(true);
        questionText.setWrapStyleWord(true);
        questionText.setEditable(false);
        questionText.setFocusable(false);
        questionText.setBackground(Color.WHITE);
        answerButtons = new JButton[4];
        for (int i = 0; i < 4; i++) {
            answerButtons[i] = createAnswerButton(i);
        }
        feedbackLabel = createLabel("", Font.BOLD, 24, Color.BLACK);
        feedbackLabel.setHorizontalAlignment(SwingConstants.CENTER);
        feedbackLabel.setOpaque(false);
        feedbackLabel.setPreferredSize(new Dimension(200, 40));
        createScoreboardPanel();
    }
    private void createScoreboardPanel() {
        scoreboardPanel = new JPanel();
        scoreboardPanel.setLayout(new BoxLayout(scoreboardPanel, BoxLayout.Y_AXIS));
        scoreboardPanel.setBackground(new Color(45, 55, 72));
        scoreboardPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        scoreboardPanel.setPreferredSize(new Dimension(220, 400));
        JLabel scoreboardTitle = new JLabel("SCOREBOARD");
        scoreboardTitle.setFont(new Font("Arial", Font.BOLD, 18));
        scoreboardTitle.setForeground(Color.WHITE);
        scoreboardTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        scoreboardPanel.add(scoreboardTitle);
        scoreboardPanel.add(Box.createVerticalStrut(15));
        teamLabels = new JLabel[MAX_TEAMS];
        teamScoreLabels = new JLabel[MAX_TEAMS];
        for (int i = 0; i < MAX_TEAMS; i++) {
            JPanel teamRow = new JPanel(new BorderLayout(10, 0));
            teamRow.setBackground(new Color(45, 55, 72));
            teamRow.setMaximumSize(new Dimension(200, 35));
            teamRow.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            teamLabels[i] = new JLabel("");
            teamLabels[i].setFont(new Font("Arial", Font.BOLD, 14));
            teamLabels[i].setForeground(Color.WHITE);
            teamScoreLabels[i] = new JLabel("0 pts");
            teamScoreLabels[i].setFont(new Font("Arial", Font.BOLD, 14));
            teamScoreLabels[i].setForeground(Color.WHITE);
            teamScoreLabels[i].setHorizontalAlignment(SwingConstants.RIGHT);
            teamRow.add(teamLabels[i], BorderLayout.WEST);
            teamRow.add(teamScoreLabels[i], BorderLayout.EAST);
            teamRow.setVisible(false);
            scoreboardPanel.add(teamRow);
        }
        scoreboardPanel.add(Box.createVerticalGlue());
    }
    private JLabel createLabel(String text, int style, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", style, size));
        label.setForeground(color);
        return label;
    }
    private JButton createAnswerButton(int index) {
        JButton btn = new JButton();
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setBackground(COR_BOTAO);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled())
                    btn.setBackground(COR_BOTAO.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn.isEnabled() && selectedAnswer != index)
                    btn.setBackground(COR_BOTAO);
            }
        });
        btn.addActionListener(e -> handleAnswerClick(index));
        return btn;
    }
    private void layoutComponents() {
        frame.setLayout(new BorderLayout(10, 10));
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COR_HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JLabel title = createLabel("IsKahoot Quiz", Font.BOLD, 24, Color.WHITE);
        header.add(title, BorderLayout.CENTER);
        frame.add(header, BorderLayout.NORTH);
        frame.add(createQuestionPanel(), BorderLayout.CENTER);
        frame.add(scoreboardPanel, BorderLayout.EAST);
    }
    private JPanel createQuestionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COR_FUNDO);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        questionNumLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(questionNumLabel, BorderLayout.NORTH);
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setBackground(Color.WHITE);
        textPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        textPanel.setPreferredSize(new Dimension(500, 100));
        textPanel.add(questionText, BorderLayout.CENTER);
        JPanel centerContent = new JPanel(new BorderLayout(10, 10));
        centerContent.setBackground(COR_FUNDO);
        centerContent.add(textPanel, BorderLayout.CENTER);
        centerContent.add(feedbackLabel, BorderLayout.SOUTH);
        panel.add(centerContent, BorderLayout.CENTER);
        JPanel answers = new JPanel(new GridLayout(2, 2, 15, 15));
        answers.setBackground(COR_FUNDO);
        answers.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        for (JButton btn : answerButtons)
            answers.add(btn);
        panel.add(answers, BorderLayout.SOUTH);
        return panel;
    }
    private void handleAnswerClick(int index) {
        if (selectedAnswer != -1)
            return;
        selectedAnswer = index;
        answerButtons[index].setBackground(new Color(70, 130, 180));
        enableAnswerButtons(false);
        if (answerListener != null)
            answerListener.onAnswerSelected(index);
    }
    public void displayQuestion(Question q, int currentNum, int totalQuestions) {
        selectedAnswer = -1;
        questionNumLabel.setText(String.format("Question %d/%d", currentNum, totalQuestions));
        questionText.setText(q.getQuestionText());
        if (restartButton != null && restartButton.isVisible()) {
            restartButton.setVisible(false);
            restartButton.setText("Recomeçar");
            restartButton.setEnabled(true);
        }
        if (!feedbackLabel.getText().isEmpty()) {
            javax.swing.Timer clearTimer = new javax.swing.Timer(3000, e -> {
                feedbackLabel.setText("");
            });
            clearTimer.setRepeats(false);
            clearTimer.start();
        } else {
            feedbackLabel.setText("");
        }
        String[] labels = { "A", "B", "C", "D" };
        for (int i = 0; i < 4; i++) {
            String text = String.format("<html><div style='text-align: center;'><b>%s)</b><br/>%s</div></html>",
                    labels[i], q.getOption(i));
            answerButtons[i].setText(text);
            answerButtons[i].setBackground(COR_BOTAO);
            answerButtons[i].setEnabled(true);
        }
    }
    public void showResult(boolean correct, int correctIndex) {
        if (correct) {
            feedbackLabel.setText("CERTO");
            feedbackLabel.setForeground(COR_CORRETA);
        } else {
            feedbackLabel.setText("ERRADO");
            feedbackLabel.setForeground(COR_ERRADA);
        }
        answerButtons[correctIndex].setBackground(COR_CORRETA);
        if (!correct && selectedAnswer != -1 && selectedAnswer != correctIndex) {
            answerButtons[selectedAnswer].setBackground(COR_ERRADA);
        }
    }
    public void enableAnswerButtons(boolean enabled) {
        for (JButton btn : answerButtons)
            btn.setEnabled(enabled);
    }
    public int getSelectedAnswerIndex() {
        return selectedAnswer;
    }
    public void setAnswerSelectionListener(AnswerSelectionListener listener) {
        this.answerListener = listener;
    }
    public void showGameComplete() {
        questionText.setText("Quiz Complete! Thank you for playing.");
        feedbackLabel.setText("");
        enableAnswerButtons(false);
        if (restartButton == null) {
            restartButton = new JButton("Recomeçar");
            restartButton.setFont(new Font("Arial", Font.BOLD, 16));
            restartButton.setBackground(COR_HEADER);
            restartButton.setForeground(Color.WHITE);
            restartButton.setFocusPainted(false);
            restartButton.addActionListener(e -> {
                if (restartListener != null) {
                    restartButton.setVisible(false);
                    restartListener.onRestartRequested();
                }
            });
        }
        JPanel centerPanel = (JPanel) feedbackLabel.getParent();
        if (centerPanel != null) {
            restartButton.setVisible(true);
            centerPanel.add(restartButton, BorderLayout.SOUTH);
            centerPanel.revalidate();
            centerPanel.repaint();
        }
    }
    public void showWaitingForConnection() {
        questionNumLabel.setText("Connecting...");
        questionText.setText("Connecting to server, please wait...");
        feedbackLabel.setText("");
        enableAnswerButtons(false);
        for (int i = 0; i < 4; i++) {
            answerButtons[i].setText("");
        }
    }
    public void showWaitingForPlayers(int currentPlayers) {
        questionNumLabel.setText("Waiting for Players");
        questionText.setText(
                String.format("Connected successfully!\n\nWaiting for other players to join...\n(%d players connected)",
                        currentPlayers));
        feedbackLabel.setText("");
        enableAnswerButtons(false);
        for (int i = 0; i < 4; i++) {
            answerButtons[i].setText("");
        }
    }
    public void setGameClient(GameClient client, int questionNumber) {
        this.gameClient = client;
        this.currentQuestionNumber = questionNumber;
        if (client != null) {
            for (int i = 0; i < 4; i++) {
                final int answerIndex = i;
                for (var listener : answerButtons[i].getActionListeners()) {
                    answerButtons[i].removeActionListener(listener);
                }
                answerButtons[i].addActionListener(e -> handleNetworkAnswerSelection(answerIndex));
            }
        }
    }
    public void setPlayerUsername(String username) {
        this.playerUsername = username;
    }
    private void handleNetworkAnswerSelection(int answerIndex) {
        if (gameClient == null || selectedAnswer != -1) {
            return;
        }
        selectedAnswer = answerIndex;
        answerButtons[answerIndex].setBackground(COR_BOTAO.brighter());
        enableAnswerButtons(false);
        try {
            gameClient.sendAnswer(answerIndex, currentQuestionNumber);
            feedbackLabel.setText("Answer sent! Waiting for other players...");
        } catch (Exception e) {
            feedbackLabel.setText("Error sending answer: " + e.getMessage());
        }
    }
    public void displayScoreboard(network.ScoreboardMessage scoreboard) {
        updateScoreboardPanel(scoreboard);
        boolean isLastRound = scoreboard.getCurrentRound() == scoreboard.getTotalRounds();
        if (playerUsername != null) {
            for (network.ScoreboardMessage.TeamScore team : scoreboard.getTeamScores().values()) {
                for (network.ScoreboardMessage.PlayerScore player : team.getPlayers()) {
                    if (player.getUsername().equals(playerUsername)) {
                        if (player.wasCorrect()) {
                            feedbackLabel.setText("CERTO! +" + player.getRoundPoints() + " pts");
                            feedbackLabel.setForeground(COR_CORRETA);
                        } else {
                            feedbackLabel.setText("ERRADO!");
                            feedbackLabel.setForeground(COR_ERRADA);
                        }
                        break;
                    }
                }
            }
        }
        if (isLastRound) {
            questionText.setText("Quiz Complete! Final Scores:");
            enableAnswerButtons(false);
            if (restartButton == null) {
                restartButton = new JButton("Recomeçar");
                restartButton.setFont(new Font("Arial", Font.BOLD, 16));
                restartButton.setBackground(COR_HEADER);
                restartButton.setForeground(Color.WHITE);
                restartButton.setFocusPainted(false);
                restartButton.addActionListener(e -> {
                    if (restartListener != null) {
                        restartButton.setText("Votou! (aguarde...)");
                        restartButton.setEnabled(false);
                        restartListener.onRestartRequested();
                    }
                });
            } else {
                restartButton.setText("Recomeçar");
                restartButton.setEnabled(true);
            }
            JPanel centerPanel = (JPanel) feedbackLabel.getParent();
            if (centerPanel != null) {
                restartButton.setVisible(true);
                centerPanel.add(restartButton, BorderLayout.SOUTH);
                centerPanel.revalidate();
                centerPanel.repaint();
            }
        } else {
            selectedAnswer = -1;
            enableAnswerButtons(true);
            for (int i = 0; i < 4; i++) {
                answerButtons[i].setBackground(COR_BOTAO);
            }
        }
    }
    private void updateScoreboardPanel(network.ScoreboardMessage scoreboard) {
        java.util.List<network.ScoreboardMessage.TeamScore> sortedTeams = new java.util.ArrayList<>(
                scoreboard.getTeamScores().values());
        sortedTeams.sort((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()));
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < MAX_TEAMS; i++) {
                teamLabels[i].getParent().setVisible(false);
            }
            int rank = 1;
            for (network.ScoreboardMessage.TeamScore team : sortedTeams) {
                if (rank > MAX_TEAMS)
                    break;
                int index = rank - 1;
                teamLabels[index].setText(rank + ". " + team.getTeamCode());
                String scoreText = team.getTotalPoints() + " pts";
                if (team.getRoundPoints() > 0) {
                    scoreText += " (+" + team.getRoundPoints() + ")";
                }
                teamScoreLabels[index].setText(scoreText);
                teamLabels[index].setForeground(Color.WHITE);
                teamScoreLabels[index].setForeground(Color.WHITE);
                teamLabels[index].getParent().setVisible(true);
                rank++;
            }
            scoreboardPanel.revalidate();
            scoreboardPanel.repaint();
        });
    }
    public void initializeScoreboard(java.util.List<String> teamCodes) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < MAX_TEAMS; i++) {
                teamLabels[i].getParent().setVisible(false);
            }
            int index = 0;
            for (String teamCode : teamCodes) {
                if (index >= MAX_TEAMS)
                    break;
                teamLabels[index].setText((index + 1) + ". " + teamCode);
                teamScoreLabels[index].setText("0 pts");
                teamLabels[index].setForeground(Color.WHITE);
                teamScoreLabels[index].setForeground(Color.WHITE);
                teamLabels[index].getParent().setVisible(true);
                index++;
            }
            scoreboardPanel.revalidate();
            scoreboardPanel.repaint();
        });
    }
    public interface AnswerSelectionListener {
        void onAnswerSelected(int answerIndex);
    }
    public interface RestartListener {
        void onRestartRequested();
    }
    public void setRestartListener(RestartListener listener) {
        this.restartListener = listener;
    }
}