package org.stool.myserver.core.net;

import io.netty.buffer.ByteBuf;
import org.stool.myserver.core.net.impl.BufferImpl;

public interface Buffer {


    static Buffer buffer(ByteBuf byteBuf) {
        return new BufferImpl(byteBuf);
    }

    static Buffer buffer() {
        return new BufferImpl();
    }

    static Buffer buffer(String chunk) {
        return new BufferImpl(chunk);
    }

    ByteBuf getByteBuf();

    Buffer appendBuffer(Buffer buffer);

    int length();
}
