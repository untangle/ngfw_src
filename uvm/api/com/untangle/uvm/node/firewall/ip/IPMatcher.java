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

package com.untangle.uvm.node.firewall.ip;

import java.net.InetAddress;

/**
 * An interface to test for an address.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public interface IPMatcher
{
    /**
     * Return true if <param>address</param> matches this matcher.
     *
     * @param address The address to test
     * @return True if the <param>address</param> matches.
     */
    public boolean isMatch( InetAddress address );

    /**
     * Retrieve the database representation of this address matcher.
     *
     * @return The database representation of this address matcher.
     */
    public String toDatabaseString();
}
