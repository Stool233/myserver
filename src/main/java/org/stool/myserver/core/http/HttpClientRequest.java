package org.stool.myserver.core.http;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.net.Buffer;

public interface HttpClientRequest {

    HttpClientRequest exceptionHandler(Handler<Throwable> handler);

    HttpClientRequest write(Buffer data);

    HttpClientRequest setHeader(HttpHeaders headers);

    HttpClientRequest setWriteQueueMaxSize(int maxSize);

    HttpClientRequest drainHandler(Handler<Void> handler);

    HttpClientRequest handler(Handler<AsyncResult<HttpClientResponse>> handler);


    HttpClientRequest setChunked(boolean chunked);

    boolean isChunked();

    HttpMethod method();

    String getRawMethod();

    HttpClientRequest setRawMethod(String method);


    String absoluteURI();


    String uri();

    String path();

    String query();

    HttpClientRequest setHost(String host);

    String getHost();

    void handleException(Throwable cause);

    void handleResponse(HttpClientResponse response);

    void handleDrained();


    HttpHeaders headers();

    HttpClientRequest putHeader(String name, String value);

    HttpClientRequest putHeader(CharSequence name, CharSequence value);


    HttpClientRequest write(String chunk);


    HttpClientRequest sendHead();

    HttpClientRequest sendHead(Handler<HttpVersion> completionHandler);



    void end(String chunk);


    void end(Buffer chunk);

    void end();

    HttpClientRequest setTimeout(long timeoutMs);


    boolean reset(long code);
    default boolean reset() {
        return reset(0L);
    }

    HttpConnection connection();

    HttpClientRequest connectionHandler(Handler<HttpConnection> handler);

    boolean writeQueueFull();
}
