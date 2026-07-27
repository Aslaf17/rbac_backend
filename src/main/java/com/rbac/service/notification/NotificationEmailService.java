package com.rbac.service.notification;

import com.rbac.model.login.User;
import com.rbac.model.notification.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Async
    public void sendNotificationEmails(Notification notification, List<User> recipients) {
        for (User recipient : recipients) {
            if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
                continue;
            }
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromAddress);
                message.setTo(recipient.getEmail());
                message.setSubject("[" + notification.getPriority() + "] " + notification.getTitle());
                message.setText(
                        "Hi " + recipient.getUsername() + ",\n\n" +
                                notification.getMessage() + "\n\n" +
                                "— " + notification.getSenderName() + " (" + notification.getSenderRole() + ")"
                );
                mailSender.send(message);
            } catch (Exception ex) {
                log.error("Failed to email notification {} to {}: {}", notification.getId(), recipient.getEmail(), ex.getMessage());
            }
        }
    }
}