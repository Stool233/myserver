package org.stool.myserver.core.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpConnection;
import org.stool.myserver.core.http.impl.HttpClientStream;
import org.stool.myserver.core.net.NetSocket;


public interface HttpClientConnection extends HttpConnection {

    Channel channel();

    ChannelHandlerContext channelHandlerContext();

    void close();

    void createStream(Handler<AsyncResult<HttpClientStream>> handler);

    Context getContext();

}
