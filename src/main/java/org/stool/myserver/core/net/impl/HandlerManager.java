package org.stool.myserver.core.net.impl;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import org.stool.myserver.core.Context;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class HandlerManager<T> {


    private final MyEventLoopGroup availableWorkers;
    // eventLoop(工作线程)与handlers的映射
    // 可以通过该数据结构找到eventLoop工作线程对应的handlers
    private final ConcurrentMap<EventLoop, Handlers<T>> handlerMap = new ConcurrentHashMap<>();

    private volatile boolean hasHandlers;

    public HandlerManager(MyEventLoopGroup availableWorkers) {
        this.availableWorkers = availableWorkers;
    }

    public boolean hasHandlers() {
        return hasHandlers;
    }

    public HandlerHolder<T> chooseHandler(EventLoop worker) {
        // 取出工作线程worker对应的handlers
        Handlers<T> handlers = handlerMap.get(worker);
        // 从handlers中取一个handlerHolder返回
        return handlers == null ? null : handlers.chooseHandler();
    }

    public synchronized void addHandler(T handler, Context context) {
        EventLoop worker = context.getEventLoop();
        availableWorkers.addWorker(worker);
        // 尝试从handlerMap中取出worker对应的handlers，若取不到就加一个新的handlers
        Handlers<T> handlers = new Handlers<>();
        Handlers<T> prev = handlerMap.putIfAbsent(worker, handlers);
        if (prev != null) {
            handlers = prev;
        }
        // 向handlers中加入一个HandlerHolder，其包装了context与对应的handler
        handlers.addHandler(new HandlerHolder<>(context, handler));
        hasHandlers = true;
    }

    public synchronized void removeHandler(T handler, Context context) {
        EventLoop worker = context.getEventLoop();
        Handlers<T> handlers = handlerMap.get(worker);
        if (!handlers.removeHandler(new HandlerHolder<>(context, handler))) {
            throw new IllegalStateException("Can't find handler");
        }
        if (handlers.isEmpty()) {
            hasHandlers = false;
        }
        availableWorkers.removeWorker(worker);
    }

    /**
     * 该数据结构实现HandlerHolder的池，可以从中取出可以用的HandlerHolder
     * @param <T>
     */
    private static final class Handlers<T> {
        private int pos;
        // CopyOnWrite保证线程安全
        private final List<HandlerHolder<T>> list = new CopyOnWriteArrayList<>();

        /**
         * 通过轮转的方式取HandlerHolder，也可以当作是一个有界循环列表。
         * @return
         */
        HandlerHolder<T> chooseHandler() {
            HandlerHolder<T> handler = list.get(pos);
            pos++;
            checkPos();
            return handler;
        }

        void addHandler(HandlerHolder<T> handler) {
            list.add(handler);
        }

        boolean removeHandler(HandlerHolder<T> handler) {
            if (list.remove(handler)) {
                checkPos();
                return true;
            } else {
                return false;
            }
        }

        /**
         * pos到达列表边界了，重新归0
         * 通过该方法避免取模（%），以达到更高的性能
         */
        void checkPos() {
            if (pos == list.size()) {
                pos = 0;
            }
        }

        public boolean isEmpty() {
            return list.isEmpty();
        }
    }
}
