/**
 * $Id$
 */
package com.untangle.app.http;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP method, RFC 2616 section 5.1.1.
 */
@SuppressWarnings("serial")
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
    public static final HttpMethod NON_STANDARD = new HttpMethod('X', "NON-STANDARD");

    private static final Map<Character, HttpMethod> INSTANCES = new HashMap<>();
    private static final Map<String, HttpMethod> BY_NAME = new HashMap<>();

    static {
        INSTANCES.put(OPTIONS.getKey(), OPTIONS);
        INSTANCES.put(GET.getKey(), GET);
        INSTANCES.put(HEAD.getKey(), HEAD);
        INSTANCES.put(POST.getKey(), POST);
        INSTANCES.put(PUT.getKey(), PUT);
        INSTANCES.put(DELETE.getKey(), DELETE);
        INSTANCES.put(TRACE.getKey(), TRACE);
        INSTANCES.put(CONNECT.getKey(), CONNECT);
        INSTANCES.put(NON_STANDARD.getKey(), NON_STANDARD);

        BY_NAME.put(OPTIONS.toString(), OPTIONS);
        BY_NAME.put(GET.toString(), GET);
        BY_NAME.put(HEAD.toString(), HEAD);
        BY_NAME.put(POST.toString(), POST);
        BY_NAME.put(PUT.toString(), PUT);
        BY_NAME.put(DELETE.toString(), DELETE);
        BY_NAME.put(TRACE.toString(), TRACE);
        BY_NAME.put(CONNECT.toString(), CONNECT);
        BY_NAME.put(NON_STANDARD.toString(), NON_STANDARD);
    }

    private final char key;
    private final String method;

    /**
     * Private constructor to stop instantiation - use factories
     * @param key - the key for the method
     * @param method - the method string
     */
    private HttpMethod(char key, String method)
    {
        this.key = key;
        this.method = method;
    }

    /**
     * Factory to get the global instance for the specified key char
     * @param key - the key char
     * @return the global singleton of that key
     */
    public static HttpMethod getInstance(char key)
    {
        return INSTANCES.get(key);
    }

    /**
     * Factory to get the global instance for the specified method
     * @param methStr - the method , ie "GET"
     * @return the global singleton for that method
     */
    public static HttpMethod getInstance(String methStr)
    {
        HttpMethod method = BY_NAME.get(methStr.toUpperCase());
        if (null == method) { /* XXX setting about accepting unknown methods */
            method = new HttpMethod('X', methStr);
        }

        return method;
    }

    /**
     * Get the key character
     * @return char
     */
    public char getKey()
    {
        return key;
    }

    /**
     * Get human readable string
     * @return string
     */
    public String toString()
    {
        return method;
    }
}
