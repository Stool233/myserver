package org.stool.myserver.route;

import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpMethod;

public interface Route {

    Route method(HttpMethod method);

    Route path(String path);

    Route produces(String contentType);

    Route consumes(String contentType);

    Route handler(Handler<RoutingContext> requestHandler);

    Route blockingHandler(Handler<RoutingContext> requestHandler);

    Route failureHandler(Handler<RoutingContext> failureHandler);


    boolean matches(RoutingContext context);

    void handleContext(RoutingContext context);

    void handleFailure(RoutingContext context);

    boolean isExactPath();

    String path();
}
