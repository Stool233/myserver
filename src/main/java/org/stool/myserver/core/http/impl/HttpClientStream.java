package org.stool.myserver.core.http.impl;

import io.netty.buffer.ByteBuf;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.http.HttpConnection;
import org.stool.myserver.core.net.NetSocket;

public interface HttpClientStream {

    int id();

    HttpConnection connection();
    Context getContext();

    void writeBuffer(ByteBuf buf, boolean end);

    void doSetWriteQueueMaxSize(int size);
    boolean isNotWritable();
    void doPause();
    void doResume();
    void doFetch(long amount);

    NetSocket createNetSocket();
}
