package org.stool.myserver.core.net.impl;

import io.netty.channel.ChannelHandlerContext;
import org.stool.myserver.core.EntryPointInternal;
import org.stool.myserver.core.impl.ContextImpl;

public class HttpServerConnction extends HttpBaseConnection {

    public HttpServerConnction(EntryPointInternal entryPoint, ChannelHandlerContext chctx, ContextImpl context) {
        super(entryPoint, chctx, context);
    }

    @Override
    protected void handleInterestedOpsChanged() {

    }
}
