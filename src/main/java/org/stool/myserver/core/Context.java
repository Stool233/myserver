package org.stool.myserver.core;

import io.netty.channel.EventLoop;
import org.stool.myserver.core.impl.ContextImpl;

/**
 * 执行上下文
 */
public interface Context {


    static boolean isOnWorkerThread() {
        return ContextImpl.isOnMyThread(true);
    }

    static boolean isOnEventLoopThread() {
        return ContextImpl.isOnMyThread(false);
    }

    /**
     * 在当前的Context执行一个action，异步执行
     * @param action
     */
    void runOnContext(Handler<Void> action);

    /**
     * 执行异步任务
     * @param task
     */
    void executeAsync(Handler<Void> task);

    /**
     * 在工作线程池中执行阻塞任务
     * @param blockingCodeHandler 阻塞任务，在工作线程池中执行
     * @param resultHandler 在阻塞任务执行完成后会执行的任务，在调用者的线程中执行
     * @param <T>
     */
    <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, Handler<AsyncResult<T>> resultHandler);

    /**
     * 直接在IO线程执行任务
     * @param task
     */
    void executeFromIO(ContextTask task);

    void executeFromIO(Handler<Void> task);

    <T> void executeFromIO(T value, Handler<T> task);

    <T> T get(String key);

    <T> void put(String key, T value);

    boolean remove(String key);

    EntryPoint owner();

    /**
     * 设置异常处理
     * @param handler
     * @return 返回自己
     */
    Context exceptionHandler(Handler<Throwable> handler);

    /**
     * 返回异常处理
     * @return
     */
    Handler<Throwable> exceptionHandler();

    EventLoop getEventLoop();

    EventLoop nettyEventLoop();

    void addCloseHook(Closeable hook);

    void removeCloseHook(Closeable hook);

    void runCloseHooks(Handler<AsyncResult<Void>> completionHandler);

    /**
     * 上下文任务
     */
    interface ContextTask {
        void run();
    }

}
