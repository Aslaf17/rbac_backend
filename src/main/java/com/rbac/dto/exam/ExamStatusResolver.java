package com.rbac.dto.exam;

import com.rbac.model.exam.Exam;
import com.rbac.model.exam.ExamStatus;

import java.time.Instant;

public final class ExamStatusResolver {

    private ExamStatusResolver() {
    }

    public static ExamStatus resolve(Exam exam) {
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            return exam.getStatus();
        }

        Instant now = Instant.now();
        Instant start = exam.getExamDate();
        Instant end = start.plusSeconds((long) exam.getDurationMinutes() * 60);

        if (now.isBefore(start)) {
            return ExamStatus.PUBLISHED;
        } else if (now.isBefore(end)) {
            return ExamStatus.ONGOING;
        } else {
            return ExamStatus.COMPLETED;
        }
    }

    public static boolean isAvailableToStudents(Exam exam) {
        ExamStatus effective = resolve(exam);
        return effective == ExamStatus.PUBLISHED || effective == ExamStatus.ONGOING;
    }
}
