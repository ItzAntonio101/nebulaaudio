package io.nebulaaudio.websocket;

import io.javalin.Javalin;
import io.nebulaaudio.player.PlayerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Registers the /v4/websocket endpoint (Lavalink-style single connection per
 * node, identified by headers Authorization + User-Id + Client-Name). On
 * connect we mint or accept a session id, dispatch a "ready" op, and register
 * the context with EventDispatcher so AudioPlayer instances can push events.
 */
public class WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    private final EventDispatcher dispatcher;
    private final PlayerManager playerManager;
    private final String password;

    public WebSocketServer(EventDispatcher dispatcher, PlayerManager playerManager, String password) {
        this.dispatcher = dispatcher;
        this.playerManager = playerManager;
        this.password = password;
    }

    public void register(Javalin app) {
        app.ws("/v4/websocket", ws -> {
            ws.onConnect(ctx -> {
                String auth = ctx.header("Authorization");
                if (password != null && !password.isEmpty() && !password.equals(auth)) {
                    log.warn("Rejected websocket connection with bad Authorization header");
                    ctx.closeSession(4001, "Unauthorized");
                    return;
                }

                String resumeId = ctx.header("Session-Id");
                String sessionId = (resumeId != null && !resumeId.isBlank()) ? resumeId : UUID.randomUUID().toString();
                boolean resumed = resumeId != null && dispatcher.hasSession(resumeId);

                ctx.attribute("sessionId", sessionId);
                dispatcher.registerSession(sessionId, ctx);
                dispatcher.dispatchReady(sessionId, resumed);
                log.info("WebSocket connected: session={} resumed={}", sessionId, resumed);
            });

            ws.onClose(ctx -> {
                String sessionId = ctx.attribute("sessionId");
                if (sessionId != null) {
                    dispatcher.unregisterSession(sessionId);
                    playerManager.destroyAllForSession(sessionId);
                }
            });

            ws.onError(ctx -> {
                log.warn("WebSocket error: {}", ctx.error() != null ? ctx.error().getMessage() : "unknown");
            });
        });
    }
}
