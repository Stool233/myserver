package org.stool.myserver.core.http;

import io.netty.handler.codec.http.HttpHeaders;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.net.Buffer;
import org.stool.myserver.core.net.NetSocket;
import org.stool.myserver.core.net.SocketAddress;

import java.util.Map;

public interface HttpServerRequest {

    HttpServerRequest exceptionHandler(Handler<Throwable> handler);

    HttpServerRequest handler(Handler<Buffer> handler);

    HttpServerRequest pause();

    HttpServerRequest resume();

    HttpServerRequest fetch(long amount);

    HttpServerRequest endHandler(Handler<Void> endHandler);

    NetSocket netSocket();

    HttpMethod method();

    String uri();

    String path();

    String query();

    String host();

    /**
     * @return 请求体的总字节数
     */
    long bytesRead();

    HttpHeaders headers();

    String getHeader(String headerName);

    String getHeader(CharSequence headerName);

    Map<String, String> params();

    String getParam(String paramName);

    Map<String, String> formAttributes();

    String getFormAttribute(String attributeName);

    SocketAddress localAddress();

    SocketAddress remoteAddress();

    String absoluteURI();

    /**
     * 一次性接收所有请求体
     * @param bodyHandler
     * @return
     */
    default HttpServerRequest bodyHandler(Handler<Buffer> bodyHandler) {
        if (bodyHandler != null) {
            Buffer body = Buffer.buffer();
            handler(body::appendBuffer);
            endHandler(v -> bodyHandler.handle(body));
        }
        return this;
    }

    boolean isEnded();

    HttpServerResponse response();

    HttpConnection connection();

    Context context();
}
