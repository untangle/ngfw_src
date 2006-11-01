/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.spyware;

import java.io.Serializable;
import java.net.InetAddress;

public class BlockDetails implements Serializable
{
    private final String nonce;
    private final String host;
    private final String uri;
    private final InetAddress clientAddr;

    public BlockDetails(String nonce, String host, String uri,
                        InetAddress clientAddr)
    {
        this.nonce = nonce;
        this.host = host;
        this.uri = uri;
        this.clientAddr = clientAddr;
    }

    public String getNonce()
    {
        return nonce;
    }

    public String getHost()
    {
        return host;
    }

    public String getWhitelistHost()
    {
        if (host.startsWith("www.") && 4 < host.length()) {
            return host.substring(4);
        } else {
            return host;
        }
    }

    public String getUrl()
    {
        return "http://" + host + uri;

    }

    public InetAddress getClientAddress()
    {
        return clientAddr;
    }
}