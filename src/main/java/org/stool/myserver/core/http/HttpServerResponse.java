package org.stool.myserver.core.http;

import io.netty.handler.codec.http.HttpHeaders;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.impl.HttpServerResponseImpl;
import org.stool.myserver.core.net.Buffer;
import org.stool.myserver.core.net.NetSocket;

import java.util.Map;

public interface HttpServerResponse {

    HttpHeaders headers();

    Map<String, String> trailers();

    int getStatusCode();

    HttpServerResponse setStatusCode(int statusCode);

    String getStatusMessage();

    HttpServerResponse setStatusMessage(String statusMessage);

    HttpServerResponse putHeader(String key, String value);

    boolean writeQueueFull();

    HttpServerResponse closeHandler(Handler<Void> handler);

    HttpServerResponse endHandler(Handler<Void> handler);

    HttpServerResponse exceptionHandler(Handler<Throwable> handler);

    HttpServerResponse write(Buffer data);

    HttpServerResponse write(String data);

    HttpServerResponse setWriteQueueMaxSize(int maxSize);

    HttpServerResponse drainHandler(Handler<Void> handler);

    void end(String chunk);

    void end(Buffer chunk);

    void end();

    NetSocket netSocket(boolean connected);

    void close();

    boolean ended();

    boolean closed();

    boolean headWritten();

    long bytesWritten();

    HttpServerResponse headersEndHandler(Handler<Void> handler);

    HttpServerResponse bodyEndHandler(Handler<Void> handler);

    void handleDrained();

    HttpServerResponse sendFile(String filename, long offset, long length);

    HttpServerResponse sendFile(String filename, long start, long end, Handler<AsyncResult<Void>> resultHandler);

    default HttpServerResponse sendFile(String filename) {
        return sendFile(filename, 0);
    }

    default HttpServerResponse sendFile(String filename, long offset) {
        return sendFile(filename, offset, Long.MAX_VALUE);
    }

}
