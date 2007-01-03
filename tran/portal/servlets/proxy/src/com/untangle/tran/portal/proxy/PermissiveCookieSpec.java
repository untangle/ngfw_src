/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.portal.proxy;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.cookie.MalformedCookieException;
import org.apache.commons.httpclient.cookie.NetscapeDraftSpec;
import org.apache.commons.httpclient.cookie.RFC2109Spec;

public class PermissiveCookieSpec extends RFC2109Spec
{
    private final NetscapeDraftSpec nutscape = new NetscapeDraftSpec();

    public PermissiveCookieSpec() { }

    public Cookie[] parse(String host, int port, String path,
                          boolean secure, String header)
        throws MalformedCookieException
    {
        return nutscape.parse(host, port, path, secure, header);
    }
}
