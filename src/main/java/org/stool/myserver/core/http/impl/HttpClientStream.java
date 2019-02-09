package org.stool.myserver.core.http.impl;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.http.HttpClientRequest;
import org.stool.myserver.core.http.HttpConnection;
import org.stool.myserver.core.http.HttpMethod;
import org.stool.myserver.core.net.NetSocket;

public interface HttpClientStream {

    int id();

    HttpConnection connection();
    Context getContext();

    void writeHead(HttpMethod method, String uri, HttpHeaders headers, String hostHeader, boolean chunked, ByteBuf buf, boolean end);

    void writeBuffer(ByteBuf buf, boolean end);

    void doSetWriteQueueMaxSize(int size);
    boolean isNotWritable();
    void doPause();
    void doResume();
    void doFetch(long amount);

    void reset(long code);
    void beginRequest(HttpClientRequest req);
    void endRequest();

    NetSocket createNetSocket();

}
