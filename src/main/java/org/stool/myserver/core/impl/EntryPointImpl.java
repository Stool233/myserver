package org.stool.myserver.core.impl;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpServer;
import org.stool.myserver.core.http.impl.HttpServerImpl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class EntryPointImpl implements EntryPoint {

    private EventLoopGroup acceptorEventLoopGroup;
    private EventLoopGroup ioworkerEventLoopGroup;

    private ExecutorService workerPool;

    private ThreadFactory threadFactory;


    public EntryPointImpl() {

        threadFactory = new MyThreadFactory();

        acceptorEventLoopGroup = eventLoopGroup(1, threadFactory, 100);
        ioworkerEventLoopGroup = eventLoopGroup(4, threadFactory, 50);
        workerPool = Executors.newFixedThreadPool(20, threadFactory);

    }

    private EventLoopGroup eventLoopGroup(int nThreads, ThreadFactory threadFactory, int ioRatio) {
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(nThreads, threadFactory);
        eventLoopGroup.setIoRatio(ioRatio);
        return eventLoopGroup;
    }

    @Override
    public EventLoopGroup getIOWorkerEventLoopGroup() {
        return ioworkerEventLoopGroup;
    }

    @Override
    public EventLoopGroup getAcceptorEventLoopGroup() {
        return acceptorEventLoopGroup;
    }

    @Override
    public ExecutorService getWorkerPool() {
        return workerPool;
    }

    @Override
    public Context getContext(){
        Context context = context();
        if (context != null || context.owner() == this) {
            return context;
        }
        return null;
    }

    @Override
    public Context getOrCreateContext() {
        Context context = getContext();
        if (context == null) {
            context = new EventLoopContext(this);
        }
        return context;
    }

    @Override
    public Context currentContext() {
        return context();
    }

    public static Context context() {
        Thread current = Thread.currentThread();
        if (current instanceof MyThread) {
            return ((MyThread) current).getContext();
        }
        return null;
    }

    @Override
    public void runOnContext(Handler<Void> action) {
        Context context = getOrCreateContext();
        context.runOnContext(action);
    }

    @Override
    public HttpServer createHttpServer() {
        return new HttpServerImpl(this);
    }
}
