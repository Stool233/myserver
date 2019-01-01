package org.stool.myserver.core.net;

import io.netty.buffer.ByteBuf;

public interface Buffer {


    static Buffer buffer(ByteBuf byteBuf) {
        return null;
        // todo
    }

    static Buffer buffer() {
        return null;
        // todo
    }

    static Buffer buffer(String chunk) {
        return null;
        // todo
    }

    ByteBuf getByteBuf();

    Buffer appendBuffer(Buffer buffer);

    int length();
}
