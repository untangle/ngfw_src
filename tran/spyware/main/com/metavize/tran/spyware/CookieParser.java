/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: CookieParser.java,v 1.3 2005/01/27 10:00:42 amread Exp $
 */

package com.metavize.tran.spyware;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class CookieParser
{
    private static final int KEY_STATE = 0;
    private static final int VALUE_STATE = 1;
    private static final int END_AVPAIR_STATE = 2;

    private static final Logger logger = Logger.getLogger(CookieParser.class);

    public static Map parseCookie(String v)
    {
        logger.debug("parsing cookie: " + v);

        int state = KEY_STATE;

        Map m = new HashMap();

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
                        keyBuffer.append(c);
                    }
                    break;
                }
            case VALUE_STATE:
                {
                    if ('"' == c) {
                        if (0 < valueBuffer.length()) {
                            state = END_AVPAIR_STATE;
                        }
                    } else if (';' == c || ',' == c) {
                        m.put(keyBuffer.toString().trim(),
                              valueBuffer.toString().trim());
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
                        m.put(keyBuffer.toString().trim(),
                              valueBuffer.toString().trim());
                        keyBuffer = new StringBuilder();
                        valueBuffer = new StringBuilder();
                        state = KEY_STATE;
                    }
                }
            }
        }

        if (0 < keyBuffer.length()) {
            m.put(keyBuffer.toString().trim(), valueBuffer.toString().trim());
        }

        return m;
    }
}
