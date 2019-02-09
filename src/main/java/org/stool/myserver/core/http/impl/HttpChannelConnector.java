package org.stool.myserver.core.http.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.Future;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpClient;
import org.stool.myserver.core.http.impl.pool.ConnectResult;
import org.stool.myserver.core.http.impl.pool.ConnectionListener;
import org.stool.myserver.core.http.impl.pool.ConnectionProvider;
import org.stool.myserver.core.net.SocketAddress;
import org.stool.myserver.core.net.impl.ChannelProvider;
import org.stool.myserver.core.net.impl.MyNettyHandler;

public class HttpChannelConnector implements ConnectionProvider<HttpClientConnection> {

    private final HttpClient client;
    private final long weight;
    private final long maxConcurrency;
    private final String peerHost;
    private final String host;
    private final int port;

    public HttpChannelConnector(HttpClient client, String peerHost, String host, int port) {
        this.client = client;
        this.weight = 1;
        this.maxConcurrency = 1;
        this.peerHost = peerHost;
        this.host = host;
        this.port = port;
    }

    public long weight() {
        return weight;
    }

    @Override
    public void connect(ConnectionListener<HttpClientConnection> listener, Context context, Handler<AsyncResult<ConnectResult<HttpClientConnection>>> handler) {
        Future<ConnectResult<HttpClientConnection>> future = Future.<ConnectResult<HttpClientConnection>>future().setHandler(handler);
        try {
            doConnect(listener, context,future);
        } catch (Exception e) {
            future.tryFail(e);
        }
    }

    private void doConnect(ConnectionListener<HttpClientConnection> listener, Context context, Future<ConnectResult<HttpClientConnection>> future) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(context.getEventLoop());
        bootstrap.channelFactory(NioSocketChannel::new);

        ChannelProvider channelProvider = new ChannelProvider(bootstrap, context);

        Handler<AsyncResult<Channel>> channelHandler = res -> {
            if (res.succeeded()) {
                Channel ch = res.result();
                ChannelPipeline pipeline = ch.pipeline();
                applyHttp1xConnectionOptions(pipeline);
                http1xConnected(listener, host, port, context, ch, future);
            } else {
                connectFailed(channelProvider.channel(), listener, res.cause(), future);
            }
        };

        channelProvider.connect(SocketAddress.inetSocketAddress(host, port),
                SocketAddress.inetSocketAddress(peerHost, port),
                peerHost,
                channelHandler);
    }

    private void connectFailed(Channel ch, ConnectionListener<HttpClientConnection> listener, Throwable t, Future<ConnectResult<HttpClientConnection>> future) {
        if (ch != null) {
            try {
                ch.close();
            } catch (Exception ignore) {
            }
        }
        future.tryFail(t);
    }

    private void http1xConnected(ConnectionListener<HttpClientConnection> listener,
                                 String host,
                                 int port,
                                 Context context,
                                 Channel ch,
                                 Future<ConnectResult<HttpClientConnection>> future) {
        MyNettyHandler<HttpClientConnection> clientHandler = MyNettyHandler.create(context, chctx -> {
            HttpClientConnection conn = new HttpClientConnection(listener, client, chctx, host, port, context);
            return conn;
        });
        clientHandler.addHandler(conn -> {
            future.complete(new ConnectResult<>(conn, maxConcurrency, weight));
        });
        clientHandler.removeHandler(conn -> {
            listener.onEvict();
        });
        ch.pipeline().addLast("handler", clientHandler);
    }

    private void applyHttp1xConnectionOptions(ChannelPipeline pipeline) {
        pipeline.addLast("codec", new HttpClientCodec());
    }

    @Override
    public void close(HttpClientConnection conn) {
        conn.close();
    }
}
