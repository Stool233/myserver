package org.stool.myserver.core.net.impl;

import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stool.myserver.core.*;
import org.stool.myserver.core.impl.ContextImpl;
import org.stool.myserver.core.net.SocketAddress;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;

/**
 * 保证能够线程安全地使用netty底层api，当只有一个eventLoop使用它时，其能借助偏向锁保证最佳性能
 */
public abstract class BaseConnection {

    private static final Logger log = LoggerFactory.getLogger(BaseConnection.class);
    private static final int MAX_REGION_SIZE = 1024 * 1024;

    protected final EntryPoint entryPoint;
    protected final ChannelHandlerContext chctx;
    protected final Context context;

    private final VoidChannelPromise voidPromise;

    private boolean read;
    private boolean needsFlush;
    private int writeInProgress;

    private Handler<Void> closeHandler;
    private Handler<Throwable> exceptionHandler;

    public BaseConnection(EntryPoint entryPoint, ChannelHandlerContext chctx, Context context) {
        this.entryPoint = entryPoint;
        this.chctx = chctx;
        this.context = context;

        this.voidPromise = new VoidChannelPromise(chctx.channel(), false);
    }


    public MyNettyHandler handler() {
        return (MyNettyHandler) chctx.handler();
    }

    public synchronized final void startRead() {
        checkContext();
        read = true;
    }

    private void write(Object msg, ChannelPromise promise) {
        // 如果还在读请求，或者还有没有写的content，则不需要flush
        if (read || writeInProgress > 0) {
            needsFlush = true;
            chctx.write(msg, promise);
        } else {
            needsFlush = false;
            chctx.writeAndFlush(msg, promise);
        }
    }

    public synchronized void writeToChannel(Object msg, ChannelPromise promise) {
        if (chctx.executor().inEventLoop() && writeInProgress == 0) {
            write(msg, promise);
        } else {
            queueForWrite(msg, promise);
        }
    }

    private void queueForWrite(Object msg, ChannelPromise promise) {
        writeInProgress++;
        context.runOnContext(v -> {
            synchronized (BaseConnection.this) {
                writeInProgress--;
                write(msg, promise);
            }
        });
    }

    public void writeToChannel(Object obj) {
        writeToChannel(obj, voidPromise);
    }

    public boolean isNotWritable() {
        return !chctx.channel().isWritable();
    }



    protected synchronized final void endReadAndFlush() {
        if (read) {
            read = false;
            if (needsFlush && writeInProgress == 0) {
                needsFlush = false;
                chctx.flush();
            }
        }
    }



    protected void checkContext() {
        if (context != entryPoint.getContext()) {
            throw new IllegalStateException("Wrong context!");
        }
    }

    public Channel channel() {
        return chctx.channel();
    }

    public Context getContext() {
        return context;
    }

    protected abstract void handleInterestedOpsChanged();

    public ChannelPromise channelFuture() {
        return chctx.newPromise();
    }


    public void close() {
        endReadAndFlush();
        chctx.channel().close();
    }

    public synchronized BaseConnection closeHandler(Handler<Void> handler) {
        closeHandler = handler;
        return this;
    }

    public void doPause() {
        chctx.channel().config().setAutoRead(false);
    }

    public void doResume() {
        chctx.channel().config().setAutoRead(true);
    }

    protected void handleClosed() {
        Handler<Void> handler;
        synchronized (this) {
            handler = closeHandler;
        }
        if (handler != null) {
            handler.handle(null);
        }
    }

    public void handleRead(Object msg) {
        synchronized (this) {
            read = true;
        }
        handleMessage(msg);
    }

    public abstract void handleMessage(Object msg);

    protected void addFuture(final Handler<AsyncResult<Void>> completionHandler, final ChannelFuture future) {
        if (future != null) {
            future.addListener(channelFuture -> context.executeFromIO(() -> {
                if (completionHandler != null) {
                    if (channelFuture.isSuccess()) {
                        completionHandler.handle(Future.succeededFuture());
                    } else {
                        completionHandler.handle(Future.failedFuture(channelFuture.cause()));
                    }
                } else if (!channelFuture.isSuccess()) {
                    handleException(channelFuture.cause());
                }
            }));
        }
    }

    public synchronized BaseConnection exceptionHandler(Handler<Throwable> handler) {
        this.exceptionHandler = handler;
        return this;
    }

    protected void handleException(Throwable t) {
        if (exceptionHandler != null) {
            exceptionHandler.handle(t);
        } else {
            if (log.isDebugEnabled()) {
                log.error(t.getMessage(), t);
            } else {
                log.error(t.getMessage());
            }
        }
    }

    public SocketAddress remoteAddress() {
        InetSocketAddress addr = (InetSocketAddress) chctx.channel().remoteAddress();
        if (addr == null) {
            return null;
        }
        return new SocketAddressImpl(addr);
    }


    public void doSetWriteQueueMaxSize(int size) {
        ChannelConfig config = chctx.channel().config();
        config.setWriteBufferWaterMark(new WriteBufferWaterMark(size / 2, size));
    }

    public void fail(Throwable error) {
        handler().fail(error);
    }

    public ChannelOutboundInvoker channelHandlerContext() {
        return chctx;
    }

    public final ChannelFuture sendFile(RandomAccessFile raf, long offset, long length) throws IOException {
        ChannelPromise writeFuture = chctx.newPromise();
        sendFileRegion(raf, offset, length, writeFuture);
        if (writeFuture != null) {
            writeFuture.addListener(future -> raf.close());
        } else {
            raf.close();
        }
        return writeFuture;
    }

    private void sendFileRegion(RandomAccessFile file, long offset, long length, ChannelPromise writeFuture) {
        if (length <MAX_REGION_SIZE) {
            writeToChannel(new DefaultFileRegion(file.getChannel(), offset, length), writeFuture);
        } else {
            ChannelPromise promise = chctx.newPromise();
            FileRegion region = new DefaultFileRegion(file.getChannel(), offset, MAX_REGION_SIZE);

            region.retain();
            writeToChannel(region, promise);
            promise.addListener(future -> {
                if (future.isSuccess()) {
                    sendFileRegion(file, offset + MAX_REGION_SIZE, length - MAX_REGION_SIZE, writeFuture);
                } else {
                    future.cause().printStackTrace();
                    writeFuture.setFailure(future.cause());
                }
            });
        }
    }

}
