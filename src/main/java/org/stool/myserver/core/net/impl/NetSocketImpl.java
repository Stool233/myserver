package org.stool.myserver.core.net.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stool.myserver.core.*;
import org.stool.myserver.core.impl.ContextImpl;
import org.stool.myserver.core.net.Buffer;
import org.stool.myserver.core.net.NetSocket;
import org.stool.myserver.core.net.SocketAddress;
import org.stool.myserver.core.streams.InboundBuffer;

import java.nio.charset.Charset;

public class NetSocketImpl extends BaseConnection implements NetSocket {

    private static final Handler<Object> NULL_MSG_HANDLER = event ->  {
        if (event instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) event;
            byteBuf.release();
        }
    };

    private static final Logger log = LoggerFactory.getLogger(NetSocketImpl.class);

    private final SocketAddress remoteAddress;

    private Handler<Void> endHandler;
    private Handler<Void> drainHandler;
    private InboundBuffer<Object> pending;
    private boolean closed;



    public NetSocketImpl(EntryPoint entryPoint, ChannelHandlerContext chctx, Context context, SocketAddress remoteAddress) {
        super(entryPoint, chctx, context);
        this.remoteAddress = remoteAddress;
        pending = new InboundBuffer<>(context);
        pending.drainHandler(v -> doResume());
        pending.handler(NULL_MSG_HANDLER);
        pending.emptyHandler(v -> checkEnd());
    }

    public NetSocketImpl(EntryPoint entryPoint, ChannelHandlerContext chctx, Context context) {
        this(entryPoint, chctx, context, null);
    }

    @Override
    public NetSocketImpl exceptionHandler(Handler<Throwable> handler) {
        super.exceptionHandler(handler);
        return this;
    }

    public NetSocketImpl closeHandler(Handler<Void> handler) {
        super.closeHandler(handler);
        return this;
    }

    @Override
    public NetSocket endHandler(Handler<Void> endHandler) {
        this.endHandler = endHandler;
        return this;
    }

    @Override
    public NetSocket handler(Handler<Buffer> dataHandler) {
        if (dataHandler != null) {
            messageHandler(new DataMessageHandler(channelHandlerContext().alloc(), dataHandler));
        } else {
            messageHandler(null);
        }
        return this;
    }

    @Override
    public NetSocket messageHandler(Handler<Object> handler) {
        if (handler != null) {
            pending.handler(handler);
        } else {
            pending.handler(NULL_MSG_HANDLER);
        }
        return this;
    }

    @Override
    public NetSocket drainHandler(Handler<Void> handler) {
        this.drainHandler = handler;
        entryPoint.runOnContext(v -> callDrainHandler());
        return this;
    }

    private synchronized void callDrainHandler() {
        if (drainHandler != null) {
            if (!writeQueueFull()) {
                drainHandler.handle(null);
            }
        }
    }

    @Override
    public NetSocket setWriteQueueMaxSize(int maxSize) {
        doSetWriteQueueMaxSize(maxSize);
        return this;
    }



    @Override
    public NetSocket pause() {
        pending.pause();
        return this;
    }

    @Override
    public synchronized NetSocket resume() {
        pending.resume();
        return this;
    }

    @Override
    public NetSocket fetch(long amount) {
        pending.fetch(amount);
        return this;
    }



    @Override
    public NetSocket write(Buffer data) {
        writeMessage(data.getByteBuf());
        return this;
    }

    @Override
    public NetSocket write(String str) {
        writeMessage(Unpooled.copiedBuffer(str, CharsetUtil.UTF_8));
        return this;
    }

    @Override
    public NetSocket write(Buffer message, Handler<AsyncResult<Void>> handler) {
        writeMessage(message.getByteBuf(), handler);
        return this;
    }

    @Override
    public NetSocket write(String str, String enc) {
        if (enc == null) {
            write(str);
        } else {
            writeMessage(Unpooled.copiedBuffer(str, Charset.forName(enc)));
        }
        return this;
    }



    @Override
    public NetSocket writeMessage(Object message) {
        if (closed) {
            throw new IllegalStateException("Socket is closed");
        }
        super.writeToChannel(message);
        return this;
    }

    @Override
    public NetSocket writeMessage(Object message, Handler<AsyncResult<Void>> handler) {
        ChannelPromise promise = chctx.newPromise();
        super.writeToChannel(message, promise);
        promise.addListener((future -> {
            if (future.isSuccess()) {
                handler.handle(Future.succeededFuture());
            } else {
                handler.handle(Future.failedFuture(future.cause()));
            }
        }));
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return isNotWritable();
    }

    @Override
    public void end() {
        close();
    }


    @Override
    public synchronized void close() {
        chctx.write(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        chctx.flush();
    }

    @Override
    protected void handleInterestedOpsChanged() {
        checkContext();
        callDrainHandler();
    }


    @Override
    protected void handleClosed() {
        synchronized (this) {
            if (closed) {
                return ;
            }
            closed = true;
        }
        checkEnd();
        super.handleClosed();
    }

    private void checkEnd() {
        Handler<Void> handler;
        synchronized (this) {
            if (!closed || pending.size() > 0 || (handler = endHandler) == null) {
                return ;
            }
        }
        handler.handle(null);
    }

    public synchronized void handleMessage(Object msg) {
        checkContext();
        if (!pending.write(msg)) {
            doPause();
        }
    }

    @Override
    public ChannelHandlerContext channelHandlerContext() {
        return chctx;
    }

    @Override
    public SocketAddress remoteAddress() {
        return super.remoteAddress();
    }

    private class DataMessageHandler implements Handler<Object> {

        private final Handler<Buffer> dataHandler;
        private final ByteBufAllocator allocator;

        public DataMessageHandler(ByteBufAllocator alloc, Handler<Buffer> dataHandler) {
            this.allocator = alloc;
            this.dataHandler = dataHandler;
        }

        @Override
        public void handle(Object event) {
            if (event instanceof ByteBuf) {
                ByteBuf byteBuf = (ByteBuf) event;
                byteBuf = MyNettyHandler.safeBuffer(byteBuf, allocator);
                Buffer data = Buffer.buffer(byteBuf);
                dataHandler.handle(data);
            }
        }
    }
}
