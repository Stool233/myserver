package org.stool.myserver.core.http.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stool.myserver.core.*;
import org.stool.myserver.core.http.*;
import org.stool.myserver.core.net.Buffer;

import java.util.Map;
import java.util.Objects;

import static io.netty.handler.codec.rtsp.RtspHeaderNames.CONTENT_LENGTH;

public class HttpClientRequestImpl extends HttpClientRequestBase implements HttpClientRequest{


    static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    private final EntryPoint entryPoint;
    private final Future<HttpClientResponse> respFut;
    private boolean chunked;
    private String hostHeader;
    private String rawMethod;
    private Handler<Void> drainHandler;
    private Handler<HttpConnection> connectionHandler;
    private Handler<Throwable> exceptionHandler;
    private boolean completed;
    private Handler<Void> completionHandler;
    private Long reset;
    private ByteBuf pendingChunks;
    private int pendingMaxSize = -1;
    private long written;
    private HttpHeaders headers;
    private HttpClientStream stream;
    private boolean connecting;

    public HttpClientRequestImpl(HttpClient client, HttpMethod method, String host, int port, String relativeURI,
                                 EntryPoint entryPoint) {
        super(client, method, host, port, relativeURI);
        this.chunked = false;
        this.entryPoint = entryPoint;
        this.respFut = Future.future();
    }


    @Override
    public void handleException(Throwable t) {
        super.handleException(t);
        Handler<Throwable> handler;
        synchronized (this) {
            exceptionOccurred = t;
            if (exceptionHandler != null) {
                handler = exceptionHandler;
            } else {
                handler = e -> log.error("exception", e);
            }
        }
        handler.handle(t);
        respFut.tryFail(t);
    }

    @Override
    public synchronized HttpClientRequest handler(Handler<AsyncResult<HttpClientResponse>> handler) {
        if (handler != null) {
            checkComplete();
        }
        respFut.setHandler(handler);
        return this;
    }


    @Override
    public HttpClientRequestImpl setChunked(boolean chunked) {
        synchronized (this) {
            checkComplete();
            this.chunked = chunked;
            return this;
        }
    }


    @Override
    public synchronized boolean isChunked() {
        return chunked;
    }

    @Override
    public synchronized String getRawMethod() {
        return rawMethod;
    }

    @Override
    public synchronized HttpClientRequest setRawMethod(String method) {
        this.rawMethod = method;
        return this;
    }

    @Override
    public synchronized HttpClientRequest setHost(String host) {
        this.hostHeader = host;
        return this;
    }

    @Override
    public synchronized String getHost() {
        return hostHeader;
    }


    @Override
    public synchronized HttpHeaders headers() {
        if (headers == null) {
            headers = new DefaultHttpHeaders();
        }
        return headers;
    }


    @Override
    public synchronized HttpClientRequest putHeader(String name, String value) {
        // todo
        return this;
    }

    @Override
    public synchronized HttpClientRequest setHeader(HttpHeaders headers) {
        this.headers = headers;
        return this;
    }


    @Override
    public HttpClientRequest setWriteQueueMaxSize(int maxSize) {
        HttpClientStream s;
        synchronized (this) {
            checkComplete();
            if ((s = stream) == null) {
                pendingMaxSize = maxSize;
                return this;
            }
        }
        s.doSetWriteQueueMaxSize(maxSize);
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        HttpClientStream s;
        synchronized (this) {
            checkComplete();
            if ((s = stream) == null) {
                // Should actually check with max queue size and not always blindly return false
                return false;
            }
        }
        return s.isNotWritable();
    }


    private synchronized Handler<Throwable> exceptionHandler() {
        return exceptionHandler;
    }

    public synchronized HttpClientRequest exceptionHandler(Handler<Throwable> handler) {
        if (handler != null) {
            checkComplete();
            this.exceptionHandler = handler;
        } else {
            this.exceptionHandler = null;
        }
        return this;
    }

    @Override
    public HttpClientRequest drainHandler(Handler<Void> handler) {
        synchronized (this) {
            if (handler != null) {
                checkComplete();
                drainHandler = handler;
                HttpClientStream s;
                if ((s = stream) == null) {
                    return this;
                }
                s.getContext().runOnContext(v -> {
                    synchronized (HttpClientRequestImpl.this) {
                        if (!stream.isNotWritable()) {
                            handleDrained();
                        }
                    }
                });
            } else {
                drainHandler = null;
            }
            return this;
        }
    }


    @Override
    public HttpClientRequest sendHead() {
        return sendHead(null);
    }

    @Override
    public synchronized HttpClientRequest sendHead(Handler<HttpVersion> headersHandler) {
        checkComplete();
        checkResponseHandler();
        if (stream != null) {
            throw new IllegalStateException("Head already written");
        } else {
            connect(headersHandler);
        }
        return this;
    }

    private synchronized void connect(Handler<HttpVersion> headersHandler) {
        if (!connecting) {

            if (method == HttpMethod.OTHER && rawMethod == null) {
                throw new IllegalStateException("You must provide a rawMethod when using an HttpMethod.OTHER method");
            }

            String peerHost;
            if (hostHeader != null) {
                int idx = hostHeader.lastIndexOf(":");
                if (idx != -1) {
                    peerHost = hostHeader.substring(0, idx);
                } else {
                    peerHost = hostHeader;
                }
            } else {
                peerHost = host;
            }

            Handler<HttpConnection> h1 = connectionHandler;
            Handler<HttpConnection> h2 = client.connectionHandler();
            Handler<HttpConnection> initializer;
            // request中的connectionHandler优先级比client中的connectionHandler高
            if (h1 != null) {
                if (h2 != null) {
                    initializer = conn -> {
                        h1.handle(conn);
                        h2.handle(conn);
                    };
                } else {
                    initializer = h1;
                }
            } else {
                initializer = h2;
            }
            Context connectCtx = entryPoint.getOrCreateContext();

            connecting = true;
            client.getConnectionForRequest(connectCtx, peerHost, port, host, ar1 ->  {
                if (ar1.succeeded()) {
                    HttpClientStream stream = ar1.result();
                    Context ctx = stream.getContext();
                    // id为1，是第一次创建的stream
                    if (stream.id() == 1 && initializer != null) {
                        ctx.executeFromIO(v -> initializer.handle(stream.connection()));
                    }

                    if (exceptionOccurred != null || reset != null) {
                        // 若出现了错误，或者reset不为空，则reset
                        stream.reset(0);
                    } else {
                        // 否则，建立连接
                        ctx.executeFromIO(v -> connected(headersHandler, stream));
                    }
                } else {
                    connectCtx.executeFromIO(v -> handleException(ar1.cause()));
                }
            });
        }
    }

    private void connected(Handler<HttpVersion> headersHandler, HttpClientStream stream) {
        synchronized (this) {
            this.stream = stream;
            stream.beginRequest(this);

            if (pendingMaxSize != -1) {
                stream.doSetWriteQueueMaxSize(pendingMaxSize);
            }

            if (pendingChunks != null) {
                ByteBuf pending = pendingChunks;
                pendingChunks = null;

                if (completed) {
                    // we also need to write the head so optimize this and write all out in once
                    stream.writeHead(method, uri, headers, hostHeader(), chunked, pending, true);
                    stream.endRequest();
                } else {
                    stream.writeHead(method, uri, headers, hostHeader(), chunked, pending, false);
                }
            } else {
                if (completed) {
                    // we also need to write the head so optimize this and write all out in once
                    stream.writeHead(method, uri, headers, hostHeader(), chunked, null, true);
                    stream.endRequest();
                } else {
                    stream.writeHead(method, uri, headers, hostHeader(), chunked, null, false);
                }
            }
            this.connecting = false;
            this.stream = stream;
        }
        if (headersHandler != null) {
            headersHandler.handle(null);
        }

    }

    private void checkResponseHandler() {
        if (stream == null && !connecting && respFut.getHandler() == null) {
            throw new IllegalStateException("You must set a response handler before connecting to the server");
        }
    }

    @Override
    public void end(String chunk) {
        end(Buffer.buffer(chunk));
    }



    @Override
    public void end(Buffer chunk) {
        write(chunk.getByteBuf(), true);
    }

    @Override
    public void end() {
        write(null, true);
    }

    @Override
    public HttpClientRequestImpl write(Buffer chunk) {
        ByteBuf buf = chunk.getByteBuf();
        write(buf, false);
        return this;
    }

    @Override
    public HttpClientRequestImpl write(String chunk) {
        return write(Buffer.buffer(chunk));
    }


    private void write(ByteBuf buff, boolean end) {
        HttpClientStream s;
        synchronized (this) {
            checkComplete();
            checkResponseHandler();
            if (end) {
                if (buff != null && !chunked && !contentLengthSet()) {
                    headers().set(CONTENT_LENGTH, String.valueOf(buff.readableBytes()));
                }
            } else {
                if (!chunked && !contentLengthSet()) {
                    throw new IllegalStateException("You must set the Content-Length header to be the total size of the message "
                            + "body BEFORE sending any data if you are not using HTTP chunked encoding.");
                }
            }
            if (buff == null && !end) {
                // nothing to write to the connection just return
                return;
            }
            if (buff != null) {
                written += buff.readableBytes();
            }
            // 第一次写 建立连接
            if ((s = stream) == null) {
                if (buff != null) {
                    if (pendingChunks == null) {
                        pendingChunks = buff;
                    } else {
                        CompositeByteBuf pending;
                        if (pendingChunks instanceof CompositeByteBuf) {
                            pending = (CompositeByteBuf) pendingChunks;
                        } else {
                            pending = Unpooled.compositeBuffer();
                            pending.addComponent(true, pendingChunks);
                            pendingChunks = pending;
                        }
                        pending.addComponent(true, buff);
                    }
                }
                if (end) {
                    tryComplete();
                    if (completionHandler != null) {
                        completionHandler.handle(null);
                    }
                }
                connect(null);
                return;
            }
        }
        s.writeBuffer(buff, end);

        if (end) {
            Handler<Void> handler;
            synchronized (this) {
                tryComplete();
                s.endRequest();
                if ((handler = completionHandler) == null) {
                    return;
                }
            }
            handler.handle(null);
        }
    }

    private boolean tryComplete() {
        if (!completed) {
            completed = true;
            drainHandler = null;
            exceptionHandler = null;
            return true;
        } else {
            return false;
        }
    }

    private boolean contentLengthSet() {
        return headers != null && headers().contains(CONTENT_LENGTH);
    }

    @Override
    protected void checkComplete() {
        if (completed) {
            throw new IllegalStateException("Request already complete");
        }
    }

    @Override
    protected void doHandleResponse(HttpClientResponse resp, long timeoutMs) {
        if (reset == null) {
            respFut.complete(resp);
        }
    }

    @Override
    public void handleDrained() {
        Handler<Void> handler;
        synchronized (this) {
            if ((handler = drainHandler) == null) {
                return;
            }
        }
        try {
            handler.handle(null);
        } catch (Throwable t) {
            handleException(t);
        }
    }

    @Override
    public HttpClientRequest putHeader(CharSequence name, CharSequence value) {
        return null;
    }


    @Override
    public boolean reset(long code) {
        HttpClientStream s;
        synchronized (this) {
            if (reset != null) {
                return false;
            }
            reset = code;
            if (tryComplete()) {
                if (completionHandler != null) {
                    completionHandler.handle(null);
                }
            }
            s = stream;
        }
        if (s != null) {
            s.reset(code);
        }
        return true;
    }

    @Override
    public HttpConnection connection() {
        HttpClientStream s;
        synchronized (this) {
            if ((s = stream) == null) {
                return null;
            }
        }
        return s.connection();
    }

    @Override
    public synchronized HttpClientRequest connectionHandler(Handler<HttpConnection> handler) {
        connectionHandler = handler;
        return this;
    }
    

}
