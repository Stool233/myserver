package org.stool.myserver.core.http.impl;

import io.netty.handler.codec.http.HttpVersion;

public class HttpClientOptions {

    public static final int DEFAULT_MAX_POOL_SIZE = 5;

    public static final boolean DEFAULT_KEEP_ALIVE = true;

    public static final int DEFAULT_KEEP_ALIVE_TIMEOUT = 60;

    public static final String DEFAULT_DEFAULT_HOST = "localhost";

    public static final int DEFAULT_DEFAULT_PORT = 80;

    public static final HttpVersion DEFAULT_PROTOCOL_VERSION = HttpVersion.HTTP_1_1;

    public static final int DEFAULT_MAX_CHUNK_SIZE = 8192;

    public static final int DEFAULT_MAX_INITIAL_LINE_LENGTH = 4096;

    public static final int DEFAULT_MAX_HEADER_SIZE = 8192;

    public static final int DEFAULT_MAX_WAIT_QUEUE_SIZE = -1;

    public static final int DEFAULT_POOL_CLEANER_PERIOD = 1000;

    private int maxPoolSize;
    private boolean keepAlive;
    private int keepAliveTimeout;

    private int maxChunkSize;
    private int maxInitialLineLength;
    private int maxHeaderSize;
    private int maxWaitQueueSize;
    private int poolCleanerPeriod;

    public HttpClientOptions() {
        this.maxPoolSize = DEFAULT_MAX_POOL_SIZE;
        this.keepAlive = DEFAULT_KEEP_ALIVE;
        this.keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;
        this.maxChunkSize = DEFAULT_MAX_CHUNK_SIZE;
        this.maxInitialLineLength = DEFAULT_MAX_INITIAL_LINE_LENGTH;
        this.maxHeaderSize = DEFAULT_MAX_HEADER_SIZE;
        this.maxWaitQueueSize = DEFAULT_MAX_WAIT_QUEUE_SIZE;
        poolCleanerPeriod = DEFAULT_POOL_CLEANER_PERIOD;

    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public int getMaxInitialLineLength() {
        return maxInitialLineLength;
    }

    public int getMaxHeaderSize() {
        return maxHeaderSize;
    }

    public int getMaxWaitQueueSize() {
        return maxWaitQueueSize;
    }

    public int getPoolCleanerPeriod() {
        return poolCleanerPeriod;
    }
}
