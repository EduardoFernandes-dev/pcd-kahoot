package network;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class ScoreboardMessage extends Message {
    private static final long serialVersionUID = 1L;
    private final Map<String, TeamScore> teamScores;
    private final int currentRound;
    private final int totalRounds;
    public ScoreboardMessage(Map<String, TeamScore> teamScores, int currentRound, int totalRounds) {
        super(MessageType.SCOREBOARD);
        if (teamScores == null) {
            throw new IllegalArgumentException("Team scores cannot be null");
        }
        if (currentRound < 1 || totalRounds < 1) {
            throw new IllegalArgumentException("Round numbers must be positive");
        }
        this.teamScores = new HashMap<>(teamScores);
        this.currentRound = currentRound;
        this.totalRounds = totalRounds;
    }
    public Map<String, TeamScore> getTeamScores() {
        return new HashMap<>(teamScores);
    }
    public int getCurrentRound() {
        return currentRound;
    }
    public int getTotalRounds() {
        return totalRounds;
    }
    public static class TeamScore implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String teamCode;
        private final int totalPoints;
        private final int roundPoints;
        private final List<PlayerScore> players;
        public TeamScore(String teamCode, int totalPoints, int roundPoints, List<PlayerScore> players) {
            if (teamCode == null || teamCode.trim().isEmpty()) {
                throw new IllegalArgumentException("Team code cannot be null or empty");
            }
            if (players == null) {
                throw new IllegalArgumentException("Players list cannot be null");
            }
            this.teamCode = teamCode;
            this.totalPoints = totalPoints;
            this.roundPoints = roundPoints;
            this.players = new ArrayList<>(players);
        }
        public String getTeamCode() {
            return teamCode;
        }
        public int getTotalPoints() {
            return totalPoints;
        }
        public int getRoundPoints() {
            return roundPoints;
        }
        public List<PlayerScore> getPlayers() {
            return new ArrayList<>(players);
        }
    }
    public static class PlayerScore implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String username;
        private final int roundPoints;
        private final boolean wasCorrect;
        private final int bonusMultiplier;
        public PlayerScore(String username, int roundPoints, boolean wasCorrect, int bonusMultiplier) {
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username cannot be null or empty");
            }
            if (bonusMultiplier != 1 && bonusMultiplier != 2) {
                throw new IllegalArgumentException("Bonus multiplier must be 1 or 2");
            }
            this.username = username;
            this.roundPoints = roundPoints;
            this.wasCorrect = wasCorrect;
            this.bonusMultiplier = bonusMultiplier;
        }
        public String getUsername() {
            return username;
        }
        public int getRoundPoints() {
            return roundPoints;
        }
        public boolean wasCorrect() {
            return wasCorrect;
        }
        public int getBonusMultiplier() {
            return bonusMultiplier;
        }
    }
    @Override
    public String toString() {
        return String.format("ScoreboardMessage{round=%d/%d, teams=%d}",
            currentRound, totalRounds, teamScores.size());
    }
}
