package org.stool.myserver.core.http.impl;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.LastHttpContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpClientRequest;
import org.stool.myserver.core.http.HttpClientResponse;
import org.stool.myserver.core.http.HttpConnection;
import org.stool.myserver.core.net.Buffer;

import java.util.List;

public class HttpClientResponseImpl implements HttpClientResponse {

    private static final Logger log = LoggerFactory.getLogger(HttpClientResponseImpl.class);


    private final int statusCode;
    private final String statusMessage;
    private final HttpClientRequest request;
    private final HttpConnection conn;
    private HttpClientStream stream;

    private Handler<Buffer> dataHandler;
    private Handler<Void> endHandler;
    private Handler<Throwable> exceptionHandler;

    private HttpHeaders headers;
    private HttpHeaders trailers;
    private List<String> cookies;

    public HttpClientResponseImpl(HttpClientRequest request, HttpClientStream stream,
                                  int statusCode, String statusMessage, HttpHeaders headers) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.request = request;
        this.stream = stream;
        this.conn = stream.connection();
        this.headers = headers;
    }

    @Override
    public HttpClientResponse pause() {
        stream.doPause();
        return this;
    }

    @Override
    public HttpClientResponse resume() {
        stream.doResume();
        return this;
    }

    @Override
    public HttpClientResponse fetch(long amount) {
        stream.doFetch(amount);
        return this;
    }

    @Override
    public HttpClientResponse handler(Handler<Buffer> dataHandler) {
        synchronized (conn) {
            this.dataHandler = dataHandler;
            return this;
        }
    }

    @Override
    public HttpClientResponse exceptionHandler(Handler<Throwable> exceptionHandler) {
        synchronized (conn) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }
    }


    @Override
    public HttpClientResponse endHandler(Handler<Void> endHandler) {
        synchronized (conn) {
            this.endHandler = endHandler;
            return this;
        }
    }

    @Override
    public HttpClientResponse bodyHandler(Handler<Buffer> bodyHandler) {
        BodyHandler handler = new BodyHandler();
        handler(handler);
        endHandler(v -> handler.notifyHandler(bodyHandler));
        return this;
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public String statusMessage() {
        return statusMessage;
    }

    @Override
    public String getHeader(String headerName) {
        return null;
    }

    @Override
    public String getHeader(CharSequence headerName) {
        return null;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }


    @Override
    public HttpClientRequest request() {
        return request;
    }

    @Override
    public void handleException(Throwable e) {

    }

    @Override
    public void handleChunk(Buffer data) {
        synchronized (conn) {
            if (dataHandler != null) {
                try {
                    dataHandler.handle(data);
                } catch (Throwable t) {
                    handleException(t);
                }
            }
        }
    }

    @Override
    public void handleEnd(LastHttpContent trailer) {
        if (endHandler != null) {
            try {
                endHandler.handle(null);
            } catch (Throwable t) {
                handleException(t);
            }
        }
    }

    private static final class BodyHandler implements Handler<Buffer> {
        private Buffer body;

        @Override
        public void handle(Buffer event) {
            body().appendBuffer(event);
        }

        private Buffer body() {
            if (body == null) {
                body = Buffer.buffer();
            }
            return body;
        }

        void notifyHandler(Handler<Buffer> bodyHandler) {
            bodyHandler.handle(body());
            // reset body so it can get GC'ed
            body = null;
        }
    }

}
