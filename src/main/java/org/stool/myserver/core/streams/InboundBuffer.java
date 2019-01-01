package org.stool.myserver.core.streams;

import org.stool.myserver.core.Context;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.impl.EntryPointImpl;

import java.util.ArrayDeque;

/**
 * 缓冲区，使用背压将元素传递给处理程序。
 * 生产者需要跟缓冲区配合使其不会过载
 *
 * write返回的布尔值表示它是否可以继续安全地添加更多元素。
 * 生产者可以设置drainHandler，以便在它可以在再次恢复写入时发出信号
 *
 * 当write返回false时，当缓冲区再次可写时，将调用drainHandler，消费者需要设置handler来消费元素
 *
 * Buffer mode:
 *   flowing: 流动，元素传递给处理程序
 *   fetch: 提取，只有所请求的元素数量将被传递给处理程序
 *
 * resume() -> flowing mode
 * pause() -> fetch mode; demand = 0
 * fetch(long) -> 请求特定数量的元素
 * @param <E>
 */
public class InboundBuffer<E> {
    private final Context context;
    private final ArrayDeque<E> pending;    // 等待写到对端的元素列表
    private final long highWaterMark;       // 等待列表的最高水位
    private long demand;                    // 目前所需要/可以流动的元素个数
    private Handler<E> handler;             // 消费者的handler
    private Handler<Void> drainHandler;     // 生产者的排水handler
    private Handler<Void> emptyHandler;
    private Handler<Throwable> exceptionHandler;
    private boolean emitting;               // 是否在传递数据
    private boolean overflow;               // 是否溢出等待队列了


    public InboundBuffer(Context context) {
        this(context, 16L);
    }

    public InboundBuffer(Context context, long highWaterMark) {
        this.context = context;
        this.highWaterMark = highWaterMark;
        this.demand = Long.MAX_VALUE;
        this.pending = new ArrayDeque<>();
    }

    private void checkContext() {
        if (context != EntryPointImpl.context()) {
            throw new IllegalStateException("This operation must be called from the context thread");
        }
    }

    public boolean write(E element) {
        checkContext();
        Handler<E> handler;
        synchronized (this) {
            // 若当前正在传递数据或者可以流动的元素个数为0时，元素element加入等待队列。
            if (emitting || demand == 0L) {
                pending.add(element);
                boolean writable = pending.size() <= highWaterMark;
                overflow |= !writable;
                return writable;    // 当等待队列还没有超过水位时，生产者还可以继续写
            } else {
                // 否则，开始写，demand减1，设置emitting，执行消费者的handler
                if (demand != Long.MAX_VALUE) {
                    --demand;
                }
                emitting = true;
                handler = this.handler;
            }
        }
        handleEvent(handler, element);  //执行消费者的handler
        return emitPending();
    }

    /**
     * 尝试发送等待队列中的元素
     * @return 告诉生产者还能不能继续写
     */
    private boolean emitPending() {
        E element;
        Handler<E> handler;
        while(true) {
            synchronized (this) {
                int size = pending.size();
                if (size == 0) {
                    // 如果等待队列为空，说明目前可以继续写了，可以通知生产者这件事，
                    // 可能之前处于overflow状态，所以需要回调它的注册的方法，
                    // 并取消emitting状态
                    checkCallDrainHandler();
                    emitting = false;
                    return true;
                } else if (demand == 0L) {
                    // 如果允许流动的元素个数等于0时，则取消emitting的状态，
                    // 判断一下等待队列的状态，然后告诉生产者可不可以继续写。
                    emitting = false;
                    boolean writable = pending.size() <= highWaterMark;
                    overflow |= !writable;
                    return writable;
                }
                if (demand != Long.MAX_VALUE) {
                    demand--;
                }
                // 从等待队列中取元素，执行消费者handler。
                element = pending.poll();
                handler = this.handler;
            }
            handleEvent(handler, element);
        }
    }

    /**
     * 检查当前状态是否是溢出了，
     * 如果是，取消溢出状态，
     * 并执行生产者的drainHandler，向生产者发出信号，告诉其目前可写了。
     */
    private void checkCallDrainHandler() {
        if (overflow) {
            overflow = false;
            context.runOnContext(v -> {
                Handler<Void> drainHandler;
                synchronized (InboundBuffer.this) {
                    drainHandler = this.drainHandler;
                }
                handleEvent(drainHandler, null);
            });
        }
    }

    private <T> void handleEvent(Handler<T> handler, T element) {
        if (handler != null) {
            handler.handle(element);
        }
    }

    /**
     * 请求特定数量的元素
     * @param amount
     * @return
     */
    public InboundBuffer<E> fetch(long amount) {
        synchronized (this) {
            demand += amount;
            // demand最大为Long.MAX_VALUE
            if (demand < 0L) {
                demand = Long.MAX_VALUE;
            }
            // 如果目前正在传递数据，或者没有元素，则直接返回
            if (emitting || pending.isEmpty()) {
                return this;
            }
            // 设置传递状态
            emitting = true;
        }
        // 执行排水任务，将等待写队列中的元素取出来
        context.runOnContext(v -> drain());
        return this;
    }

    /**
     * 从等待队列中将元素取出来，执行消费者handler，与emitPending类似，不过该方法还会执行emptyHandler
     * 执行该方法时要求demand > 0L && !pending.isEmpty() 为真
     */
    private void drain() {
        Handler<Void> emptyHandler = null;
        while (true) {
            E element;
            Handler<E> handler;
            synchronized (this) {
                int size = pending.size();
                if (size == 0) {
                    emitting = false;
                    checkCallDrainHandler();
                    emptyHandler = this.emptyHandler;
                    break;
                } else if (demand == 0L) {
                    emitting = false;
                    return ;
                }
                if (demand != Long.MAX_VALUE) {
                    demand--;
                }
                element = pending.poll();
                handler = this.handler;
            }
            handleEvent(handler, element);
        }
        handleEvent(emptyHandler, null);
    }

    public E read() {
        synchronized (this) {
            return pending.poll();
        }
    }

    public synchronized InboundBuffer<E> clear() {
        pending.clear();
        return this;
    }

    /**
     * 暂停缓冲区的工作，即设置当前允许流动的元素个数为0即可。
     * @return
     */
    public synchronized InboundBuffer<E> pause() {
        demand = 0L;
        return this;
    }

    /**
     * 恢复缓冲区的任务，即请求最大数量的元素。
     * @return
     */
    public InboundBuffer<E> resume() {
        return fetch(Long.MAX_VALUE);
    }

    /**
     * 设置消费者handler
     * @param handler
     * @return
     */
    public synchronized InboundBuffer<E> handler(Handler<E> handler) {
        this.handler = handler;
        return this;
    }

    public synchronized InboundBuffer<E> drainHandler(Handler<Void> handler) {
        drainHandler = handler;
        return this;
    }

    public synchronized InboundBuffer<E> emptyHandler(Handler<Void> handler) {
        emptyHandler = handler;
        return this;
    }

    public synchronized InboundBuffer<E> exceptionHandler(Handler<Throwable> handler) {
        exceptionHandler = handler;
        return this;
    }

    public synchronized boolean isEmpty() {
        return pending.isEmpty();
    }

    public synchronized boolean isWritable() {
        return pending.size() <= highWaterMark;
    }

    public synchronized boolean isPaused() {
        return demand == 0L;
    }

    public synchronized int size() {
        return pending.size();
    }
}
