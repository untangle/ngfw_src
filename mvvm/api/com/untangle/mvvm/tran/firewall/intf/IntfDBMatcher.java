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

package com.untangle.mvvm.tran.firewall.intf;

import java.io.Serializable;

public abstract class IntfDBMatcher implements IntfMatcher, Serializable
{
    /** Package protected so that only classes in the package can add to the list
     * of database saveable intf matchers */
    IntfDBMatcher()
    {
    }

    public abstract boolean isMatch( byte intf );
    public abstract String toDatabaseString();
}
