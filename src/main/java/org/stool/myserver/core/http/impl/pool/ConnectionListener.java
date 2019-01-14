package org.stool.myserver.core.http.impl.pool;

public interface ConnectionListener<C> {

    void onConcurrencyChange(long concurrency);

    void onRecycle(long expirationTimestamp);

    void onEvict();
}
