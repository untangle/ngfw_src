/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.http;

import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

/**
 * HTTP method, RFC 2616 section 5.1.1.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class HttpMethod implements Serializable
{
    public static final HttpMethod OPTIONS = new HttpMethod('O', "OPTIONS");
    public static final HttpMethod GET = new HttpMethod('G', "GET");
    public static final HttpMethod HEAD = new HttpMethod('H', "HEAD");
    public static final HttpMethod POST = new HttpMethod('P', "POST");
    public static final HttpMethod PUT = new HttpMethod('U', "PUT");
    public static final HttpMethod DELETE = new HttpMethod('D', "DELETE");
    public static final HttpMethod TRACE = new HttpMethod('T', "TRACE");
    public static final HttpMethod CONNECT = new HttpMethod('C', "CONNECT");

    private static final Map INSTANCES = new HashMap();
    private static final Map BY_NAME = new HashMap();

    static {
        INSTANCES.put(OPTIONS.getKey(), OPTIONS);
        INSTANCES.put(GET.getKey(), GET);
        INSTANCES.put(HEAD.getKey(), HEAD);
        INSTANCES.put(POST.getKey(), POST);
        INSTANCES.put(PUT.getKey(), PUT);
        INSTANCES.put(DELETE.getKey(), DELETE);
        INSTANCES.put(TRACE.getKey(), TRACE);
        INSTANCES.put(CONNECT.getKey(), TRACE);

        BY_NAME.put(OPTIONS.toString(), OPTIONS);
        BY_NAME.put(GET.toString(), GET);
        BY_NAME.put(HEAD.toString(), HEAD);
        BY_NAME.put(POST.toString(), POST);
        BY_NAME.put(PUT.toString(), PUT);
        BY_NAME.put(DELETE.toString(), DELETE);
        BY_NAME.put(TRACE.toString(), TRACE);
        BY_NAME.put(CONNECT.toString(), TRACE);
    }

    private final char key;
    private final String method;

    private HttpMethod(char key, String method)
    {
        this.key = key;
        this.method = method;
    }

    public static HttpMethod getInstance(char key)
    {
        return (HttpMethod)INSTANCES.get(key);
    }

    public static HttpMethod getInstance(String method)
    {
        return (HttpMethod)BY_NAME.get(method.toUpperCase());
    }

    public char getKey()
    {
        return key;
    }

    // Object methods ---------------------------------------------------------

    public String toString() { return method; }

    // Serialization ----------------------------------------------------------

    Object readResolve()
    {
        return getInstance(key);
    }
}
