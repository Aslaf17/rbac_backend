package com.rbac.model.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "feedback")
@CompoundIndexes({
        // one feedback per student per session
        @CompoundIndex(name = "session_student_unique", def = "{'sessionId': 1, 'studentId': 1}", unique = true)
})
public class Feedback {

    @Id
    private String id;

    private String sessionId;
    private String sessionTitle;

    private String studentId;
    private String studentName;

    private String trainerId;
    private String trainerName;

    private int rating;
    private String review;
    private FeedbackTag tag;

    private Instant createdAt = Instant.now();
}