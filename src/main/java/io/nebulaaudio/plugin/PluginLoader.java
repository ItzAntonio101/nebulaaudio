package io.nebulaaudio.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Scans plugins/*.jar, loads each into its own URLClassLoader (isolating
 * plugin classpaths from each other and from the core app), and discovers
 * NebulaPlugin implementations via ServiceLoader. Each JAR must ship a
 * META-INF/services/io.nebulaaudio.plugin.NebulaPlugin file naming its
 * implementation class(es), same convention Lavalink's plugin system uses.
 */
public class PluginLoader {
    private static final Logger log = LoggerFactory.getLogger(PluginLoader.class);

    private final Path pluginsDir;
    private final List<NebulaPlugin> loaded = new ArrayList<>();

    public PluginLoader(Path pluginsDir) {
        this.pluginsDir = pluginsDir;
    }

    public List<NebulaPlugin> loadAll() {
        if (!Files.isDirectory(pluginsDir)) {
            log.info("Plugins directory {} does not exist, skipping plugin load", pluginsDir);
            return loaded;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDir, "*.jar")) {
            for (Path jar : stream) {
                loadJar(jar);
            }
        } catch (IOException e) {
            log.error("Failed to scan plugins directory {}: {}", pluginsDir, e.getMessage(), e);
        }

        return loaded;
    }

    private void loadJar(Path jar) {
        try {
            URL url = jar.toUri().toURL();
            URLClassLoader classLoader = new URLClassLoader(new URL[]{url}, getClass().getClassLoader());
            ServiceLoader<NebulaPlugin> serviceLoader = ServiceLoader.load(NebulaPlugin.class, classLoader);

            boolean any = false;
            for (NebulaPlugin plugin : serviceLoader) {
                any = true;
                try {
                    plugin.onEnable();
                    loaded.add(plugin);
                    log.info("Loaded plugin '{}' from {}", plugin.getName(), jar.getFileName());
                } catch (Exception e) {
                    log.error("Plugin {} threw during onEnable(): {}", plugin.getName(), e.getMessage(), e);
                }
            }

            if (!any) {
                log.warn("{} contains no registered NebulaPlugin service (missing META-INF/services entry?)", jar.getFileName());
            }
        } catch (Exception e) {
            log.error("Failed to load plugin jar {}: {}", jar, e.getMessage(), e);
        }
    }

    public void disableAll() {
        for (NebulaPlugin plugin : loaded) {
            try {
                plugin.onDisable();
            } catch (Exception e) {
                log.warn("Plugin {} threw during onDisable(): {}", plugin.getName(), e.getMessage());
            }
        }
    }
}
