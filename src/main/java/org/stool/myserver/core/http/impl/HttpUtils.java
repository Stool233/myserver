package org.stool.myserver.core.http.impl;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AsciiString;

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

    static HttpMethod toNettyHttpMethod(org.stool.myserver.core.http.HttpMethod method) {
        switch (method) {
            case CONNECT: {
                return HttpMethod.CONNECT;
            }
            case GET: {
                return HttpMethod.GET;
            }
            case PUT: {
                return HttpMethod.PUT;
            }
            case POST: {
                return HttpMethod.POST;
            }
            case DELETE: {
                return HttpMethod.DELETE;
            }
            case HEAD: {
                return HttpMethod.HEAD;
            }
            case OPTIONS: {
                return HttpMethod.OPTIONS;
            }
            case TRACE: {
                return HttpMethod.TRACE;
            }
            case PATCH: {
                return HttpMethod.PATCH;
            }
            default: {
                return HttpMethod.GET;
            }
        }
    }

    private static final AsciiString TIMEOUT_EQ = AsciiString.of("timeout=");

    public static int parseKeepAliveHeaderTimeout(CharSequence value) {
        int len = value.length();
        int pos = 0;
        while (pos < len) {
            int idx = AsciiString.indexOf(value, ',', pos);
            int next;
            if (idx == -1) {
                idx = next = len;
            } else {
                next = idx + 1;
            }
            while (pos < idx && value.charAt(pos) == ' ') {
                pos++;
            }
            int to = idx;
            while (to > pos && value.charAt(to -1) == ' ') {
                to--;
            }
            if (AsciiString.regionMatches(value, true, pos, TIMEOUT_EQ, 0, TIMEOUT_EQ.length())) {
                pos += TIMEOUT_EQ.length();
                if (pos < to) {
                    int ret = 0;
                    while (pos < to) {
                        int ch = value.charAt(pos++);
                        if (ch >= '0' && ch < '9') {
                            ret = ret * 10 + (ch - '0');
                        } else {
                            ret = -1;
                            break;
                        }
                    }
                    if (ret > -1) {
                        return ret;
                    }
                }
            }
            pos = next;
        }
        return -1;
    }
}
