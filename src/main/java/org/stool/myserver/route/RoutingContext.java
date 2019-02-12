package org.stool.myserver.route;

import io.netty.handler.codec.http.cookie.Cookie;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpServerRequest;
import org.stool.myserver.core.http.HttpServerResponse;
import org.stool.myserver.session.Session;

import java.util.Map;

public interface RoutingContext {

    HttpServerRequest request();

    HttpServerResponse response();

    void next();

    Route currentRoute();

    Context context();

    void addCookie(Cookie cookie);

    void addHeadersEndHandler(Handler<Void> handler);

    Map<String, Cookie> cookies();

    Session session();

    void session(Session session);
}
