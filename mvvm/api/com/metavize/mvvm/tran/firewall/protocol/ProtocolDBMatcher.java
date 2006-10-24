/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tran.firewall.protocol;

import java.io.Serializable;

import com.metavize.mvvm.tapi.Protocol;

public abstract class ProtocolDBMatcher implements ProtocolMatcher, Serializable
{
    /** Package protected so that only classes in the package can add to the list
     * of database saveable ip matchers */
    ProtocolDBMatcher()
    {
    }

    public abstract boolean isMatch( Protocol protocol );
    public abstract boolean isMatch( short protocol );
    public abstract String toDatabaseString();
}
