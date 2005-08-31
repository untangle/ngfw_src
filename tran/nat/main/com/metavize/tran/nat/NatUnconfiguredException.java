/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat;


/* Just used to indicate that the outside interface is not configured, it happens in one very well defined
 * case, hence it doesn't need a message 
 */
class NatUnconfiguredException extends Exception {
    private static NatUnconfiguredException INSTANCE = new NatUnconfiguredException();
    
    NatUnconfiguredException()
    {
        super();
    }

    static NatUnconfiguredException getInstance() {
        return INSTANCE;
    }
}

