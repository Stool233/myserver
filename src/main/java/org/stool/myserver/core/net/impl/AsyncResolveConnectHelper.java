package org.stool.myserver.core.net.impl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.Future;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.net.SocketAddress;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class AsyncResolveConnectHelper {

    private List<Handler<AsyncResult<Channel>>> handlers = new ArrayList<>();
    private ChannelFuture future;
    private AsyncResult<Channel> result;

    public synchronized void addListener(Handler<AsyncResult<Channel>> handler) {
        if (result != null) {
            if (future != null) {
                future.addListener(v -> handler.handle(result));
            } else {
                handler.handle(result);
            }
        } else {
            handlers.add(handler);
        }

    }

    private synchronized void handle(ChannelFuture cf, AsyncResult<Channel> res) {
        if (result == null) {
            for (Handler<AsyncResult<Channel>> handler: handlers) {
                handler.handle(res);
            }
            future = cf;
            result = res;
        } else {
            throw new IllegalStateException("Already complete!");
        }
    }

    public static AsyncResolveConnectHelper doBind(EntryPoint entryPoint, int port, ServerBootstrap bootstrap) {
        AsyncResolveConnectHelper asyncResolveConnectHelper = new AsyncResolveConnectHelper();
        bootstrap.channelFactory(NioServerSocketChannel::new);
        InetSocketAddress t = new InetSocketAddress(port);
        ChannelFuture future = bootstrap.bind(t);
        future.addListener(f -> {
            if (f.isSuccess()) {
                asyncResolveConnectHelper.handle(future, Future.succeededFuture(future.channel()));
            } else {
                asyncResolveConnectHelper.handle(future, Future.failedFuture(f.cause()));
            }
        });
        return asyncResolveConnectHelper;
    }
}
