package org.stool.myserver.core.http;

import org.stool.myserver.core.Server;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.Handler;

public interface HttpServer extends Server{

    //-----------工厂方法 start---------------

    static HttpServer server() {
        EntryPoint entryPoint = EntryPoint.entryPoint();
        return entryPoint.createHttpServer();
    }

    //-----------工厂方法 end---------------

    HttpServer requestHandler(Handler<HttpServerRequest> handler);

    Handler<HttpServerRequest> requestHandler();

    HttpServer connectionHandler(Handler<HttpConnection> handler);

    HttpServer exceptionHandler(Handler<Throwable> handler);

    /**
     * 监听端口
     * @return
     */
    HttpServer listen();

    HttpServer listen(int port);

    HttpServer listen(String host, int port);

    HttpServer listen(Handler<AsyncResult<HttpServer>> listenHandler);

    HttpServer listen(int port, Handler<AsyncResult<HttpServer>> listenHandler);

    HttpServer listen(String host, int port, Handler<AsyncResult<HttpServer>> listenHandler);

    /**
     * 关闭HttpServer
     */
    void close();

    void close(Handler<AsyncResult<Void>> completionHandler);


    int actualPort();

    HttpServer elastic(int serverPortSize);
}
