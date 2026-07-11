package io.nebulaaudio.config;

import java.util.ArrayList;
import java.util.List;

public class ClusterConfig {
    private boolean enabled = false;
    private String strategy = "least-load";
    private List<String> nodes = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public List<String> getNodes() {
        return nodes;
    }

    public void setNodes(List<String> nodes) {
        this.nodes = nodes;
    }
}
