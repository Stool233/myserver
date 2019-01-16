package org.stool.myserver.core.net.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.net.SocketAddress;



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
            
        };
    }
}
