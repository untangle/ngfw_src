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

package com.metavize.mvvm.networking;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import java.io.Serializable;

import java.net.Inet4Address;
import java.net.InetAddress;

import com.metavize.mvvm.InterfaceAlias;
import com.metavize.mvvm.NetworkingConfiguration;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.Equivalence;
import com.metavize.mvvm.tran.Validatable;
import com.metavize.mvvm.tran.ValidateException;

import com.metavize.mvvm.networking.NetworkUtil;

public class NetworkingConfigurationImpl implements Serializable, NetworkingConfiguration, Equivalence
{
    // private static final long serialVersionUID = 172494253701617361L;

    public static final String  DEFAULT_HOSTNAME = NetworkUtil.DEFAULT_HOSTNAME;
    
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
    private String publicAddress = null;
    private boolean isHostnamePublic = false;

    private IPaddr host     = NetworkUtil.EMPTY_IPADDR;
    private IPaddr netmask  = NetworkUtil.EMPTY_IPADDR;

    /**
     * List of aliases for the outside interface.
     */
    private List<InterfaceAlias> aliasList = new LinkedList<InterfaceAlias>();

    /**
     * Default route/gateway for the EdgeGuard GSP
     */
    private IPaddr gateway = NetworkUtil.EMPTY_IPADDR;

    /**
     * IP Address of the first DNS server.
     */
    private IPaddr dns1 = NetworkUtil.EMPTY_IPADDR;

    /**
     * IP Address of the second DNS server, may be null.
     */
    private IPaddr dns2 = NetworkUtil.EMPTY_IPADDR;

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

    private IPaddr outsideNetwork = NetworkUtil.DEF_OUTSIDE_NETWORK;
    private IPaddr outsideNetmask = NetworkUtil.DEF_OUTSIDE_NETMASK;
    
    private int httpsPort = DEF_HTTPS_PORT;

    /* This is a script that gets executed after the bridge configuration runs */
    private String postConfigurationScript = DEF_POST_CONFIGURATION;

    public NetworkingConfigurationImpl()
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
     * Set the hostname with a string, this method is deprecated.
     */
    public void hostname( String hostname )
    {
        setHostname( hostname );
    }

    /* This is from the interface, this is the non-deprecated method */
    public void setHostname( String hostname )
    {
        if ( hostname == null || ( hostname.trim().length() == 0 )) hostname = DEFAULT_HOSTNAME;
	// do some shizzle 'n checks here
	this.hostname = hostname;
    }

    /* Returns if the hostname for this box is publicly resolvable to this box */
    public boolean getIsHostnamePublic()
    {
        return this.isHostnamePublic;
    }
    
    public void setIsHostnamePublic( boolean newValue )
    {
        this.isHostnamePublic = newValue;
    }

    public String hostname()
    {
        return getHostname();
    }

    /* This is from the interface, this is the non-deprecated method */
    public String getHostname()
    {
        if ( this.hostname == null || ( this.hostname.trim().length() == 0 )) this.hostname = DEFAULT_HOSTNAME;
        return this.hostname;
    }

    /** @return the public url for the box, this is the address (may be hostname or ip address) */
    public String getPublicAddress()
    {
        return this.publicAddress;
    }

    public void setPublicAddress( String newValue )
    {
        this.publicAddress = newValue;
    }

    /* Return true if the current settings have a public address */
    public boolean hasPublicAddress()
    {
        return (( this.publicAddress == null ) || ( this.publicAddress.length() == 0 ));
    }


    /**
     * Set the host with an IP addr
     */
    public void host( IPaddr host )
    {
        if ( host == null ) host = NetworkUtil.EMPTY_IPADDR;

        this.host = host;
    }

    public IPaddr host()
    {
        if ( this.host == null ) this.host = NetworkUtil.EMPTY_IPADDR;

        return this.host;
    }

    public void netmask( IPaddr netmask )
    {
        if ( netmask == null ) netmask = NetworkUtil.EMPTY_IPADDR;

        this.netmask = netmask;
    }

    public IPaddr netmask()
    {
        if ( this.netmask == null ) this.netmask = NetworkUtil.EMPTY_IPADDR;

        return this.netmask;
    }

    /**
     * Gateway of the EdgeGuard GSP
     */
    public void gateway( IPaddr gateway )
    {
        if ( gateway == null ) gateway = NetworkUtil.EMPTY_IPADDR;

        this.gateway = gateway;
    }

    public IPaddr gateway()
    {
        if ( this.gateway == null ) this.gateway = NetworkUtil.EMPTY_IPADDR;

        return this.gateway;
    }

    public void dns1( IPaddr dns1 ) 
    {
        if ( dns1 == null ) dns1 = NetworkUtil.EMPTY_IPADDR;

        this.dns1 = dns1;
    }

    public IPaddr dns1() 
    {
        if ( this.dns1 == null ) this.dns1 = NetworkUtil.EMPTY_IPADDR;
        
        return this.dns1;
    }

    public void dns2( IPaddr dns2 )
    {
        if ( dns2 == null ) dns2 = NetworkUtil.EMPTY_IPADDR;
        
        this.dns2 = dns2;
    }

    public IPaddr dns2() {
        if ( this.dns2 == null ) this.dns2 = NetworkUtil.EMPTY_IPADDR;

        return this.dns2;
    }

    public boolean hasDns2() 
    {
        return ( this.dns2 != null && !this.dns2.equals( NetworkUtil.EMPTY_IPADDR ));
    }

    /* Retrieve the current list of interface aliases */
    public List<InterfaceAlias> getAliasList()
    {
        if ( this.aliasList == null ) this.aliasList = new LinkedList<InterfaceAlias>();
        return this.aliasList;
    }
         
    /* Set the current list of interface aliaes */
    public void setAliasList( List<InterfaceAlias> aliasList )
    {
        if ( aliasList == null ) aliasList = new LinkedList<InterfaceAlias>();
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
        if ( network == null ) network = NetworkUtil.DEF_OUTSIDE_NETWORK;
            

        this.outsideNetwork = network;
    }

    public IPaddr outsideNetwork()
    {
        if ( this.outsideNetwork == null ) this.outsideNetwork = NetworkUtil.DEF_OUTSIDE_NETWORK;

        return this.outsideNetwork;
    }

    /**
     * Set the network with an IP Maddr
     */
    public void outsideNetmask( IPaddr netmask )
    {
        if ( netmask == null ) netmask = NetworkUtil.DEF_OUTSIDE_NETMASK;

        this.outsideNetmask = netmask;
    }

    public IPaddr outsideNetmask()
    {
        if ( this.outsideNetmask == null ) this.outsideNetmask = NetworkUtil.DEF_OUTSIDE_NETMASK;

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

    @Override
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

    public String toString()
    {
        /* The networking configuration could be rewritten as the composition of 
         * a BasicNetworkSettingsImpl(doesn't exist yet) and a RemoteSettingsImpl.
         * this would make this function a lot shorter */
        return 
            "dhcp:        "   + isDhcpEnabled() +
            "\nhostname:    " + hostname() +
            "\nhost:        " + host() +
            "\nnetmask:     " + netmask() +
            "\ngateway:     " + gateway() +
            "\ndns 1:       " + dns1() +
            "\ndns 2:       " + dns2() +
            "\n aliases:    " + getAliasList() +
            "\nscript:      " + getPostConfigurationScript() +
            "\nssh:         " + isSshEnabled() +
            "\nexceptions:  " + isExceptionReportingEnabled() +
            "\ntcp window:  " + isTcpWindowScalingEnabled() +
            "\ninside in:   " + isInsideInsecureEnabled() +
            "\noutside:     " + isOutsideAccessEnabled() + 
            "\nrestriced:   " + isOutsideAccessRestricted() +
            "\nrestriction: " + outsideNetwork() + "/" + outsideNetmask() +
            "\nHTTPS:       " + httpsPort();

    }

}
