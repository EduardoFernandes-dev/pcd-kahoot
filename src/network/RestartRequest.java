package network;
public class RestartRequest extends Message {
    private static final long serialVersionUID = 1L;
    private final String username;
    public RestartRequest(String username) {
        super(MessageType.RESTART_REQUEST);
        this.username = username;
    }
    public String getUsername() {
        return username;
    }
    @Override
    public String toString() {
        return "RestartRequest{username='" + username + "'}";
    }
}
