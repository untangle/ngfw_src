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

package com.untangle.uvm.node.firewall.port;

/**
 * An interface to test for an port.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public interface PortMatcher
{
    /**
     * Return true if <param>port</param> matches this matcher.
     *
     * @param port The interface to test
     * @return True if the <param>port</param> matches.
     */
    public boolean isMatch( int port );

    /**
     * Retrieve the database representation of this address matcher.
     * The following must be true:
     * PortMatcherFactory pmf = PortMatcherFactory.getInstance();
     * PortMatcher pm = pmf.parse( value );
     * pmf.parse( pm.toDatabaseString()) must equal pm.
     *
     * @return The database representation of this address matcher.
     */
    public String toDatabaseString();
}
