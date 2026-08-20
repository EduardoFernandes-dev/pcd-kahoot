package network;
import java.io.Serializable;
public abstract class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private final MessageType type;
    private final long timestamp;
    public enum MessageType {
        CONNECTION_REQUEST, 
        CONNECTION_RESPONSE, 
        QUESTION, 
        ANSWER, 
        SCOREBOARD, 
        GAME_START, 
        GAME_END, 
        RESTART_REQUEST 
    }
    protected Message(MessageType type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
    public MessageType getType() {
        return type;
    }
    public long getTimestamp() {
        return timestamp;
    }
    @Override
    public String toString() {
        return String.format("%s[type=%s, timestamp=%d]",
                getClass().getSimpleName(), type, timestamp);
    }
}
