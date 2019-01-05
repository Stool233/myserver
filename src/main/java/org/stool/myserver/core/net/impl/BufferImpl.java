package org.stool.myserver.core.net.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.stool.myserver.core.net.Buffer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class BufferImpl implements Buffer {

    private ByteBuf buffer;

    public BufferImpl() {
        this(0);
    }

    public BufferImpl(int initialSizeHint) {
        buffer = Unpooled.unreleasableBuffer(Unpooled.buffer(initialSizeHint, Integer.MAX_VALUE));
    }

    public BufferImpl(byte[] bytes) {
        buffer = Unpooled.unreleasableBuffer(Unpooled.buffer(bytes.length, Integer.MAX_VALUE)).writeBytes(bytes);
    }


    public BufferImpl(String str, String enc) {
        this(str.getBytes(Charset.forName(Objects.requireNonNull(enc))));
    }

    public BufferImpl(String str, Charset cs) {
        this(str.getBytes(cs));
    }

    public BufferImpl(String str) {
        this(str, StandardCharsets.UTF_8);
    }

    public BufferImpl(ByteBuf buffer) {
        this.buffer = Unpooled.unreleasableBuffer(buffer);
    }



    public String toString() {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    public String toString(String enc) {
        return buffer.toString(Charset.forName(enc));
    }

    public String toString(Charset enc) {
        return buffer.toString(enc);
    }



    @Override
    public ByteBuf getByteBuf() {
        return buffer;
    }

    @Override
    public Buffer appendBuffer(Buffer buff) {
        buffer.writeBytes(buff.getByteBuf());
        return this;
    }

    @Override
    public int length() {
        return buffer.writerIndex();
    }
}
