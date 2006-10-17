/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.networking.ping;

import com.metavize.mvvm.networking.NetworkException;
import com.metavize.mvvm.tran.ValidateException;

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
