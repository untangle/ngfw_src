/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm;

import java.util.List;

import com.untangle.uvm.networking.AccessSettings;
import com.untangle.uvm.networking.AddressSettings;
import com.untangle.uvm.networking.BasicNetworkSettings;
import com.untangle.uvm.networking.IPNetwork;
import com.untangle.uvm.networking.Interface;
import com.untangle.uvm.networking.MiscSettings;
import com.untangle.uvm.networking.NetworkException;
import com.untangle.uvm.networking.NetworkSpacesSettingsImpl;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ValidateException;

public interface RemoteNetworkManager
{
    /**
     * Retrieve the basic network settings
     */
    BasicNetworkSettings getBasicSettings();

    /* Save the basic network settings during the wizard */
    void setSetupSettings(AddressSettings address, BasicNetworkSettings basic)
        throws NetworkException, ValidateException;

    /* Save the basic network settings during the wizard.
     * This can double for refresh because it returns the new, populated network settings.*/
    BasicNetworkSettings setSetupSettings(BasicNetworkSettings basic)
        throws NetworkException, ValidateException;

    /**
     * Retrieve the settings related to limiting access to the box.
     */
    AccessSettings getAccessSettings();

    void setAccessSettings( AccessSettings access );

    /**
     * Retrieve the settings related to the hostname and the address
     * used to access to the box.
     */
    AddressSettings getAddressSettings();

    void setAddressSettings( AddressSettings address );

    /**
     * Retrieve the miscellaneous settings that don't really belong
     * anywhere else.
     */
    MiscSettings getMiscSettings();

    void setMiscSettings( MiscSettings misc );

    /**
     * Retrieve the current network configuration
     */
    NetworkSpacesSettingsImpl getNetworkSettings();

    /**
     * Retrieve the list of interfaces
     * @param updateStatus True if you want to update the interface status.
     */
    List<Interface> getInterfaceList( boolean updateStatus );

    /**
     * Remap the interfaces
     * @param osArray Array of os names (eth0, eth1, etc)
     * @param userArray Array of system names (External, Internal, etc);
     */
    void remapInterfaces( String[] osArray, String[] userArray ) throws NetworkException;

    /* Set the access and address settings, used by the Remote Panel */
    void setSettings( AccessSettings access, AddressSettings address )
        throws NetworkException, ValidateException;

    /* Set the Access, Misc and Network settings at once.  Used by the
     * support panel */
    void setSettings( AccessSettings access, MiscSettings misc )
        throws NetworkException, ValidateException;

    /** Update the internal representation of the address */
    void updateAddress() throws NetworkException;

    /* Get the external HTTPS port */
    int getPublicHttpsPort();

    /* Get the hostname of the box */
    HostName getHostname();

    /* Get the domain name of the box (alpaca domainNameSuffix) */
    HostName getDomainName();

    /* Get the public URL of the box */
    String getPublicAddress();

    /* Allow the setup wizard to setup NAT properly, or disable it. */
    void setWizardNatEnabled(IPaddr address, IPaddr netmask, boolean enableDhcpServer ) 
        throws NetworkException;
    void setWizardNatDisabled() throws NetworkException;

    /* returns a recommendation for the internal network. */
    /* @param externalAddress The external address, if null, this uses
     * the external address of the box. */
    IPNetwork getWizardInternalAddressSuggesstion(IPaddr externalAddress);

    /* Returns true if address is local to the edgeguard */
    boolean isAddressLocal( IPaddr address );

    /* Returns true if single nic mode is enabled */
    boolean isSingleNicModeEnabled();

    /* Returns a list of the physical interfaces on the box. (eth0, eth1, etc). */
    public List<String> getPhysicalInterfaceNames() throws NetworkException;

    /* Forces the link status to be re-examined, since it is likely to
     * have changed */
    void updateLinkStatus();

    public Boolean isQosEnabled();

}
