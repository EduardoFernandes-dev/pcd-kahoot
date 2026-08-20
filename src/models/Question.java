package models;
import java.io.Serializable;
public class Question implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int questionNumber;
    private final String questionText;
    private final String[] options;  
    private final int correctAnswerIndex;  
    private final int points;  
    private final String type;  
    public Question(int questionNumber, String questionText, String[] options, int correctAnswerIndex) {
        this(questionNumber, questionText, options, correctAnswerIndex, 5, "individual");
    }
    public Question(int questionNumber, String questionText, String[] options, int correctAnswerIndex, int points, String type) {
        if (options.length != 4) {
            throw new IllegalArgumentException("Question must have exactly 4 options");
        }
        if (correctAnswerIndex < 0 || correctAnswerIndex > 3) {
            throw new IllegalArgumentException("Correct answer index must be 0-3");
        }
        if (points <= 0) {
            throw new IllegalArgumentException("Points must be positive");
        }
        if (type == null || (!type.equals("individual") && !type.equals("team"))) {
            throw new IllegalArgumentException("Type must be 'individual' or 'team'");
        }
        this.questionNumber = questionNumber;
        this.questionText = questionText;
        this.options = options.clone();  
        this.correctAnswerIndex = correctAnswerIndex;
        this.points = points;
        this.type = type;
    }
    public int getQuestionNumber() {
        return questionNumber;
    }
    public String getQuestionText() {
        return questionText;
    }
    public String[] getOptions() {
        return options.clone();  
    }
    public String getOption(int index) {
        if (index < 0 || index > 3) {
            throw new IllegalArgumentException("Option index must be 0-3");
        }
        return options[index];
    }
    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }
    public int getPoints() {
        return points;
    }
    public String getType() {
        return type;
    }
    public boolean isIndividual() {
        return "individual".equals(type);
    }
    public boolean isTeamQuestion() {
        return "team".equals(type);
    }
    @Override
    public String toString() {
        return String.format("Question %d: %s [%s, %d pts]", questionNumber, questionText, type, points);
    }
}
