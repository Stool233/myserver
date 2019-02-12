package org.stool.myserver.route.impl;

import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpMethod;
import org.stool.myserver.route.Route;
import org.stool.myserver.route.RouteHandler;
import org.stool.myserver.route.RoutingContext;

import java.util.HashSet;
import java.util.Set;

public class RouteImpl implements Route {

    private RouteHandler routeHandler;
    private Set<HttpMethod> methods = new HashSet<>();
    private String path;
    private boolean exactPath;
    private Set<String> consumedContentTypes = new HashSet<>();
    private Set<String> producedContentType = new HashSet<>();

    private Handler<RoutingContext> contextHandler;
    private Handler<RoutingContext> failureHandler;

    private boolean added;

    public RouteImpl(RouteHandler routeHandler) {
        this.routeHandler = routeHandler;
    }

    public RouteImpl(RouteHandler routeHandler, HttpMethod method, String path) {
        this(routeHandler);
        methods.add(method);
        checkPath(path);
        setPath(path);
    }

    private void setPath(String path) {
        if (path.charAt(path.length() - 1) != '*') {
            exactPath = true;
            this.path = path;
        } else {
            exactPath = false;
            this.path = path.substring(0, path.length() - 1);
        }
    }

    public RouteImpl(RouteHandlerImpl router, String path) {
        this(router);
        checkPath(path);
        setPath(path);
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
            routeHandler.add(this);
            added = true;
        }
    }

    @Override
    public synchronized Route blockingHandler(Handler<RoutingContext> requestHandler) {
        // todo
        return this;
    }

    @Override
    public synchronized Route failureHandler(Handler<RoutingContext> failureHandler) {
        this.failureHandler = failureHandler;
        checkAdd();
        return this;
    }

    @Override
    public synchronized boolean matches(RoutingContext context) {

        if (!methods.isEmpty() && !methods.contains(context.request().method())) {
            return false;
        }

        if (path != null && !pathMatches(context)) {
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

    @Override
    public void handleFailure(RoutingContext context) {
        Handler<RoutingContext> failureHandler;

        synchronized (this) {
            failureHandler = this.failureHandler;
        }

        failureHandler.handle(context);
    }

    @Override
    public boolean isExactPath() {
        return exactPath;
    }

    @Override
    public String path() {
        return path;
    }

    private boolean pathMatches(RoutingContext context) {
        if (exactPath) {
            return path.equals(context.request().path());
        } else {
            return context.request().path().startsWith(path);
        }
    }

    private void checkPath(String path) {
        if ("".equals(path) || path.charAt(0) != '/') {
            throw new IllegalArgumentException("Path must start with /");
        }
    }
}
