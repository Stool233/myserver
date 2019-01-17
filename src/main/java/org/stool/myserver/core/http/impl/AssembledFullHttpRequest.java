package org.stool.myserver.core.http.impl;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;

class AssembledFullHttpRequest extends AssembledHttpRequest implements FullHttpRequest {

    public AssembledFullHttpRequest(HttpRequest request, LastHttpContent content) {
        super(request, content);
    }

    public AssembledFullHttpRequest(HttpRequest request) {
        super(request, LastHttpContent.EMPTY_LAST_CONTENT);
    }

    public AssembledFullHttpRequest(HttpRequest request, ByteBuf buf) {
        super(request, toLastContent(buf));
    }

    private static LastHttpContent toLastContent(ByteBuf buf) {
        if (buf.isReadable()) {
            return new DefaultLastHttpContent(buf, false);
        } else {
            return LastHttpContent.EMPTY_LAST_CONTENT;
        }
    }

    @Override
    public AssembledFullHttpRequest replace(ByteBuf content) {
        super.replace(content);
        return this;
    }

    @Override
    public AssembledFullHttpRequest retainedDuplicate() {
        super.retainedDuplicate();
        return this;
    }

    @Override
    public AssembledFullHttpRequest setUri(String uri) {
        super.setUri(uri);
        return this;
    }

    @Override
    public AssembledFullHttpRequest setProtocolVersion(HttpVersion version) {
        super.setProtocolVersion(version);
        return this;
    }

    @Override
    public AssembledFullHttpRequest setMethod(HttpMethod method) {
        super.setMethod(method);
        return this;
    }

    @Override
    public AssembledFullHttpRequest duplicate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AssembledFullHttpRequest copy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders trailingHeaders() {
        return ((LastHttpContent) content).trailingHeaders();
    }

    @Override
    public AssembledFullHttpRequest retain() {
        super.retain();
        return this;
    }

    @Override
    public AssembledFullHttpRequest retain(int increment) {
        super.retain(increment);
        return this;
    }

    @Override
    public AssembledFullHttpRequest touch(Object hint) {
        super.touch(hint);
        return this;
    }

    @Override
    public AssembledFullHttpRequest touch() {
        super.touch();
        return this;
    }
}
