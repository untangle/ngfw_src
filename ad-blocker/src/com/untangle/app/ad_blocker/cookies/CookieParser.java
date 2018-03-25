/**
 * $Id$
 */
package com.untangle.app.ad_blocker.cookies;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * This class provides static cookie parsing utility methods
 */
public class CookieParser
{
    private static final int KEY_STATE = 0;
    private static final int VALUE_STATE = 1;
    private static final int END_AVPAIR_STATE = 2;

    private final static Logger logger = Logger.getLogger(CookieParser.class);

    /**
     * Parses a cookie, keys and values are converted to lower case.
     *
     * XXX find a 3rd party parser, or beef this one up.
     *
     * XXX converting to lower case probably bad, we need to preserve
     * case, yet allow lookups.
     *
     * @param v the cookie string from the HTTP header.
     * @return a map of cookie keys and values.
     */
    public static Map<String, String> parseCookie(String v)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("parsing cookie: " + v);
        }

        int state = KEY_STATE;

        Map<String, String> hashMap = new HashMap<>();

        StringBuilder keyBuffer = new StringBuilder();
        StringBuilder valueBuffer = new StringBuilder();
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);

            switch (state) {
            case KEY_STATE:
                {
                    if ('=' == c) {
                        state = VALUE_STATE;
                    } else {
                        keyBuffer.append(Character.toLowerCase(c));
                    }
                    break;
                }
            case VALUE_STATE:
                {
                    if ('"' == c) {
                        if (0 < valueBuffer.length()) {
                            state = END_AVPAIR_STATE;
                        }
                    } else if (';' == c) {
                        hashMap.put( keyBuffer.toString().trim(), valueBuffer.toString().trim() );
                        keyBuffer = new StringBuilder();
                        valueBuffer = new StringBuilder();
                        state = KEY_STATE;
                    } else {
                        valueBuffer.append(Character.toLowerCase(c));
                    }
                    break;
                }
            case END_AVPAIR_STATE:
                {
                    if (';' == c || ',' == c) {
                        hashMap.put( keyBuffer.toString().trim(), valueBuffer.toString().trim() );
                        keyBuffer = new StringBuilder();
                        valueBuffer = new StringBuilder();
                        state = KEY_STATE;
                    }
                }
            }
        }

        if (0 < keyBuffer.length()) {
            hashMap.put( keyBuffer.toString().trim(), valueBuffer.toString().trim() );
        }

        return hashMap;
    }
}
