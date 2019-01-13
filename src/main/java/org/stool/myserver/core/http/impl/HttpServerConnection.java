package org.stool.myserver.core.http.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpConnection;
import org.stool.myserver.core.http.HttpServerRequest;
import org.stool.myserver.core.net.Buffer;
import org.stool.myserver.core.net.NetSocket;
import org.stool.myserver.core.net.SocketAddress;
import org.stool.myserver.core.net.impl.MyNettyHandler;
import org.stool.myserver.core.net.impl.NetSocketImpl;

import java.util.HashMap;
import java.util.Map;

public class HttpServerConnection extends HttpBaseConnection  implements HttpConnection {

    private static final Logger log = LoggerFactory.getLogger(HttpServerConnection.class);
    public Handler<HttpServerRequest> requestHandler;

    private HttpServerRequestImpl requestInProgress;    // 准备接收的请求
    private HttpServerRequestImpl responseInProgress;   // 准备回复的请求
    private boolean channelPaused;

    public HttpServerConnection(EntryPoint entryPoint,
                                ChannelHandlerContext channel,
                                Context context,
                                HttpHandlers handlers) {
        super(entryPoint, channel, context);
        this.requestHandler = requestHandler(handlers);
        exceptionHandler(handlers.exceptionHandler);
    }

    private static Handler<HttpServerRequest> requestHandler(HttpHandlers handlers) {
        if (handlers.connectionHandler != null) {
            class Adapter implements Handler<HttpServerRequest> {
                private boolean isFirst = true;

                @Override
                public void handle(HttpServerRequest request) {
                    if (isFirst) {
                        isFirst = false;
                        handlers.connectionHandler.handle(request.connection());
                    }
                    handlers.requestHandler.handle(request);
                }
            }
            return new Adapter();
        } else {
            return handlers.requestHandler;
        }
    }



    @Override
    public void handleInterestedOpsChanged() {
        if (!isNotWritable()) {
            if (responseInProgress != null) {
                responseInProgress.response().handleDrained();
            }
        }
    }

    @Override
    public synchronized void handleMessage(Object msg) {
        if (msg instanceof HttpRequest) {                           // 读取到请求头
            DefaultHttpRequest request = (DefaultHttpRequest) msg;
            if (request.decoderResult() != DecoderResult.SUCCESS) {
                // todo
                return ;
            }
            HttpServerRequestImpl req = new HttpServerRequestImpl(this, request);
            requestInProgress = req;
            if (responseInProgress == null) {
                responseInProgress = requestInProgress;
                req.handleBegin();
            } else {
                // 暂停，直到当前的响应结束
                req.pause();
                responseInProgress.appendRequest(req);
            }
        } else if (msg == LastHttpContent.EMPTY_LAST_CONTENT) {     // 读取到请求终止行
            handleEnd();
        } else if (msg instanceof HttpContent) {                    // 读取到请求体
            handleContent(msg);
        } else {
            handleOther(msg);
        }
    }

    private void handleOther(Object msg) {
    }

    private void handleContent(Object msg) {
        HttpContent content = (HttpContent) msg;
        if (content.decoderResult() != DecoderResult.SUCCESS) {
            // todo
            return ;
        }
        Buffer buffer = Buffer.buffer(MyNettyHandler.safeBuffer(content.content(), chctx.alloc()));
        requestInProgress.handleContent(buffer);
        if (content instanceof LastHttpContent) {
            handleEnd();
        }
    }

    private void handleEnd() {
        HttpServerRequestImpl request = requestInProgress;
        requestInProgress = null;
        request.handleEnd();
    }

    public synchronized void responseComplete() {
        // 使用局部变量保存当前响应，并将当前响应清空，使当前线程能处理其他请求
        HttpServerRequestImpl request = responseInProgress;
        responseInProgress = null;
        // 若响应链表还有下一个请求节点，则继续处理
        HttpServerRequestImpl next = request.nextRequest();
        if (next != null) {
            handleNext(next);
        }
    }

    private void handleNext(HttpServerRequestImpl next) {
        // 开启异步任务继续处理响应链表接下来的请求节点
        responseInProgress = next;
        getContext().runOnContext(v -> responseInProgress.handlePipelined());
    }

    @Override
    public void doPause() {
        if (!channelPaused) {
            channelPaused = true;
            super.doPause();
        }
    }

    @Override
    public void doResume() {
        if (channelPaused) {
            channelPaused = false;
            super.doResume();
        }
    }

    public EntryPoint entryPoint() {
        return entryPoint;
    }


    @Override
    public SocketAddress localAddress() {
        return null;
    }

    @Override
    public int getWindowSize() {
        return 0;
    }

    public NetSocket createNetSocket() {
        Map<Channel, NetSocketImpl> connectionMap = new HashMap<>(1);

        NetSocketImpl socket = new NetSocketImpl(entryPoint, chctx, context){
            @Override
            protected void handleClosed() {
                connectionMap.remove(chctx.channel());
                super.handleClosed();
            }

            @Override
            public synchronized void handleMessage(Object msg) {
                if (msg instanceof HttpContent) {
                    ReferenceCountUtil.release(msg);
                }
                super.handleMessage(msg);
            }
        };
        connectionMap.put(chctx.channel(), socket);

        endReadAndFlush();

        ChannelPipeline pipeline = chctx.pipeline();

        pipeline.remove("httpDecoder");

        chctx.pipeline().replace("handler", "handler", MyNettyHandler.create(socket));

        chctx.pipeline().remove("httpEncoder");

        return socket;
    }





}
