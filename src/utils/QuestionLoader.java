package utils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import models.Question;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public class QuestionLoader {
    public static List<Question> loadAllQuestions(String jsonPath) throws IOException {
        if (jsonPath == null || jsonPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Caminho do ficheiro JSON não pode ser nulo ou vazio");
        }
        Path path = Paths.get(jsonPath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Ficheiro não encontrado: " + jsonPath);
        }
        if (!Files.isReadable(path)) {
            throw new IOException("Ficheiro não pode ser lido: " + jsonPath);
        }
        String jsonContent = Files.readString(path);
        if (jsonContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Ficheiro JSON está vazio: " + jsonPath);
        }
        Gson gson = new Gson();
        QuizCollection collection;
        try {
            collection = gson.fromJson(jsonContent, QuizCollection.class);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("JSON malformado: " + e.getMessage(), e);
        }
        if (collection == null || collection.quizzes == null || collection.quizzes.isEmpty()) {
            throw new IllegalArgumentException("Ficheiro JSON inválido ou vazio");
        }
        QuizDTO firstQuiz = collection.quizzes.get(0);
        if (firstQuiz.questions == null || firstQuiz.questions.isEmpty()) {
            throw new IllegalArgumentException("Quiz não contém perguntas");
        }
        return convertQuestions(firstQuiz.questions);
    }
    private static class QuizCollection {
        List<QuizDTO> quizzes;
    }
    private static class QuizDTO {
        String name;
        List<QuestionDTO> questions;
    }
    private static class QuestionDTO {
        String question;
        int correct;           
        List<String> options;
        Integer points;        
        String type;           
    }
    private static List<Question> convertQuestions(List<QuestionDTO> dtos) {
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            QuestionDTO dto = dtos.get(i);
            if (dto == null) {
                throw new IllegalArgumentException("Pergunta " + (i + 1) + " é nula");
            }
            if (dto.question == null || dto.question.trim().isEmpty()) {
                throw new IllegalArgumentException("Pergunta " + (i + 1) + " não tem texto");
            }
            if (dto.options == null || dto.options.size() != 4) {
                throw new IllegalArgumentException(
                    "Pergunta " + (i + 1) + " deve ter exatamente 4 opções");
            }
            if (dto.correct < 0 || dto.correct > 3) {
                throw new IllegalArgumentException(
                    "Pergunta " + (i + 1) + " tem índice de resposta correta inválido: " + dto.correct);
            }
            for (int j = 0; j < dto.options.size(); j++) {
                if (dto.options.get(j) == null || dto.options.get(j).trim().isEmpty()) {
                    throw new IllegalArgumentException(
                        "Pergunta " + (i + 1) + " tem opção " + (j + 1) + " vazia");
                }
            }
            String[] optionsArray = dto.options.toArray(new String[0]);
            int points = (dto.points != null) ? dto.points : 5;
            String type = (dto.type != null && !dto.type.trim().isEmpty()) ? dto.type : "individual";
            try {
                questions.add(new Question(
                    i + 1,              
                    dto.question,
                    optionsArray,
                    dto.correct,
                    points,
                    type
                ));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Pergunta " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return questions;
    }
}
