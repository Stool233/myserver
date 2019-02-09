package org.stool.myserver.route.impl;

import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.http.HttpMethod;
import org.stool.myserver.core.http.HttpServerRequest;
import org.stool.myserver.route.Route;
import org.stool.myserver.route.Router;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class RouterImpl implements Router {

    private final EntryPoint entryPoint;
    private final Set<Route> routes = new HashSet<>();

    public RouterImpl(EntryPoint entryPoint) {
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
