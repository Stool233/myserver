package org.stool.myserver.core.net.impl;

import io.netty.channel.*;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.EntryPointInternal;
import org.stool.myserver.core.Future;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.impl.ContextImpl;

public abstract class BaseConnection {

    protected final EntryPointInternal entryPoint;
    protected final ChannelHandlerContext chctx;
    protected final ContextImpl context;

    private final VoidChannelPromise voidPromise;

    private boolean read;
    private boolean needsFlush;
    private int writeInProgress;

    public BaseConnection(EntryPointInternal entryPoint, ChannelHandlerContext chctx, ContextImpl context) {
        this.entryPoint = entryPoint;
        this.chctx = chctx;
        this.context = context;

        this.voidPromise = new VoidChannelPromise(chctx.channel(), false);
    }


    protected Object encode(Object obj) {
        return obj;
    }

    public ChannelHandler handler() {
        return chctx.handler();
    }

    public synchronized final void startRead() {
        checkContext();
        read = true;
    }

    private void write(Object msg, ChannelPromise promise) {
        msg = encode(msg);
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

    public ContextImpl getContext() {
        return context;
    }

    protected abstract void handleInterestedOpsChanged();

    public ChannelPromise channelFuture() {
        return chctx.newPromise();
    }




}
