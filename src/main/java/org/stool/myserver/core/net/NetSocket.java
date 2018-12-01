package org.stool.myserver.core.net;

import org.stool.myserver.core.Handler;

public interface NetSocket {

    NetSocket exceptionHandler(Handler<Throwable> handler);

    NetSocket handler(Handler<Buffer> handler);

    NetSocket pause();

    NetSocket resume();

    NetSocket endHandler(Handler<Void> endHandler);

    NetSocket write(Buffer data);

    NetSocket setWriteQueueMaxSize(int maxSize);

    NetSocket drainHandler(Handler<Void> handler);

}
