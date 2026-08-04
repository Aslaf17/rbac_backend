package com.rbac.config;

import com.rbac.model.login.Role;
import com.rbac.model.login.User;
import com.rbac.repository.UserRepository;
import com.rbac.service.classroom.SessionRecoveryService;
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
public class TrainerPresenceListener {

    private final UserRepository userRepository;
    private final SessionRecoveryService sessionRecoveryService;

    private final Map<String, AtomicInteger> activeConnectionsByUserId = new ConcurrentHashMap<>();

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        withTrainer(event.getUser(), (user) -> {
            int nowActive = activeConnectionsByUserId
                    .computeIfAbsent(user.getId(), id -> new AtomicInteger(0))
                    .incrementAndGet();

            if (nowActive == 1) {
                // First live connection for this user - if they had a session waiting on them, recover it.
                sessionRecoveryService.handleTrainerReconnected(user.getId(), user.getUsername());
            }
        });
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        withTrainer(event.getUser(), (user) -> {
            int nowActive = activeConnectionsByUserId
                    .computeIfAbsent(user.getId(), id -> new AtomicInteger(0))
                    .updateAndGet(count -> Math.max(0, count - 1));

            if (nowActive == 0) {
                // Their last connection just dropped - start the reconnect countdown.
                sessionRecoveryService.handleTrainerDisconnected(user.getId(), user.getUsername());
            }
        });
    }

    private void withTrainer(Principal principal, java.util.function.Consumer<User> action) {
        if (principal == null || principal.getName() == null) {
            return;
        }
        userRepository.findByUsername(principal.getName()).ifPresent(user -> {
            if (user.getRole() == Role.TEACHER || user.getRole() == Role.ADMIN) {
                action.accept(user);
            }
        });
    }
}