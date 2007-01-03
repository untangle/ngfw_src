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

package com.untangle.mvvm.tran.firewall.ip;

import java.net.InetAddress;

import java.io.Serializable;

public abstract class IPDBMatcher implements IPMatcher, Serializable
{
    /** Package protected so that only classes in the package can add to the list
     * of database saveable ip matchers */
    IPDBMatcher()
    {
    }

    public abstract boolean isMatch( InetAddress address );
    public abstract String toDatabaseString();
}
