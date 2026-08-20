package server;
import models.*;
import network.AnswerMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class ScoringEngine {
    public static void scoreIndividualQuestion(
        Question question,
        Map<String, AnswerMessage> answers,
        Map<String, Integer> multipliers,
        GameState gameState
    ) {
        if (question == null || answers == null || multipliers == null || gameState == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        int basePoints = question.getPoints();
        int correctAnswerIndex = question.getCorrectAnswerIndex();
        for (Map.Entry<String, AnswerMessage> entry : answers.entrySet()) {
            String username = entry.getKey();
            AnswerMessage answer = entry.getValue();
            PlayerScore playerScore = gameState.getPlayerScore(username);
            if (playerScore == null) {
                System.out.println("Warning: No PlayerScore found for " + username);
                continue;
            }
            int multiplier = multipliers.getOrDefault(username, 1);
            boolean correct = (answer.getSelectedAnswer() == correctAnswerIndex);
            playerScore.addPoints(basePoints, multiplier, correct);
        }
        for (TeamScore teamScore : gameState.getAllTeamScores().values()) {
            teamScore.calculateTotalFromPlayers();
        }
    }
    public static void scoreTeamQuestion(
        Question question,
        Map<String, Map<String, AnswerMessage>> teamAnswers,
        GameState gameState
    ) {
        if (question == null || teamAnswers == null || gameState == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        int basePoints = question.getPoints();
        int correctAnswerIndex = question.getCorrectAnswerIndex();
        for (Map.Entry<String, Map<String, AnswerMessage>> entry : teamAnswers.entrySet()) {
            String teamCode = entry.getKey();
            Map<String, AnswerMessage> answers = entry.getValue();
            TeamScore teamScore = gameState.getTeamScore(teamCode);
            if (teamScore == null) {
                System.out.println("Warning: No TeamScore found for " + teamCode);
                continue;
            }
            boolean allCorrect = true;
            int maxIndividualScore = 0;
            for (Map.Entry<String, AnswerMessage> answerEntry : answers.entrySet()) {
                String username = answerEntry.getKey();
                AnswerMessage answer = answerEntry.getValue();
                boolean correct = (answer.getSelectedAnswer() == correctAnswerIndex);
                if (!correct) {
                    allCorrect = false;
                }
                PlayerScore playerScore = gameState.getPlayerScore(username);
                if (playerScore != null) {
                    if (correct) {
                        playerScore.addPoints(basePoints, 1, true);
                        maxIndividualScore = Math.max(maxIndividualScore, basePoints);
                    } else {
                        playerScore.addPoints(basePoints, 1, false);
                    }
                }
            }
            int teamPoints;
            if (allCorrect) {
                teamPoints = basePoints * 2;
            } else {
                teamPoints = maxIndividualScore;
            }
            teamScore.addTeamPoints(teamPoints);
        }
        for (TeamScore teamScore : gameState.getAllTeamScores().values()) {
            teamScore.calculateTotalFromPlayers();
        }
    }
    public static Map<String, Map<String, AnswerMessage>> groupAnswersByTeam(
        Map<String, AnswerMessage> answers,
        GameState gameState
    ) {
        Map<String, Map<String, AnswerMessage>> grouped = new HashMap<>();
        for (Map.Entry<String, AnswerMessage> entry : answers.entrySet()) {
            String username = entry.getKey();
            AnswerMessage answer = entry.getValue();
            Player player = gameState.getPlayer(username);
            if (player == null) {
                continue;
            }
            String teamCode = player.getTeamCode();
            grouped.putIfAbsent(teamCode, new HashMap<>());
            grouped.get(teamCode).put(username, answer);
        }
        return grouped;
    }
    public static boolean isCorrect(AnswerMessage answer, Question question) {
        if (answer == null || question == null) {
            return false;
        }
        return answer.getSelectedAnswer() == question.getCorrectAnswerIndex();
    }
}
