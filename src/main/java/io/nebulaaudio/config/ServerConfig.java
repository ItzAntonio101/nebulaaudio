package io.nebulaaudio.config;

public class ServerConfig {
    private String host = "0.0.0.0";
    private int port = 2333;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
