package org.stool.myserver.core.net.impl;

import io.netty.channel.*;
import io.netty.util.concurrent.AbstractEventExecutorGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class MyEventLoopGroup extends AbstractEventExecutorGroup implements EventLoopGroup {

    private int pos;
    private final List<EventLoopHolder> workers = new ArrayList<>();

    @Override
    public EventLoop next() {
        if (workers.isEmpty()) {
            throw new IllegalStateException();
        } else {
            EventLoop worker = workers.get(pos).worker;
            pos++;
            checkPos();
            return worker;
        }
    }

    private void checkPos() {
        if (pos == workers.size()) {
            pos = 0;
        }
    }

    public synchronized void addWorker(EventLoop worker) {
        EventLoopHolder holder = findHolder(worker);
        if (holder == null) {
            workers.add(new EventLoopHolder(worker));
        } else {
            holder.count++;
        }
    }

    public synchronized void removeWorker(EventLoop worker) {
        EventLoopHolder holder = findHolder(worker);
        if (holder != null) {
            holder.count--;
            if (holder.count == 0) {
                workers.remove(holder);
            }
            checkPos();
        } else {
            throw new IllegalStateException("Can't find worker to remove");
        }
    }

    public synchronized int workerCount() {
        return workers.size();
    }

    private EventLoopHolder findHolder(EventLoop worker) {
        EventLoopHolder wh = new EventLoopHolder(worker);
        for (EventLoopHolder holder : workers) {
            if (holder.equals(wh)) {
                return holder;
            }
        }
        return null;
    }

    @Override
    public ChannelFuture register(Channel channel) {
        return next().register(channel);
    }

    @Override
    public ChannelFuture register(ChannelPromise promise) {
        return next().register(promise);
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelPromise promise) {
        return next().register(channel, promise);
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException("Should never be called");
    }


    @Override
    public boolean isShuttingDown() {
        return false;
    }

    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException("Should never be called");
    }

    @Override
    public Future<?> terminationFuture() {
        throw new UnsupportedOperationException("Should never be called");
    }

    @Override
    public Iterator<EventExecutor> iterator() {
        return children.iterator();
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    private static class EventLoopHolder {
        int count = 1;
        final EventLoop worker;

        public EventLoopHolder(EventLoop worker) {
            this.worker = worker;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EventLoopHolder that = (EventLoopHolder) o;
            return Objects.equals(worker, that.worker);
        }

        @Override
        public int hashCode() {
            return Objects.hash(worker);
        }
    }

    private final Set<EventExecutor> children = new Set<EventExecutor>() {
        @Override
        public int size() {
            return workers.size();
        }

        @Override
        public boolean isEmpty() {
            return workers.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return workers.contains(o);
        }

        @Override
        public Iterator<EventExecutor> iterator() {
            return new EventLoopIterator(workers.iterator());
        }

        @Override
        public Object[] toArray() {
            return workers.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return workers.toArray(a);
        }

        @Override
        public boolean add(EventExecutor eventExecutor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return workers.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends EventExecutor> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    };

    private static final class EventLoopIterator implements Iterator<EventExecutor> {
        private final Iterator<EventLoopHolder> holderIt;

        public EventLoopIterator(Iterator<EventLoopHolder> holderIt) {
            this.holderIt = holderIt;
        }

        @Override
        public boolean hasNext() {
            return holderIt.hasNext();
        }

        @Override
        public EventExecutor next() {
            return holderIt.next().worker;
        }
    }
}
