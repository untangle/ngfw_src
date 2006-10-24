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

package com.metavize.mvvm.tran.firewall.port;

import java.io.Serializable;

public abstract class PortDBMatcher implements PortMatcher, Serializable
{
    /** Package protected so that only classes in the package can add to the list
     * of database saveable port matchers */
    PortDBMatcher()
    {
    }

    public abstract boolean isMatch( int port );
    public abstract String toDatabaseString();
}
