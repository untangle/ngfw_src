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

package com.untangle.jnetcap;

public interface NetcapHook {
    /* This the callback that will be called for the UDP/TCP/ICMP hooks */
    public void event( int sessionId );
}
