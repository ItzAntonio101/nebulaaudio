package io.nebulaaudio.plugin;

/**
 * Contract every plugin JAR dropped into plugins/ must implement, exposed
 * via a META-INF/services/io.nebulaaudio.plugin.NebulaPlugin service entry
 * (standard java.util.ServiceLoader discovery — no custom classloading
 * protocol to document/maintain).
 */
public interface NebulaPlugin {
    void onEnable();

    void onDisable();

    /** Human-readable name shown in logs and /v4/info. Defaults to the class's simple name. */
    default String getName() {
        return getClass().getSimpleName();
    }
}
