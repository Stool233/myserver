package org.stool.myserver.core.net;

import java.util.Objects;

public class ProxyOptions {

    public static final String DEFAULT_HOST = "localhost";

    public static final int DEFAULT_PORT = 3128;

    private String host;
    private int port;

    public ProxyOptions() {
        host = DEFAULT_HOST;
        port = DEFAULT_PORT;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProxyOptions that = (ProxyOptions) o;
        return port == that.port &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
