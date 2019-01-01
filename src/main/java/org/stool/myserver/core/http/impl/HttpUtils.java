package org.stool.myserver.core.http.impl;

import io.netty.handler.codec.http.QueryStringDecoder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpUtils {

    public static String parsePath(String uri) {
        int i;
        if (uri.charAt(0) == '/') {
            i = 0;
        } else {
            i = uri.indexOf("://");
            if (i == -1) {
                i = 0;
            } else {
                i = uri.indexOf('/', i + 3);
                if (i == -1) {
                    // contains no /
                    return "/";
                }
            }
        }

        int queryStart = uri.indexOf('?', i);
        if (queryStart == -1) {
            queryStart = uri.length();
        }
        return uri.substring(i, queryStart);
    }

    public static String parseQuery(String uri) {
        int i = uri.indexOf('?');
        if (i == -1) {
            return null;
        } else {
            return uri.substring(i + 1 , uri.length());
        }
    }

    public static Map<String, String> params(String uri) {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        Map<String, List<String>> paramLists = queryStringDecoder.parameters();
        Map<String, String> params = new HashMap<>();
        for (String key : paramLists.keySet()) {
            if (paramLists.get(key) != null && paramLists.get(key).size() > 0) {
                params.put(key, paramLists.get(key).get(0));
            } else {
                params.put(key, null);
            }
        }
        return params;
    }

    public static String absoluteURI(HttpServerRequestImpl httpServerRequest) throws URISyntaxException {
        String absoluteURI = null;
        URI uri = new URI(httpServerRequest.uri());
        String scheme = uri.getScheme();
        if (scheme != null && (scheme.equals("http") || scheme.equals("https"))) {
            absoluteURI = uri.toString();
        } else {
            // todo
        }
        return absoluteURI;
    }
}
