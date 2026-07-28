package com.rbac.model.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_reads")
@CompoundIndexes({
        @CompoundIndex(name = "notification_user_idx", def = "{'notificationId': 1, 'userId': 1}", unique = true)
})
public class NotificationRead {

    @Id
    private String id;

    private String notificationId;

    private String userId;

    private LocalDateTime readAt;
}