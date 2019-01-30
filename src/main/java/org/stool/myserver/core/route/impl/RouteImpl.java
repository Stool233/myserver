package org.stool.myserver.core.route.impl;

import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpMethod;
import org.stool.myserver.core.route.Route;
import org.stool.myserver.core.route.Router;
import org.stool.myserver.core.route.RoutingContext;

import java.util.HashSet;
import java.util.Set;

public class RouteImpl implements Route {

    private Router router;
    private Set<HttpMethod> httpMethods = new HashSet<>();
    private String path;
    private Set<String> consumedContentTypes = new HashSet<>();
    private Set<String> producedContentType = new HashSet<>();

    private Handler<RoutingContext> contextHandler;
    private Handler<RoutingContext> failureHandler;

    public RouteImpl(Router router) {
        this.router = router;
    }

    @Override
    public Route method(HttpMethod method) {
        httpMethods.add(method);
        return this;
    }

    @Override
    public Route path(String path) {
        this.path = path;
        return this;
    }

    @Override
    public Route produces(String contentType) {
        producedContentType.add(contentType);
        return this;
    }

    @Override
    public Route consumes(String contentType) {
        consumedContentTypes.add(contentType);
        return this;
    }

    @Override
    public Route handler(Handler<RoutingContext> requestHandler) {
        this.contextHandler = requestHandler;
        return this;
    }

    @Override
    public Route blockingHandler(Handler<RoutingContext> requestHandler) {
        // todo
        return null;
    }

    @Override
    public Route failureHandler(Handler<RoutingContext> failureHandler) {
        this.failureHandler = failureHandler;
        return this;
    }
}
