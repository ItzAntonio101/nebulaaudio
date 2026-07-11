package io.nebulaaudio;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.nebulaaudio.api.LoadTracksController;
import io.nebulaaudio.api.PlayerController;
import io.nebulaaudio.api.SessionController;
import io.nebulaaudio.config.NebulaConfig;
import io.nebulaaudio.plugin.PluginLoader;
import io.nebulaaudio.player.PlayerManager;
import io.nebulaaudio.source.SourceManager;
import io.nebulaaudio.util.Json;
import io.nebulaaudio.websocket.EventDispatcher;
import io.nebulaaudio.websocket.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

public class NebulaAudio {
    private static final Logger log = LoggerFactory.getLogger(NebulaAudio.class);

    public static void main(String[] args) {
        printBanner();

        Path configPath = Path.of(System.getProperty("nebula.config", "nebula.yml"));
        NebulaConfig config = NebulaConfig.load(configPath);

        SourceManager sourceManager = new SourceManager(config.getSources());
        EventDispatcher dispatcher = new EventDispatcher();
        PlayerManager playerManager = new PlayerManager(dispatcher, config);

        PluginLoader pluginLoader = new PluginLoader(Path.of("plugins"));
        pluginLoader.loadAll();

        if (config.getCluster().isEnabled()) {
            log.info("Cluster mode enabled (strategy={}), known nodes: {}",
                    config.getCluster().getStrategy(), config.getCluster().getNodes());
            log.warn("Cluster mode is scaffolded (config + node list are loaded) but node discovery/" +
                    "failover/player-migration networking is not implemented in this build.");
        }

        LoadTracksController loadTracksController = new LoadTracksController(sourceManager);
        SessionController sessionController = new SessionController(playerManager);
        PlayerController playerController = new PlayerController(playerManager);
        WebSocketServer webSocketServer = new WebSocketServer(dispatcher, playerManager, config.getPassword());

        Javalin app = Javalin.create(cfg -> {
            cfg.showJavalinBanner = false;
            cfg.jsonMapper(new JavalinJackson(Json.mapper(), false));
        });

        app.before(ctx -> {
            // Skip auth for the websocket upgrade path; WebSocketServer checks it itself.
            if (ctx.path().equals("/v4/websocket")) return;
            String expected = config.getPassword();
            if (expected != null && !expected.isEmpty()) {
                String provided = ctx.header("Authorization");
                if (!expected.equals(provided)) {
                    ctx.status(401).json(Map.of("error", "Unauthorized"));
                }
            }
        });

        app.get("/v4/loadtracks", loadTracksController::loadTracks);
        app.get("/v4/info", loadTracksController::info);
        app.get("/v4/stats", sessionController::stats);
        app.get("/v4/sessions/{sessionId}/players", playerController::listAll);
        app.get("/v4/sessions/{sessionId}/players/{guildId}", playerController::get);
        app.patch("/v4/sessions/{sessionId}/players/{guildId}", playerController::patch);
        app.delete("/v4/sessions/{sessionId}/players/{guildId}", playerController::delete);

        webSocketServer.register(app);

        app.exception(Exception.class, (e, ctx) -> {
            log.error("Unhandled exception on {} {}: {}", ctx.method(), ctx.path(), e.getMessage(), e);
            ctx.status(500).json(Map.of("error", "Internal server error", "message", String.valueOf(e.getMessage())));
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            pluginLoader.disableAll();
            app.stop();
        }, "shutdown-hook"));

        app.start(config.getServer().getHost(), config.getServer().getPort());
        log.info("NebulaAudio listening on {}:{}", config.getServer().getHost(), config.getServer().getPort());
    }

    private static void printBanner() {
        try (InputStream in = NebulaAudio.class.getClassLoader().getResourceAsStream("banner.txt")) {
            if (in != null) {
                System.out.println(new String(in.readAllBytes()));
            }
        } catch (IOException ignored) {
            // Banner is cosmetic; failure to print it is not worth failing startup over.
        }
    }
}
