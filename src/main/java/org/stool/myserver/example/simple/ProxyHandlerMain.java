package org.stool.myserver.example.simple;

import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.http.HttpServer;
import org.stool.myserver.core.proxy.ProxyHandler;
import org.stool.myserver.route.RouteHandler;

public class ProxyHandlerMain {

    public static void main(String[] args) {

        EntryPoint entryPoint = EntryPoint.entryPoint();
        HttpServer httpServer = entryPoint.createHttpServer();

        RouteHandler routeHandler = RouteHandler.create(entryPoint);

        routeHandler.route("/proxy/*").handler(ProxyHandler.create(entryPoint)
                .addRemoteServer("127.0.0.1", 8081)
                .addRemoteServer("127.0.0.1", 8082));

        httpServer.requestHandler(routeHandler).listen(8080);
    }

}
