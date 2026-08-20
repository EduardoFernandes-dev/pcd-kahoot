package network;
import models.Question;
public class QuestionMessage extends Message {
    private static final long serialVersionUID = 1L;
    private final Question question;
    private final int questionNumber;  
    private final int totalQuestions;  
    public QuestionMessage(Question question, int questionNumber, int totalQuestions) {
        super(MessageType.QUESTION);
        if (question == null) {
            throw new IllegalArgumentException("Question cannot be null");
        }
        if (questionNumber < 1 || questionNumber > totalQuestions) {
            throw new IllegalArgumentException(
                String.format("Invalid question number: %d (total: %d)",
                    questionNumber, totalQuestions));
        }
        this.question = question;
        this.questionNumber = questionNumber;
        this.totalQuestions = totalQuestions;
    }
    public Question getQuestion() {
        return question;
    }
    public int getQuestionNumber() {
        return questionNumber;
    }
    public int getTotalQuestions() {
        return totalQuestions;
    }
    @Override
    public String toString() {
        return String.format("QuestionMessage[questionNum=%d/%d, question=%s]",
            questionNumber, totalQuestions, question.getQuestionText());
    }
}
