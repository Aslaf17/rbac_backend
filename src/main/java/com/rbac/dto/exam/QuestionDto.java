package com.rbac.dto.exam;

import com.rbac.model.exam.Question;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDto {

    private String id;

    @NotBlank(message = "questionText is required")
    private String questionText;

    @NotEmpty(message = "options must contain at least two choices")
    private List<String> options;

    @PositiveOrZero(message = "correctOptionIndex must be >= 0")
    private int correctOptionIndex;

    private double marks;

    public Question toEntity() {
        Question q = new Question();
        q.setId(id != null && !id.isBlank() ? id : UUID.randomUUID().toString());
        q.setQuestionText(questionText);
        q.setOptions(options);
        q.setCorrectOptionIndex(correctOptionIndex);
        q.setMarks(marks);
        return q;
    }

    public static QuestionDto fromEntity(Question q) {
        return QuestionDto.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .options(q.getOptions())
                .correctOptionIndex(q.getCorrectOptionIndex())
                .marks(q.getMarks())
                .build();
    }

    public static QuestionDto fromEntityForStudent(Question q) {
        return QuestionDto.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .options(q.getOptions())
                .marks(q.getMarks())
                .build();
    }
}
