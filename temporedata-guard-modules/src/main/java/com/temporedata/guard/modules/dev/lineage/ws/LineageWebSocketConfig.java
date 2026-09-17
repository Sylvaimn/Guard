package com.temporedata.guard.modules.dev.lineage.ws;

import com.temporedata.guard.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket endpoint /ws/lineage?token=xxx
 * Token is resolved on handshake to bind the session to its tenant.
 */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class LineageWebSocketConfig implements WebSocketConfigurer {

    private final JwtTokenProvider tokenProvider;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new LineageWsHandler(tokenProvider), "/ws/lineage")
                .setAllowedOrigins("*");
    }

    static class LineageWsHandler extends TextWebSocketHandler {

        private final JwtTokenProvider tokenProvider;

        LineageWsHandler(JwtTokenProvider tokenProvider) {
            this.tokenProvider = tokenProvider;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            String tenantId = resolveTenant(session);
            if (tenantId == null) {
                log.warn("Lineage WS rejected: no tenant in token");
                try {
                    session.close(CloseStatus.POLICY_VIOLATION);
                } catch (Exception ignored) {
                }
                return;
            }
            LineageWsHub.register(tenantId, session);
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            // client pings/acks are ignored; server only pushes events
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            String tenantId = resolveTenant(session);
            if (tenantId != null) LineageWsHub.unregister(tenantId, session);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            String tenantId = resolveTenant(session);
            if (tenantId != null) LineageWsHub.unregister(tenantId, session);
        }

        private String resolveTenant(WebSocketSession session) {
            try {
                URI uri = session.getUri();
                if (uri == null) return null;
                Map<String, String> params = parseQuery(uri.getQuery());
                String token = params.get("token");
                if (token == null) return null;
                Claims claims = tokenProvider.parseToken(token);
                Object tenantId = claims.get("tenantId");
                return tenantId == null ? null : tenantId.toString();
            } catch (Exception e) {
                log.debug("Lineage WS token resolve failed: {}", e.getMessage());
                return null;
            }
        }

        private Map<String, String> parseQuery(String query) {
            java.util.HashMap<String, String> map = new java.util.HashMap<>();
            if (query == null) return map;
            for (String pair : query.split("&")) {
                int idx = pair.indexOf('=');
                if (idx > 0) map.put(pair.substring(0, idx), pair.substring(idx + 1));
            }
            return map;
        }
    }
}