package org.stool.myserver.core.impl;

import io.netty.channel.EventLoop;
import org.stool.myserver.core.*;

import java.util.Map;
import java.util.concurrent.ExecutorService;

public abstract class ContextImpl implements Context {

    private EntryPoint owner;
    private EventLoop eventLoop;
    private ExecutorService workerPool;

    private static EventLoop getEventLoop(EntryPoint entryPoint) {
        if (entryPoint.getIOWorkerEventLoopGroup() != null) {
            return entryPoint.getIOWorkerEventLoopGroup().next();
        }
        return null;
    }

    public ContextImpl(EntryPoint owner) {
        this.owner = owner;
        this.eventLoop = getEventLoop(owner);
        workerPool = owner.getWorkerPool();
    }


    public static void setContext(ContextImpl context) {
        Thread current = Thread.currentThread();
        if (current instanceof MyThread) {
            setContext((MyThread) current, context);
        } else {
            throw new IllegalStateException("Attempt to setContext on non Vert.x thread " + Thread.currentThread());
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
}
