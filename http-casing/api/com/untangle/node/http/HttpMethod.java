/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.http;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP method, RFC 2616 section 5.1.1.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class HttpMethod implements Serializable
{
	private static final long serialVersionUID = -8178244830103574962L;
	
	public static final HttpMethod OPTIONS = new HttpMethod('O', "OPTIONS");
    public static final HttpMethod GET = new HttpMethod('G', "GET");
    public static final HttpMethod HEAD = new HttpMethod('H', "HEAD");
    public static final HttpMethod POST = new HttpMethod('P', "POST");
    public static final HttpMethod PUT = new HttpMethod('U', "PUT");
    public static final HttpMethod DELETE = new HttpMethod('D', "DELETE");
    public static final HttpMethod TRACE = new HttpMethod('T', "TRACE");
    public static final HttpMethod CONNECT = new HttpMethod('C', "CONNECT");
    public static final HttpMethod NON_STANDARD = new HttpMethod('X', "NON-STANDARD");

    private static final Map<Character, HttpMethod> INSTANCES = new HashMap<Character, HttpMethod>();
    private static final Map<String, HttpMethod> BY_NAME = new HashMap<String, HttpMethod>();

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

    // constructors -----------------------------------------------------------

    private HttpMethod(char key, String method)
    {
        this.key = key;
        this.method = method;
    }

    // static factories -------------------------------------------------------

    public static HttpMethod getInstance(char key)
    {
        return INSTANCES.get(key);
    }

    public static HttpMethod getInstance(String methStr)
    {
        HttpMethod method = BY_NAME.get(methStr.toUpperCase());
        if (null == method) { /* XXX setting about accepting unknown methods */
            method = new HttpMethod('X', methStr);
        }

        return method;
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
