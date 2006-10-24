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

package com.metavize.mvvm.tran.firewall.user;

import java.util.List;

import java.io.Serializable;

public abstract class UserDBMatcher implements UserMatcher, Serializable
{
    private static final long serialVersionUID = -6040354037968024420L;

    /** Package protected so that only classes in the package can add to the list
     * of database saveable user matchers */
    UserDBMatcher()
    {
    }

    public abstract boolean isMatch( String username );

    public String toDatabaseString() {
        // Kinda yucky, but...
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String s : toDatabaseList()) {
            if (i > 0)
                sb.append(UserMatcherConstants.MARKER_SEPERATOR);
            sb.append(s);
            i++;
        }
        return sb.toString();
    }

    /* These lists are typically not modifiable */
    public abstract List<String> toDatabaseList();
}
