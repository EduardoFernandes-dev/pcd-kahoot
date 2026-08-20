package models;
import java.util.*;
public class GameState {
    public enum GamePhase {
        WAITING_FOR_PLAYERS, 
        READY_TO_START, 
        IN_PROGRESS, 
        FINISHED 
    }
    private final String roomCode;
    private final Map<String, Team> teams; 
    private final Map<String, Player> players; 
    private final List<Question> questions;
    private int currentQuestionIndex;
    private GamePhase phase;
    private final Map<String, PlayerScore> playerScores; 
    private final Map<String, TeamScore> teamScores; 
    public GameState(String roomCode) {
        if (roomCode == null || roomCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Room code cannot be null or empty");
        }
        this.roomCode = roomCode.trim();
        this.teams = new HashMap<>();
        this.players = new HashMap<>();
        this.questions = new ArrayList<>();
        this.currentQuestionIndex = 0;
        this.phase = GamePhase.WAITING_FOR_PLAYERS;
        this.playerScores = new HashMap<>();
        this.teamScores = new HashMap<>();
    }
    public String getRoomCode() {
        return roomCode;
    }
    public GamePhase getPhase() {
        return phase;
    }
    public synchronized void setPhase(GamePhase phase) {
        this.phase = phase;
    }
    public synchronized void addTeam(Team team) {
        teams.put(team.getTeamCode(), team);
    }
    public Team getTeam(String teamCode) {
        return teams.get(teamCode);
    }
    public Collection<Team> getAllTeams() {
        return Collections.unmodifiableCollection(teams.values());
    }
    public boolean hasTeam(String teamCode) {
        return teams.containsKey(teamCode);
    }
    public synchronized boolean addPlayer(Player player) {
        if (players.containsKey(player.getUsername())) {
            return false;
        }
        Team team = teams.get(player.getTeamCode());
        if (team == null) {
            return false;
        }
        if (!team.addPlayer(player)) {
            return false; 
        }
        players.put(player.getUsername(), player);
        return true;
    }
    public Player getPlayer(String username) {
        return players.get(username);
    }
    public Collection<Player> getAllPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }
    public boolean isUsernameTaken(String username) {
        return players.containsKey(username);
    }
    public synchronized int getConnectedPlayerCount() {
        return players.size();
    }
    public synchronized boolean areAllTeamsComplete() {
        if (teams.isEmpty()) {
            return false;
        }
        for (Team team : teams.values()) {
            if (!team.isComplete()) {
                return false;
            }
        }
        return true;
    }
    public synchronized void setQuestions(List<Question> questions) {
        this.questions.clear();
        this.questions.addAll(questions);
        this.currentQuestionIndex = 0;
    }
    public Question getCurrentQuestion() {
        if (currentQuestionIndex < questions.size()) {
            return questions.get(currentQuestionIndex);
        }
        return null;
    }
    public int getCurrentQuestionNumber() {
        return currentQuestionIndex + 1;
    }
    public int getTotalQuestions() {
        return questions.size();
    }
    public synchronized boolean nextQuestion() {
        if (currentQuestionIndex < questions.size() - 1) {
            currentQuestionIndex++;
            return true;
        }
        return false;
    }
    public boolean hasMoreQuestions() {
        return currentQuestionIndex < questions.size() - 1;
    }
    public synchronized void initializeScores() {
        playerScores.clear();
        teamScores.clear();
        for (Team team : teams.values()) {
            TeamScore teamScore = new TeamScore(team.getTeamCode());
            teamScores.put(team.getTeamCode(), teamScore);
            for (Player player : team.getPlayers()) {
                PlayerScore playerScore = new PlayerScore(player.getUsername(), team.getTeamCode());
                playerScores.put(player.getUsername(), playerScore);
                teamScore.addPlayer(playerScore);
            }
        }
    }
    public PlayerScore getPlayerScore(String username) {
        return playerScores.get(username);
    }
    public TeamScore getTeamScore(String teamCode) {
        return teamScores.get(teamCode);
    }
    public synchronized Map<String, PlayerScore> getAllPlayerScores() {
        return new HashMap<>(playerScores);
    }
    public synchronized Map<String, TeamScore> getAllTeamScores() {
        return new HashMap<>(teamScores);
    }
    public synchronized void resetRoundScores() {
        for (TeamScore teamScore : teamScores.values()) {
            teamScore.resetRoundData();
        }
    }
    public synchronized void resetForRestart() {
        currentQuestionIndex = 0;
        questions.clear();
        phase = GamePhase.WAITING_FOR_PLAYERS;
        playerScores.clear();
        teamScores.clear();
        System.out.println("GameState reset for restart");
    }
    @Override
    public String toString() {
        return String.format("GameState[room=%s, phase=%s, players=%d, teams=%d, question=%d/%d]",
                roomCode, phase, players.size(), teams.size(),
                getCurrentQuestionNumber(), getTotalQuestions());
    }
}
