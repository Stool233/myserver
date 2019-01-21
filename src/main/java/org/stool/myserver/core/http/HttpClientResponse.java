package org.stool.myserver.core.http;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.LastHttpContent;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.net.Buffer;

public interface HttpClientResponse {

    HttpClientResponse fetch(long amount);

    HttpClientResponse resume();

    HttpClientResponse exceptionHandler(Handler<Throwable> handler);

    HttpClientResponse handler(Handler<Buffer> handler);

    HttpClientResponse pause();

    HttpClientResponse endHandler(Handler<Void> endHandler);

    int statusCode();

    String statusMessage();

    String getHeader(String headerName);

    String getHeader(CharSequence headerName);

    HttpHeaders headers();

    HttpClientResponse bodyHandler(Handler<Buffer> bodyHandler);

    HttpClientRequest request();

    void handleException(Throwable cause);

    void handleChunk(Buffer data);

    void handleEnd(LastHttpContent trailer);
}
