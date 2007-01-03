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

package com.untangle.mvvm.networking;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.untangle.mvvm.InterfaceAlias;
import com.untangle.mvvm.IntfConstants;
import com.untangle.mvvm.NetworkingConfiguration;
import com.untangle.mvvm.networking.NetworkUtil;
import com.untangle.mvvm.tran.AddressValidator;
import com.untangle.mvvm.tran.Equivalence;
import com.untangle.mvvm.tran.HostName;
import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.Validatable;
import com.untangle.mvvm.tran.ValidateException;

public class NetworkingConfigurationImpl
    implements Validatable, Serializable, NetworkingConfiguration, Equivalence
{
    // private static final long serialVersionUID = 172494253701617361L;

    public static final HostName  DEFAULT_HOSTNAME = NetworkUtil.DEFAULT_HOSTNAME;

    public static final boolean DEF_IS_DHCP_EN            = false;
    public static final boolean DEF_IS_INSIDE_INSECURE_EN = NetworkUtil.DEF_IS_INSIDE_INSECURE_EN;
    public static final boolean DEF_IS_OUTSIDE_EN         = NetworkUtil.DEF_IS_OUTSIDE_EN;
    public static final boolean DEF_IS_OUTSIDE_RESTRICTED = NetworkUtil.DEF_IS_OUTSIDE_RESTRICTED;
    public static final boolean DEF_IS_SSH_EN             = NetworkUtil.DEF_IS_SSH_EN;
    public static final boolean DEF_IS_EXCEPTION_REPORTING_EN = NetworkUtil.DEF_IS_EXCEPTION_REPORTING_EN;
    public static final boolean DEF_IS_TCP_WIN_EN         = NetworkUtil.DEF_IS_TCP_WIN_EN;
    
    public static final boolean DEF_IS_OUTSIDE_ADMIN_EN   = NetworkUtil.DEF_OUTSIDE_ADMINISTRATION;
    public static final boolean DEF_IS_OUTSIDE_QUARAN_EN  = NetworkUtil.DEF_OUTSIDE_QUARANTINE;
    public static final boolean DEF_IS_OUTSIDE_REPORT_EN  = NetworkUtil.DEF_OUTSIDE_REPORTING;

    /* Post configuration script is empty */
    public static final String DEF_POST_CONFIGURATION = "";

    /* Default custom rules are empty */
    public static final String DEF_CUSTOM_RULES = "";

    /**
     * True if DHCP is enabled
     */
    private boolean isDhcpEnabled = DEF_IS_DHCP_EN;

    public static final int DEF_HTTPS_PORT = NetworkUtil.DEF_HTTPS_PORT;

    /* True if the hostname is publicy resolvable */
    private boolean isHostnamePublic = false;

    /**
     * Hostname, Host and Netmask of the Untangle Server
     */
    private HostName hostname = DEFAULT_HOSTNAME;

    private boolean isPublicAddressEnabled = false;
    private boolean isPublicAddressSetup = false;
    private IPaddr publicIPaddr = null;
    private int publicPort = 0;

    private IPaddr host     = NetworkUtil.EMPTY_IPADDR;
    private IPaddr netmask  = NetworkUtil.EMPTY_IPADDR;

    /**
     * List of aliases for the outside interface.
     */
    private List<InterfaceAlias> aliasList = new LinkedList<InterfaceAlias>();

    /**
     * Default route/gateway for the Untangle Server
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

    /* This is a script that is executed after the rule generator */
    private String customRulesScript = DEF_CUSTOM_RULES;

    private boolean isOutsideAdministrationEnabled = DEF_IS_OUTSIDE_ADMIN_EN;
    private boolean isOutsideQuarantineEnabled = DEF_IS_OUTSIDE_QUARAN_EN;
    private boolean isOutsideReportingEnabled = DEF_IS_OUTSIDE_REPORT_EN;

    /* These are the PPPoE Settings for the external interface */
    private PPPoEConnectionRule pppoeSettings = getDefaultPPPoESettings();

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
    public void hostname( HostName hostname )
    {
        setHostname( hostname );
    }

    /* This is from the interface, this is the non-deprecated method */
    public void setHostname( HostName hostname )
    {
        if ( hostname == null ) hostname = DEFAULT_HOSTNAME;
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

    public HostName hostname()
    {
        return getHostname();
    }

    /* This is from the interface, this is the non-deprecated method */
    public HostName getHostname()
    {
        if ( this.hostname == null ) this.hostname = DEFAULT_HOSTNAME;
        return this.hostname;
    }

    public boolean getIsPublicAddressEnabled()
    {
        return this.isPublicAddressEnabled;
    }

    public void setIsPublicAddressEnabled( boolean newValue )
    {
        this.isPublicAddressEnabled = newValue;
    }

    /** @return the public url for the box, this is the address (may be hostname or ip address) */
    public String getPublicAddress()
    {
        return NetworkUtil.getInstance().generatePublicAddress( getPublicIPaddr(), getPublicPort());
    }

    public void setPublicAddress( String newValue ) throws ParseException
    {
        NetworkUtil.getInstance().parsePublicAddress( this, newValue );
    }

    /** @return the public url for the box, this is the address (may be hostname or ip address) */
    public IPaddr getPublicIPaddr()
    {
        return this.publicIPaddr;
    }

    public void setPublicIPaddr( IPaddr newValue )
    {
        this.publicIPaddr = newValue;
    }

    /** @return the public url for the box, this is the address (may be hostname or ip address) */
    public int getPublicPort()
    {
        return this.publicPort;
    }

    public void setPublicPort( int newValue )
    {
        this.publicPort = newValue;
    }

    /* Return true if the current settings have a public address */
    public boolean hasPublicAddress()
    {
        return (( this.publicIPaddr != null ) &&  !this.publicIPaddr.isEmpty());
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
     * Gateway of the Untangle Server
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

    /* Get the post configuration script */
    public String getCustomRules()
    {
        if ( this.customRulesScript == null ) this.customRulesScript = DEF_CUSTOM_RULES;
        return this.customRulesScript;
    }

    /* XXXX This should be validated */
    public void setCustomRules( String newValue )
    {
        if ( newValue == null ) newValue = DEF_CUSTOM_RULES;
        this.customRulesScript = newValue;
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

    /* Configuration for the web services */
    public boolean getIsOutsideAdministrationEnabled()
    {
        return this.isOutsideAdministrationEnabled;
    }

    public void setIsOutsideAdministrationEnabled( boolean newValue )
    {
        this.isOutsideAdministrationEnabled = newValue;
    }

    public boolean getIsOutsideQuarantineEnabled()
    {
        return this.isOutsideQuarantineEnabled;
    }

    public void setIsOutsideQuarantineEnabled( boolean newValue )
    {
        this.isOutsideQuarantineEnabled = newValue;
    }

    public boolean getIsOutsideReportingEnabled()
    {
        return this.isOutsideReportingEnabled;
    }

    public void setIsOutsideReportingEnabled( boolean newValue )
    {
        this.isOutsideReportingEnabled = newValue;
    }

    /* Get the settings PPPoE settings for the external interface. */
    public PPPoEConnectionRule getPPPoESettings()
    {
        if ( null == this.pppoeSettings ) this.pppoeSettings = getDefaultPPPoESettings();
        return this.pppoeSettings;
    }

    /* Set the settings PPPoE settings for the external interface. */
    public void setPPPoESettings( PPPoEConnectionRule newValue )
    {
        if ( null == newValue ) newValue = getDefaultPPPoESettings();
        this.pppoeSettings = newValue;
    }

    public void validate() throws ValidateException
    {
        /* Check for collisions in the alias list */
        Set<InetAddress> addressSet = new HashSet<InetAddress>();

        AddressValidator av = AddressValidator.getInstance();

        if ( !isDhcpEnabled ) {
            InetAddress defaultRoute = gateway().getAddr();
            InetAddress host = host().getAddr();

            if ( host.equals( defaultRoute )) {
                throw new ValidateException( "The \"Default Route\" and \"IP Address\" are the same." );
            }

            addressSet.add( host );

            if ( av.isIllegalAddress( host )) {
                throw new ValidateException( "\"IP Address\" is invalid." );
            }

            if ( av.isIllegalAddress( defaultRoute )) {
                throw new ValidateException( "\"Default Route\" is invalid." );
            }

            if ( av.isIllegalAddress( dns1.getAddr())) {
                throw new ValidateException( "\"Primary DNS\" is invalid." );
            }

            if (( this.dns2 != null ) && ( !this.dns2.isEmpty()) &&
                av.isIllegalAddress( this.dns2.getAddr())) {
                throw new ValidateException( "\"Secondary DNS\" is invalid." );
            }
        }

        int index = 0;
        for ( InterfaceAlias alias : getAliasList()) {
            index++;

            InetAddress address = alias.getAddress().getAddr();

            if ( av.isIllegalAddress( address )) {
                throw new ValidateException( "\"External Address Alias\" at index " + index + " is invalid." );
            }

            /* Check if the address is already used */
            if ( !addressSet.add( address )) {
                throw new ValidateException( "\"External Address Alias\" " + address.getHostAddress() +
                                             " is duplicated.\n" );
            }

            /* Check if the address is the default route (only if DHCP is disabled) */
            if ( !this.isDhcpEnabled && address.equals( this.gateway().getAddr())) {
                throw new ValidateException( "\"External Address Alias\" " + address.getHostAddress() +
                                             " is both the \"Default Route\" and an alias." );
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

        if (!curNC.host().equals(newNC.host())) {
            return false;
        }

        if (!curNC.netmask().equals(newNC.netmask())) {
            return false;
        }

        if (!curNC.getPostConfigurationScript().equals(newNC.getPostConfigurationScript())) {
            return false;
        }

        if (!curNC.getCustomRules().equals(newNC.getCustomRules())) {
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

        if (curNC.getIsOutsideAdministrationEnabled() != newNC.getIsOutsideAdministrationEnabled()) {
            return false;
        }

        if (curNC.getIsOutsideQuarantineEnabled() != newNC.getIsOutsideQuarantineEnabled()) {
            return false;
        }

        if (curNC.getIsOutsideReportingEnabled() != newNC.getIsOutsideReportingEnabled()) {
            return false;
        }

        return true;
    }

    public String toString()
    {
        /* The networking configuration could be rewritten as the composition of
         * a BasicNetworkSettingsImpl(doesn't exist yet) and a RemoteSettingsImpl.
         * this would make this function a lot shorter */
        StringBuilder sb = new StringBuilder();

        sb.append( "dhcp:        "   + isDhcpEnabled());
        sb.append( "\nhostname:    " + hostname());
        sb.append( "\nhost:        " + host());
        sb.append( "\nnetmask:     " + netmask());
        sb.append( "\ngateway:     " + gateway());
        sb.append( "\ndns 1:       " + dns1());
        sb.append( "\ndns 2:       " + dns2());
        sb.append( "\n aliases:    " + getAliasList());
        sb.append( "\nscript:      " + getPostConfigurationScript());
        sb.append( "\ncustomrules: " + getCustomRules());
        sb.append( "\nssh:         " + isSshEnabled());
        sb.append( "\nexceptions:  " + isExceptionReportingEnabled());
        sb.append( "\ntcp window:  " + isTcpWindowScalingEnabled());
        sb.append( "\ninside in:   " + isInsideInsecureEnabled());
        sb.append( "\noutside:     " + isOutsideAccessEnabled());
        sb.append( "\nrestriced:   " + isOutsideAccessRestricted());
        sb.append( "\nrestriction: " + outsideNetwork() + "/" + outsideNetmask());
        sb.append( "\nHTTPS:       " + httpsPort());
        sb.append( "\nadmin:       " + getIsOutsideAdministrationEnabled());
        sb.append( "\nquarantine:  " + getIsOutsideQuarantineEnabled());
        sb.append( "\nreporting:   " + getIsOutsideReportingEnabled());

        return sb.toString();
    }

    private PPPoEConnectionRule getDefaultPPPoESettings()
    {
        PPPoEConnectionRule rule = new PPPoEConnectionRule();
        rule.setArgonIntf( IntfConstants.EXTERNAL_INTF );
        rule.setLive( false );
        return rule;
    }
}
