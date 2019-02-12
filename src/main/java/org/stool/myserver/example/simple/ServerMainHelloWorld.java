package org.stool.myserver.example.simple;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stool.myserver.core.http.HttpServer;
import org.stool.myserver.core.net.Buffer;

import java.nio.charset.Charset;

public class ServerMainHelloWorld {


    private static Logger log = LoggerFactory.getLogger(ServerMainHelloWorld.class);

    public static void main(String[] args) {
        startServer();
    }

    private static void startServer() {

        HttpServer server = HttpServer.server();


        server.requestHandler(request -> {

            request.response().end("Hello World!");
            

        }).listen(8081);

        server.start();

    }


}
