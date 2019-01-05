package org.stool.myserver.core;

import io.netty.channel.EventLoopGroup;
import org.stool.myserver.core.http.HttpServer;
import org.stool.myserver.core.impl.EntryPointImpl;

import java.util.concurrent.ExecutorService;

/**
 * 调用核心API的入口点
 * 可以创建执行上下文
 * 可以创建服务器实例
 */
public interface EntryPoint {

    Context getOrCreateContext();

    Context currentContext();

    HttpServer createHttpServer();

    static EntryPoint entryPoint() {
        return new EntryPointImpl();
    }

    EventLoopGroup getIOWorkerEventLoopGroup();

    EventLoopGroup getAcceptorEventLoopGroup();

    ExecutorService getWorkerPool();


    Context getContext();

    void runOnContext(Handler<Void> action);
}
