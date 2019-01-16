package org.stool.myserver.core.http.impl;

import io.netty.channel.ChannelHandlerContext;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.net.NetSocket;

import java.nio.channels.Channel;

public interface HttpClientConnection {

    Channel channel();

    ChannelHandlerContext channelHandlerContext();

    void close();

    void createStream(Handler<AsyncResult<HttpClientStream>> handler);

    Context getContext();

}
