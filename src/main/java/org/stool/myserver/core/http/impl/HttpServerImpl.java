package org.stool.myserver.core.http.impl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.EntryPointInternal;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpServer;
import org.stool.myserver.core.http.HttpServerRequest;
import org.stool.myserver.core.impl.ContextImpl;

public class HttpServerImpl implements HttpServer {

    private Handler<HttpServerRequest> requestHandler;

    private ContextImpl listenContext;
    private volatile boolean listening;

    private EntryPointInternal entryPoint;
    private ServerBootstrap bootstrap;


    @Override
    public HttpServer requestHandler(Handler<HttpServerRequest> handler) {
        return null;
    }

    @Override
    public Handler<HttpServerRequest> requestHandler() {
        return null;
    }

    @Override
    public HttpServer exceptionHandler(Handler<Throwable> handler) {
        return null;
    }

    @Override
    public HttpServer listen() {
        return null;
    }

    @Override
    public HttpServer listen(int port) {
        bootstrap = new ServerBootstrap();
        bootstrap.group(entryPoint.getAcceptorEventLoopGroup(), entryPoint.getIOWorkerEventLoopGroup());

        bootstrap.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

            }
        });

        return this;
    }

    private void configureHttp(ChannelPipeline pipeline) {
        pipeline.addLast("httpDecoder", new HttpRequestDecoder());
        pipeline.addLast("httpEncoder", new HttpRequestEncoder());

    }

    @Override
    public HttpServer listen(String host, int port) {
        return null;
    }

    @Override
    public HttpServer listen(Handler<AsyncResult<HttpServer>> listenHandler) {
        return null;
    }

    @Override
    public HttpServer listen(int port, Handler<AsyncResult<HttpServer>> listenHandler) {
        return null;
    }

    @Override
    public HttpServer listen(String host, int port, Handler<AsyncResult<HttpServer>> listenHandler) {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void close(Handler<AsyncResult<Void>> completionHandler) {

    }

    @Override
    public void init(EntryPoint entryPoint) {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
