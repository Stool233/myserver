package org.stool.myserver.core.net.impl;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpConnection;
import org.stool.myserver.core.http.HttpServerRequest;
import org.stool.myserver.core.http.impl.HttpServerRequestImpl;
import org.stool.myserver.core.impl.ContextImpl;
import org.stool.myserver.core.net.NetSocket;
import org.stool.myserver.core.net.SocketAddress;

public class HttpServerConnection extends HttpBaseConnection  implements HttpConnection {

    private static final Logger log = LoggerFactory.getLogger(HttpServerConnection.class);
    public Handler<HttpServerRequest> requestHandler;


    public HttpServerConnection(EntryPoint entryPoint, ChannelHandlerContext chctx, ContextImpl context) {
        super(entryPoint, chctx, context);
    }

    @Override
    public void handleInterestedOpsChanged() {

    }

    @Override
    public void handleMessage(Object msg) {

    }

    @Override
    public SocketAddress localAddress() {
        return null;
    }

    @Override
    public int getWindowSize() {
        return 0;
    }

    public NetSocket createNetSocket() {
        // todo
        return null;
    }

    public void responseComplete() {
    }

    public EntryPoint entryPoint() {
        return null;
        // todo
    }
}
