/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.httpblocker;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// XXX to enum when we XDoclet gets out of the way

public class Reason implements Serializable
{
    private static final long serialVersionUID = -1388743204136725990L;

    public static final Reason BLACKLIST_DOMAIN = new Reason('D', "domain");
    public static final Reason BLACKLIST_URI = new Reason('U', "URI");
    public static final Reason USER_URI = new Reason('I', "URI");
    public static final Reason EXTENSION = new Reason('E', "extension");
    public static final Reason MIME_TYPE = new Reason('M', "mime-type");
    public static final Reason CLIENT_ADDR = new Reason('C', "client-addr");

    private static final Map INSTANCES = new HashMap();

    static {
        INSTANCES.put('D', BLACKLIST_DOMAIN);
        INSTANCES.put('U', BLACKLIST_URI);
        INSTANCES.put('I', USER_URI);
        INSTANCES.put('E', EXTENSION);
        INSTANCES.put('M', MIME_TYPE);
        INSTANCES.put('C', CLIENT_ADDR);
    }

    private char key;
    private String reason;

    private Reason(char key, String reason)
    {
        this.key = key;
        this.reason = reason;
    }

    public static Reason getInstance(char key)
    {
        return (Reason)INSTANCES.get(key);
    }

    public char getKey()
    {
        return key;
    }

    public String toString()
    {
        return reason;
    }

    // Serializable methods ---------------------------------------------------

    private Object reasResolve()
    {
        return INSTANCES.get(key);
    }
}
