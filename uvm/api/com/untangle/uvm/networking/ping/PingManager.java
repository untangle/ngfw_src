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

package com.untangle.uvm.networking.ping;

import com.untangle.uvm.networking.NetworkException;
import com.untangle.uvm.node.ValidateException;

public interface PingManager
{
    /* Both of the commands take strings, and SHOULD NOT take
     * InetAddresses since the name lookup should be performed on the
     * server side, not on the client side */

    /* Issue a ping and return the results, this is a blocking call. */
    public PingResult ping( String addressString ) throws ValidateException, NetworkException;
    
    /* Issue count pings and return the results, this is a blocking call. */
    public PingResult ping( String addressString, int count ) throws ValidateException, NetworkException;
}
