/*
 * Copyright (c) 2006 Metavize Inc.
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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class BlockDetails implements Serializable
{
    private final String nonce;
    private final HttpBlockerSettings settings;
    private final String host;
    private final URI uri;
    private final String reason;

    public BlockDetails(String nonce, HttpBlockerSettings settings,
                        String host, URI uri, String reason)
    {
        this.nonce = nonce;
        this.settings = settings;
        this.host = host;
        this.uri = uri;
        this.reason = reason;
    }

    public String getNonce()
    {
        return nonce;
    }

    public String getHeader()
    {
        return settings.getBlockTemplate().getHeader();
    }

    public String getContact()
    {
        return settings.getBlockTemplate().getContact();
    }

    public String getHost()
    {
        return host;
    }

    public String getRuleSite()
    {
        if (host.startsWith("www.") && 4 < host.length()) {
            return host.substring(4);
        } else {
            return host;
        }
    }

    public URI getUri()
    {
        return uri;
    }

    public URL getUrl()
    {
        try {
            return uri.resolve("http://" + host).toURL();
        } catch (MalformedURLException exn) {
            throw new RuntimeException(exn);
        }
    }

    public String getReason()
    {
        return reason;
    }
}