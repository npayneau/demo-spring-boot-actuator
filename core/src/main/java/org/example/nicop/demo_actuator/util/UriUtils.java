package org.example.nicop.demo_actuator.util;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;

public final class UriUtils {

    public static Map<String, String> splitQuery(URI uri) {

        String query = uri.getQuery();
        if(query == null) {
            return emptyMap();
        }
        Map<String, String> queryPairs = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if(idx != -1)
                queryPairs.put(decodeAsUtf8(pair.substring(0, idx)), decodeAsUtf8(pair.substring(idx + 1)));
            else
                queryPairs.put(decodeAsUtf8(pair), "");
        }
        return queryPairs;
    }

    private static String decodeAsUtf8(String str) {
        try {
            return URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
}
