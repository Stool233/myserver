package org.stool.myserver.example.simple;

import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.http.HttpServer;
import org.stool.myserver.core.proxy.ElasticHandler;

public class ElasticMain {

    public static void main(String[] args) {

        EntryPoint entryPoint = EntryPoint.entryPoint();
        HttpServer server = entryPoint.createHttpServer();

        server.requestHandler(request -> {
            request.response().end("Hello World!");
        }).elastic(4).listen(8080);
    }
}
