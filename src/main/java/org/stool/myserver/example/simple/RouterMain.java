package org.stool.myserver.example.simple;

import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.http.HttpMethod;
import org.stool.myserver.core.http.HttpServer;
import org.stool.myserver.route.Route;
import org.stool.myserver.route.Router;

public class RouterMain {

    public static void main(String[] args) {
        EntryPoint entryPoint = EntryPoint.entryPoint();
        HttpServer httpServer = entryPoint.createHttpServer();

        Router router = Router.router(entryPoint);

        router.route(HttpMethod.GET, "/a").handler(routingContext -> {
           routingContext.response().end("a");
        });

        router.route(HttpMethod.GET, "/b").handler(routingContext -> {
            routingContext.response().end("b");
        });

        httpServer.requestHandler(router).listen(8080);
    }
}
