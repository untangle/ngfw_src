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

package com.untangle.uvm;

import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;

import com.untangle.uvm.networking.AccessSettings;
import com.untangle.uvm.networking.AddressSettings;
import com.untangle.uvm.networking.BasicNetworkSettings;
import com.untangle.uvm.networking.DhcpStatus;
import com.untangle.uvm.networking.DynamicDNSSettings;
import com.untangle.uvm.networking.MiscSettings;
import com.untangle.uvm.networking.NetworkException;
import com.untangle.uvm.networking.NetworkSpacesSettings;
import com.untangle.uvm.networking.NetworkSpacesSettingsImpl;

import com.untangle.uvm.node.ValidateException;


/* XXXXXXXXX This should be renamed to RemoteNetworkManager */
public interface NetworkManager
{
    /**
     * Retrieve the basic network settings
     */
    public BasicNetworkSettings getBasicSettings();

    /* Save the basic network settings */
    public void setBasicSettings( BasicNetworkSettings basic ) 
        throws NetworkException, ValidateException;

    /* Save the basic network settings during the wizard */
    public void setSetupSettings( AddressSettings address, BasicNetworkSettings basic )
        throws NetworkException, ValidateException;
        
    /**
     * Retrieve the settings related to limiting access to the box.
     */
    public AccessSettings getAccessSettings();
    
    public void setAccessSettings( AccessSettings access );

    /**
     * Retrieve the settings related to the hostname and the address used to access to the box.
     */
    public AddressSettings getAddressSettings();
    
    public void setAddressSettings( AddressSettings address );

    /**
     * Retrieve the miscellaneous settings that don't really belong anywhere else.
     */
    public MiscSettings getMiscSettings();
    
    public void setMiscSettings( MiscSettings misc );

    /**
     * Retrieve the current network configuration
     */
    public NetworkSpacesSettingsImpl getNetworkSettings();
    
    public void setNetworkSettings( NetworkSpacesSettings networkSettings ) 
        throws NetworkException, ValidateException;

    /* Set the network settings and the address settings at once, used
     * by the networking panel */
    public void setSettings( BasicNetworkSettings basic, AddressSettings address )
        throws NetworkException, ValidateException;

    /* Set the access and address settings, used by the Remote Panel */
    public void setSettings( AccessSettings access, AddressSettings address )
        throws NetworkException, ValidateException;

    /* Set the Access, Misc and Network settings at once.  Used by the
     * support panel */
    public void setSettings( AccessSettings access, MiscSettings misc, NetworkSpacesSettings network )
        throws NetworkException, ValidateException;

    /** Update the internal representation of the address */
    public void updateAddress() throws NetworkException;

    /* Helper function for running some pppoe stuff */
    public void pppoe( String args[] ) throws NetworkException;

    /* Get the current dynamic dns settings */
    public DynamicDNSSettings getDynamicDnsSettings();

    /* Set the dynamic dns settings */
    public void setDynamicDnsSettings( DynamicDNSSettings newValue );

    /** This should require an interface list to block */
    public void disableDhcpForwarding();
    public void enableDhcpForwarding();

    public void subscribeLocalOutside( boolean newValue );
    
    /* Renew the DHCP address and return a new network settings with the updated address */
    public BasicNetworkSettings renewDhcpLease() throws NetworkException;
    
    /* Get the external HTTPS port */
    public int getPublicHttpsPort();

    /* Get the hostname of the box */
    public HostName getHostname();

    /* Get the public URL of the box */
    public String getPublicAddress();

    /* Allow the setup wizard to setup NAT properly, or disable it. */
    public void setWizardNatEnabled(IPaddr address, IPaddr netmask);
    public void setWizardNatDisabled();

    /* Returns true if address is local to the edgeguard */
    public boolean isAddressLocal( IPaddr address );

    /* Forces the link status to be re-examined, since it is likely to have changed */
    public void updateLinkStatus();
}
