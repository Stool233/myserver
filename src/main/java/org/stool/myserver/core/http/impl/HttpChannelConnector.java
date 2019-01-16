package org.stool.myserver.core.http.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.Future;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpClient;
import org.stool.myserver.core.http.impl.pool.ConnectResult;
import org.stool.myserver.core.http.impl.pool.ConnectionListener;
import org.stool.myserver.core.http.impl.pool.ConnectionProvider;

public class HttpChannelConnector implements ConnectionProvider<HttpClientConnection{

    private final HttpClient client;
    private final long weight;
    private final long maxConcurrency;
    private final String peerHost;
    private final String host;
    private final int port;

    public HttpChannelConnector(HttpClient client, String peerHost, String host, int port) {
        this.client = client;
        this.weight = 8;
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

    }

    @Override
    public void close(HttpClientConnection conn) {
        conn.close();
    }
}
