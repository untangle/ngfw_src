/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: Spyware.java 8515 2007-01-03 00:13:24Z amread $
 */

package com.untangle.tran.spyware;

import java.net.InetAddress;

import com.untangle.tran.http.BlockDetails;

public class SpywareBlockDetails extends BlockDetails
{
    private final InetAddress clientAddr;

    public SpywareBlockDetails(String host, String uri, InetAddress clientAddr)
    {
        super(host, uri);
        this.clientAddr = clientAddr;
    }
    public InetAddress getClientAddress()
    {
        return clientAddr;
    }
}
