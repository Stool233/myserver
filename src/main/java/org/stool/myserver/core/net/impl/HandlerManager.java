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
    private final ConcurrentMap<EventLoop, Handlers<T>> handlerMap = new ConcurrentHashMap<>();

    private volatile boolean hasHandlers;

    public HandlerManager(MyEventLoopGroup availableWorkers) {
        this.availableWorkers = availableWorkers;
    }

    public boolean hasHandlers() {
        return hasHandlers;
    }

    public HandlerHolder<T> chooseHandler(EventLoop worker) {
        Handlers<T> handlers = handlerMap.get(worker);
        return handlers == null ? null : handlers.chooseHandler();
    }

    public synchronized void addHandler(T handler, Context context) {
        EventLoop worker = context.getEventLoop();
        availableWorkers.addWorker(worker);
        Handlers<T> handlers = new Handlers<>();
        Handlers<T> prev = handlerMap.putIfAbsent(worker, handlers);
        if (prev != null) {
            handlers = prev;
        }
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

    // 轮转
    private static final class Handlers<T> {
        private int pos;
        private final List<HandlerHolder<T>> list = new CopyOnWriteArrayList<>();
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
