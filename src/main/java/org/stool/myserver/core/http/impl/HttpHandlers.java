package org.stool.myserver.core.http.impl;

import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpConnection;
import org.stool.myserver.core.http.HttpServerRequest;

import java.util.Objects;

public class HttpHandlers {

    final Handler<HttpServerRequest> requestHandler;
    final Handler<HttpConnection> connectionHandler;
    final Handler<Throwable> exceptionHandler;

    public HttpHandlers(Handler<HttpServerRequest> requestHandler, Handler<HttpConnection> connectionHandler, Handler<Throwable> exceptionHandler) {
        this.requestHandler = requestHandler;
        this.connectionHandler = connectionHandler;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpHandlers that = (HttpHandlers) o;
        return Objects.equals(requestHandler, that.requestHandler) &&
                Objects.equals(connectionHandler, that.connectionHandler) &&
                Objects.equals(exceptionHandler, that.exceptionHandler);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestHandler, connectionHandler, exceptionHandler);
    }
}
