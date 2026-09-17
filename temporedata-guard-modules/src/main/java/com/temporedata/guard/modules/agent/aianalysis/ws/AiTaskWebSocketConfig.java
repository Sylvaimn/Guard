/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.security.jwt.JwtTokenProvider
 *  io.jsonwebtoken.Claims
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.web.socket.CloseStatus
 *  org.springframework.web.socket.TextMessage
 *  org.springframework.web.socket.WebSocketHandler
 *  org.springframework.web.socket.WebSocketSession
 *  org.springframework.web.socket.config.annotation.EnableWebSocket
 *  org.springframework.web.socket.config.annotation.WebSocketConfigurer
 *  org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
 *  org.springframework.web.socket.handler.TextWebSocketHandler
 */
package com.temporedata.guard.modules.agent.aianalysis.ws;


import com.temporedata.guard.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Configuration
@EnableWebSocket
public class AiTaskWebSocketConfig
implements WebSocketConfigurer {
    private static final Logger log = LoggerFactory.getLogger(AiTaskWebSocketConfig.class);
    private final JwtTokenProvider tokenProvider;

    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler((WebSocketHandler)new AiTaskWsHandler(this.tokenProvider), new String[]{"/ws/tasks"}).setAllowedOrigins(new String[]{"*"});
    }

    public AiTaskWebSocketConfig(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    static class AiTaskWsHandler
    extends TextWebSocketHandler {
        private final JwtTokenProvider tokenProvider;

        AiTaskWsHandler(JwtTokenProvider tokenProvider) {
            this.tokenProvider = tokenProvider;
        }

        public void afterConnectionEstablished(WebSocketSession session) {
            String tenantId = this.resolveTenant(session);
            String userId = this.resolveUserId(session);
            if (tenantId == null) {
                try {
                    session.close(CloseStatus.POLICY_VIOLATION);
                }
                catch (Exception exception) {
                    // empty catch block
                }
                return;
            }
            AiTaskWsHub.register(userId, tenantId, session);
        }

        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        }

        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            String tenantId = this.resolveTenant(session);
            if (tenantId != null) {
                AiTaskWsHub.unregister(tenantId, session);
            }
        }

        public void handleTransportError(WebSocketSession session, Throwable exception) {
            String tenantId = this.resolveTenant(session);
            if (tenantId != null) {
                AiTaskWsHub.unregister(tenantId, session);
            }
        }

        private String resolveTenant(WebSocketSession session) {
            Claims claims = this.resolveClaims(session);
            if (claims == null) {
                return null;
            }
            Object tenantId = claims.get((Object)"tenantId");
            return tenantId == null ? null : tenantId.toString();
        }

        private String resolveUserId(WebSocketSession session) {
            Claims claims = this.resolveClaims(session);
            return claims == null ? null : claims.getSubject();
        }

        private Claims resolveClaims(WebSocketSession session) {
            try {
                URI uri = session.getUri();
                if (uri == null) {
                    return null;
                }
                Map<String, String> params = this.parseQuery(uri.getQuery());
                String token = params.get("token");
                if (token == null) {
                    return null;
                }
                return this.tokenProvider.parseToken(token);
            }
            catch (Exception e) {
                return null;
            }
        }

        private Map<String, String> parseQuery(String query) {
            HashMap<String, String> map = new HashMap<String, String>();
            if (query == null) {
                return map;
            }
            for (String pair : query.split("&")) {
                int idx = pair.indexOf(61);
                if (idx <= 0) continue;
                map.put(pair.substring(0, idx), pair.substring(idx + 1));
            }
            return map;
        }
    }
}

