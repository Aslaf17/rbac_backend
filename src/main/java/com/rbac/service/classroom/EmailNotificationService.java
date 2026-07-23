package com.rbac.service.classroom;

import com.rbac.model.login.User;
import com.rbac.model.session.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public void notifySessionStarted(Session session, List<User> students) {
        for (User student : students) {
            if (student.getEmail() == null || student.getEmail().isBlank()) {
                continue;
            }
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromAddress);
                message.setTo(student.getEmail());
                message.setSubject("Live class started: " + session.getTitle());
                message.setText(
                        "Hi " + student.getUsername() + ",\n\n" +
                                "Your trainer " + session.getTrainerName() + " has just started the live session \"" +
                                session.getTitle() + "\".\n\n" +
                                "Join now using session ID: " + session.getId() + "\n\n" +
                                "See you in class!"
                );
                mailSender.send(message);
            } catch (Exception ex) {
                log.error("Failed to email session-start notice to {}: {}", student.getEmail(), ex.getMessage());
            }
        }
    }
}