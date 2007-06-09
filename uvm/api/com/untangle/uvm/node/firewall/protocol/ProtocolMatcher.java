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

package com.untangle.uvm.node.firewall.protocol;

import com.untangle.uvm.tapi.Protocol;

/**
 * An interface to test for an address.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public interface ProtocolMatcher
{
    /**
     * Return true if <param>protocol</param> matches this matcher.
     *
     * @param protocol The protocol to test
     * @return True if the <param>protocol</param> matches.
     */
    public boolean isMatch( Protocol protocol );

    /**
     * Return true if <param>protocol</param> matches this matcher.
     *
     * @param protocol The protocol to test
     * @return True if the <param>protocol</param> matches.
     */
    public boolean isMatch( short protocol );

    /**
     * Retrieve the database representation of this protocol matcher.
     *
     * @return The database representation of this protocol matcher.
     */
    public String toDatabaseString();
}
