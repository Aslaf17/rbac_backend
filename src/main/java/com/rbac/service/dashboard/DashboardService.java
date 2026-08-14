package com.rbac.service.dashboard;

import com.rbac.dto.dashboard.AdminDashboardResponse;
import com.rbac.model.batch.Batch;
import com.rbac.model.login.User;
import com.rbac.model.session.Session;
import com.rbac.model.assignment.AssignmentSubmission;
import com.rbac.model.assignment.SubmissionStatus;
import com.rbac.model.certificate.Certificate;
import com.rbac.model.course.Course;
import com.rbac.model.exam.Exam;
import org.springframework.data.mongodb.core.query.Criteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface DashboardService {
    AdminDashboardResponse getAdminDashboard();
}

@Slf4j
@Service
@RequiredArgsConstructor
class DashboardServiceImpl implements DashboardService {

    private static final int RECENT_REGISTRATIONS_LIMIT = 10;

    private final MongoTemplate mongoTemplate;

    @Override
    public AdminDashboardResponse getAdminDashboard() {
        Map<String, Long> userRoleCounts = countByField(User.class, "role");
        Map<String, Long> sessionStatusCounts = countByField(Session.class, "status");
        long totalBatches = mongoTemplate.count(new Query(), Batch.class);

        AdminDashboardResponse.StatCards stats = AdminDashboardResponse.StatCards.builder()
                .totalStudents(userRoleCounts.getOrDefault("STUDENT", 0L))
                .totalTrainers(userRoleCounts.getOrDefault("TEACHER", 0L))
                .totalBatches(totalBatches)
                .activeLiveSessions(sessionStatusCounts.getOrDefault("LIVE", 0L))
                .completedSessions(sessionStatusCounts.getOrDefault("ENDED", 0L))
                .totalCourses(totalCourses())
                .totalExams(totalExams())
                .totalCertificates(totalCertificates())
                .pendingAssignments(pendingAssignments())
                .build();

        return AdminDashboardResponse.builder()
                .stats(stats)
                .recentRegistrations(recentRegistrations())
                .generatedAt(Instant.now())
                .build();
    }

    private <T> Map<String, Long> countByField(Class<T> collectionClass, String field) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group(field).count().as("count")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, collectionClass, Map.class);

        return results.getMappedResults().stream()
                .filter(row -> row.get("_id") != null)
                .collect(Collectors.toMap(
                        row -> String.valueOf(row.get("_id")),
                        row -> ((Number) row.get("count")).longValue()
                ));
    }

    private List<AdminDashboardResponse.RecentRegistration> recentRegistrations() {
        Query query = new Query()
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .limit(RECENT_REGISTRATIONS_LIMIT);

        List<User> users = mongoTemplate.find(query, User.class);

        return users.stream()
                .map(user -> AdminDashboardResponse.RecentRegistration.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole() != null ? user.getRole().name() : null)
                        .registeredAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // --- Live counts pulled straight from Mongo ---

    private long totalCourses() {
        return mongoTemplate.count(new Query(), Course.class);
    }

    private long totalExams() {
        return mongoTemplate.count(new Query(), Exam.class);
    }

    private long totalCertificates() {
        return mongoTemplate.count(new Query(), Certificate.class);
    }

    private long pendingAssignments() {
        // "Pending" = student has submitted but it hasn't been graded yet
        Query query = new Query(
                Criteria.where("status").in(
                        SubmissionStatus.SUBMITTED,
                        SubmissionStatus.PENDING_EVALUATION
                )
        );
        return mongoTemplate.count(query, AssignmentSubmission.class);
    }
}

