package org.stool.myserver.route;

import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpMethod;
import org.stool.myserver.core.http.HttpServerRequest;
import org.stool.myserver.route.impl.RouteImpl;
import org.stool.myserver.route.impl.RouterImpl;

public interface Router extends Handler<HttpServerRequest>{

    static Router router(EntryPoint entryPoint) {
        return new RouterImpl(entryPoint);
    }

    Route route();

    Route route(HttpMethod method, String path);

    Route route(String path);

    EntryPoint entryPoint();

    void add(Route route);
}
