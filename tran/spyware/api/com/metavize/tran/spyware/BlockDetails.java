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

package com.metavize.tran.spyware;

import java.io.Serializable;

public class BlockDetails implements Serializable
{
    private final String nonce;
    private final String host;
    private final String uri;

    public BlockDetails(String nonce, String host, String uri)
    {
        this.nonce = nonce;
        this.host = host;
        this.uri = uri;
    }

    public String getNonce()
    {
        return nonce;
    }

    public String getHost()
    {
        return host;
    }

    public String getUrl()
    {
        return "http://" + host + uri;

    }
}