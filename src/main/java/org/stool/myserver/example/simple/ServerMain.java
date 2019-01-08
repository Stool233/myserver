package org.stool.myserver.example.simple;

import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.http.HttpServer;

public class ServerMain {


    public static void main(String[] args) {
        startServer();
    }

    private static void startServer() {

        HttpServer server = HttpServer.server();

        server.requestHandler(request -> {
            request.response().write("Hello World!");
            request.response().write("Hello World!");
            request.response().write("Hello World!");
            request.response().write("Hello World!");
            request.response().write("Hello World!");
            request.response().write("Hello World!");
            request.response().write("Hello World!");
            request.response().write("Hello World!");
            request.response().write("Hello World!");
            request.response().write("Hello World!");
            request.response().write("Hello World!");
            request.response().write("Hello World!");
            request.response().end("Hello World!");
        }).listen(8080);

        server.start();

    }
}
