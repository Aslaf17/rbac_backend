package com.rbac.config;

import com.rbac.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        try {

            if (!(request instanceof ServletServerHttpRequest servletRequest)) {
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                return false;
            }

            String token = servletRequest.getServletRequest().getParameter("token");

            System.out.println("========== WebSocket Handshake ==========");
            System.out.println("Request URI : " + request.getURI());
            System.out.println("Token       : " + token);

            if (token == null || token.isBlank()) {
                System.out.println("No JWT token found.");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            String username = jwtUtil.extractUsername(token);

            if (username == null || username.isBlank()) {
                System.out.println("Invalid username inside token.");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            attributes.put("username", username);

            System.out.println("Authenticated User : " + username);
            System.out.println("========================================");

            return true;

        } catch (Exception ex) {

            System.out.println("WebSocket authentication failed");
            ex.printStackTrace();

            response.setStatusCode(HttpStatus.UNAUTHORIZED);

            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {

        if (exception != null) {
            exception.printStackTrace();
        }
    }
}