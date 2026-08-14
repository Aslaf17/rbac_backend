package com.rbac.dto.assignment;

import com.rbac.model.assignment.Assignment;
import com.rbac.model.assignment.AssignmentStatus;

import java.time.Instant;

public final class AssignmentStatusResolver {

    private AssignmentStatusResolver() {
    }

    public static AssignmentStatus resolve(Assignment assignment) {
        AssignmentStatus stored = assignment.getStatus();

        if (stored != AssignmentStatus.PUBLISHED && stored != AssignmentStatus.OPEN) {
            return stored;
        }

        Instant now = Instant.now();
        if (assignment.getDueDate() != null && now.isAfter(assignment.getDueDate())) {
            return AssignmentStatus.COMPLETED;
        }
        return AssignmentStatus.OPEN;
    }

    public static boolean isAvailableToStudents(Assignment assignment) {
        AssignmentStatus effective = resolve(assignment);
        return effective == AssignmentStatus.OPEN;
    }

    public static boolean isPastDue(Assignment assignment) {
        return assignment.getDueDate() != null && Instant.now().isAfter(assignment.getDueDate());
    }
}