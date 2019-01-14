package org.stool.myserver.core.http.impl.pool;

import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.Future;
import org.stool.myserver.core.Handler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

public class Pool<C> {

    public class Holder implements ConnectionListener<C> {

        boolean removed;
        C connection;
        long concurrency;
        long capacity;
        long weight;
        long expirationTimestamp;

        private void init(C connection, long concurrency, long weight) {
            this.connection = connection;
            this.concurrency = concurrency;
            this.weight = weight;
            this.expirationTimestamp = -1L;
        }

        @Override
        public void onConcurrencyChange(long concurrency) {

        }

        @Override
        public void onRecycle(long expirationTimestamp) {

        }

        @Override
        public void onEvict() {

        }

        void connect() {

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
    private final Deque<Waiter<C>> waitersQueue = new ArrayDeque<>();

    private final Deque<Holder> available;
    private final boolean fifo;
    private long capacity;
    private long connecting;

    private final long initialWeight;
    private final long maxWeight;
    private long weight;

    private boolean checkInProgress;
    private boolean closed;
    private final Handler<Void> poolClosed;

    public Pool(Context context,
                ConnectionProvider<C> connector,
                Consumer<C> connectionAdded,
                Consumer<C> connectionRemoved,
                int queueMaxSize,
                boolean fifo,
                long initialWeight,
                long maxWeight,
                Handler<Void> poolClosed) {
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

    public synchronized int waitersInQueue() {
        return waitersQueue.size();
    }

    public synchronized long weight() {
        return weight;
    }

    public synchronized long capacity() {
        return capacity;
    }

    public synchronized boolean getConnection(Handler<AsyncResult<C>> handler) {
        if (closed) {
            return false;
        }
        Waiter<C> waiter = new Waiter<>(handler);
        waitersQueue.add(waiter);
        checkProgress();
        return true;
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

    private boolean canAcquireConnection() {
        return capacity > 0;
    }


    private boolean needToCreateConnection() {
        return weight < maxWeight && (waitersQueue.size() - connecting) > 0;
    }

    private boolean canEvictWaiter() {
        return queueMaxSize >= 0 && (waitersQueue.size() - connecting) > queueMaxSize;
    }

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
        }
    }

    private Runnable nextTask() {
        if (waitersQueue.size() > 0) {
            if (canAcquireConnection()) {
                Holder conn = available.peek();
                capacity--;
                if (--conn.capacity == 0) {
                    conn.expirationTimestamp = -1L;
                    available.poll();
                }
                Waiter<C> waiter = waitersQueue.poll();
                return () -> waiter.handler.handle(Future.succeededFuture(conn.connection));
            } else if (needToCreateConnection()) {
                connecting++;
                weight += initialWeight;
                Holder holder = new Holder();
                return holder::connect;
            } else if (canEvictWaiter()) {
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
