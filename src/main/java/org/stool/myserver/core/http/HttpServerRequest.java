package org.stool.myserver.core.http;

import org.stool.myserver.core.Handler;
import org.stool.myserver.core.net.Buffer;
import org.stool.myserver.core.net.NetSocket;

public interface HttpServerRequest {

    HttpServerRequest exceptionHandler(Handler<Throwable> handler);

    HttpServerRequest handler(Handler<Buffer> handler);

    HttpServerRequest pause();

    HttpServerRequest resume();

    HttpServerRequest endHandler(Handler<Void> endHandler);

    NetSocket netSocket();

    HttpServerResponse response();
}
