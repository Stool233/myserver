package org.stool.myserver.core.http.impl;

import io.netty.channel.ChannelHandlerContext;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpConnection;
import org.stool.myserver.core.impl.ContextImpl;
import org.stool.myserver.core.net.impl.BaseConnection;

public abstract class HttpBaseConnection extends BaseConnection implements HttpConnection {

    public HttpBaseConnection(EntryPoint entryPoint, ChannelHandlerContext chctx, Context context) {
        super(entryPoint, chctx, context);
    }

    @Override
    public HttpBaseConnection closeHandler(Handler<Void> handler) {
        return (HttpBaseConnection) super.closeHandler(handler);
    }

    @Override
    public HttpBaseConnection exceptionHandler(Handler<Throwable> handler) {
        return (HttpBaseConnection) super.exceptionHandler(handler);
    }
}
