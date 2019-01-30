package org.stool.myserver.core.route;

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


}
