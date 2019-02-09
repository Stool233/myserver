package org.stool.myserver.core.http.impl;

import io.netty.channel.Channel;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.Future;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpClient;
import org.stool.myserver.core.http.impl.pool.Pool;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {

    private final int maxWaitQueueSize;
    private final HttpClient client;
    private final Map<Channel, HttpClientConnection> connectionMap = new ConcurrentHashMap<>();
    private final Map<EndpointKey, Endpoint> endpointMap = new ConcurrentHashMap<>();
    private final long maxWeight;   // 总权重，也是MaxPoolSize
    private long timerID;

    public ConnectionManager(HttpClient client, long maxWeight, int maxWaitQueueSize) {
        this.maxWaitQueueSize = maxWaitQueueSize;
        this.client = client;
        this.maxWeight = maxWeight;
    }

    public synchronized void start() {
        long period = client.getOptions().getPoolCleanerPeriod();
        this.timerID = period > 0 ? client.getEntryPoint().setTimer(period, id -> checkExpired(period)) : -1;
    }

    private synchronized void checkExpired(long period) {
        long timestamp = System.currentTimeMillis();
        endpointMap.values().forEach(e -> e.pool.closeIdle(timestamp));
        timerID = client.getEntryPoint().setTimer(period, id -> checkExpired(period));
    }

    void getConnection(Context ctx, String peerHost, int port, String host, Handler<AsyncResult<HttpClientConnection>> handler) {
        EndpointKey key = new EndpointKey(port, peerHost, host);
        // 获取connection
        while(true) {
            Endpoint endpoint = endpointMap.computeIfAbsent(key, targetAddress -> {
                HttpChannelConnector connector = new HttpChannelConnector(client, peerHost, host, port);
                Pool<HttpClientConnection> pool = new Pool<>(ctx, connector, maxWaitQueueSize, connector.weight(), maxWeight,
                        v -> endpointMap.remove(key),
                        conn -> connectionMap.put(conn.channel(), conn),
                        conn -> connectionMap.remove(conn.channel(), conn),
                        false);
                return new Endpoint(pool);
            });

            if (endpoint.pool.getConnection(ar -> {
                if (ar.succeeded()) {
                    HttpClientConnection conn = ar.result();
                    handler.handle(Future.succeededFuture(conn));
                } else {
                    handler.handle(Future.failedFuture(ar.cause()));
                }
            })) {
                break;
            }
        }
    }

    public void close() {
        synchronized (this) {
            if (timerID >= 0) {
                client.getEntryPoint().cancelTimer(timerID);
                timerID = -1;
            }
        }
        endpointMap.clear();
        for (HttpClientConnection conn : connectionMap.values()) {
            conn.close();
        }
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
