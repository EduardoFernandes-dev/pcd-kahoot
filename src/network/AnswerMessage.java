package network;
public class AnswerMessage extends Message {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final int questionNumber;
    private final int selectedAnswer;
    private final long submitTimestamp;
    public AnswerMessage(String username, int questionNumber, int selectedAnswer) {
        super(MessageType.ANSWER);
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (questionNumber < 1) {
            throw new IllegalArgumentException("Question number must be positive");
        }
        if (selectedAnswer < 0 || selectedAnswer > 3) {
            throw new IllegalArgumentException("Selected answer must be between 0 and 3");
        }
        this.username = username;
        this.questionNumber = questionNumber;
        this.selectedAnswer = selectedAnswer;
        this.submitTimestamp = System.currentTimeMillis();
    }
    public String getUsername() {
        return username;
    }
    public int getQuestionNumber() {
        return questionNumber;
    }
    public int getSelectedAnswer() {
        return selectedAnswer;
    }
    public long getSubmitTimestamp() {
        return submitTimestamp;
    }
    @Override
    public String toString() {
        return String.format("AnswerMessage{user='%s', question=%d, answer=%d}",
            username, questionNumber, selectedAnswer);
    }
}
