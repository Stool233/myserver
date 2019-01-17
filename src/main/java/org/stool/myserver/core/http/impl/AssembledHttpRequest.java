package org.stool.myserver.core.http.impl;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import io.netty.handler.codec.http.*;

class AssembledHttpRequest implements HttpContent, HttpRequest {
    private final HttpRequest request;
    protected final HttpContent content;

    AssembledHttpRequest(HttpRequest request, ByteBuf buf) {
        this(request, new DefaultHttpContent(buf));
    }

    AssembledHttpRequest(HttpRequest request, HttpContent content) {
        this.request = request;
        this.content = content;
    }

    @Override
    public AssembledHttpRequest copy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AssembledHttpRequest duplicate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpContent retainedDuplicate() {
        throw new UnsupportedMessageTypeException();
    }

    @Override
    public HttpContent replace(ByteBuf content) {
        throw new UnsupportedMessageTypeException();
    }

    @Override
    public AssembledHttpRequest retain() {
        content.retain();
        return this;
    }

    @Override
    public AssembledHttpRequest retain(int increment) {
        content.retain(increment);
        return this;
    }

    @Override
    public AssembledHttpRequest touch(Object hint) {
        content.touch(hint);
        return this;
    }

    @Override
    public AssembledHttpRequest touch() {
        content.touch();
        return this;
    }

    @Override
    public HttpMethod method() {
        return request.method();
    }

    @Override
    public HttpMethod getMethod() {
        return request.method();
    }

    @Override
    public String uri() {
        return request.uri();
    }

    @Override
    public String getUri() {
        return request.uri();
    }

    @Override
    public HttpHeaders headers() {
        return request.headers();
    }

    @Override
    public HttpRequest setMethod(HttpMethod method) {
        return request.setMethod(method);
    }

    @Override
    public HttpVersion protocolVersion() {
        return request.protocolVersion();
    }

    @Override
    public HttpVersion getProtocolVersion() {
        return request.protocolVersion();
    }

    @Override
    public HttpRequest setUri(String uri) {
        return request.setUri(uri);
    }

    @Override
    public HttpRequest setProtocolVersion(HttpVersion version) {
        return request.setProtocolVersion(version);
    }

    @Override
    public DecoderResult decoderResult() {
        return request.decoderResult();
    }

    @Override
    public DecoderResult getDecoderResult() {
        return request.decoderResult();
    }

    @Override
    public void setDecoderResult(DecoderResult result) {
        request.setDecoderResult(result);
    }

    @Override
    public ByteBuf content() {
        return content.content();
    }

    @Override
    public int refCnt() {
        return content.refCnt();
    }

    @Override
    public boolean release() {
        return content.release();
    }

    @Override
    public boolean release(int decrement) {
        return content.release(decrement);
    }
}
