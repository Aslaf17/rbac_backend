package com.rbac.repository;

import com.rbac.model.notification.NotificationRead;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationReadRepository extends MongoRepository<NotificationRead, String> {

    Optional<NotificationRead> findByNotificationIdAndUserId(String notificationId, String userId);

    List<NotificationRead> findByUserIdAndNotificationIdIn(String userId, List<String> notificationIds);

    long countByNotificationId(String notificationId);
}