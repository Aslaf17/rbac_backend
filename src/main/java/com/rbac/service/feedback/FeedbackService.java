package com.rbac.service.feedback;

import com.rbac.dto.feedback.FeedbackAnalyticsResponse;
import com.rbac.dto.feedback.FeedbackListResponse;
import com.rbac.dto.feedback.FeedbackResponse;
import com.rbac.dto.feedback.PageResponse;
import com.rbac.dto.feedback.SubmitFeedbackRequest;
import com.rbac.exception.feedback.DuplicateFeedbackException;
import com.rbac.exception.feedback.InvalidRequestException;
import com.rbac.exception.feedback.ResourceNotFoundException;
import com.rbac.exception.feedback.UnauthorizedActionException;
import com.rbac.model.feedback.Feedback;
import com.rbac.model.feedback.FeedbackTag;
import com.rbac.model.login.Role;
import com.rbac.model.login.User;
import com.rbac.model.session.Session;
import com.rbac.repository.FeedbackRepository;
import com.rbac.repository.SessionRepository;
import com.rbac.repository.UserRepository;
import com.rbac.security.chat.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface FeedbackService {

    FeedbackResponse submit(SubmitFeedbackRequest request, AuthenticatedUser requester);

    FeedbackListResponse getBySession(String sessionId, String search, int page, int size, AuthenticatedUser requester);

    FeedbackListResponse getByTrainer(String trainerId, String search, int page, int size, AuthenticatedUser requester);
}

@Slf4j
@Service
@RequiredArgsConstructor
class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public FeedbackResponse submit(SubmitFeedbackRequest request, AuthenticatedUser requester) {
        if (!requester.hasRole(Role.STUDENT)) {
            throw new UnauthorizedActionException("Only students can submit session feedback");
        }

        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + request.getSessionId()));

        if (StringUtils.hasText(session.getTrainerId()) && !session.getTrainerId().equals(request.getTrainerId())) {
            throw new InvalidRequestException("trainerId does not match the trainer for this session");
        }

        if (feedbackRepository.existsBySessionIdAndStudentId(request.getSessionId(), requester.getUserId())) {
            throw new DuplicateFeedbackException("You have already submitted feedback for this session");
        }

        User trainer = userRepository.findById(request.getTrainerId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found: " + request.getTrainerId()));

        Feedback feedback = Feedback.builder()
                .sessionId(session.getId())
                .sessionTitle(session.getTitle())
                .studentId(requester.getUserId())
                .studentName(requester.getUserName())
                .trainerId(trainer.getId())
                .trainerName(trainer.getUsername())
                .rating(request.getRating())
                .review(request.getReview())
                .tag(request.getTag())
                .build();

        Feedback saved;
        try {
            saved = feedbackRepository.save(feedback);
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            // guards against a race between the existence check and the insert
            throw new DuplicateFeedbackException("You have already submitted feedback for this session");
        }

        log.info("Feedback {} submitted by student {} for session {}", saved.getId(), requester.getUserId(), session.getId());
        return FeedbackResponse.fromEntity(saved);
    }

    @Override
    public FeedbackListResponse getBySession(String sessionId, String search, int page, int size, AuthenticatedUser requester) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        assertCanViewAggregatedFeedback(session.getTrainerId(), requester);

        Criteria criteria = Criteria.where("sessionId").is(sessionId);
        return buildListResponse(criteria, search, page, size);
    }

    @Override
    public FeedbackListResponse getByTrainer(String trainerId, String search, int page, int size, AuthenticatedUser requester) {
        assertCanViewAggregatedFeedback(trainerId, requester);

        Criteria criteria = Criteria.where("trainerId").is(trainerId);
        return buildListResponse(criteria, search, page, size);
    }

    // ---- helpers ----

    private void assertCanViewAggregatedFeedback(String trainerId, AuthenticatedUser requester) {
        if (requester.hasRole(Role.ADMIN)) {
            return;
        }
        if (requester.hasRole(Role.TEACHER) && requester.getUserId().equals(trainerId)) {
            return;
        }
        throw new UnauthorizedActionException("You do not have permission to view this feedback");
    }

    private FeedbackListResponse buildListResponse(Criteria baseCriteria, String search, int page, int size) {
        Query query = new Query(baseCriteria);
        if (StringUtils.hasText(search)) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("review").regex(search, "i"),
                    Criteria.where("studentName").regex(search, "i"),
                    Criteria.where("sessionTitle").regex(search, "i")
            ));
        }

        long total = mongoTemplate.count(query, Feedback.class);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        query.with(pageable);

        List<Feedback> results = mongoTemplate.find(query, Feedback.class);
        List<FeedbackResponse> content = results.stream().map(FeedbackResponse::fromEntity).toList();

        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        PageResponse<FeedbackResponse> pageResponse = new PageResponse<>(content, page, size, total, totalPages);

        FeedbackAnalyticsResponse analytics = computeAnalytics(baseCriteria);

        return FeedbackListResponse.builder()
                .feedback(pageResponse)
                .analytics(analytics)
                .build();
    }

    private FeedbackAnalyticsResponse computeAnalytics(Criteria baseCriteria) {
        Aggregation summaryAgg = Aggregation.newAggregation(
                Aggregation.match(baseCriteria),
                Aggregation.group().count().as("total").avg("rating").as("avgRating")
        );
        AggregationResults<Document> summaryResults = mongoTemplate.aggregate(summaryAgg, "feedback", Document.class);
        Document summary = summaryResults.getUniqueMappedResult();

        long total = summary != null ? ((Number) summary.get("total")).longValue() : 0L;
        double avgRating = summary != null && summary.get("avgRating") != null
                ? Math.round(((Number) summary.get("avgRating")).doubleValue() * 100.0) / 100.0
                : 0.0;

        Map<Integer, Long> ratingDistribution = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0L);
        }
        Aggregation ratingAgg = Aggregation.newAggregation(
                Aggregation.match(baseCriteria),
                Aggregation.group("rating").count().as("count")
        );
        AggregationResults<Document> ratingResults = mongoTemplate.aggregate(ratingAgg, "feedback", Document.class);
        for (Document doc : ratingResults.getMappedResults()) {
            Object idVal = doc.get("_id");
            if (idVal instanceof Number number) {
                ratingDistribution.put(number.intValue(), ((Number) doc.get("count")).longValue());
            }
        }

        Map<String, Long> tagDistribution = new LinkedHashMap<>();
        for (FeedbackTag tag : FeedbackTag.values()) {
            tagDistribution.put(tag.name(), 0L);
        }
        Aggregation tagAgg = Aggregation.newAggregation(
                Aggregation.match(baseCriteria),
                Aggregation.group("tag").count().as("count")
        );
        AggregationResults<Document> tagResults = mongoTemplate.aggregate(tagAgg, "feedback", Document.class);
        for (Document doc : tagResults.getMappedResults()) {
            Object idVal = doc.get("_id");
            if (idVal != null) {
                tagDistribution.put(idVal.toString(), ((Number) doc.get("count")).longValue());
            }
        }

        return FeedbackAnalyticsResponse.builder()
                .totalFeedback(total)
                .averageRating(avgRating)
                .ratingDistribution(ratingDistribution)
                .tagDistribution(tagDistribution)
                .build();
    }
}