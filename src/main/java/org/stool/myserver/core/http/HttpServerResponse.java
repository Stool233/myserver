package org.stool.myserver.core.http;

import org.stool.myserver.core.Handler;
import org.stool.myserver.core.net.Buffer;

public interface HttpServerResponse {

    HttpServerResponse exceptionHandler(Handler<Throwable> handler);

    HttpServerResponse write(Buffer data);

    HttpServerResponse setWriteQueueMaxSize(int maxSize);

    HttpServerResponse drainHandler(Handler<Void> handler);

    void end(String chunk);

    void end(Buffer chunk);

    void end();
}
