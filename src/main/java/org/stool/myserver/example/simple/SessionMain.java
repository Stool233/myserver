package org.stool.myserver.example.simple;

import org.stool.myserver.cookie.CookieHandler;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.route.RouteHandler;
import org.stool.myserver.session.Session;
import org.stool.myserver.session.SessionHandler;
import org.stool.myserver.session.impl.LocalSessionStore;

public class SessionMain {

    public static void main(String[] args) {
        EntryPoint entryPoint = EntryPoint.entryPoint();

        RouteHandler routeHandler = RouteHandler.create(entryPoint);

        routeHandler.route().handler(CookieHandler.create());
        routeHandler.route().handler(SessionHandler.create(LocalSessionStore.create()));

        routeHandler.route("/test").handler(routingContext -> {
            String newValue = routingContext.request().getParam("test");
            Session session = routingContext.session();
            String oldValue = session.get("test");
            session.put("test", newValue);
            if (oldValue != null) {
                routingContext.response().end(oldValue);
            } else {
                routingContext.response().end("null");
            }

        });

        entryPoint.createHttpServer().requestHandler(routeHandler).listen(8080);
    }
}
