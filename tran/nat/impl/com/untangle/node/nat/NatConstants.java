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

package com.untangle.tran.nat;

import com.untangle.mvvm.tran.Transform;

class NatConstants
{
    static final int BLOCK_COUNTER = Transform.GENERIC_0_COUNTER;
    static final int NAT_COUNTER   = Transform.GENERIC_1_COUNTER;
    static final int REDIR_COUNTER = Transform.GENERIC_2_COUNTER;
    static final int DMZ_COUNTER   = Transform.GENERIC_3_COUNTER;
    
    /* TCP Port range for nat */
    static final int TCP_NAT_PORT_START = 10000;
    static final int TCP_NAT_PORT_END   = 60000;
    
    /* UDP Port range for nat */
    static final int UDP_NAT_PORT_START = 10000;
    static final int UDP_NAT_PORT_END   = 60000;
    
    /* ICMP PID range for nat */
    static final int ICMP_PID_START     = 1;
    static final int ICMP_PID_END       = 60000;
    
    /* Port the server receives data on, probably not the best place for this constant */
    static final int FTP_SERVER_PORT    = 21;
}
