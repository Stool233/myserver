package org.stool.myserver.core.net.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.net.ProxyOptions;
import org.stool.myserver.core.net.SocketAddress;



public class ChannelProvider {

    private final Bootstrap bootstrap;
    private final Context context;
    private final ProxyOptions proxyOptions;
    private Channel channel;

    public ChannelProvider(Bootstrap bootstrap, Context context, ProxyOptions proxyOptions) {
        this.bootstrap = bootstrap;
        this.context = context;
        this.proxyOptions = proxyOptions;
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
        if (proxyOptions != null) {

        }

    }
}
