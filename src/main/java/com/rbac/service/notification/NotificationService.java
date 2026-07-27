package com.rbac.service.notification;

import com.rbac.dto.notification.PageResponse;
import com.rbac.dto.notification.CreateNotificationRequest;
import com.rbac.dto.notification.NotificationResponse;
import com.rbac.dto.notification.NotificationSummaryResponse;
import com.rbac.dto.notification.UpdateNotificationRequest;
import com.rbac.exception.notification.InvalidRequestException;
import com.rbac.exception.notification.ResourceNotFoundException;
import com.rbac.exception.notification.UnauthorizedActionException;
import com.rbac.model.login.User;
import com.rbac.model.login.Role;
import com.rbac.model.notification.Notification;
import com.rbac.model.notification.NotificationPriority;
import com.rbac.model.notification.NotificationRead;
import com.rbac.model.notification.NotificationStatus;
import com.rbac.model.notification.RecipientType;
import com.rbac.repository.NotificationReadRepository;
import com.rbac.repository.NotificationRepository;
import com.rbac.repository.UserRepository;
import com.rbac.security.chat.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface NotificationService {

    NotificationResponse create(CreateNotificationRequest request, AuthenticatedUser sender);

    PageResponse<NotificationResponse> getAll(
            String search, String priority, String recipientType, String status,
            LocalDateTime from, LocalDateTime to, int page, int size, AuthenticatedUser requester);

    NotificationResponse getById(String id, AuthenticatedUser requester);

    PageResponse<NotificationResponse> getMyNotifications(
            String search, String priority, Boolean read, int page, int size, AuthenticatedUser requester);

    NotificationResponse update(String id, UpdateNotificationRequest request, AuthenticatedUser requester);

    void markAsRead(String id, AuthenticatedUser requester);

    void softDelete(String id, AuthenticatedUser requester);

    NotificationSummaryResponse getSummary(AuthenticatedUser requester);
}

@Slf4j
@Service
@RequiredArgsConstructor
class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final NotificationEmailService notificationEmailService;
    private final NotificationSocketService notificationSocketService;

    @Override
    public NotificationResponse create(CreateNotificationRequest request, AuthenticatedUser sender) {
        if (!sender.isTrainerOrAdmin()) {
            throw new UnauthorizedActionException("Only trainers or admins can create notifications");
        }
        validateRecipientPayload(request.getRecipientType(), request.getRecipientId(), request.getBatchId());

        LocalDateTime now = LocalDateTime.now();
        String senderRole = sender.getRoleEnums().stream().findFirst().map(Enum::name).orElse("UNKNOWN");

        Notification notification = Notification.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .senderId(sender.getUserId())
                .senderName(sender.getUserName())
                .senderRole(senderRole)
                .recipientType(request.getRecipientType())
                .recipientId(request.getRecipientId())
                .batchId(request.getBatchId())
                .priority(request.getPriority())
                .status(NotificationStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification {} created by {}", saved.getId(), sender.getUserId());

        NotificationResponse response = NotificationResponse.fromEntity(saved, false);

        List<User> recipients = resolveRecipients(saved);
        notificationEmailService.sendNotificationEmails(saved, recipients);
        notificationSocketService.push(saved.getRecipientType(), response, recipients);

        return response;
    }

    private List<User> resolveRecipients(Notification notification) {
        return switch (notification.getRecipientType()) {
            case ALL -> userRepository.findAll();
            case BATCH -> userRepository.findByBatchId(notification.getBatchId());
            case USER -> userRepository.findById(notification.getRecipientId()).map(List::of).orElse(List.of());
        };
    }

    @Override
    public PageResponse<NotificationResponse> getAll(
            String search, String priority, String recipientType, String status,
            LocalDateTime from, LocalDateTime to, int page, int size, AuthenticatedUser requester) {

        if (!requester.isTrainerOrAdmin()) {
            throw new UnauthorizedActionException("Only trainers or admins can view all notifications");
        }

        Query query = new Query();
        applyCommonFilters(query, search, priority, recipientType, status, from, to);

        long total = mongoTemplate.count(query, Notification.class);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        query.with(pageable);

        List<Notification> notifications = mongoTemplate.find(query, Notification.class);
        Map<String, Boolean> readMap = buildReadMap(notifications, requester.getUserId());

        List<NotificationResponse> content = notifications.stream()
                .map(n -> NotificationResponse.fromEntity(n, readMap.getOrDefault(n.getId(), false)))
                .toList();

        return toPageResponse(content, page, size, total);
    }

    @Override
    public NotificationResponse getById(String id, AuthenticatedUser requester) {
        Notification notification = findActiveOrThrow(id);
        assertCanView(notification, requester);
        boolean read = notificationReadRepository.findByNotificationIdAndUserId(id, requester.getUserId()).isPresent();
        return NotificationResponse.fromEntity(notification, read);
    }

    @Override
    public PageResponse<NotificationResponse> getMyNotifications(
            String search, String priority, Boolean read, int page, int size, AuthenticatedUser requester) {

        User user = userRepository.findById(requester.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + requester.getUserId()));

        Query query = new Query();
        Criteria audience = new Criteria().orOperator(
                Criteria.where("recipientType").is(RecipientType.ALL),
                Criteria.where("recipientType").is(RecipientType.USER).and("recipientId").is(user.getId()),
                StringUtils.hasText(user.getBatchId())
                        ? Criteria.where("recipientType").is(RecipientType.BATCH).and("batchId").is(user.getBatchId())
                        : Criteria.where("recipientType").is(RecipientType.BATCH).and("batchId").is("__no_batch__")
        );
        query.addCriteria(audience);
        query.addCriteria(Criteria.where("status").is(NotificationStatus.ACTIVE));

        if (StringUtils.hasText(search)) {
            query.addCriteria(Criteria.where("title").regex(search, "i"));
        }
        if (StringUtils.hasText(priority)) {
            query.addCriteria(Criteria.where("priority").is(parsePriority(priority)));
        }

        long total = mongoTemplate.count(query, Notification.class);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        query.with(pageable);

        List<Notification> notifications = mongoTemplate.find(query, Notification.class);
        Map<String, Boolean> readMap = buildReadMap(notifications, requester.getUserId());

        List<NotificationResponse> content = notifications.stream()
                .map(n -> NotificationResponse.fromEntity(n, readMap.getOrDefault(n.getId(), false)))
                .filter(n -> read == null || n.isRead() == read)
                .toList();

        return toPageResponse(content, page, size, total);
    }

    @Override
    public NotificationResponse update(String id, UpdateNotificationRequest request, AuthenticatedUser requester) {
        if (!requester.isTrainerOrAdmin()) {
            throw new UnauthorizedActionException("Only trainers or admins can update notifications");
        }
        validateRecipientPayload(request.getRecipientType(), request.getRecipientId(), request.getBatchId());

        Notification notification = findActiveOrThrow(id);

        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setRecipientType(request.getRecipientType());
        notification.setRecipientId(request.getRecipientId());
        notification.setBatchId(request.getBatchId());
        notification.setPriority(request.getPriority());
        notification.setUpdatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);
        log.info("Notification {} updated by {}", id, requester.getUserId());

        boolean read = notificationReadRepository.findByNotificationIdAndUserId(id, requester.getUserId()).isPresent();
        return NotificationResponse.fromEntity(saved, read);
    }

    @Override
    public void markAsRead(String id, AuthenticatedUser requester) {
        Notification notification = findActiveOrThrow(id);
        assertCanView(notification, requester);

        notificationReadRepository.findByNotificationIdAndUserId(id, requester.getUserId())
                .orElseGet(() -> notificationReadRepository.save(
                        NotificationRead.builder()
                                .notificationId(id)
                                .userId(requester.getUserId())
                                .readAt(LocalDateTime.now())
                                .build()
                ));
    }

    @Override
    public void softDelete(String id, AuthenticatedUser requester) {
        if (!requester.hasRole(Role.ADMIN)) {
            throw new UnauthorizedActionException("Only admins can delete notifications");
        }
        Notification notification = findActiveOrThrow(id);
        notification.setStatus(NotificationStatus.DELETED);
        notification.setUpdatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
        log.info("Notification {} soft-deleted by {}", id, requester.getUserId());
    }

    @Override
    public NotificationSummaryResponse getSummary(AuthenticatedUser requester) {
        PageResponse<NotificationResponse> mine = getMyNotifications(null, null, null, 0, 5, requester);

        long unread = mine.getContent().stream().filter(n -> !n.isRead()).count();
        // unread count above is only within the fetched page; recompute properly:
        PageResponse<NotificationResponse> all = getMyNotifications(null, null, false, 0, Integer.MAX_VALUE, requester);
        PageResponse<NotificationResponse> high = getMyNotifications(null, "HIGH", null, 0, Integer.MAX_VALUE, requester);

        return NotificationSummaryResponse.builder()
                .total((long) getMyNotifications(null, null, null, 0, Integer.MAX_VALUE, requester).getContent().size())
                .unread((long) all.getContent().size())
                .highPriority((long) high.getContent().size())
                .recent(mine.getContent())
                .build();
    }

    // ---- helpers ----

    private void applyCommonFilters(Query query, String search, String priority, String recipientType,
                                    String status, LocalDateTime from, LocalDateTime to) {
        if (StringUtils.hasText(search)) {
            query.addCriteria(Criteria.where("title").regex(search, "i"));
        }
        if (StringUtils.hasText(priority)) {
            query.addCriteria(Criteria.where("priority").is(parsePriority(priority)));
        }
        if (StringUtils.hasText(recipientType)) {
            query.addCriteria(Criteria.where("recipientType").is(parseRecipientType(recipientType)));
        }
        query.addCriteria(Criteria.where("status").is(
                StringUtils.hasText(status) ? parseStatus(status) : NotificationStatus.ACTIVE));
        if (from != null || to != null) {
            Criteria dateCriteria = Criteria.where("createdAt");
            if (from != null) dateCriteria = dateCriteria.gte(from);
            if (to != null) dateCriteria = dateCriteria.lte(to);
            query.addCriteria(dateCriteria);
        }
    }

    private Map<String, Boolean> buildReadMap(List<Notification> notifications, String userId) {
        List<String> ids = notifications.stream().map(Notification::getId).toList();
        if (ids.isEmpty()) return Map.of();
        Set<String> readIds = notificationReadRepository.findByUserIdAndNotificationIdIn(userId, ids)
                .stream().map(NotificationRead::getNotificationId).collect(Collectors.toSet());
        return notifications.stream()
                .collect(Collectors.toMap(Notification::getId, n -> readIds.contains(n.getId())));
    }

    private PageResponse<NotificationResponse> toPageResponse(List<NotificationResponse> content, int page, int size, long total) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    private Notification findActiveOrThrow(String id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
        if (notification.getStatus() == NotificationStatus.DELETED) {
            throw new ResourceNotFoundException("Notification not found: " + id);
        }
        return notification;
    }

    private void assertCanView(Notification notification, AuthenticatedUser requester) {
        if (requester.isTrainerOrAdmin()) return;
        boolean isTargeted = switch (notification.getRecipientType()) {
            case ALL -> true;
            case USER -> requester.getUserId().equals(notification.getRecipientId());
            case BATCH -> userRepository.findById(requester.getUserId())
                    .map(u -> notification.getBatchId() != null && notification.getBatchId().equals(u.getBatchId()))
                    .orElse(false);
        };
        if (!isTargeted) {
            throw new UnauthorizedActionException("You do not have access to this notification");
        }
    }

    private void validateRecipientPayload(RecipientType type, String recipientId, String batchId) {
        if (type == RecipientType.USER && !StringUtils.hasText(recipientId)) {
            throw new InvalidRequestException("recipientId is required when recipientType is USER");
        }
        if (type == RecipientType.BATCH && !StringUtils.hasText(batchId)) {
            throw new InvalidRequestException("batchId is required when recipientType is BATCH");
        }
    }

    private NotificationPriority parsePriority(String value) {
        try {
            return NotificationPriority.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid priority: " + value);
        }
    }

    private RecipientType parseRecipientType(String value) {
        try {
            return RecipientType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid recipientType: " + value);
        }
    }

    private NotificationStatus parseStatus(String value) {
        try {
            return NotificationStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid status: " + value);
        }
    }
}