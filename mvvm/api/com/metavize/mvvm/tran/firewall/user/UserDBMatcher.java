/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tran.firewall.user;

import java.util.List;

import java.io.Serializable;

public abstract class UserDBMatcher implements UserMatcher, Serializable
{
    /** Package protected so that only classes in the package can add to the list
     * of database saveable user matchers */
    UserDBMatcher()
    {
    }

    public abstract boolean isMatch( String username );

    /* These lists are typically not modifiable */
    public abstract List<String> toDatabaseList();
}
