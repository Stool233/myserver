package org.stool.myserver.core.http.impl;

import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpServerRequest;
import org.stool.myserver.core.http.HttpServerResponse;
import org.stool.myserver.core.net.Buffer;
import org.stool.myserver.core.net.NetSocket;

public class HttpServerRequestImpl implements HttpServerRequest{

    @Override
    public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
        return null;
    }

    @Override
    public HttpServerRequest handler(Handler<Buffer> handler) {
        return null;
    }

    @Override
    public HttpServerRequest pause() {
        return null;
    }

    @Override
    public HttpServerRequest resume() {
        return null;
    }

    @Override
    public HttpServerRequest endHandler(Handler<Void> endHandler) {
        return null;
    }

    @Override
    public NetSocket netSocket() {
        return null;
    }

    @Override
    public HttpServerResponse response() {
        return null;
    }
}
