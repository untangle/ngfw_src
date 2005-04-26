/*
 * Copyright (c) 2003, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: Argon.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.mvvm;

import java.net.InetAddress;

public interface ArgonManager
{
    void shieldStatus( InetAddress ip, int port );

    void shieldReconfigure();

    /* The box has received a new IP address and must be reconfigured */
    public void updateAddress();

    /* Remove the local antisubscribes, this is really only useful for NAT */
    public void disableLocalAntisubscribe();

    /* Remove the local antisubscribes, this is really only useful for NAT */
    public void enableLocalAntisubscribe();

    /* Turn off DHCP forwarding, this will disallow DHCP requests from outside and vice-versa */
    public void disableDhcpForwarding();

    /* Turn on DHCP forwarding, this will disallow DHCP requests from outside and vice-versa */
    public void enableDhcpForwarding();
}
