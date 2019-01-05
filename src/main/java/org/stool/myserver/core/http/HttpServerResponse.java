package org.stool.myserver.core.http;

import io.netty.handler.codec.http.HttpHeaders;
import org.stool.myserver.core.Handler;
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
}
