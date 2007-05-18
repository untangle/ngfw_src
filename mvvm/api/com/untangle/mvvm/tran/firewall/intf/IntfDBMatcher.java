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

/**
 * An IntfMatcher that is capable of being saved to the database.
 * Done as an abstract class so only classes in this package can
 * implement Database Matchers.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
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
