/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: NetcapHook.java,v 1.1 2005/01/05 10:17:13 rbscott Exp $
 */

package com.metavize.jnetcap;

public interface NetcapHook {
    /* This the callback that will be called for the UDP/TCP/ICMP hooks */
    public void event( int sessionId );
}
