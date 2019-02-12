package org.stool.myserver.example.simple;

import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.staticfile.StaticHandler;
import org.stool.myserver.core.http.HttpServer;
import org.stool.myserver.route.RouteHandler;

public class StaticMain {

    public static void main(String[] args) {
        String dir = "C:/Users/Administrator/Downloads/myserver/src/main/resources/";

        EntryPoint entryPoint = EntryPoint.entryPoint();
        HttpServer httpServer = entryPoint.createHttpServer();

        RouteHandler routeHandler = RouteHandler.create(entryPoint);

        routeHandler.route("/static/*").handler(StaticHandler.create(dir));

        httpServer.requestHandler(routeHandler).listen(8080);
    }
}
