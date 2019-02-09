package org.stool.myserver.core.http.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.Future;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.*;
import org.stool.myserver.core.http.HttpMethod;
import org.stool.myserver.core.http.impl.pool.ConnectionListener;
import org.stool.myserver.core.net.Buffer;
import org.stool.myserver.core.net.NetSocket;
import org.stool.myserver.core.net.SocketAddress;
import org.stool.myserver.core.net.impl.BaseConnection;
import org.stool.myserver.core.net.impl.MyNettyHandler;
import org.stool.myserver.core.net.impl.SocketAddressImpl;
import org.stool.myserver.core.streams.InboundBuffer;

import java.net.InetSocketAddress;


public class HttpClientConnection extends HttpBaseConnection implements org.stool.myserver.core.http.HttpClientConnection {

    private final ConnectionListener<HttpClientConnection> listener;
    private final HttpClient client;
    private final String host;
    private final int port;
    private final HttpClientOptions options;

    private StreamImpl requestInProgress;
    private StreamImpl responseInProgress;

    private boolean close;
    private int keepAliveTimeout;
    private int seq = 1;

    public HttpClientConnection(ConnectionListener<HttpClientConnection> listener,
                                HttpClient client,
                                ChannelHandlerContext channel,
                                String host,
                                int port,
                                Context context) {
        super(client.getEntryPoint(), channel, context);
        this.listener = listener;
        this.client = client;
        this.host = host;
        this.port = port;
        this.options = client.getOptions();
    }


    @Override
    protected void handleInterestedOpsChanged() {
        if (!isNotWritable()) {
            if (requestInProgress != null) {
                requestInProgress.request.handleDrained();
            }
        }
    }

    @Override
    public void handleMessage(Object msg) {
        Throwable error = validateMessage(msg);
        if (error != null) {
            fail(error);
        } else if (msg instanceof HttpObject) {
            HttpObject obj = (HttpObject) msg;
            handleHttpMessage(obj);
        } else {
            throw new IllegalStateException("Invalid object " + msg);
        }
    }

    private void handleHttpMessage(HttpObject obj) {
        if (obj instanceof HttpResponse) {
            handleResponseBegin((HttpResponse) obj);
        } else if (obj instanceof HttpContent) {
            HttpContent chunk = (HttpContent) obj;
            if (chunk.content().isReadable()) {
                Buffer buff = Buffer.buffer(MyNettyHandler.safeBuffer(chunk.content(), chctx.alloc()));
                handleResponseChunk(buff);
            }
            if (chunk instanceof LastHttpContent) {
                handleResponseEnd((LastHttpContent) chunk);
            }
        }
    }

    private void handleResponseEnd(LastHttpContent trailer) {
        StreamImpl stream;
        synchronized (this) {
            stream = responseInProgress;
            responseInProgress = stream.next;
        }
        if (stream.endResponse(trailer)) {
            checkLifecycle();
        }
    }

    private void checkLifecycle() {
        if (close) {
            close();
        } else {
            recycle();
        }
    }

    private void handleResponseChunk(Buffer buff) {
        StreamImpl resp;
        synchronized (this) {
            resp = responseInProgress;
        }
        if (resp != null) {
            if (!resp.handleChunk(buff)) {
                doPause();
            }
        }
    }

    private void handleResponseBegin(HttpResponse resp) {
        StreamImpl stream;
        HttpClientResponse response;
        HttpClientRequest request;
        Throwable err;
        synchronized (this) {
            stream = responseInProgress;
            request = stream.request;
            try {
                response = stream.beginResponse(resp);
                err = null;
            } catch (Exception e) {
                err = e;
                response = null;
            }
        }
        if (response != null) {
            request.handleResponse(response);
        } else {
            request.handleException(err);
        }
    }


    private Throwable validateMessage(Object msg) {
        return null;
    }

    @Override
    public Channel channel() {
        return chctx.channel();
    }

    @Override
    public ChannelHandlerContext channelHandlerContext() {
        return chctx;
    }

    @Override
    public void close() {
        endReadAndFlush();
        chctx.channel().close();
    }

    @Override
    public void createStream(Handler<AsyncResult<HttpClientStream>> handler) {
        StreamImpl stream;
        synchronized (this) {
            stream = new StreamImpl(this, seq++, handler);
            if (requestInProgress != null) {
                requestInProgress.append(stream);
                return ;
            }
            requestInProgress = stream;
        }
        stream.fut.complete(stream);
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public SocketAddress remoteAddress() {
        InetSocketAddress addr = (InetSocketAddress) chctx.channel().remoteAddress();
        if (addr == null) return null;
        return new SocketAddressImpl(addr);
    }

    @Override
    public SocketAddress localAddress() {
        InetSocketAddress addr = (InetSocketAddress) chctx.channel().localAddress();
        if (addr == null) return null;
        return new SocketAddressImpl(addr);
    }

    private void handleRequestEnd(boolean recycle) {
        StreamImpl next;
        synchronized (this) {
            next = requestInProgress.next;
            requestInProgress = next;
        }
        if (recycle) {
            checkLifecycle();
        }
        if (next != null) {
            next.fut.complete(next);
        }
    }

    private void recycle() {
        long expiration = keepAliveTimeout == 0 ? 0L : System.currentTimeMillis() + keepAliveTimeout * 1000;
        listener.onRecycle(expiration);
    }

    private static class StreamImpl implements HttpClientStream {

        private final int id;
        private final HttpClientConnection conn;
        private final Future<HttpClientStream> fut;
        private HttpClientRequest request;
        private HttpClientResponse response;
        private boolean requestEnded;
        private boolean responseEnded;
        private boolean reset;
        private InboundBuffer<Buffer> queue;
        private StreamImpl next;

        StreamImpl(HttpClientConnection conn, int id, Handler<AsyncResult<HttpClientStream>> handler) {
            this.conn = conn;
            this.fut = Future.<HttpClientStream>future().setHandler(handler);
            this.id = id;
            this.queue = new InboundBuffer<>(conn.context, 5);
        }

        private void append(StreamImpl s) {
            StreamImpl c = this;
            while (c.next != null) {
                c = c.next;
            }
            c.next = s;
        }

        @Override
        public int id() {
            return id;
        }

        @Override
        public HttpConnection connection() {
            return conn;
        }

        @Override
        public Context getContext() {
            return conn.context;
        }

        @Override
        public void writeHead(HttpMethod method, String uri, HttpHeaders headers, String hostHeader, boolean chunked, ByteBuf buf, boolean end) {
            HttpRequest request = createRequest(method, uri, headers);
            prepareRequestHeaders(request, hostHeader, chunked);
            sendRequest(request, buf, end);
            if (conn.responseInProgress == null) {
                conn.responseInProgress = this;
            } else {
                conn.responseInProgress.append(this);
            }
            next = null;
        }

        private void sendRequest(HttpRequest request, ByteBuf buf, boolean end) {
            if (end) {
                if (buf != null) {
//                    request = new AssembledFullHttpRequest(request, buf);
                    conn.writeToChannel(new DefaultFullHttpRequest(request.protocolVersion(), request.method(),
                            request.uri(), buf, request.headers(), EmptyHttpHeaders.INSTANCE));
                } else {
//                    request = new AssembledFullHttpRequest(request);
                    conn.writeToChannel(new DefaultFullHttpRequest(request.protocolVersion(), request.method(),
                            request.uri(), Unpooled.EMPTY_BUFFER, request.headers(), EmptyHttpHeaders.INSTANCE));
                }
            } else {
                if (buf != null) {
//                    request = new AssembledHttpRequest(request, buf);
                    conn.writeToChannel(new DefaultHttpRequest(request.protocolVersion(), request.method(),
                            request.uri(), request.headers()));
                    conn.writeToChannel(new DefaultHttpContent(buf));
                }
            }
//            conn.writeToChannel(request);
        }



        private void prepareRequestHeaders(HttpRequest request, String hostHeader, boolean chunked) {
            HttpHeaders headers = request.headers();
            headers.remove(HttpHeaderNames.TRANSFER_ENCODING);
            if (!headers.contains(HttpHeaderNames.HOST)) {
                request.headers().set(HttpHeaderNames.HOST, hostHeader);
            }
            if (chunked) {
                HttpUtil.setTransferEncodingChunked(request, true);
            }
            if (!conn.options.isKeepAlive()) {
                request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }else {
                request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            }

        }

        private HttpRequest createRequest(HttpMethod method, String uri, HttpHeaders headers) {
            DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpUtils.toNettyHttpMethod(method), uri);
            if (headers != null) {
                request.headers().set(headers);
            }
            return request;
        }

        @Override
        public void writeBuffer(ByteBuf buff, boolean end) {
            if (end) {
                if (buff != null && buff.isReadable()) {
                    conn.writeToChannel(new DefaultLastHttpContent(buff, false));
                } else {
                    conn.writeToChannel(LastHttpContent.EMPTY_LAST_CONTENT);
                }
            } else if (buff != null) {
                conn.writeToChannel(new DefaultHttpContent(buff));
            }
        }

        @Override
        public void doSetWriteQueueMaxSize(int size) {
            conn.doSetWriteQueueMaxSize(size);
        }

        @Override
        public boolean isNotWritable() {
            return conn.isNotWritable();
        }

        @Override
        public void doPause() {
            queue.pause();
        }

        @Override
        public void doResume() {
            queue.resume();
        }

        @Override
        public void doFetch(long amount) {
            queue.fetch(amount);
        }

        @Override
        public void reset(long code) {
            synchronized (conn) {
                if (!reset) {
                    reset = true;
                    if (conn.requestInProgress == this) {
                        if (request == null) {
                            conn.requestInProgress = null;
                            conn.recycle();
                        } else {
                            conn.close();
                        }
                    } else if (!responseEnded) {
                        conn.close();
                    }
                }
            }
        }

        @Override
        public void beginRequest(HttpClientRequest req) {
            synchronized (conn) {
                if (request != null) {
                    throw new IllegalStateException("Already writing a request");
                }
                if (conn.requestInProgress != this) {
                    throw new IllegalStateException("Connection is already writing another request");
                }
                request = req;
            }
        }

        @Override
        public void endRequest() {
            boolean doRecycle;
            synchronized (conn) {
                StreamImpl s = conn.requestInProgress;
                if (s != this) {
                    throw new IllegalStateException("No write in progress");
                }
                if (requestEnded) {
                    throw new IllegalStateException("Request already sent");
                }
                requestEnded = true;

                doRecycle = responseEnded;
            }
            conn.handleRequestEnd(doRecycle);
        }

        @Override
        public NetSocket createNetSocket() {
            return null;
        }

        private HttpClientResponse beginResponse(HttpResponse resp) {
            response = new HttpClientResponseImpl(request, this, resp.status().code(), resp.status().reasonPhrase(), resp.headers());

            if (request.method() != HttpMethod.CONNECT) {
                String responseConnectionHeader = resp.headers().get(HttpHeaderNames.CONNECTION);
                String requestConnectionHeader = request.headers().get(HttpHeaderNames.CONNECTION);
                if (HttpHeaderValues.CLOSE.contentEqualsIgnoreCase(responseConnectionHeader) ||
                        HttpHeaderValues.CLOSE.contentEqualsIgnoreCase(requestConnectionHeader)) {
                    conn.close = true;
                }
                String keepAliveHeader = resp.headers().get(HttpHeaderNames.KEEP_ALIVE);
                if (keepAliveHeader != null) {
                    int timeout = HttpUtils.parseKeepAliveHeaderTimeout(keepAliveHeader);
                    if (timeout != -1) {
                        conn.keepAliveTimeout = timeout;
                    }
                }
            }
            queue.handler(buf -> response.handleChunk(buf));
            queue.emptyHandler(v -> {
                if (responseEnded) {
                    response.handleEnd(null);
                }
            });
            queue.drainHandler(v -> {
                if (!responseEnded) {
                    conn.doResume();
                }
            });
            return response;
        }

        private boolean endResponse(LastHttpContent trailer) {
            synchronized (conn) {
                if (queue.isEmpty()) {
                    response.handleEnd(trailer);
                }
                responseEnded = true;
                conn.close |= !conn.options.isKeepAlive();
                conn.doResume();
                return requestEnded;
            }
        }

        void handleException(Throwable cause) {
            HttpClientRequest request;
            HttpClientResponse response;
            Future<HttpClientStream> fut;
            boolean requestEnded;
            synchronized (conn) {
                request = this.request;
                response = this.response;
                fut = this.fut;
                requestEnded = this.requestEnded;
            }
            if (request != null) {
                if (response == null) {
                    request.handleException(cause);
                } else {
                    if (!requestEnded) {
                        request.handleException(cause);
                    }
                    response.handleException(cause);
                }
            } else {
                fut.tryFail(cause);
            }
        }

        public boolean handleChunk(Buffer buff) {
            return queue.write(buff);
        }


    }


}
