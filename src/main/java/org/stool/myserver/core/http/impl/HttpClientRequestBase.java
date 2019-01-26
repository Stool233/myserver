package org.stool.myserver.core.http.impl;

import org.stool.myserver.core.http.HttpClient;
import org.stool.myserver.core.http.HttpClientRequest;
import org.stool.myserver.core.http.HttpClientResponse;
import org.stool.myserver.core.http.HttpMethod;

import java.util.concurrent.TimeoutException;

public abstract class HttpClientRequestBase implements HttpClientRequest {

    protected final HttpClient client;
    protected final HttpMethod method;
    protected final String uri;
    protected final String path;
    protected final String host;
    protected final int port;
    protected final String query;
    private long currentTimeoutTimerId = -1;
    private long currentTimeoutMs;
    private long lastDataReceived;
    protected Throwable exceptionOccurred;

    public HttpClientRequestBase(HttpClient client, HttpMethod method, String host, int port, String uri) {
        this.client = client;
        this.uri = uri;
        this.method = method;
        this.host = host;
        this.port = port;
        this.path = uri.length() > 0 ? HttpUtils.parsePath(uri) : "";
        this.query = HttpUtils.parseQuery(uri);
    }

    protected abstract void doHandleResponse(HttpClientResponse resp, long timeoutMs);
    protected abstract void checkComplete();

    protected String hostHeader() {
        return host + ':' + port;
    }


    @Override
    public String absoluteURI() {
        return "http://" + hostHeader() + uri;
    }


    public String query() {
        return query;
    }

    public String path() {
        return path;
    }

    public String uri() {
        return uri;
    }

    public String host() {
        return host;
    }


    @Override
    public HttpMethod method() {
        return method;
    }

    @Override
    public synchronized HttpClientRequest setTimeout(long timeoutMs) {
        cancelOutstandingTimeoutTimer();
        currentTimeoutMs = timeoutMs;
        currentTimeoutTimerId = client.getEntryPoint().setTimer(timeoutMs, id -> handleTimeout(timeoutMs));
        return this;
    }

    private void handleTimeout(long timeoutMs) {
        if (lastDataReceived == 0) {
            timeout(timeoutMs);
        } else {
            long now = System.currentTimeMillis();
            long timeSinceLastData = now - lastDataReceived;
            if (timeSinceLastData >= timeoutMs) {
                timeout(timeoutMs);
            } else {
                lastDataReceived = 0;
                setTimeout(timeoutMs - timeSinceLastData);
            }
        }
    }

    private void timeout(long timeoutMs) {
        String msg = "The timeout period of " + timeoutMs + "ms has been exceeded while executing " + method + " " + uri + " for host " + host;
        // Use a stack-less exception
        handleException(new TimeoutException(msg) {
            @Override
            public synchronized Throwable fillInStackTrace() {
                return this;
            }
        });
        reset(0);
    }

    public void handleException(Throwable t) {
        cancelOutstandingTimeoutTimer();
    }

    @Override
    public synchronized void handleResponse(HttpClientResponse resp) {
        long timeoutMS;
        synchronized (this) {
            // If an exception occurred (e.g. a timeout fired) we won't receive the response.
            if (exceptionOccurred != null) {
                return;
            }
            timeoutMS = cancelOutstandingTimeoutTimer();
        }
        try {
            doHandleResponse(resp, timeoutMS);
        } catch (Throwable t) {
            handleException(t);
        }
    }

    private long cancelOutstandingTimeoutTimer() {
        long ret;
        if ((ret = currentTimeoutTimerId) != -1) {
            client.getEntryPoint().cancelTimer(currentTimeoutTimerId);
            currentTimeoutTimerId = -1;
            ret = currentTimeoutMs;
            currentTimeoutMs = 0;
        }
        return ret;
    }

    public synchronized void dataReceived() {
        if (currentTimeoutTimerId != -1) {
            lastDataReceived = System.currentTimeMillis();
        }
    }
}
