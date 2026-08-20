package network;
public class ConnectionResponse extends Message {
    private static final long serialVersionUID = 1L;
    private final boolean accepted;
    private final String reason;
    private final String assignedTeam;  
    private final int teamPlayerCount;  
    private final int totalPlayerCount; 
    public static ConnectionResponse rejected(String reason) {
        return new ConnectionResponse(false, reason, null, 0, 0);
    }
    public static ConnectionResponse accepted(String assignedTeam, int teamPlayerCount, int totalPlayerCount) {
        return new ConnectionResponse(true, "Connection accepted", assignedTeam, teamPlayerCount, totalPlayerCount);
    }
    private ConnectionResponse(boolean accepted, String reason, String assignedTeam,
                               int teamPlayerCount, int totalPlayerCount) {
        super(MessageType.CONNECTION_RESPONSE);
        this.accepted = accepted;
        this.reason = reason;
        this.assignedTeam = assignedTeam;
        this.teamPlayerCount = teamPlayerCount;
        this.totalPlayerCount = totalPlayerCount;
    }
    public boolean isAccepted() {
        return accepted;
    }
    public String getReason() {
        return reason;
    }
    public String getAssignedTeam() {
        return assignedTeam;
    }
    public int getTeamPlayerCount() {
        return teamPlayerCount;
    }
    public int getTotalPlayerCount() {
        return totalPlayerCount;
    }
    @Override
    public String toString() {
        if (accepted) {
            return String.format("ConnectionResponse[accepted=true, team=%s, teamPlayers=%d, totalPlayers=%d]",
                assignedTeam, teamPlayerCount, totalPlayerCount);
        } else {
            return String.format("ConnectionResponse[accepted=false, reason=%s]", reason);
        }
    }
}
