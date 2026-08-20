package models;
import java.io.ObjectOutputStream;
import java.io.Serializable;
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String teamCode;
    private transient ObjectOutputStream outputStream;  
    private boolean connected;
    public Player(String username, String teamCode) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (teamCode == null || teamCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Team code cannot be null or empty");
        }
        this.username = username.trim();
        this.teamCode = teamCode.trim();
        this.connected = false;
        this.outputStream = null;
    }
    public String getUsername() {
        return username;
    }
    public String getTeamCode() {
        return teamCode;
    }
    public boolean isConnected() {
        return connected;
    }
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
    public ObjectOutputStream getOutputStream() {
        return outputStream;
    }
    public void setOutputStream(ObjectOutputStream outputStream) {
        this.outputStream = outputStream;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Player)) return false;
        Player other = (Player) obj;
        return username.equals(other.username);
    }
    @Override
    public int hashCode() {
        return username.hashCode();
    }
    @Override
    public String toString() {
        return String.format("Player[username=%s, team=%s, connected=%s]",
            username, teamCode, connected);
    }
}
