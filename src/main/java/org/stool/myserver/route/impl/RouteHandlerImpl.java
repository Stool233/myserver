package org.stool.myserver.route.impl;

import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.http.HttpMethod;
import org.stool.myserver.core.http.HttpServerRequest;
import org.stool.myserver.route.Route;
import org.stool.myserver.route.RouteHandler;

import java.util.ArrayList;
import java.util.List;

public class RouteHandlerImpl implements RouteHandler {

    private final EntryPoint entryPoint;
    private final List<Route> routes = new ArrayList<>();

    public RouteHandlerImpl(EntryPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    @Override
    public Route route() {
        return new RouteImpl(this);
    }

    @Override
    public Route route(HttpMethod method, String path) {
        return new RouteImpl(this, method, path);
    }

    @Override
    public Route route(String path) {
        return new RouteImpl(this, path);
    }

    @Override
    public EntryPoint entryPoint() {
        return entryPoint;
    }

    @Override
    public void handle(HttpServerRequest request) {
        new RoutingContextImpl(request, this, routes).next();
    }

    @Override
    public void add(Route route) {
        routes.add(route);
    }
}
