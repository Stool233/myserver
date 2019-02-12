package org.stool.myserver.session;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.stool.myserver.core.Handler;
import org.stool.myserver.route.RoutingContext;

public class SessionHandler implements Handler<RoutingContext> {

    private static final String SESSION_NAME = "scau.session";

    private static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000;

    private static final boolean DEFAULT_COOKIE_HTTP_ONLY_FLAG = true;

    private static final String DEFAULT_SESSION_COOKIE_PATH = "/";

    private SessionStore sessionStore;


    public static SessionHandler create(SessionStore sessionStore) {
        return new SessionHandler(sessionStore);
    }

    public SessionHandler(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public void handle(RoutingContext context) {
        Cookie sessionCookie = null;
        if ((sessionCookie = context.cookies().get(SESSION_NAME)) != null) {
            String sessionId = sessionCookie.value();
            Session session = null;
            if ((session = sessionStore.get(sessionId)) != null) {
                context.session(session);
            }
        }

        context.addHeadersEndHandler(v -> {
            sessionStore.put(context.session().id(), context.session());

            Cookie cookie = new DefaultCookie(SESSION_NAME, context.session().id());
            cookie.setHttpOnly(DEFAULT_COOKIE_HTTP_ONLY_FLAG);
            cookie.setMaxAge(DEFAULT_SESSION_TIMEOUT);
            cookie.setPath(DEFAULT_SESSION_COOKIE_PATH);
            context.cookies().put(SESSION_NAME, cookie);
        });
    }
}
