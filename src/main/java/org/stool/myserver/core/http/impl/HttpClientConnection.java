package org.stool.myserver.core.http.impl;

import io.netty.buffer.ByteBuf;
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
import org.stool.myserver.core.streams.InboundBuffer;


public class HttpClientConnection extends HttpBaseConnection implements org.stool.myserver.core.http.HttpClientConnection {

    private final ConnectionListener<HttpClientConnection> listener;
    private final HttpClient client;
    private final String host;
    private final int port;
    private final HttpClientOptions options;

    private StreamImpl requestInProgress;
    private StreamImpl responseInProgress;

    private boolean close;
    private boolean upgraded;
    private int keepAliveTimeout;
    private int seq;

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

    }

    @Override
    public void handleMessage(Object msg) {

    }

    @Override
    public Channel channel() {
        return null;
    }

    @Override
    public ChannelHandlerContext channelHandlerContext() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void createStream(Handler<AsyncResult<HttpClientStream>> handler) {

    }

    @Override
    public Context getContext() {
        return null;
    }

    @Override
    public SocketAddress remoteAddress() {
        return null;
    }

    @Override
    public SocketAddress localAddress() {
        return null;
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

        public StreamImpl(int id, HttpClientConnection conn, Future<HttpClientStream> fut) {
            this.id = id;
            this.conn = conn;
            this.fut = fut;
            this.queue = new InboundBuffer<>(conn.getContext(), 5);
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
                    request = new AssembledFullHttpRequest(request, buf);
                } else {
                    request = new AssembledFullHttpRequest(request);
                }
            } else {
                if (buf != null) {
                    request = new AssembledHttpRequest(request, buf);
                }
            }
            conn.writeToChannel(request);
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
        public void writeBuffer(ByteBuf buf, boolean end) {

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

        }

        @Override
        public void beginRequest(HttpClientRequest req) {

        }

        @Override
        public void endRequest() {

        }

        @Override
        public NetSocket createNetSocket() {
            return null;
        }
    }
}
