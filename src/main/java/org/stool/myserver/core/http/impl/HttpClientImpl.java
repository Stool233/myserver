package org.stool.myserver.core.http.impl;

import io.netty.handler.codec.http.HttpHeaders;
import org.stool.myserver.core.*;
import org.stool.myserver.core.http.*;

import java.util.function.Function;

public class HttpClientImpl implements HttpClient{

    private final EntryPoint entryPoint;
    private final Context creatingContext;
    private final HttpClientOptions options;
    private final ConnectionManager httpCM;
    private Closeable closeHook;
    private final boolean keepAlive;

    private volatile boolean closed;

    private volatile Handler<HttpConnection> connectionHandler;

    public HttpClientImpl(EntryPoint entryPoint) {
        this.entryPoint = entryPoint;
        this.creatingContext = entryPoint.getContext();
        this.options = new HttpClientOptions();
        this.keepAlive = options.isKeepAlive();
        this.closeHook = completionHandler -> {
            HttpClientImpl.this.close();
            completionHandler.handle(Future.succeededFuture());
        };
        this.httpCM = new ConnectionManager(this, options.getMaxPoolSize(), options.getMaxWaitQueueSize());
        this.httpCM.start();
    }

    @Override
    public HttpClientRequest request(HttpMethod method, String host, int port, String requestURI, HttpHeaders headers) {
        return createRequest(method, host, port, requestURI, headers);
    }

    private HttpClientRequest createRequest(HttpMethod method, String host, int port, String relativeURI, HttpHeaders headers) {
        checkClosed();
        HttpClientRequest req = new HttpClientRequestImpl(this, method, host, port, relativeURI, entryPoint);
        if (headers != null) {
            req.setHeader(headers);
        }
        return req;
    }

    private synchronized void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Client is closed");
        }
    }

    @Override
    public HttpClientRequest request(HttpMethod method, String host, int port, String requestURI, HttpHeaders headers, Handler<AsyncResult<HttpClientResponse>> responseHandler) {
        return request(method, host, port, requestURI, headers).handler(responseHandler);
    }

    @Override
    public HttpClient connectionHandler(Handler<HttpConnection> handler) {
        connectionHandler = handler;
        return this;
    }
    @Override
    public HttpClient redirectHandler(Function<HttpClientResponse, Future<HttpClientRequest>> handler) {
        return null;
    }

    @Override
    public Function<HttpClientResponse, Future<HttpClientRequest>> redirectHandler() {
        return null;
    }

    @Override
    public void close() {
        synchronized (this) {
            checkClosed();
            closed = true;
        }
        if (creatingContext != null) {
            creatingContext.removeCloseHook(closeHook);
        }
        httpCM.close();
    }

    @Override
    public EntryPoint getEntryPoint() {
        return entryPoint;
    }

    @Override
    public HttpClientOptions getOptions() {
        return options;
    }

    @Override
    public Handler<HttpConnection> connectionHandler() {
        return connectionHandler;
    }

    @Override
    public void getConnectionForRequest(Context ctx, String peerHost, int port, String host, Handler<AsyncResult<HttpClientStream>> handler) {
        httpCM.getConnection(ctx, peerHost, port, host, ar -> {
            if (ar.succeeded()) {
                ar.result().createStream(handler);
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }
}
