package org.stool.myserver.core.route.impl;

import org.stool.myserver.core.http.HttpServerRequest;
import org.stool.myserver.core.http.HttpServerResponse;
import org.stool.myserver.core.route.Route;
import org.stool.myserver.core.route.Router;
import org.stool.myserver.core.route.RoutingContext;

import java.util.Set;

public class RoutingContextImpl implements RoutingContext {

    private HttpServerRequest request;
    private HttpServerResponse response;
    private Router router;
    private Set<Route> routes;

    public RoutingContextImpl(HttpServerRequest request, Router router, Set<Route> routes) {
        this.request = request;
        this.response = request.response();
        this.router = router;
        this.routes = routes;
    }


    @Override
    public HttpServerRequest request() {
        return request;
    }

    @Override
    public HttpServerResponse response() {
        return response;
    }
}
