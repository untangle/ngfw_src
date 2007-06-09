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

/**
 * An interface to test for particular interfaces.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public interface IntfMatcher
{
    /**
     * Return true if <param>intf</param> matches this matcher.
     *
     * @param intf The interface to test
     * @return True if the <param>intf</param> matches.
     */
    public boolean isMatch( byte intf );

    
    /**
     * Retrieve the database representation of this interface matcher.
     *
     * @return The database representation of this interface matcher.
     */
    public String toDatabaseString();
}
