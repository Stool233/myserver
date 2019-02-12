package org.stool.myserver.cookie;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import org.stool.myserver.core.Handler;
import org.stool.myserver.route.RoutingContext;

import java.util.Map;
import java.util.Set;

public class CookieHandler implements Handler<RoutingContext> {


    @Override
    public void handle(RoutingContext context) {
        String cookieHeader = context.request().headers().get(HttpHeaderNames.COOKIE);

        if (cookieHeader != null) {
            Set<Cookie> nettyCookies = ServerCookieDecoder.STRICT.decode(cookieHeader);
            for (io.netty.handler.codec.http.cookie.Cookie cookie : nettyCookies) {
                context.addCookie(cookie);
            }
        }

        context.addHeadersEndHandler(v -> {
            Map<String, Cookie> cookies = context.cookies();
            for (Cookie cookie : cookies.values()) {
                context.response().headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
            }
        });
    }

    public static CookieHandler create() {
        return new CookieHandler();
    }
}
