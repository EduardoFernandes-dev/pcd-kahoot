package models;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class TeamScore implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String teamCode;
    private int totalPoints;
    private int lastRoundPoints;
    private final Map<String, PlayerScore> players;
    public TeamScore(String teamCode) {
        if (teamCode == null || teamCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Team code cannot be null or empty");
        }
        this.teamCode = teamCode;
        this.totalPoints = 0;
        this.lastRoundPoints = 0;
        this.players = new HashMap<>();
    }
    public synchronized void addPlayer(PlayerScore player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (!player.getTeamCode().equals(teamCode)) {
            throw new IllegalArgumentException("Player team code does not match");
        }
        players.put(player.getUsername(), player);
    }
    public synchronized void addTeamPoints(int points) {
        lastRoundPoints = points;
        totalPoints += points;
    }
    public synchronized void calculateTotalFromPlayers() {
        int roundSum = 0;
        for (PlayerScore player : players.values()) {
            roundSum += player.getLastRoundPoints();
        }
        lastRoundPoints = roundSum;
        totalPoints = 0;
        for (PlayerScore player : players.values()) {
            totalPoints += player.getTotalPoints();
        }
    }
    public synchronized PlayerScore getPlayerScore(String username) {
        return players.get(username);
    }
    public synchronized List<PlayerScore> getPlayers() {
        return new ArrayList<>(players.values());
    }
    public synchronized void resetRoundData() {
        lastRoundPoints = 0;
        for (PlayerScore player : players.values()) {
            player.resetRoundData();
        }
    }
    public String getTeamCode() {
        return teamCode;
    }
    public synchronized int getTotalPoints() {
        return totalPoints;
    }
    public synchronized int getLastRoundPoints() {
        return lastRoundPoints;
    }
    public synchronized int getPlayerCount() {
        return players.size();
    }
    @Override
    public String toString() {
        return String.format("TeamScore{team='%s', total=%d, players=%d}",
            teamCode, totalPoints, players.size());
    }
}
