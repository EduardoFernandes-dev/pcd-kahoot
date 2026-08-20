package network;
public class ConnectionRequest extends Message {
    private static final long serialVersionUID = 1L;
    private final String salaCode;     
    private final String equipaCode;   
    private final String username;     
    public ConnectionRequest(String salaCode, String equipaCode, String username) {
        super(MessageType.CONNECTION_REQUEST);
        if (salaCode == null || salaCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Sala code cannot be null or empty");
        }
        if (equipaCode == null || equipaCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Equipa code cannot be null or empty");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        this.salaCode = salaCode.trim();
        this.equipaCode = equipaCode.trim();
        this.username = username.trim();
    }
    public String getSalaCode() {
        return salaCode;
    }
    public String getEquipaCode() {
        return equipaCode;
    }
    public String getUsername() {
        return username;
    }
    @Override
    public String toString() {
        return String.format("ConnectionRequest[sala=%s, equipa=%s, username=%s]",
            salaCode, equipaCode, username);
    }
}
