package models;
import java.io.Serializable;
public class PlayerScore implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String teamCode;
    private int totalPoints;
    private int lastRoundPoints;
    private boolean lastAnswerCorrect;
    private int lastBonusMultiplier;
    public PlayerScore(String username, String teamCode) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (teamCode == null || teamCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Team code cannot be null or empty");
        }
        this.username = username;
        this.teamCode = teamCode;
        this.totalPoints = 0;
        this.lastRoundPoints = 0;
        this.lastAnswerCorrect = false;
        this.lastBonusMultiplier = 1;
    }
    public synchronized void addPoints(int basePoints, int multiplier, boolean correct) {
        if (correct) {
            lastRoundPoints = basePoints * multiplier;
            totalPoints += lastRoundPoints;
            lastBonusMultiplier = multiplier;
        } else {
            lastRoundPoints = 0;
            lastBonusMultiplier = 1;
        }
        lastAnswerCorrect = correct;
    }
    public synchronized void resetRoundData() {
        lastRoundPoints = 0;
        lastAnswerCorrect = false;
        lastBonusMultiplier = 1;
    }
    public String getUsername() {
        return username;
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
    public synchronized boolean wasLastAnswerCorrect() {
        return lastAnswerCorrect;
    }
    public synchronized int getLastBonusMultiplier() {
        return lastBonusMultiplier;
    }
    @Override
    public String toString() {
        return String.format("PlayerScore{user='%s', total=%d, lastRound=%d}",
            username, totalPoints, lastRoundPoints);
    }
}
