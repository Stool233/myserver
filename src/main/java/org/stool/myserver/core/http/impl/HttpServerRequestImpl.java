package org.stool.myserver.core.http.impl;

import io.netty.handler.codec.Headers;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpConnection;
import org.stool.myserver.core.http.HttpMethod;
import org.stool.myserver.core.http.HttpServerRequest;
import org.stool.myserver.core.http.HttpServerResponse;
import org.stool.myserver.core.net.Buffer;
import org.stool.myserver.core.net.NetSocket;
import org.stool.myserver.core.net.SocketAddress;
import org.stool.myserver.core.streams.InboundBuffer;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class HttpServerRequestImpl implements HttpServerRequest{

    private static final Logger log = LoggerFactory.getLogger(HttpServerRequestImpl.class);

    private final HttpServerConnection conn;

    private DefaultHttpRequest request;
    private HttpMethod method;
    private String uri;
    private String path;
    private String query;

    private HttpServerResponseImpl response;
    private HttpServerRequestImpl next;

    private Handler<Buffer> dataHandler;
    private Handler<Throwable> exceptionHandler;

    private Map<String, String> params;
    private HttpHeaders headers;
    private String absoluteURI;

    private Handler<Void> endHandler;
    private Map<String, String> attributes;
    private HttpPostRequestDecoder decoder;
    private boolean ended;

    private InboundBuffer<Buffer> pending;

    public HttpServerRequestImpl(HttpServerConnection conn, DefaultHttpRequest request) {
        this.conn = conn;
        this.request = request;
    }

    void handleBegin() {
        response = new HttpServerResponseImpl(conn.entryPoint(), conn, request);
        conn.requestHandler.handle(this);
    }


    DefaultHttpRequest getRequest() {
        synchronized (conn) {
            return request;
        }
    }


    void setRequest(DefaultHttpRequest request) {
        synchronized (conn) {
            this.request = request;
        }
    }

    void handleContent(Buffer buffer) {
        if (pending != null) {
            enqueueData(buffer);
        } else {
            handleData(buffer);
        }
    }

    void appendRequest(HttpServerRequestImpl next) {
        HttpServerRequestImpl current = this;
        while(current.next != null) {
            current = current.next;
        }
        current.next = next;
    }

    private void enqueueData(Buffer chunk) {
        // 当pendingQueue无法再写入时暂停写入
        if (!pendingQueue().write(chunk)) {
            conn.doPause();
        }
    }

    private InboundBuffer<Buffer> pendingQueue() {
        if (pending == null) {
            pending = new InboundBuffer<>(conn.getContext(), 8);
            pending.drainHandler(v -> conn.doResume());
            pending.emptyHandler(v -> {
                if (ended) {
                    doEnd();
                }
            });
            pending.handler(this::handleData);
        }
        return pending;
    }



    private void handleData(Buffer data) {
        synchronized (conn) {
            if (decoder != null) {
                decoder.offer(new DefaultHttpContent(data.getByteBuf()));
            }
            if (dataHandler != null) {
                dataHandler.handle(data);
            }
        }
    }

    private void doEnd() {
        if (decoder != null) {
            endDecode();
        }
        if (endHandler != null) {
            endHandler.handle(null);
        }
    }

    private void endDecode() {
        try {
            decoder.offer(LastHttpContent.EMPTY_LAST_CONTENT);
            while (decoder.hasNext()) {
                InterfaceHttpData data = decoder.next();
                if (data instanceof Attribute) {
                    Attribute attr = (Attribute) data;
                    try {
                        attributes().put(attr.getName(), attr.getValue());
                    } catch (Exception e) {
                        handleException(e);
                    }
                }
            }
        } catch (HttpPostRequestDecoder.ErrorDataDecoderException e) {
            handleException(e);
        } catch (HttpPostRequestDecoder.EndOfDataDecoderException e) {
            // ignore
        } finally {
            decoder.destroy();
        }
    }

    private void handleException(Exception e) {
        // todo
    }

    private Map<String, String> attributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }

    @Override
    public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
        synchronized (conn) {
            this.exceptionHandler = handler;
            return this;
        }
    }

    @Override
    public HttpServerRequest handler(Handler<Buffer> handler) {
        synchronized (conn) {
            if (handler != null) {
                checkEnded();
            }
            dataHandler = handler;
            return this;
        }
    }

    private void checkEnded() {
        if (isEnded()) {
            throw new IllegalStateException("Request is already been read");
        }
    }

    @Override
    public HttpServerRequest pause() {
        synchronized (conn) {
            if (!isEnded()) {
                pendingQueue().pause();
            }
            return this;
        }
    }

    @Override
    public HttpServerRequest resume() {
        synchronized (conn) {
            if (!isEnded()) {
                pendingQueue().resume();
            }
            return this;
        }
    }

    @Override
    public HttpServerRequest fetch(long amount) {
        synchronized (conn) {
            pendingQueue().fetch(amount);
            return this;
        }
    }

    @Override
    public HttpServerRequest endHandler(Handler<Void> endHandler) {
        synchronized (conn) {
            if (endHandler != null) {
                checkEnded();
            }
            this.endHandler = endHandler;
            return this;
        }
    }

    @Override
    public NetSocket netSocket() {
        synchronized (conn) {
            return response.netSocket(method() == HttpMethod.CONNECT);
        }
    }

    @Override
    public HttpMethod method() {
        if (method == null) {
            String sMethod = request.method().toString();
            try {
                method = HttpMethod.valueOf(sMethod);
            } catch (IllegalArgumentException e) {
                method = HttpMethod.OTHER;
            }
        }
        return method;
    }

    @Override
    public String uri() {
        if (uri == null) {
            uri = request.uri();
        }
        return uri;
    }

    @Override
    public String path() {
        if (path == null) {
            path = HttpUtils.parsePath(uri());
        }
        return path;
    }

    @Override
    public String query() {
        if (query == null) {
            query = HttpUtils.parseQuery(uri());
        }
        return query;
    }

    @Override
    public String host() {
        return getHeader(HttpHeaderNames.HOST);
    }

    @Override
    public long bytesRead() {
        return 0;
    }

    @Override
    public HttpHeaders headers() {
        return request.headers();

    }

    @Override
    public String getHeader(String headerName) {
        return headers().get(headerName);
    }

    @Override
    public String getHeader(CharSequence headerName) {
        return headers().get(headerName);
    }

    @Override
    public Map<String, String> params() {
        if (params == null) {
            params = HttpUtils.params(uri());
        }
        return params;
    }

    @Override
    public String getParam(String paramName) {
        return params().get(paramName);
    }

    @Override
    public Map<String, String> formAttributes() {
        return attributes();
    }

    @Override
    public String getFormAttribute(String attributeName) {
        return formAttributes().get(attributeName);
    }

    @Override
    public SocketAddress localAddress() {
        return conn.localAddress();
    }

    @Override
    public SocketAddress remoteAddress() {
        return conn.remoteAddress();
    }

    @Override
    public String absoluteURI() {
        if (absoluteURI == null) {
            try {
                absoluteURI = HttpUtils.absoluteURI(this);
            } catch (URISyntaxException e) {
                log.error("Failed to create abs uri", e);
            }
        }
        return absoluteURI;
    }

    @Override
    public boolean isEnded() {
        synchronized (conn) {
            return ended && (pending == null || pending.isEmpty());
        }
    }

    @Override
    public HttpServerResponse response() {
        return response;
    }

    @Override
    public HttpConnection connection() {
        return conn;
    }

    @Override
    public Context context() {
        return connection().getContext();
    }

    void handleEnd() {
        synchronized (conn) {
            ended = true;
            if (isEnded()) {
                doEnd();
            }
        }
    }

    HttpServerRequestImpl nextRequest() {
        return next;
    }

    void handlePipelined() {
        // 使用局部变量保存ended状态，取消ended状态
        boolean end = ended;
        ended = false;
        handleBegin();
        // 若不为暂停状态，且pending不为空，恢复pending的流动状态
        if (pending != null && pending.size() > 0) {
            pending.resume();
        }
        if (end) {
            handleEnd();
        }
    }
}
