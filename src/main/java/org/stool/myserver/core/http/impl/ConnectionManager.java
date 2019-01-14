package org.stool.myserver.core.http.impl;

import org.stool.myserver.core.http.HttpClient;
import org.stool.myserver.core.http.impl.pool.Pool;

import java.nio.channels.Channel;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {

    private final int maxWaitQueueSize;
    private final HttpClient client;
    private final Map<Channel, HttpClientConnection> connectionMap = new ConcurrentHashMap<>();
    private final Map<EndpointKey,Endpoint> endpointMap = new ConcurrentHashMap<>();
    private final long maxSize;

    public ConnectionManager(int maxWaitQueueSize, HttpClient client, long maxSize) {
        this.maxWaitQueueSize = maxWaitQueueSize;
        this.client = client;
        this.maxSize = maxSize;
    }

    private static final class EndpointKey {

        private final int port;
        private final String peerHost;
        private final String host;

        public EndpointKey(int port, String peerHost, String host) {
            this.port = port;
            this.peerHost = peerHost;
            this.host = host;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EndpointKey that = (EndpointKey) o;
            return port == that.port &&
                    Objects.equals(peerHost, that.peerHost) &&
                    Objects.equals(host, that.host);
        }

        @Override
        public int hashCode() {

            return Objects.hash(port, peerHost, host);
        }
    }

    class Endpoint {
        private final Pool<HttpClientConnection> pool;

        public Endpoint(Pool<HttpClientConnection> pool) {
            this.pool = pool;
        }
    }
}
