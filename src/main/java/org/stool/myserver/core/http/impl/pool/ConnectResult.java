package org.stool.myserver.core.http.impl.pool;

public class ConnectResult<C> {
    private final C conn;
    private final long concurrency;
    private final long weight;

    public ConnectResult(C conn, long concurrency, long weight) {
        this.conn = conn;
        this.concurrency = concurrency;
        this.weight = weight;
    }

    public C getConn() {
        return conn;
    }

    public long getConcurrency() {
        return concurrency;
    }

    public long getWeight() {
        return weight;
    }
}
