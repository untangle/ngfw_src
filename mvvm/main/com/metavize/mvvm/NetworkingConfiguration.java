/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: NetworkingConfiguration.java,v 1.11 2005/03/09 07:00:10 rbscott Exp $
 */

package com.metavize.mvvm;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;

import com.metavize.mvvm.tran.IPaddr;

public class NetworkingConfiguration implements Serializable
{
    private static final long serialVersionUID = -8235095115996174553L;

    public static final IPaddr  EMPTY_IPADDR;

    public static final boolean DEF_IS_DHCP_EN            = false;
    public static final boolean DEF_IS_INSIDE_INSECURE_EN = true;
    public static final boolean DEF_IS_OUTSIDE_EN         = false;
    public static final boolean DEF_IS_OUTSIDE_RESTRICTED = false;
    public static final boolean DEF_IS_SSH_EN             = false;
    public static final boolean DEF_IS_TCP_WIN_EN         = false;

    /**
     * True if DHCP is enabled
     */
    private boolean isDhcpEnabled = DEF_IS_DHCP_EN;

    /**
     * Host and Netmask of the EdgeGuard GSP
     */
    private IPaddr host = EMPTY_IPADDR;
    private IPaddr netmask = EMPTY_IPADDR;

    /**
     * Default route/gateway for the EdgeGuard GSP
     */
    private IPaddr gateway = EMPTY_IPADDR;

    /**
     * IP Address of the first DNS server.
     */
    private IPaddr dns1 = EMPTY_IPADDR;

    /**
     * IP Address of the second DNS server, may be null.
     */
    private IPaddr dns2 = EMPTY_IPADDR;

    /**
     * True if SSH remote debugging is enabled.
     */
    private boolean isSshEnabled = DEF_IS_SSH_EN;

    /**
     * True if TCP Window Scaling is enabled.
     * disabled by default.
     * See: http://oss.sgi.com/archives/netdev/2004-07/msg00121.html or bug 163
     */
    private boolean isTcpWindowScalingEnabled = DEF_IS_TCP_WIN_EN;

    private boolean isInsideInsecureEnabled   = DEF_IS_INSIDE_INSECURE_EN;
    private boolean isOutsideAccessEnabled    = DEF_IS_OUTSIDE_EN;
    private boolean isOutsideAccessRestricted = DEF_IS_OUTSIDE_RESTRICTED;


    private IPaddr outsideNetwork;
    private IPaddr outsideNetmask;

    public NetworkingConfiguration()
    {
    }

    public void isDhcpEnabled( boolean isEnabled )
    {
        this.isDhcpEnabled = isEnabled;
    }

    public boolean isDhcpEnabled()
    {
        return isDhcpEnabled;
    }

    /**
     * Set the host with an IP Maddr
     */
    public void host( IPaddr host )
    {
        if ( host == null ) 
            host = EMPTY_IPADDR;

        this.host = host;
    }

    public IPaddr host()
    {
        if ( this.host == null )
            this.host = EMPTY_IPADDR;

        return this.host;
    }

    public void netmask( IPaddr netmask )
    {
        if ( netmask == null ) 
            netmask = EMPTY_IPADDR;

        this.netmask = netmask;
    }

    public IPaddr netmask()
    {
        if ( this.netmask == null )
            this.netmask = EMPTY_IPADDR;

        return this.netmask;
    }

    /**
     * Gateway of the EdgeGuard GSP
     */
    public void gateway( IPaddr gateway )
    {
        if ( gateway == null )
            gateway = EMPTY_IPADDR;

        this.gateway = gateway;
    }

    public IPaddr gateway()
    {
        if ( this.gateway == null )
            this.gateway = EMPTY_IPADDR;

        return this.gateway;
    }

    public void dns1( IPaddr dns1 ) 
    {
        if ( dns1 == null )
            dns1 = EMPTY_IPADDR;

        this.dns1 = dns1;
    }

    public IPaddr dns1() 
    {
        if ( this.dns1 == null )
            this.dns1 = EMPTY_IPADDR;

        return this.dns1;
    }

    public void dns2( IPaddr dns2 )
    {
        if ( dns2 == null )
            dns2 = EMPTY_IPADDR;

        this.dns2 = dns2;
    }

    public IPaddr dns2() {
        if ( this.dns2 == null )
            this.dns2 = EMPTY_IPADDR;

        return this.dns2;
    }

    public boolean isSshEnabled()
    {
        return this.isSshEnabled;
    }

    public void isSshEnabled( boolean isEnabled ) 
    {
        this.isSshEnabled = isEnabled;
    }

    public void isTcpWindowScalingEnabled( boolean isEnabled )
    {
        this.isTcpWindowScalingEnabled = isEnabled;
    }

    public boolean isTcpWindowScalingEnabled()
    {
        return isTcpWindowScalingEnabled;
    }

    /**
     * True if insecure access from the inside is enabled.
     */
    public void isInsideInsecureEnabled( boolean isEnabled )
    {
        this.isInsideInsecureEnabled = isEnabled;
    }

    public boolean isInsideInsecureEnabled()
    {
        return isInsideInsecureEnabled;
    }

    /**
     * True if outside (secure) access is enabled.
     */
    public void isOutsideAccessEnabled( boolean isEnabled )
    {
        this.isOutsideAccessEnabled = isEnabled;
    }

    public boolean isOutsideAccessEnabled()
    {
        return isOutsideAccessEnabled;
    }

    /**
     * True if outside (secure) access is restricted.
     */
    public void isOutsideAccessRestricted( boolean isRestricted )
    {
        this.isOutsideAccessRestricted = isRestricted;
    }

    public boolean isOutsideAccessRestricted()
    {
        return isOutsideAccessRestricted;
    }

    /**
     * The netmask of the network/host that is allowed to administer the box from outside
     * This is ignored if outside access is not enabled, null for just
     * one host.
     */

    /**
     * Set the network with an IP Maddr
     */
    public void outsideNetwork( IPaddr network )
    {
        if ( network == null ) 
            network = EMPTY_IPADDR;

        this.outsideNetwork = network;
    }

    public IPaddr outsideNetwork()
    {
        if ( this.outsideNetwork == null ) 
            this.outsideNetwork = EMPTY_IPADDR;

        return this.outsideNetwork;
    }

    /**
     * Set the network with an IP Maddr
     */
    public void outsideNetmask( IPaddr netmask )
    {
        if ( netmask == null ) 
            netmask = EMPTY_IPADDR;

        this.outsideNetmask = netmask;
    }

    public IPaddr outsideNetmask()
    {
        if ( this.outsideNetmask == null ) 
            this.outsideNetmask = EMPTY_IPADDR;

        return this.outsideNetmask;
    }

    static {
        Inet4Address emptyAddr = null;

        try {
            emptyAddr = (Inet4Address)InetAddress.getByName( "0.0.0.0" );
        } catch( Exception ex ) {
            /* THIS SHOULD NEVER HAPPEN */
        }

        EMPTY_IPADDR = new IPaddr( emptyAddr );
    }
}
