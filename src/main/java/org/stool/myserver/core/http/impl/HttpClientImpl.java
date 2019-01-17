package org.stool.myserver.core.http.impl;

import org.stool.myserver.core.*;
import org.stool.myserver.core.http.*;

import java.util.function.Function;

public class HttpClientImpl implements HttpClient{

    private final EntryPoint entryPoint;
    private final Context creatingContext;
//    private final ConnectionManager httpCM;

    private final boolean keepAlive;

    private volatile Handler<HttpConnection> connectionHandler;

    public HttpClientImpl(EntryPoint entryPoint, boolean keepAlive) {
        this.entryPoint = entryPoint;
        this.creatingContext = entryPoint.getContext();
        this.keepAlive = keepAlive;
    }

    @Override
    public HttpClientRequest request(HttpMethod method, String host, int port, String requestURI) {
        return createRequest(method, host, port, requestURI);
    }

    private HttpClientRequest createRequest(HttpMethod method, String host, int port, String requestURI) {
        return null;
    }

    @Override
    public HttpClientRequest request(HttpMethod method, String host, int port, String requestURI, Handler<AsyncResult<HttpClientResponse>> responseHandler) {
        return null;
    }

    @Override
    public HttpClient connectionHandler(Handler<HttpConnection> handler) {
        return null;
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

    }
}
