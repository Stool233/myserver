package org.stool.myserver.core.http.impl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stool.myserver.core.*;
import org.stool.myserver.core.http.HttpConnection;
import org.stool.myserver.core.http.HttpServer;
import org.stool.myserver.core.http.HttpServerRequest;
import org.stool.myserver.core.impl.ContextImpl;
import org.stool.myserver.core.net.impl.*;
import org.stool.myserver.core.proxy.ElasticHandler;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpServerImpl implements HttpServer {

    static final Logger log = LoggerFactory.getLogger(HttpServerImpl.class);

    private static final Handler<Throwable> DEFAULT_EXCEPTION_HANDLER = t -> log.trace("Connection failure", t);
    private final Map<Channel, BaseConnection> connectionMap = new ConcurrentHashMap<>();
    private final MyEventLoopGroup availableWorkers = new MyEventLoopGroup();
    private final HandlerManager<HttpHandlers> httpHandlerMgr = new HandlerManager<>(availableWorkers);
    private final HttpStreamHandler<HttpServerRequest> requestStream = new HttpStreamHandler<>();

    private Handler<HttpConnection> connectionHandler;

    private ChannelGroup serverChannelGroup;
    private Context listenContext;
    private volatile boolean listening;

    private EntryPoint entryPoint;
    private ServerBootstrap bootstrap;
    private AsyncResolveConnectHelper bindFuture;
    private int actualPort;


    public HttpServerImpl(EntryPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    @Override
    public HttpServer requestHandler(Handler<HttpServerRequest> handler) {
        requestStream.handler(handler);
        return this;
    }

    @Override
    public Handler<HttpServerRequest> requestHandler() {
        return requestStream.handler;
    }

    @Override
    public synchronized HttpServer connectionHandler(Handler<HttpConnection> handler) {
        if (listening) {
            throw new IllegalStateException();
        }
        connectionHandler = handler;
        return this;
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
        serverChannelGroup = new DefaultChannelGroup("my-accptor-channels", GlobalEventExecutor.INSTANCE);
        listenContext = entryPoint.getOrCreateContext();

        bootstrap = new ServerBootstrap();
        bootstrap.group(entryPoint.getAcceptorEventLoopGroup(), availableWorkers);

        bootstrap.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                handleHttp(ch);
            }
        });

        addHandlers(this, listenContext);
        bindFuture = AsyncResolveConnectHelper.doBind(entryPoint, port, bootstrap);
        bindFuture.addListener(res -> {
            if (res.succeeded()) {
                Channel serverChannel = res.result();
                if (serverChannel.localAddress() instanceof InetSocketAddress) {
                    HttpServerImpl.this.actualPort = ((InetSocketAddress)serverChannel.localAddress()).getPort();
                } else {
                    HttpServerImpl.this.actualPort = port;
                }
                serverChannelGroup.add(serverChannel);
            }
        });
        bindFuture.addListener(future -> {
            if (future.failed()) {
                listening = false;
                log.error(future.cause().getMessage());
            }
        });

        return this;
    }

    private void addHandlers(HttpServerImpl httpServer, Context listenContext) {
        httpServer.httpHandlerMgr.addHandler(
                new HttpHandlers(
                        requestStream.handler(),
                        connectionHandler,
                        DEFAULT_EXCEPTION_HANDLER
                ),
                listenContext
        );
    }

    private void handleHttp(Channel ch) {
        HandlerHolder<HttpHandlers> holder = httpHandlerMgr.chooseHandler(ch.eventLoop());
        if (holder == null) {
            sendServiceUnavailable(ch);
            return ;
        }
        configureHttp(ch.pipeline(), holder);
    }

    private void sendServiceUnavailable(Channel ch) {
        ch.writeAndFlush(
                Unpooled.copiedBuffer("HTTP/1.1 503 Service Unavailable\r\n" +
                        "Content-Length:0\r\n" +
                        "\r\n", StandardCharsets.ISO_8859_1))
                .addListener(ChannelFutureListener.CLOSE);
    }

    private void configureHttp(ChannelPipeline pipeline, HandlerHolder<HttpHandlers> holder) {
        pipeline.addLast("httpDecoder", new HttpRequestDecoder());
        pipeline.addLast("httpEncoder", new HttpResponseEncoder());

        MyNettyHandler<HttpServerConnection> handler = MyNettyHandler.create(holder.context, chctx -> {
            HttpServerConnection conn = new HttpServerConnection(entryPoint, chctx, holder.context, holder.handler);
            return conn;
        });
        handler.addHandler(conn -> {
            connectionMap.put(pipeline.channel(), conn);
        });
        handler.removeHandler(conn -> {
            connectionMap.remove(pipeline.channel());
        });
        pipeline.addLast("handler", handler);

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
        for (BaseConnection conn : connectionMap.values()) {
            conn.close();
        }
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

    private class HttpStreamHandler<C extends HttpServerRequest> {

        private Handler<C> handler;
        private long demand = Long.MAX_VALUE;
        private Handler<Void> endHandler;

        Handler<C> handler() {
            synchronized (HttpServerImpl.this) {
                return handler;
            }
        }

        boolean accept() {
            synchronized (HttpServerImpl.this) {
                boolean accept = demand > 0L;
                if (accept && demand != Long.MAX_VALUE) {
                    demand--;
                }
                return accept;
            }
        }

        Handler<Void> endHandler() {
            synchronized (HttpServerImpl.this) {
                return endHandler;
            }
        }


        public HttpStreamHandler handler(Handler<C> handler) {
            synchronized (HttpServerImpl.this) {
                if (listening) {
                    throw new IllegalStateException("Please set handler before server is listening");
                }
                this.handler = handler;
                return this;
            }
        }

        public HttpStreamHandler pause() {
            synchronized (HttpServerImpl.this) {
                demand = 0L;
                return this;
            }
        }

        public HttpStreamHandler fetch(long amount) {
            if (amount > 0L) {
                demand += amount;
                if (demand < 0L) {
                    demand = Long.MAX_VALUE;
                }
            }
            return this;
        }

        public HttpStreamHandler resume() {
            synchronized (HttpServerImpl.this) {
                demand = Long.MAX_VALUE;
                return this;
            }
        }

        public HttpStreamHandler endHandler(Handler<Void> endHandler) {
            synchronized (HttpServerImpl.this) {
                this.endHandler = endHandler;
                return this;
            }
        }


    }

    @Override
    public int actualPort() {
        return actualPort;
    }

    @Override
    public HttpServer elastic(int serverPortSize) {
        this.requestHandler(ElasticHandler.create(entryPoint, serverPortSize, this.requestHandler()));
        return this;
    }
}
