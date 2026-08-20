package server;
import coordination.CustomBarrier;
import coordination.ModifiedCountdownLatch;
import models.*;
import network.AnswerMessage;
import network.QuestionMessage;
import network.ScoreboardMessage;
import utils.QuestionLoader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
public class GameRoom {
    private final GameState gameState;
    private final int expectedPlayers;
    private ModifiedCountdownLatch individualLatch;
    private Map<String, CustomBarrier> teamBarriers;
    private Thread questionTimer;
    private volatile boolean waitingForAnswers = false;
    private final Map<String, AnswerMessage> collectedAnswers = new HashMap<>();
    private final Map<String, Integer> answerMultipliers = new HashMap<>();
    private static final int QUESTION_TIMEOUT_SECONDS = 30;
    private volatile int completedTeams = 0;
    private final Set<String> restartVotes = new HashSet<>();
    public GameRoom(String roomCode, String[] teamCodes, int playersPerTeam) {
        this.gameState = new GameState(roomCode);
        this.expectedPlayers = teamCodes.length * playersPerTeam;
        for (String teamCode : teamCodes) {
            Team team = new Team(teamCode, playersPerTeam);
            gameState.addTeam(team);
        }
        System.out.println("GameRoom created: " + roomCode);
        System.out.println("  Teams: " + teamCodes.length);
        System.out.println("  Players per team: " + playersPerTeam);
        System.out.println("  Total expected players: " + expectedPlayers);
    }
    public synchronized ValidationResult validateAndAddPlayer(String username, String equipaCode) {
        if (gameState.isUsernameTaken(username)) {
            return ValidationResult.failure("Username '" + username + "' is already in use");
        }
        if (!gameState.hasTeam(equipaCode)) {
            return ValidationResult.failure("Team '" + equipaCode + "' does not exist in this room");
        }
        Team team = gameState.getTeam(equipaCode);
        if (team.isFull()) {
            return ValidationResult.failure("Team '" + equipaCode + "' is full");
        }
        Player player = new Player(username, equipaCode);
        player.setConnected(true);
        if (!gameState.addPlayer(player)) {
            return ValidationResult.failure("Failed to add player (internal error)");
        }
        System.out.println("Player added: " + username + " → " + equipaCode +
                " (Total: " + gameState.getConnectedPlayerCount() + "/" + expectedPlayers + ")");
        return ValidationResult.success(player, team.getPlayerCount());
    }
    public synchronized boolean isReady() {
        return gameState.areAllTeamsComplete();
    }
    public GameState getGameState() {
        return gameState;
    }
    public synchronized int getConnectedPlayerCount() {
        return gameState.getConnectedPlayerCount();
    }
    public synchronized void playerDisconnected(String username, String teamCode) {
        System.out.println("Player disconnected: " + username + " from team " + teamCode);
        
        if (!waitingForAnswers) {
            return;
        }
        
        Question currentQuestion = gameState.getCurrentQuestion();
        if (currentQuestion == null) {
            return;
        }
        
        if (currentQuestion.isIndividual()) {
            if (individualLatch != null) {
                individualLatch.reduceCount();
                System.out.println("Reduced latch count for disconnected player");
            }
        } else {
            if (teamBarriers != null && teamBarriers.containsKey(teamCode)) {
                CustomBarrier barrier = teamBarriers.get(teamCode);
                barrier.reduceParties();
                System.out.println("Reduced barrier count for team " + teamCode);
            }
        }
    }
    public void loadQuestions(String jsonPath) throws IOException {
        System.out.println("Loading questions from: " + jsonPath);
        List<Question> questions = QuestionLoader.loadAllQuestions(jsonPath);
        gameState.setQuestions(questions);
        System.out.println("Loaded " + questions.size() + " questions");
    }
    public synchronized void broadcastCurrentQuestion() {
        Question currentQuestion = gameState.getCurrentQuestion();
        if (currentQuestion == null) {
            System.err.println("No current question to broadcast");
            return;
        }
        int questionNum = gameState.getCurrentQuestionNumber();
        int totalQuestions = gameState.getTotalQuestions();
        QuestionMessage message = new QuestionMessage(currentQuestion, questionNum, totalQuestions);
        System.out.println("Broadcasting question " + questionNum + "/" + totalQuestions +
                ": " + currentQuestion.getQuestionText());
        int successCount = 0;
        int failureCount = 0;
        for (Player player : gameState.getAllPlayers()) {
            try {
                ObjectOutputStream out = player.getOutputStream();
                if (out != null) {
                    out.writeObject(message);
                    out.flush();
                    successCount++;
                } else {
                    System.err.println("Player " + player.getUsername() + " has no output stream");
                    failureCount++;
                }
            } catch (IOException e) {
                System.err.println("Failed to send question to " + player.getUsername() + ": " + e.getMessage());
                failureCount++;
            }
        }
        System.out.println("Question broadcast complete: " + successCount + " sent, " + failureCount + " failed");
    }
    public synchronized void startGame() {
        gameState.setPhase(GameState.GamePhase.IN_PROGRESS);
        gameState.initializeScores();
        System.out.println("Game started in room: " + gameState.getRoomCode());
        System.out.println("Scores initialized for all players and teams");
        notifyAll(); 
    }
    public synchronized VoteResult voteForRestart(String username) throws java.io.IOException {
        restartVotes.add(username);
        int currentVotes = restartVotes.size();
        int totalPlayers = expectedPlayers;
        System.out.println("Restart vote from " + username + " (" + currentVotes + "/" + totalPlayers + ")");
        if (currentVotes >= totalPlayers) {
            System.out.println("All players voted for restart!");
            restartVotes.clear();
            restartGame();
            return new VoteResult(currentVotes, totalPlayers, true);
        }
        return new VoteResult(currentVotes, totalPlayers, false);
    }
    public synchronized VoteResult getRestartVoteStatus() {
        return new VoteResult(restartVotes.size(), expectedPlayers, false);
    }
    public static class VoteResult {
        public final int currentVotes;
        public final int totalPlayers;
        public final boolean restarted;
        public VoteResult(int currentVotes, int totalPlayers, boolean restarted) {
            this.currentVotes = currentVotes;
            this.totalPlayers = totalPlayers;
            this.restarted = restarted;
        }
    }
    public synchronized void restartGame() throws java.io.IOException {
        System.out.println("Restarting game in room: " + gameState.getRoomCode());
        restartVotes.clear();
        gameState.resetForRestart();
        if (questionTimer != null && questionTimer.isAlive()) {
            questionTimer.interrupt();
        }
        collectedAnswers.clear();
        answerMultipliers.clear();
        waitingForAnswers = false;
        completedTeams = 0;
        String[] questionsPaths = {
                "../resources/questions.json",
                "resources/questions.json",
                "Project/resources/questions.json"
        };
        for (String path : questionsPaths) {
            java.io.File f = new java.io.File(path);
            if (f.exists()) {
                loadQuestions(path);
                break;
            }
        }
        gameState.initializeScores();
        gameState.setPhase(GameState.GamePhase.IN_PROGRESS);
        broadcastCurrentQuestion();
        startQuestionRound();
        System.out.println("Game restarted successfully!");
        notifyAll();
    }
    public synchronized void submitAnswer(AnswerMessage answer, Player player) throws InterruptedException {
        if (answer == null || player == null) {
            return;
        }
        collectedAnswers.put(answer.getUsername(), answer);
        Question currentQuestion = gameState.getCurrentQuestion();
        if (currentQuestion == null) {
            return;
        }
        if (currentQuestion.isIndividual()) {
            ModifiedCountdownLatch latch = individualLatch;
            if (latch != null) {
                int multiplier = latch.countdown();
                answerMultipliers.put(answer.getUsername(), multiplier);
                System.out.println("Answer from " + answer.getUsername() +
                        " (multiplier: " + multiplier + "x)");
            }
        } else {
            String teamCode = player.getTeamCode();
            CustomBarrier barrier = teamBarriers.get(teamCode);
            if (barrier != null) {
                try {
                    barrier.await(QUESTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    System.out.println("Answer from " + answer.getUsername() +
                            " (team: " + teamCode + ")");
                } catch (Exception e) {
                    System.out.println("Barrier timeout for team " + teamCode);
                }
            }
        }
    }
    public synchronized boolean isWaitingForAnswers() {
        return waitingForAnswers;
    }
    public synchronized boolean isGameInProgress() {
        return gameState.getPhase() == GameState.GamePhase.IN_PROGRESS;
    }
    public synchronized void startQuestionRound() {
        Question currentQuestion = gameState.getCurrentQuestion();
        if (currentQuestion == null) {
            return;
        }
        collectedAnswers.clear();
        answerMultipliers.clear();
        waitingForAnswers = true;
        if (currentQuestion.isIndividual()) {
            individualLatch = new ModifiedCountdownLatch(
                    2, 
                    2, 
                    QUESTION_TIMEOUT_SECONDS * 1000, 
                    expectedPlayers 
            );
            questionTimer = new Thread(() -> {
                try {
                    individualLatch.await();
                    if (!individualLatch.isTimeoutReached()) {
                        System.out.println("All players answered");
                    } else {
                        System.out.println("Question timeout - " +
                                individualLatch.getCountdownsReceived() + "/" +
                                expectedPlayers + " answered");
                    }
                    onQuestionComplete();
                } catch (InterruptedException e) {
                    System.out.println("Timer interrupted");
                }
            });
            questionTimer.setDaemon(true);
            questionTimer.start();
        } else {
            completedTeams = 0;
            int totalTeams = gameState.getAllTeams().size();
            teamBarriers = new HashMap<>();
            for (Team team : gameState.getAllTeams()) {
                int teamSize = team.getPlayerCount();
                CustomBarrier barrier = new CustomBarrier(teamSize, () -> {
                    String teamCode = team.getTeamCode();
                    System.out.println("Team " + teamCode + " complete");
                    synchronized (GameRoom.this) {
                        completedTeams++;
                        if (completedTeams >= totalTeams) {
                            GameRoom.this.notifyAll();
                        }
                    }
                });
                teamBarriers.put(team.getTeamCode(), barrier);
            }
            questionTimer = new Thread(() -> {
                try {
                    synchronized (GameRoom.this) {
                        long deadline = System.currentTimeMillis() + (QUESTION_TIMEOUT_SECONDS * 1000L);
                        long remaining = QUESTION_TIMEOUT_SECONDS * 1000L;
                        while (completedTeams < totalTeams && remaining > 0) {
                            GameRoom.this.wait(remaining);
                            remaining = deadline - System.currentTimeMillis();
                        }
                        if (completedTeams >= totalTeams) {
                            System.out.println("All teams completed!");
                        } else {
                            System.out.println("Team question timeout");
                        }
                    }
                    onQuestionComplete();
                } catch (InterruptedException e) {
                    System.out.println("Timer interrupted");
                }
            });
            questionTimer.setDaemon(true);
            questionTimer.start();
        }
        System.out.println("Question round started (" + currentQuestion.getType() +
                ", " + QUESTION_TIMEOUT_SECONDS + "s timer)");
    }
    private void onQuestionComplete() {
        synchronized (this) {
            if (!waitingForAnswers) {
                return;
            }
            waitingForAnswers = false;
            if (questionTimer != null && questionTimer.isAlive() && 
                    Thread.currentThread() != questionTimer) {
                questionTimer.interrupt();
                try {
                    questionTimer.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            processAnswersAndScore();
            broadcastScoreboard();
        }
        System.out.println("Waiting 5 seconds for players to see feedback...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Advancing to next question...");
        synchronized (this) {
            advanceToNextQuestion();
        }
    }
    private synchronized void processAnswersAndScore() {
        Question currentQuestion = gameState.getCurrentQuestion();
        if (currentQuestion == null) {
            return;
        }
        System.out.println("Processing " + collectedAnswers.size() + " answers");
        if (currentQuestion.isIndividual()) {
            ScoringEngine.scoreIndividualQuestion(
                    currentQuestion,
                    collectedAnswers,
                    answerMultipliers,
                    gameState);
        } else {
            Map<String, Map<String, AnswerMessage>> teamAnswers = ScoringEngine.groupAnswersByTeam(collectedAnswers,
                    gameState);
            ScoringEngine.scoreTeamQuestion(
                    currentQuestion,
                    teamAnswers,
                    gameState);
        }
        System.out.println("Scoring complete");
    }
    private synchronized void broadcastScoreboard() {
        Map<String, TeamScore> teamScores = gameState.getAllTeamScores();
        Map<String, ScoreboardMessage.TeamScore> messageteamScores = new HashMap<>();
        for (Map.Entry<String, TeamScore> entry : teamScores.entrySet()) {
            TeamScore teamScore = entry.getValue();
            List<ScoreboardMessage.PlayerScore> playerScores = new ArrayList<>();
            for (PlayerScore ps : teamScore.getPlayers()) {
                playerScores.add(new ScoreboardMessage.PlayerScore(
                        ps.getUsername(),
                        ps.getLastRoundPoints(),
                        ps.wasLastAnswerCorrect(),
                        ps.getLastBonusMultiplier()));
            }
            messageteamScores.put(entry.getKey(), new ScoreboardMessage.TeamScore(
                    teamScore.getTeamCode(),
                    teamScore.getTotalPoints(),
                    teamScore.getLastRoundPoints(),
                    playerScores));
        }
        ScoreboardMessage message = new ScoreboardMessage(
                messageteamScores,
                gameState.getCurrentQuestionNumber(),
                gameState.getTotalQuestions());
        for (Player player : gameState.getAllPlayers()) {
            try {
                ObjectOutputStream out = player.getOutputStream();
                if (out != null) {
                    out.writeObject(message);
                    out.flush();
                }
            } catch (IOException e) {
                System.err.println("Failed to send scoreboard to " +
                        player.getUsername() + ": " + e.getMessage());
            }
        }
        System.out.println("Scoreboard broadcast complete");
    }
    private synchronized void advanceToNextQuestion() {
        if (gameState.hasMoreQuestions()) {
            gameState.nextQuestion();
            gameState.resetRoundScores();
            broadcastCurrentQuestion();
            startQuestionRound();
        } else {
            endGame();
        }
    }
    private synchronized void endGame() {
        gameState.setPhase(GameState.GamePhase.FINISHED);
        System.out.println("Game finished in room: " + gameState.getRoomCode());
        broadcastScoreboard();
    }
    public static class ValidationResult {
        private final boolean success;
        private final String message;
        private final Player player;
        private final int teamPlayerCount;
        private ValidationResult(boolean success, String message, Player player, int teamPlayerCount) {
            this.success = success;
            this.message = message;
            this.player = player;
            this.teamPlayerCount = teamPlayerCount;
        }
        public static ValidationResult success(Player player, int teamPlayerCount) {
            return new ValidationResult(true, "Player accepted", player, teamPlayerCount);
        }
        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message, null, 0);
        }
        public boolean isSuccess() {
            return success;
        }
        public String getMessage() {
            return message;
        }
        public Player getPlayer() {
            return player;
        }
        public int getTeamPlayerCount() {
            return teamPlayerCount;
        }
    }
}
