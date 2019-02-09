package org.stool.myserver.route.impl;

import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpMethod;
import org.stool.myserver.core.http.HttpServerRequest;
import org.stool.myserver.route.Route;
import org.stool.myserver.route.Router;
import org.stool.myserver.route.RoutingContext;

import java.util.HashSet;
import java.util.Set;

public class RouteImpl implements Route {

    private Router router;
    private Set<HttpMethod> methods = new HashSet<>();
    private String path;
    private Set<String> consumedContentTypes = new HashSet<>();
    private Set<String> producedContentType = new HashSet<>();

    private Handler<RoutingContext> contextHandler;
    private Handler<RoutingContext> failureHandler;

    private boolean added;

    public RouteImpl(Router router) {
        this.router = router;
    }

    public RouteImpl(Router router, HttpMethod method, String path) {
        this(router);
        methods.add(method);
        checkPath(path);
        this.path = path;
    }

    public RouteImpl(RouterImpl router, String path) {
        this(router);
        this.path = path;
    }

    @Override
    public Route method(HttpMethod method) {
        methods.add(method);
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
    public synchronized Route handler(Handler<RoutingContext> requestHandler) {
        this.contextHandler = requestHandler;
        checkAdd();
        return this;
    }

    private void checkAdd() {
        if (!added) {
            router.add(this);
            added = true;
        }
    }

    @Override
    public synchronized Route blockingHandler(Handler<RoutingContext> requestHandler) {
        // todo
        return null;
    }

    @Override
    public synchronized Route failureHandler(Handler<RoutingContext> failureHandler) {
        this.failureHandler = failureHandler;
        checkAdd();
        return this;
    }

    @Override
    public synchronized boolean matches(RoutingContext context) {
        HttpServerRequest request = context.request();
        if (path != null && !pathMatches(context)) {
            return false;
        }
        if (!methods.contains(context.request().method())) {
            return false;
        }

        return true;
    }

    @Override
    public void handleContext(RoutingContext context) {
        Handler<RoutingContext> contextHandler;

        synchronized (this) {
            contextHandler = this.contextHandler;
        }

        contextHandler.handle(context);
    }

    private boolean pathMatches(RoutingContext context) {
        return (path.equals(context.request().path()));
    }

    private void checkPath(String path) {
        if ("".equals(path) || path.charAt(0) != '/') {
            throw new IllegalArgumentException("Path must start with /");
        }
    }
}
