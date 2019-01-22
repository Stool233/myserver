package org.stool.myserver.core.impl;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stool.myserver.core.*;

import java.util.Map;
import java.util.concurrent.ExecutorService;

public abstract class ContextImpl implements Context {

    private static final Logger log = LoggerFactory.getLogger(ContextImpl.class);

    private EntryPoint owner;
    private EventLoop eventLoop;
    private ExecutorService workerPool;
    private CloseHooks closeHooks;

    public ContextImpl(EntryPoint owner) {
        this.owner = owner;
        this.eventLoop = getEventLoop(owner);
        workerPool = owner.getWorkerPool();
        this.closeHooks = new CloseHooks(log);
    }

    public static boolean isOnMyThread(boolean worker) {
        Thread t = Thread.currentThread();
        if (t instanceof MyThread) {
            MyThread mt = (MyThread) t;
            return mt.isWorker() == worker;
        }
        return false;
    }

    private static EventLoop getEventLoop(EntryPoint entryPoint) {
        if (entryPoint.getIOWorkerEventLoopGroup() != null) {
            return entryPoint.getIOWorkerEventLoopGroup().next();
        }
        return null;
    }

    public static void setContext(ContextImpl context) {
        Thread current = Thread.currentThread();
        if (current instanceof MyThread) {
            setContext((MyThread) current, context);
        } else {
            throw new IllegalStateException("Attempt to setContext on non MyThread " + Thread.currentThread());
        }
    }

    private static void setContext(MyThread thread, ContextImpl context) {
        thread.setContext(context);
    }

    @Override
    public void runOnContext(Handler<Void> action) {
        executeAsync(action);
    }


    @Override
    public <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered, Handler<AsyncResult<T>> asyncResultHandler) {
        Runnable command = () -> {
            Future<T> res = Future.future();
            if (blockingCodeHandler != null) {
                setContext(ContextImpl.this);
                blockingCodeHandler.handle(res);
            }

            if (asyncResultHandler != null) {
                runOnContext(action -> res.setHandler(asyncResultHandler));
            }
        };
        if (ordered) {
            // TODO
        } else {
            workerPool.execute(command);
        }
    }

    @Override
    public void executeFromIO(ContextTask task) {
        task.run();
    }

    @Override
    public void executeFromIO(Handler<Void> task) {
        executeFromIO(null, task);
    }

    @Override
    public <T> void executeFromIO(T value, Handler<T> task) {
        checkEventLoopThread();
        execute(value, task);
    }

    protected abstract <T> void execute(T value, Handler<T> task);

    public <T> boolean executeTask(T arg, Handler<T> hTask) {
        Thread th = Thread.currentThread();
        if (!(th instanceof MyThread)) {
            throw new IllegalStateException("is not MyThread");
        }
        MyThread current = (MyThread) th;

        try {
            setContext(current, this);
            hTask.handle(arg);
            return true;
        } catch (Throwable t) {
            return false;
        } finally {

        }
    }

    private void checkEventLoopThread() {
        Thread current = Thread.currentThread();
        if (!(current instanceof MyThread)) {
            throw new IllegalStateException("is not MyThread ");
        }
    }

    @Override
    public <T> T get(String key) {
        return null;
    }

    @Override
    public <T> void put(String key, T value) {

    }

    @Override
    public boolean remove(String key) {
        return false;
    }

    @Override
    public EntryPoint owner() {
        return owner;
    }

    @Override
    public Context exceptionHandler(Handler<Throwable> handler) {
        return null;
    }

    @Override
    public Handler<Throwable> exceptionHandler() {
        return null;
    }

    @Override
    public EventLoop getEventLoop() {
        return eventLoop;
    }


    @Override
    public EventLoop nettyEventLoop() {
        return eventLoop;
    }

    @Override
    public void addCloseHook(Closeable hook) {
        closeHooks.add(hook);
    }

    @Override
    public void removeCloseHook(Closeable hook) {
        closeHooks.remove(hook);
    }

    @Override
    public void runCloseHooks(Handler<AsyncResult<Void>> completionHandler) {
        closeHooks.run(completionHandler);
        // Now remove context references from threads
        MyThreadFactory.unsetContext(this);
    }

}
