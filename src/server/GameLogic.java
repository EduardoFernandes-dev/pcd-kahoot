package server;
import models.Question;
import utils.QuestionLoader;
import java.io.IOException;
import java.util.List;
public class GameLogic {
    private List<Question> questions;
    private int currentQuestionIndex;
    public GameLogic(String jsonPath) throws IOException {
        this.questions = QuestionLoader.loadAllQuestions(jsonPath);
        this.currentQuestionIndex = 0;
    }
    public GameLogic(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("Questions list cannot be null or empty");
        }
        this.questions = new java.util.ArrayList<>(questions);
        this.currentQuestionIndex = 0;
    }
    public Question getCurrentQuestion() {
        if (isGameFinished()) {
            return null;
        }
        return questions.get(currentQuestionIndex);
    }
    public boolean checkAnswer(int answerIndex) {
        Question current = getCurrentQuestion();
        if (current == null) {
            return false;
        }
        return current.getCorrectAnswerIndex() == answerIndex;
    }
    public boolean nextQuestion() {
        if (hasNextQuestion()) {
            currentQuestionIndex++;
            return true;
        }
        return false;
    }
    public boolean hasNextQuestion() {
        return currentQuestionIndex < questions.size() - 1;
    }
    public boolean isGameFinished() {
        return currentQuestionIndex >= questions.size();
    }
    public void reset() {
        currentQuestionIndex = 0;
    }
    public void resetWithNewQuestions(List<Question> newQuestions) {
        if (newQuestions == null || newQuestions.isEmpty()) {
            throw new IllegalArgumentException("Questions list cannot be null or empty");
        }
        this.questions = new java.util.ArrayList<>(newQuestions);
        this.currentQuestionIndex = 0;
    }
    public int getTotalQuestions() {
        return questions.size();
    }
    public int getCurrentQuestionNumber() {
        return currentQuestionIndex + 1;
    }
}
