package org.stool.myserver.core.http.impl.pool;

import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.Future;
import org.stool.myserver.core.Handler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * 池是一个状态机，它维护一个waiter队列和一个可用连接列表。
 * @param <C>
 */
public class Pool<C> {

    public class Holder implements ConnectionListener<C> {

        boolean removed;
        C connection;
        long concurrency;
        long capacity;
        long weight;
        long expirationTimestamp;

        private void init(long concurrency, C connection, long weight) {
            this.connection = connection;
            this.concurrency = concurrency;
            this.weight = weight;
            this.capacity = concurrency;
            this.expirationTimestamp = -1L;
        }

        @Override
        public void onConcurrencyChange(long concurrency) {
            setConcurrency(this, concurrency);
        }

        @Override
        public void onRecycle(long expirationTimestamp) {
            recycle(this, expirationTimestamp);
        }

        @Override
        public void onEvict() {
            evicted(this);
        }

        void connect() {
            connector.connect(this, context, ar -> {
                if (ar.succeeded()) {
                    connectSucceeded(this, ar.result());
                } else {
                    connectFailed(this, ar.cause());
                }
            });
        }

        @Override
        public String toString() {
            return "Holder[removed=" + removed + ",capacity=" + capacity + ",concurrency=" + concurrency + ",expirationTimestamp=" + expirationTimestamp + "]";
        }
    }


    private final Context context;
    private final ConnectionProvider<C> connector;
    private final Consumer<C> connectionAdded;
    private final Consumer<C> connectionRemoved;

    private final int queueMaxSize;
    private final Deque<Waiter<C>> waitersQueue = new ArrayDeque<>();   // 等待连接

    private final Deque<Holder> available;                              // 存活的连接
    private final boolean fifo;
    private long capacity;                                              // 存活连接的个数
    private long connecting;                                            // 正在建立连接的连接，还没有连接完成

    private final long initialWeight;   // 权重的单位，一般为1
    private final long maxWeight;       // 最大权重，也可以简单认为是最大连接数，也就是maxPoolSize
    private long weight;                // 当前权重

    private boolean checkInProgress;
    private boolean closed;
    private final Handler<Void> poolClosed;

    public Pool(Context context,
                ConnectionProvider<C> connector,
                int queueMaxSize,
                long initialWeight,
                long maxWeight,
                Handler<Void> poolClosed,
                Consumer<C> connectionAdded,
                Consumer<C> connectionRemoved,
                boolean fifo) {
        this.context = context;
        this.connector = connector;
        this.connectionAdded = connectionAdded;
        this.connectionRemoved = connectionRemoved;
        this.queueMaxSize = queueMaxSize;
        this.fifo = fifo;
        this.initialWeight = initialWeight;
        this.maxWeight = maxWeight;
        this.poolClosed = poolClosed;
        this.available = new ArrayDeque<>();
    }


    private void connectFailed(Holder holder, Throwable cause) {
        Waiter<C> waiter;
        synchronized (this) {
            connecting--;
            waiter = waitersQueue.poll();
            weight -= initialWeight;
            holder.removed = true;
            checkProgress();
        }
        if (waiter != null) {
            waiter.handler.handle(Future.failedFuture(cause));
        }
    }

    private void connectSucceeded(Holder holder, ConnectResult<C> result) {
        List<Waiter<C>> waiters;
        synchronized (this) {
            connecting--;
            weight += initialWeight - result.weight();
            holder.init(result.concurrency(), result.connection(), result.weight());
            waiters = new ArrayList<>();
            while (holder.capacity > 0 && waitersQueue.size() > 0) {
                waiters.add(waitersQueue.poll());
                holder.capacity--;
            }
            if (holder.capacity > 0) {
                available.add(holder);
                capacity += holder.capacity;
            }
            checkProgress();
        }
        connectionAdded.accept(holder.connection);
        for (Waiter<C> waiter : waiters) {
            waiter.handler.handle(Future.succeededFuture(holder.connection));
        }
    }

    private synchronized void evicted(Holder holder) {
        if (holder.removed) {
            return ;
        }
        evictConnection(holder);
        checkProgress();
    }

    private void recycle(Holder holder, long timestamp) {
        if (holder.removed) {
            return ;
        }
        C toClose;
        synchronized (this) {
            if (recycleConnection(holder, timestamp)) {
                toClose = holder.connection;
            } else {
                toClose = null;
            }
        }
        if (toClose != null) {
            connector.close(holder.connection);
        } else {
            synchronized (this) {
                checkProgress();
            }
        }
    }

    private boolean recycleConnection(Holder holder, long timestamp) {
        long newCapacity = holder.capacity + 1;
        if (newCapacity > holder.concurrency) {
            throw new AssertionError("Attempt to recycle a connection more than permitted");
        }
        if (timestamp == 0L && newCapacity == holder.concurrency && capacity >= waitersQueue.size()) {
            if (holder.capacity > 0) {
                capacity -= holder.capacity;
                available.remove(holder);
            }
            holder.expirationTimestamp = -1L;
            holder.capacity = 0;
            return true;
        } else {
            capacity++;
            if (holder.capacity == 0) {
                if (fifo) {
                    available.addLast(holder);
                } else {
                    available.addFirst(holder);
                }
            }
            holder.expirationTimestamp = timestamp;
            holder.capacity++;
            return false;
        }
    }

    private synchronized void setConcurrency(Holder holder, long concurrency) {
        if (holder.removed) {
            assert false : "Cannot recycle removed holder";
            return ;
        }
        if (holder.concurrency < concurrency) {
            long diff = concurrency - holder.concurrency;
            if (holder.capacity == 0) {
                available.add(holder);
            }
            capacity += diff;
            holder.capacity += diff;
            holder.concurrency = concurrency;
            checkProgress();
        } else if (holder.concurrency > concurrency) {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    public synchronized int waitersInQueue() {
        return waitersQueue.size();
    }

    public synchronized long weight() {
        return weight;
    }

    public synchronized long capacity() {
        return capacity;
    }

    /**
     * 为waiter异步获取一个连接
     * @param handler
     * @return
     */
    public synchronized boolean getConnection(Handler<AsyncResult<C>> handler) {
        if (closed) {
            return false;
        }
        Waiter<C> waiter = new Waiter<>(handler);
        waitersQueue.add(waiter);
        checkProgress();
        return true;
    }

    /**
     * 关闭没有用到的存活超过timestamp的连接
     * @param timestamp
     * @return
     */
    public synchronized int closeIdle(long timestamp) {
        List<C> toClose = new ArrayList<>();
        synchronized (this) {
            for (Holder holder: new ArrayList<>(available)) {
                if (holder.capacity == holder.concurrency && holder.expirationTimestamp <= timestamp && !holder.removed) {
                    toClose.add(holder.connection);
                    evictConnection(holder);
                }
            }
        }
        for (C conn : toClose) {
            connector.close(conn);
        }
        return toClose.size();
    }

    private void evictConnection(Holder holder) {
        holder.removed = true;
        connectionRemoved.accept(holder.connection);
        if (holder.capacity > 0) {
            capacity -= holder.capacity;
            available.remove(holder);
            holder.capacity = 0;
        }
        weight -= holder.weight;
    }

    private void checkProgress() {
        if (!checkInProgress && (canProgress() || canClose())) {
            checkInProgress = true;
            context.getEventLoop().execute(this::checkPendingTasks);
        }
    }

    private boolean canProgress() {
        return waitersQueue.size() > 0 && (canAcquireConnection() || needToCreateConnection() || canEvictWaiter());
    }

    /**
     * 连接池容量大于0，说明还有可以用的连接，不用重新创建
     * @return
     */
    private boolean canAcquireConnection() {
        return capacity > 0;
    }

    /**
     * 连接池还没到最大容量，且连接等待队列减去正在建立的连接数量，则可以建立新连接
     * @return
     */
    private boolean needToCreateConnection() {
        return weight < maxWeight && (waitersQueue.size() - connecting) > 0;
    }

    /**
     * 连接等待队列减去正在建立的连接数量后比queueMaxSize还大，则抛弃连接
     * @return
     */
    private boolean canEvictWaiter() {
        return queueMaxSize >= 0 && (waitersQueue.size() - connecting) > queueMaxSize;
    }

    /**
     * 权重数（连接数）为0，且等待队列长度为0，则可以关闭pool了
     * @return
     */
    private boolean canClose() {
        return weight == 0 && waitersQueue.isEmpty();
    }

    private void checkPendingTasks() {
        while (true) {
            Runnable task;
            synchronized (this) {
                task = nextTask();
                if (task == null) {
                    checkInProgress = false;
                    checkClose();
                    break;
                }
            }
            task.run();
        }
    }

    private Runnable nextTask() {
        if (waitersQueue.size() > 0) {
            if (canAcquireConnection()) {           // 从连接池中取一个连接
                Holder conn = available.peek();
                capacity--;
                if (--conn.capacity == 0) {
                    conn.expirationTimestamp = -1L;
                    available.poll();
                }
                Waiter<C> waiter = waitersQueue.poll();
                return () -> waiter.handler.handle(Future.succeededFuture(conn.connection));
            } else if (needToCreateConnection()) {  // 建立一个新连接
                connecting++;
                weight += initialWeight;
                Holder holder = new Holder();
                return holder::connect;
            } else if (canEvictWaiter()) {          // 连接太多，拒绝连接
                Waiter<C> waiter = waitersQueue.removeLast();
                return () -> waiter.handler.handle(Future.failedFuture(new Exception("Connection pool reached max wait queue size of " + queueMaxSize)));
            }
        }
        return null;
    }

    private void checkClose() {
        if (canClose()) {
            closed = true;
            poolClosed.handle(null);
        }
    }


}
