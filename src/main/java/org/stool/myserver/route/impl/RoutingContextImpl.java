package org.stool.myserver.route.impl;

import org.stool.myserver.core.http.HttpServerRequest;
import org.stool.myserver.core.http.HttpServerResponse;
import org.stool.myserver.route.Route;
import org.stool.myserver.route.Router;
import org.stool.myserver.route.RoutingContext;

import java.util.Iterator;
import java.util.Set;

public class RoutingContextImpl implements RoutingContext {

    private HttpServerRequest request;
    private HttpServerResponse response;
    private Router router;
    private Set<Route> routes;

    private Iterator<Route> iterator;



    public RoutingContextImpl(HttpServerRequest request, Router router, Set<Route> routes) {
        this.request = request;
        this.response = request.response();
        this.router = router;
        this.routes = routes;
        this.iterator = routes.iterator();
    }


    @Override
    public HttpServerRequest request() {
        return request;
    }

    @Override
    public HttpServerResponse response() {
        return response;
    }

    @Override
    public void next() {
        while (iterator.hasNext()) {
            Route route = iterator.next();
            if (route.matches(this)) {
                route.handleContext(this);
                break;
            }
        }
    }
}
