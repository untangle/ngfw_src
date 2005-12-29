/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import java.io.Serializable;

import java.net.Inet4Address;
import java.net.InetAddress;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.Equivalence;
import com.metavize.mvvm.tran.Validatable;
import com.metavize.mvvm.tran.ValidateException;

public class NetworkingConfiguration implements Serializable, Validatable, Equivalence
{
    private static final long serialVersionUID = 172494253701617361L;

    public static final String  DEFAULT_HOSTNAME = "edgeguard";
    public static final IPaddr  EMPTY_IPADDR;
    public static final IPaddr  DEF_OUTSIDE_NETWORK;
    public static final IPaddr  DEF_OUTSIDE_NETMASK;

    /* The default empty list of aliases */
    public static final List<InterfaceAlias> DEF_ALIAS_LIST = Collections.emptyList();
    
    public static final boolean DEF_IS_DHCP_EN            = false;
    public static final boolean DEF_IS_INSIDE_INSECURE_EN = true;
    public static final boolean DEF_IS_OUTSIDE_EN         = false;
    public static final boolean DEF_IS_OUTSIDE_RESTRICTED = false;
    public static final boolean DEF_IS_SSH_EN             = false;
    public static final boolean DEF_IS_EXCEPTION_REPORTING_EN = false;
    public static final boolean DEF_IS_TCP_WIN_EN         = false;

    /* Post configuration script is empty */
    public static final String DEF_POST_CONFIGURATION = "";
    
    /**
     * True if DHCP is enabled
     */
    private boolean isDhcpEnabled = DEF_IS_DHCP_EN;

    public static final int DEF_HTTPS_PORT = 443;

    /**
     * Hostname, Host and Netmask of the EdgeGuard GSP
     */
    private String hostname = DEFAULT_HOSTNAME;
    private IPaddr host = EMPTY_IPADDR;
    private IPaddr netmask = EMPTY_IPADDR;

    /**
     * List of aliases for the outside interface.
     */
    private List<InterfaceAlias> aliasList = DEF_ALIAS_LIST;

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
     * True if exception emails are to be emailed
     */
    private boolean isExceptionReportingEnabled = DEF_IS_EXCEPTION_REPORTING_EN;
    
    /**
     * True if TCP Window Scaling is enabled.
     * disabled by default.
     * See: http://oss.sgi.com/archives/netdev/2004-07/msg00121.html or bug 163
     */
    private boolean isTcpWindowScalingEnabled = DEF_IS_TCP_WIN_EN;

    private boolean isInsideInsecureEnabled   = DEF_IS_INSIDE_INSECURE_EN;
    private boolean isOutsideAccessEnabled    = DEF_IS_OUTSIDE_EN;
    private boolean isOutsideAccessRestricted = DEF_IS_OUTSIDE_RESTRICTED;

    private IPaddr outsideNetwork = DEF_OUTSIDE_NETWORK;
    private IPaddr outsideNetmask = DEF_OUTSIDE_NETMASK;
    
    private int httpsPort = DEF_HTTPS_PORT;

    /* This is a script that gets executed after the bridge configuration runs */
    private String postConfigurationScript = DEF_POST_CONFIGURATION;

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
     * Set the hostname with a string
     */
    public void hostname( String hostname )
    {
	// do some shizzle 'n checks here
	this.hostname = hostname;
    }
    public String hostname()
    {
	return hostname;
    }

    /**
     * Set the host with an IP Maddr
     */
    public void host( IPaddr host )
    {
        if ( host == null || host.getAddr() == null ) 
            host = EMPTY_IPADDR;

        this.host = host;
    }

    public IPaddr host()
    {
        if ( this.host == null || this.host.getAddr() == null )
            this.host = EMPTY_IPADDR;

        return this.host;
    }

    public void netmask( IPaddr netmask )
    {
        if ( netmask == null || netmask.getAddr() == null ) 
            netmask = EMPTY_IPADDR;

        this.netmask = netmask;
    }

    public IPaddr netmask()
    {
        if ( this.netmask == null || this.netmask.getAddr() == null )
            this.netmask = EMPTY_IPADDR;

        return this.netmask;
    }

    /**
     * Gateway of the EdgeGuard GSP
     */
    public void gateway( IPaddr gateway )
    {
        if ( gateway == null || gateway.getAddr() == null )
            gateway = EMPTY_IPADDR;

        this.gateway = gateway;
    }

    public IPaddr gateway()
    {
        if ( this.gateway == null || this.gateway.getAddr() == null  )
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

    public boolean hasDns2() 
    {
        return ( this.dns2 != null && !this.dns2.equals( EMPTY_IPADDR ));
    }

    /* Retrieve the current list of interface aliases */
    public List<InterfaceAlias> getAliasList()
    {
        if ( this.aliasList == null ) this.aliasList = DEF_ALIAS_LIST;
        return this.aliasList;
    }
         
    /* Set the current list of interface aliaes */
    public void setAliasList( List<InterfaceAlias> aliasList )
    {
        if ( aliasList == null ) aliasList = DEF_ALIAS_LIST;
        this.aliasList = aliasList;
    }

    /* Set the post configuration script */
    public String getPostConfigurationScript()
    {
        if ( this.postConfigurationScript == null ) this.postConfigurationScript = DEF_POST_CONFIGURATION;
        return this.postConfigurationScript;
    }
    
    /* XXXX This should be validated */
    public void setPostConfigurationScript( String script )
    {
        if ( script == null ) script = DEF_POST_CONFIGURATION;
        this.postConfigurationScript = script;
    }

    public boolean isSshEnabled()
    {
        return this.isSshEnabled;
    }

    public void isSshEnabled( boolean isEnabled ) 
    {
        this.isSshEnabled = isEnabled;
    }

    public boolean isExceptionReportingEnabled()
    {
        return this.isExceptionReportingEnabled;
    }
    
    public void isExceptionReportingEnabled( boolean isEnabled )
    {
        this.isExceptionReportingEnabled = isEnabled;
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
            network = DEF_OUTSIDE_NETWORK;

        this.outsideNetwork = network;
    }

    public IPaddr outsideNetwork()
    {
        if ( this.outsideNetwork == null ) 
            this.outsideNetwork = DEF_OUTSIDE_NETWORK;

        return this.outsideNetwork;
    }

    /**
     * Set the network with an IP Maddr
     */
    public void outsideNetmask( IPaddr netmask )
    {
        if ( netmask == null ) 
            netmask = DEF_OUTSIDE_NETMASK;

        this.outsideNetmask = netmask;
    }

    public IPaddr outsideNetmask()
    {
        if ( this.outsideNetmask == null ) 
            this.outsideNetmask = DEF_OUTSIDE_NETMASK;

        return this.outsideNetmask;
    }

    public int httpsPort()
    {
        /* Make sure it is a valid port */
        if ( this.httpsPort == 0 || this.httpsPort > 0xFFFF || httpsPort == 80 ) {
            this.httpsPort = DEF_HTTPS_PORT;
        }

        return this.httpsPort;
    }

    public void httpsPort( int httpsPort )
    {
        /* Make sure that it is a valid port */
        if ( httpsPort == 0 || httpsPort > 0xFFFF || httpsPort == 80 ) httpsPort = DEF_HTTPS_PORT;
        
        this.httpsPort = httpsPort;
    }

    
    
    public void validate() throws ValidateException
    {
        /* Check for collisions in the alias list */
        Set<InetAddress> addressSet = new HashSet<InetAddress>();

        InetAddress defaultRoute = gateway().getAddr();
        InetAddress host = host().getAddr();

        if ( host.equals( defaultRoute )) {
            throw new ValidateException( "The \"Default Route\" and \"IP Address\" are the same." );
        }

        addressSet.add( host );
        
        for ( InterfaceAlias alias : getAliasList()) {
            InetAddress address = alias.getAddress().getAddr();
            
            /* Check if the address is already used */
            if ( !addressSet.add( address )) {
                throw new ValidateException( "The address " + address.getHostAddress() + 
                                             " is duplicated.\n" );
            }

            /* Check if the address is the default route */
            if ( address.equals( defaultRoute )) {
                throw new ValidateException( "The address " + address.getHostAddress() +
                                             " is the \"Default Route\" and an alias." );
            }
        }
    }

    public boolean equals(Object newObject)
    {
        if (null == newObject ||
            false == (newObject instanceof NetworkingConfiguration)) {
            return false;
        }

        NetworkingConfiguration newNC = (NetworkingConfiguration) newObject;
        NetworkingConfiguration curNC = this;

        if (curNC.isDhcpEnabled() != newNC.isDhcpEnabled()) {
            return false;
        }

        if (false == curNC.host().equals(newNC.host())) {
            return false;
        }

        if (false == curNC.netmask().equals(newNC.netmask())) {
            return false;
        }

        if ( false == curNC.getPostConfigurationScript().equals(newNC.getPostConfigurationScript())) {
            return false;
        }

        if ( curNC.httpsPort() != newNC.httpsPort()) {
            return false;
        }

        /* we assume that current and new NetworkingConfigurations are valid
         * (e.g., there are no duplicate InterfaceAliases in either)
         * before we check them for equivalence
         */
        List<InterfaceAlias> curIAL = curNC.getAliasList();
        List<InterfaceAlias> newIAL = newNC.getAliasList();
        if (curIAL.size() != newIAL.size()) {
            return false;
        }

        ArrayList<InterfaceAlias> unmatchedIAL = new ArrayList(newIAL);

        boolean bMatched;

        for (InterfaceAlias curIA : curIAL) {
            bMatched = false;

            for (InterfaceAlias newIA : unmatchedIAL) {
                if (true == curIA.equals(newIA)) {
                    bMatched = true;
                    unmatchedIAL.remove(newIA);
                    break; // for newIA loop
                }
            }

            if (false == bMatched) {
                // current InterfaceAlias not found in new InterfaceAliases
                return false;
            }
        }

        if (false == curNC.gateway().equals(newNC.gateway())) {
            return false;
        }

        if (false == curNC.dns1().equals(newNC.dns1())) {
            return false;
        }

        if (false == curNC.dns2().equals(newNC.dns2())) {
            return false;
        }

        if (curNC.isSshEnabled() != newNC.isSshEnabled()) {
            return false;
        }

        if (curNC.isExceptionReportingEnabled() != newNC.isExceptionReportingEnabled()) {
            return false;
        }

        if (curNC.isTcpWindowScalingEnabled() != newNC.isTcpWindowScalingEnabled()) {
            return false;
        }

        if (curNC.isInsideInsecureEnabled() != newNC.isInsideInsecureEnabled()) {
            return false;
        }

        if (curNC.isOutsideAccessEnabled() != newNC.isOutsideAccessEnabled()) {
            return false;
        }

        if (curNC.isOutsideAccessRestricted() != newNC.isOutsideAccessRestricted()) {
            return false;
        }

        if (false == curNC.outsideNetwork().equals(newNC.outsideNetwork())) {
            return false;
        }

        if (false == curNC.outsideNetmask().equals(newNC.outsideNetmask())) {
            return false;
        }

        return true;
    }

    static
    {
        Inet4Address emptyAddr      = null;
        Inet4Address outsideNetwork = null;
        Inet4Address outsideNetmask = null;

        try {
            emptyAddr = (Inet4Address)InetAddress.getByName( "0.0.0.0" );
            outsideNetwork = (Inet4Address)InetAddress.getByName( "1.2.3.4" );
            outsideNetmask = (Inet4Address)InetAddress.getByName( "255.255.255.0" );
        } catch( Exception e ) {
            System.err.println( "this should never happen: " + e );
            /* THIS SHOULD NEVER HAPPEN */
        }

        EMPTY_IPADDR = new IPaddr( emptyAddr );
        DEF_OUTSIDE_NETWORK = new IPaddr( outsideNetwork );
        DEF_OUTSIDE_NETMASK = new IPaddr( outsideNetmask );
    }
}
