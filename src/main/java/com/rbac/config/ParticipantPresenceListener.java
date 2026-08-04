package com.rbac.config;

import com.rbac.model.login.Role;
import com.rbac.model.login.User;
import com.rbac.repository.UserRepository;
import com.rbac.service.classroom.ParticipantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipantPresenceListener {

    private final UserRepository userRepository;
    private final ParticipantService participantService;

    private final Map<String, AtomicInteger> activeConnectionsByUserId = new ConcurrentHashMap<>();

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        withParticipant(event.getUser(), (user) -> {
            int nowActive = activeConnectionsByUserId
                    .computeIfAbsent(user.getId(), id -> new AtomicInteger(0))
                    .incrementAndGet();

            if (nowActive == 1) {
                participantService.handleUserReconnecting(user.getId(), user.getUsername());
            }
        });
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        withParticipant(event.getUser(), (user) -> {
            int nowActive = activeConnectionsByUserId
                    .computeIfAbsent(user.getId(), id -> new AtomicInteger(0))
                    .updateAndGet(count -> Math.max(0, count - 1));

            if (nowActive == 0) {
                participantService.handleUserDisconnected(user.getId(), user.getUsername());
            }
        });
    }

    private void withParticipant(Principal principal, java.util.function.Consumer<User> action) {
        if (principal == null || principal.getName() == null) {
            return;
        }
        userRepository.findByUsername(principal.getName()).ifPresent(user -> {
            // Trainers/admins are handled by TrainerPresenceListener with different recovery rules.
            if (user.getRole() != Role.TEACHER && user.getRole() != Role.ADMIN) {
                action.accept(user);
            }
        });
    }
}