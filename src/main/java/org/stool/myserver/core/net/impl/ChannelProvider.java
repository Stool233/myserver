package org.stool.myserver.core.net.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import org.stool.myserver.core.*;
import org.stool.myserver.core.net.ProxyOptions;
import org.stool.myserver.core.net.SocketAddress;

import java.net.InetSocketAddress;


public class ChannelProvider {

    private final Bootstrap bootstrap;
    private final Context context;
    private Channel channel;

    public ChannelProvider(Bootstrap bootstrap, Context context) {
        this.bootstrap = bootstrap;
        this.context = context;
    }

    public Channel channel() {
        return channel;
    }

    public void connect(SocketAddress remoteAddress, SocketAddress peerAddress, String serverName, Handler<AsyncResult<Channel>> channelHandler) {
        Handler<AsyncResult<Channel>> handler = res -> {
            if (Context.isOnEventLoopThread()) {
                channelHandler.handle(res);
            } else {
                context.getEventLoop().execute(() -> channelHandler.handle(res));
            }
        };
        handleConnect(remoteAddress, peerAddress, serverName, handler);
    }

    private void handleConnect(SocketAddress remoteAddress, SocketAddress peerAddress, String serverName, Handler<AsyncResult<Channel>> channelHandler) {
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {

            }
        });

        ChannelFuture fut = bootstrap.connect(InetSocketAddress.createUnresolved(remoteAddress.host(), remoteAddress.port()));
        fut.addListener(res -> {
            if (res.isSuccess()) {
                connected(fut.channel(), channelHandler);
            } else {
                channelHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    private void connected(Channel channel, Handler<AsyncResult<Channel>> channelHandler) {
        this.channel = channel;
        channelHandler.handle(Future.succeededFuture(this.channel));
    }
}
