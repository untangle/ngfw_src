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

import com.metavize.mvvm.argon.ArgonException;

public interface ArgonManager
{
    void shieldStatus( InetAddress ip, int port );

    void shieldReconfigure();

    /* The box has received a new IP address and must be reconfigured */
    public void updateAddress() throws ArgonException;

    /**
     * Load a networking configuration
     */
    public void loadNetworkingConfiguration( NetworkingConfiguration netConfig ) throws ArgonException;

    /* Break down the bridge, (Only useful for NAT)
     * This will automatically update the iptables rules
     */
    public void destroyBridge( NetworkingConfiguration netConfig, InetAddress internalAddress, 
                               InetAddress internalNetmask ) throws ArgonException;
                               
    
    /* Restore the bridge, (only useful for NAT)
     * This will automatically update the iptables rules
     */
    public void restoreBridge( NetworkingConfiguration netConfig ) throws ArgonException;

    /* !!! None of the following will automatically update the iptables rules,
     * must run generateRules after modifying the settings
     */

    /* XXX This right now is only for outside */
    /* Remove the local antisubscribes, this is really only useful for NAT  */
    public void disableLocalAntisubscribe();

    /* XXX This right now is only for outside */
    /* Remove the local antisubscribes, this is really only useful for NAT */
    public void enableLocalAntisubscribe();

    /* XXX This will be obsolete soon */
    /* Turn off DHCP forwarding, this will disallow DHCP requests from outside and vice-versa */
    public void disableDhcpForwarding();

    /* XXX This will be obsolete soon */
    /* Turn on DHCP forwarding, this will disallow DHCP requests from outside and vice-versa */
    public void enableDhcpForwarding();

    /* Update all of the iptables rules and the internal address database */
    public void generateRules() throws ArgonException;

    /* Get the address of the inside interface */
    public InetAddress getInsideAddress();

    /* Get the netmask of the inside interface */
    public InetAddress getInsideNetmask();
    
    /* Get the address of the outside interface */
    public InetAddress getOutsideAddress();
    
    /* Get the netmask of the outside interface */
    public InetAddress getOutsideNetmask();
}
