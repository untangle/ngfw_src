/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.jnetcap;

import java.net.InetAddress;

public interface ShieldEventListener
{
    /** 
     * ip:         Ip that was limited, rejected or dropped
     * reputation: Current reputation of the IP.
     * mode:       Mode was in at the time of the rejection.
     * limited:    Number of limited responses since the last event.
     * rejected:   Number of rejected responses since the last event.
     * dropped:    Number of dropped sessions since the last event.
     */
    void event( InetAddress ip, double reputation, int mode, int limited, int rejected, int dropped );
}
