package org.stool.myserver.core.net;

import io.netty.channel.ChannelHandlerContext;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Handler;

public interface NetSocket {

    NetSocket exceptionHandler(Handler<Throwable> handler);

    NetSocket handler(Handler<Buffer> handler);

    NetSocket pause();

    NetSocket resume();

    NetSocket fetch(long amount);

    NetSocket endHandler(Handler<Void> endHandler);

    NetSocket write(Buffer data);

    NetSocket setWriteQueueMaxSize(int maxSize);

    NetSocket drainHandler(Handler<Void> handler);


    boolean writeQueueFull();

    ChannelHandlerContext channelHandlerContext();

    /**
     * 在channel的pipeline中写消息
     * @param message
     * @return
     */
    NetSocket writeMessage(Object message);

    NetSocket writeMessage(Object message, Handler<AsyncResult<Void>> handler);

    /**
     * 处理message的handler，其处理的消息可以是ByteBuf，也可以是其他对象，从channel的pipeline中得到
     * @param handler
     * @return
     */
    NetSocket messageHandler(Handler<Object> handler);

    NetSocket write(String str);

    NetSocket write(Buffer message, Handler<AsyncResult<Void>> handler);

    NetSocket write(String str, String enc);

    void end();
}
