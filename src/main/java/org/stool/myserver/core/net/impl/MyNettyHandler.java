package org.stool.myserver.core.net.impl;

import io.netty.buffer.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.impl.ContextImpl;

import java.util.function.Function;

public class MyNettyHandler<C extends BaseConnection> extends ChannelDuplexHandler {

    private static Logger log = LoggerFactory.getLogger(MyNettyHandler.class);

    public static ByteBuf safeBuffer(ByteBufHolder holder, ByteBufAllocator allocator) {
        return safeBuffer(holder.content(), allocator);
    }

    /**
     * 将buf转成heapBuffer
     * @param buf
     * @param allocator
     * @return
     */
    public static ByteBuf safeBuffer(ByteBuf buf, ByteBufAllocator allocator) {
        if (buf == Unpooled.EMPTY_BUFFER) {
            return buf;
        }
        if (buf.isDirect() || buf instanceof CompositeByteBuf) {
            try {
                if (buf.isReadable()) {
                    ByteBuf buffer = allocator.heapBuffer(buf.readableBytes());
                    buffer.writeBytes(buf);
                    return buffer;
                } else {
                    return Unpooled.EMPTY_BUFFER;
                }
            } finally {
                buf.release();
            }
        }
        return buf;
    }

    private static final Handler<Object> NULL_HANDLER = m -> {};


    public static <C extends BaseConnection> MyNettyHandler<C> create(C connection) {
        return create(connection.context, ctx -> connection);
    }

    public static <C extends BaseConnection> MyNettyHandler<C> create(Context context, Function<ChannelHandlerContext, C> connectionFactory) {
        return new MyNettyHandler<>(context, connectionFactory);
    }


    private final Function<ChannelHandlerContext, C> connectionFactory;
    private Context context;
    private C conn;
    private Handler<Void> endReadAndFlush;
    private Handler<C> addHandler;
    private Handler<C> removeHandler;
    private Handler<Object> messageHandler;

    public MyNettyHandler(Context context, Function<ChannelHandlerContext, C> connectionFactory) {
        this.connectionFactory = connectionFactory;
        this.context = context;
    }

    public MyNettyHandler<C> addHandler(Handler<C> handler) {
        this.addHandler = handler;
        return this;
    }

    public MyNettyHandler<C> removeHandler(Handler<C> handler) {
        this.removeHandler = handler;
        return this;
    }

    public C getConnection() {
        return conn;
    }

    private void setConnection(C connection) {
        conn = connection;
        endReadAndFlush = v -> conn.endReadAndFlush();
        messageHandler = ((BaseConnection)conn)::handleRead;
        if (addHandler != null) {
            addHandler.handle(connection);
        }
    }

    public void fail(Throwable error) {
        messageHandler = NULL_HANDLER;
        conn.chctx.pipeline().fireExceptionCaught(error);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        setConnection(connectionFactory.apply(ctx));
    }


    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        C conn = getConnection();
        context.executeFromIO(v -> conn.handleInterestedOpsChanged());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("error", cause);
        Channel ch = ctx.channel();
        C connection = getConnection();
        if (connection != null) {
            context.executeFromIO(() -> {
                try {
                    if (ch.isOpen()) {
                        ch.close();
                    }
                } catch (Throwable ignore) {

                }
                connection.handleException(cause);
            });
        } else {
            ch.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (removeHandler != null) {
            removeHandler.handle(conn);
        }
        context.executeFromIO(() -> conn.handleClosed());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        context.executeFromIO(msg, messageHandler);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        context.executeFromIO(endReadAndFlush);
    }


}
