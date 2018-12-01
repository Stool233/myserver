package org.stool.myserver.core;

import org.stool.myserver.core.http.HttpServer;

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
        return null;
    }
}
