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

/**
 * An IPMatcher that is capable of being saved to and loaded from
 * the database.  Done as an abstract class so only classes in this
 * package can implement Database Matchers.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
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
