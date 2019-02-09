package org.stool.myserver.core.impl;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.stool.myserver.core.*;
import org.stool.myserver.core.Future;
import org.stool.myserver.core.file.FileSystemOptions;
import org.stool.myserver.core.file.impl.FileResolver;
import org.stool.myserver.core.http.HttpClient;
import org.stool.myserver.core.http.HttpServer;
import org.stool.myserver.core.http.impl.HttpClientImpl;
import org.stool.myserver.core.http.impl.HttpClientOptions;
import org.stool.myserver.core.http.impl.HttpServerImpl;

import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class EntryPointImpl implements EntryPoint {

    private EventLoopGroup acceptorEventLoopGroup;
    private EventLoopGroup ioworkerEventLoopGroup;

    private ExecutorService workerPool;

    private ThreadFactory threadFactory;
    private final AtomicLong timeoutCounter = new AtomicLong(0);
    private ConcurrentMap<Long, InternalTimerHandler> timeouts = new ConcurrentHashMap<>();

    private final FileResolver fileResolver;


    public EntryPointImpl() {

        threadFactory = new MyThreadFactory();

        acceptorEventLoopGroup = eventLoopGroup(1, threadFactory, 100);
        ioworkerEventLoopGroup = eventLoopGroup(8, threadFactory, 50);
        workerPool = Executors.newFixedThreadPool(20, threadFactory);

        fileResolver = new FileResolver(new FileSystemOptions());
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
        if (context != null && context.owner() == this) {
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

    @Override
    public HttpClient createHttpClient() {
        return new HttpClientImpl(this);
    }

    @Override
    public File resolveFile(String fileName) {
        return fileResolver.resolveFile(fileName);
    }

    @Override
    public long setTimer(long delay, Handler<Long> handler) {
        return scheduleTimeout(getOrCreateContext(), handler, delay, false);
    }

    private long scheduleTimeout(Context context, Handler<Long> handler, long delay, boolean periodic) {
        long timerId = timeoutCounter.getAndIncrement();
        InternalTimerHandler task = new InternalTimerHandler(timerId, handler, periodic, delay, context);
        timeouts.put(timerId, task);
        context.addCloseHook(task);
        return timerId;
    }

    @Override
    public boolean cancelTimer(long id) {
        InternalTimerHandler handler = timeouts.remove(id);
        if (handler != null) {
            handler.context.removeCloseHook(handler);
            return handler.cancel();
        } else {
            return false;
        }
    }

    /**
     * 实现javascript的setTimeout和setInterval
     */
    private class InternalTimerHandler implements Handler<Void>, Closeable {
        final Handler<Long> handler;
        final boolean periodic;
        final long timerID;
        final Context context;
        final java.util.concurrent.Future<?> future;
        final AtomicBoolean cancelled;


        InternalTimerHandler(long timerID, Handler<Long> runnable, boolean periodic, long delay, Context context) {
            this.context = context;
            this.timerID = timerID;
            this.handler = runnable;
            this.periodic = periodic;
            this.cancelled = new AtomicBoolean();
            EventLoop el = context.getEventLoop();
            Runnable toRun = () -> context.runOnContext(this);
            if (periodic) {
                future = el.scheduleAtFixedRate(toRun, delay, delay, TimeUnit.MILLISECONDS);
            } else {
                future = el.schedule(toRun, delay, TimeUnit.MILLISECONDS);
            }
        }

        boolean cancel() {
            if (cancelled.compareAndSet(false, true)) {
                future.cancel(false);
                return true;
            } else {
                return false;
            }
        }

        public void handle(Void v) {
            if (!cancelled.get()) {
                try {
                    handler.handle(timerID);
                } finally {
                    if (!periodic) {
                        cleanupNonPeriodic();
                    }
                }
            }
        }

        private void cleanupNonPeriodic() {
            EntryPointImpl.this.timeouts.remove(timerID);
            Context context = getContext();
            if (context != null) {
                context.removeCloseHook(this);
            }
        }
        public void close(Handler<AsyncResult<Void>> completionHandler) {
            EntryPointImpl.this.timeouts.remove(timerID);
            cancel();
            completionHandler.handle(Future.succeededFuture());
        }
    }
}
