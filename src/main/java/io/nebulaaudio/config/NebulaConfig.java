package io.nebulaaudio.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Root configuration object, loaded from nebula.yml.
 * Uses a permissive Map-based SnakeYAML parse rather than typed constructors,
 * because the YAML uses kebab-case keys (default-volume) which don't map
 * cleanly onto Java field names without a custom naming strategy. We parse
 * to a generic Map tree and bind manually — simpler and more robust to
 * missing/partial config sections.
 */
public class NebulaConfig {
    private static final Logger log = LoggerFactory.getLogger(NebulaConfig.class);

    private String password = "";
    private final ServerConfig server = new ServerConfig();
    private final SourceConfig sources = new SourceConfig();
    private final PlayerConfig player = new PlayerConfig();
    private final ClusterConfig cluster = new ClusterConfig();

    @SuppressWarnings("unchecked")
    public static NebulaConfig load(Path path) {
        NebulaConfig config = new NebulaConfig();

        if (!Files.exists(path)) {
            log.warn("{} not found, using default configuration", path);
            return config;
        }

        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(options));

        try (InputStream in = Files.newInputStream(path)) {
            Map<String, Object> root = yaml.load(in);
            if (root == null) {
                log.warn("{} is empty, using default configuration", path);
                return config;
            }

            Map<String, Object> serverMap = (Map<String, Object>) root.get("server");
            if (serverMap != null) {
                if (serverMap.get("host") != null) config.server.setHost(String.valueOf(serverMap.get("host")));
                if (serverMap.get("port") != null) config.server.setPort((Integer) serverMap.get("port"));
            }

            Map<String, Object> nebulaMap = (Map<String, Object>) root.get("nebula");
            if (nebulaMap != null && nebulaMap.get("password") != null) {
                config.password = String.valueOf(nebulaMap.get("password"));
            }

            Map<String, Object> sourcesMap = (Map<String, Object>) root.get("sources");
            if (sourcesMap != null) {
                config.sources.setYoutube(bool(sourcesMap, "youtube", true));
                config.sources.setSoundcloud(bool(sourcesMap, "soundcloud", true));
                config.sources.setDeezer(bool(sourcesMap, "deezer", true));
                config.sources.setSpotify(bool(sourcesMap, "spotify", true));
                config.sources.setHttp(bool(sourcesMap, "http", true));
                config.sources.setLocal(bool(sourcesMap, "local", true));
            }

            Map<String, Object> playerMap = (Map<String, Object>) root.get("player");
            if (playerMap != null) {
                if (playerMap.get("default-volume") != null)
                    config.player.setDefaultVolume((Integer) playerMap.get("default-volume"));
                if (playerMap.get("opus-quality") != null)
                    config.player.setOpusQuality((Integer) playerMap.get("opus-quality"));
            }

            Map<String, Object> clusterMap = (Map<String, Object>) root.get("cluster");
            if (clusterMap != null) {
                config.cluster.setEnabled(bool(clusterMap, "enabled", false));
                if (clusterMap.get("strategy") != null)
                    config.cluster.setStrategy(String.valueOf(clusterMap.get("strategy")));
                Object nodes = clusterMap.get("nodes");
                if (nodes instanceof Iterable<?> it) {
                    for (Object n : it) {
                        config.cluster.getNodes().add(String.valueOf(n));
                    }
                }
            }

            log.info("Loaded configuration from {}", path.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to read {}: {}", path, e.getMessage(), e);
        } catch (ClassCastException e) {
            log.error("Malformed {}: {}", path, e.getMessage(), e);
        }

        return config;
    }

    private static boolean bool(Map<String, Object> map, String key, boolean fallback) {
        Object v = map.get(key);
        return v == null ? fallback : (Boolean) v;
    }

    public String getPassword() {
        return password;
    }

    public ServerConfig getServer() {
        return server;
    }

    public SourceConfig getSources() {
        return sources;
    }

    public PlayerConfig getPlayer() {
        return player;
    }

    public ClusterConfig getCluster() {
        return cluster;
    }
}
