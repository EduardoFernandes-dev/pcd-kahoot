package models;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class Team implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String teamCode;
    private final int maxPlayers;
    private final List<Player> players;
    public Team(String teamCode, int maxPlayers) {
        if (teamCode == null || teamCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Team code cannot be null or empty");
        }
        if (maxPlayers < 1) {
            throw new IllegalArgumentException("Max players must be at least 1");
        }
        this.teamCode = teamCode.trim();
        this.maxPlayers = maxPlayers;
        this.players = new ArrayList<>();
    }
    public String getTeamCode() {
        return teamCode;
    }
    public int getMaxPlayers() {
        return maxPlayers;
    }
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }
    public int getPlayerCount() {
        return players.size();
    }
    public boolean isFull() {
        return players.size() >= maxPlayers;
    }
    public boolean isComplete() {
        if (players.size() < maxPlayers) {
            return false;
        }
        for (Player player : players) {
            if (!player.isConnected()) {
                return false;
            }
        }
        return true;
    }
    public synchronized boolean addPlayer(Player player) {
        if (isFull()) {
            return false;
        }
        if (players.contains(player)) {
            return false; 
        }
        return players.add(player);
    }
    public synchronized boolean removePlayer(Player player) {
        return players.remove(player);
    }
    public boolean hasPlayer(String username) {
        for (Player player : players) {
            if (player.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }
    @Override
    public String toString() {
        return String.format("Team[code=%s, players=%d/%d, complete=%s]",
            teamCode, players.size(), maxPlayers, isComplete());
    }
}
