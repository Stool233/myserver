package org.stool.myserver.core.http.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpServerResponse;
import org.stool.myserver.core.net.Buffer;
import org.stool.myserver.core.net.NetSocket;

import java.util.Map;

public class HttpServerResponseImpl implements HttpServerResponse {

    private static final Buffer EMPTY_BUFFER = Buffer.buffer(Unpooled.EMPTY_BUFFER);
    private static final Logger log = LoggerFactory.getLogger(HttpServerResponseImpl.class);

    private final EntryPoint entryPoint;
    private final HttpServerConnection conn;
    private HttpResponseStatus status;
    private final boolean keepAlive;
    private final boolean head;

    private boolean headWritten;
    private boolean written;
    private Handler<Void> drainHandler;
    private Handler<Throwable> exceptionHandler;
    private Handler<Void> closeHandler;
    private Handler<Void> endHandler;
    private Handler<Void> headersEndHandler;
    private Handler<Void> bodyEndHandler;
    private boolean closed;
    private final HttpHeaders headers;
    private Map<String, String> trailers;
    private HttpHeaders trailingHeaders = EmptyHttpHeaders.INSTANCE;
    private String statusMessage;
    private long bytesWritten;
    private NetSocket netSocket;

    public HttpServerResponseImpl(EntryPoint entryPoint, HttpServerConnection conn, DefaultHttpRequest request) {
        this.entryPoint = entryPoint;
        this.conn = conn;
        this.headers = new DefaultHttpHeaders();
        this.status = HttpResponseStatus.OK;
        this.keepAlive = request.headers().contains(org.stool.myserver.core.http.HttpHeaders.CONNECTION);
        this.head = request.method() == HttpMethod.HEAD;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public Map<String, String> trailers() {
        return null;
    }

    @Override
    public int getStatusCode() {
        return status.code();
    }

    @Override
    public HttpServerResponse setStatusCode(int statusCode) {
        status = statusMessage != null ? new HttpResponseStatus(statusCode, statusMessage) : HttpResponseStatus.valueOf(statusCode);
        return this;
    }

    @Override
    public String getStatusMessage() {
        return status.reasonPhrase();
    }

    @Override
    public HttpServerResponse setStatusMessage(String statusMessage) {
        synchronized (conn) {
            this.statusMessage = statusMessage;
            this.status = new HttpResponseStatus(status.code(), statusMessage);
            return this;
        }
    }

    @Override
    public HttpServerResponse putHeader(String key, String value) {
        synchronized (conn) {
            checkValid();
            headers.set(key, value);
            return this;
        }
    }

    @Override
    public boolean writeQueueFull() {
        synchronized (conn) {
            checkValid();
            return conn.isNotWritable();
        }
    }

    @Override
    public HttpServerResponse drainHandler(Handler<Void> handler){
        synchronized (conn) {
            if (handler != null) {
                checkValid();
            }
            drainHandler = handler;
            conn.getContext().runOnContext(v -> conn.handleInterestedOpsChanged());
            return this;
        }
    }


    @Override
    public HttpServerResponse closeHandler(Handler<Void> handler) {
        synchronized (conn) {
            if (handler != null) {
                checkValid();
            }
            closeHandler = handler;
            return this;
        }
    }


    @Override
    public HttpServerResponse endHandler(Handler<Void> handler) {
        synchronized (conn) {
            if (handler != null) {
                checkValid();
            }
            endHandler = handler;
            return this;
        }
    }

    private void checkValid() {
        if (written) {
            throw new IllegalStateException("Response has already been written");
        }
        if (closed) {
            throw new IllegalStateException("Response is closed");
        }
    }


    @Override
    public HttpServerResponse exceptionHandler(Handler<Throwable> handler) {
        synchronized (conn) {
            if (handler != null) {
                checkValid();
            }
            exceptionHandler = handler;
            return this;
        }
    }

    @Override
    public HttpServerResponse write(Buffer data) {
        ByteBuf buf = data.getByteBuf();
        return write(buf);
    }

    private HttpServerResponse write(ByteBuf chunk) {
        synchronized (conn) {
            checkValid();

            bytesWritten += chunk.readableBytes();
            if (!headWritten) {
                prepareHeaders(-1);
                conn.writeToChannel(new AssembledHttpResponse(head, HttpVersion.HTTP_1_1, status, headers, chunk));
            } else {
                conn.writeToChannel(new DefaultHttpContent(chunk));
            }
            return this;
        }
    }

    private void prepareHeaders(long contentLength) {
        // todo
    }

    public NetSocket netSocket(boolean isConnect) {
        checkValid();
        if (netSocket == null) {
            if (isConnect) {
                if (headWritten) {
                    throw new IllegalStateException("Response for CONNECT already sent");
                }
                status = HttpResponseStatus.OK;
                prepareHeaders(-1);
                conn.writeToChannel(new AssembledHttpResponse(head, HttpVersion.HTTP_1_1, status, headers));
            }
            written = true;
            netSocket = conn.createNetSocket();
        }
        return netSocket;
    }

    @Override
    public HttpServerResponse setWriteQueueMaxSize(int maxSize) {
        synchronized (conn) {
            checkValid();
            conn.doSetWriteQueueMaxSize(maxSize);
            return this;
        }
    }


    @Override
    public void end(String chunk) {
        end(Buffer.buffer(chunk));
    }



    @Override
    public void end(Buffer chunk) {
        synchronized (conn) {
            checkValid();
            ByteBuf data = chunk.getByteBuf();
            bytesWritten += data.readableBytes();
            if (!headWritten) {
                prepareHeaders(bytesWritten);
                conn.writeToChannel(new AssembledFullHttpResponse(head, HttpVersion.HTTP_1_1, status, headers, data, trailingHeaders));
            } else {
                conn.writeToChannel(new AssembledLastHttpContent(data, trailingHeaders));
            }

            if (!keepAlive) {
                closeConnAfterWrite();
                closed = true;
            }
            written = true;
            conn.responseComplete();
            if (bodyEndHandler != null) {
                bodyEndHandler.handle(null);
            }
            if (endHandler != null) {
                endHandler.handle(null);
            }
        }
    }



    private void closeConnAfterWrite() {
        ChannelPromise channelPromise = conn.channelFuture();
        conn.writeToChannel(Unpooled.EMPTY_BUFFER, channelPromise);
        channelPromise.addListener(future -> conn.close());
    }

    @Override
    public void end() {
        end(EMPTY_BUFFER);
    }


    @Override
    public void close() {
        synchronized (conn) {
            if (!closed) {
                if (headWritten) {
                    closeConnAfterWrite();
                } else {
                    conn.close();
                }
                closed = true;
            }
        }
    }

    @Override
    public boolean ended() {
        synchronized (conn) {
            return written;
        }
    }

    @Override
    public boolean closed() {
        synchronized (conn) {
            return closed;
        }
    }

    @Override
    public boolean headWritten() {
        synchronized (conn) {
            return headWritten;
        }
    }

    @Override
    public long bytesWritten() {
        synchronized (conn) {
            return bytesWritten;
        }
    }

    @Override
    public HttpServerResponse headersEndHandler(Handler<Void> handler) {
        synchronized (conn) {
            this.headersEndHandler = handler;
            return this;
        }
    }

    @Override
    public HttpServerResponse bodyEndHandler(Handler<Void> handler) {
        synchronized (conn) {
            this.bodyEndHandler = handler;
            return this;
        }
    }

    @Override
    public void handleDrained() {
        synchronized (conn) {
            if (drainHandler != null) {
                drainHandler.handle(null);
            }
        }
    }


    private void handleClosed() {
        Handler<Void> closedHandler;
        Handler<Void> endHandler;
        Handler<Throwable> exceptionHandler;
        synchronized (conn) {
            if (closed) {
                return;
            }
            closed = true;
            exceptionHandler = written ? null : this.exceptionHandler;
            endHandler = this.endHandler;
            closedHandler = this.closeHandler;
        }
        if (exceptionHandler != null) {
            exceptionHandler.handle(null);
        }
        if (endHandler != null) {
            endHandler.handle(null);
        }
        if (closedHandler != null) {
            closedHandler.handle(null);
        }
    }
}
