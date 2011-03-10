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
import java.net.InetAddress;
import org.json.JSONArray;

import com.untangle.uvm.networking.AccessSettings;
import com.untangle.uvm.networking.AddressSettings;
import com.untangle.uvm.networking.NetworkConfiguration;
import com.untangle.uvm.networking.InterfaceConfiguration;
import com.untangle.uvm.networking.IPNetwork;
import com.untangle.uvm.networking.MiscSettings;
import com.untangle.uvm.networking.NetworkConfigurationListener;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ValidateException;
import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.NetworkManager;

public interface NetworkManager
{
    /**
     * Retrieve the network settings
     */
    NetworkConfiguration getNetworkConfiguration();

    /**
     * Save the network settings during the wizard
     */
    void setSetupSettings( AddressSettings address, InterfaceConfiguration settings )
        throws Exception, ValidateException;

    /**
     * Save the network settings during the wizard.
     * This can double for refresh because it returns the new, populated network settings.
     */
    InterfaceConfiguration setSetupSettings( InterfaceConfiguration settings )
        throws Exception, ValidateException;

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
     * Remap the interfaces
     * @param osArray Array of os names (eth0, eth1, etc)
     * @param userArray Array of system names (External, Internal, etc);
     */
    void remapInterfaces( String[] osArray, String[] userArray ) throws Exception;

    /* Set the access and address settings, used by the Remote Panel */
    void setSettings( AccessSettings access, AddressSettings address )
        throws Exception, ValidateException;

    /* Set the Access, Misc and Network settings at once.  Used by the
     * support panel */
    void setSettings( AccessSettings access, MiscSettings misc )
        throws Exception, ValidateException;

    /** Update the internal representation of the address */
    void refreshNetworkConfig() throws Exception;

    /* Get the external HTTPS port */
    int getPublicHttpsPort();

    /* Get the hostname of the box */
    HostName getHostname();

    /* Get the public URL of the box */
    String getPublicAddress();

    IPAddress getPrimaryAddress();

    /* Allow the setup wizard to setup NAT properly, or disable it. */
    void setWizardNatEnabled(IPAddress address, IPAddress netmask, boolean enableDhcpServer ) 
        throws Exception;

    void setWizardNatDisabled()
        throws Exception;

    public InterfaceConfiguration getWizardWAN();
    
    /* returns a recommendation for the internal network. */
    /* @param externalAddress The external address, if null, this uses
     * the external address of the box. */
    IPNetwork getWizardInternalAddressSuggestion(IPAddress externalAddress);

    /* Forces the link status to be re-examined, since it is likely to
     * have changed */
    void updateLinkStatus();

    public Boolean isQosEnabled();

    public JSONArray getWANSettings();

    public void setWANDownloadBandwidth(String name, int speed);

    public void setWANUploadBandwidth(String name, int speed);
    
    public void enableQos();

    /**
     * Register a service that needs outside access to HTTPs, the name
     * should be unique
     */
    void registerService( String name );

    /**
     * Remove a service that needs outside access to HTTPs, the name
     * should be unique
     */
    void unregisterService( String name );

    /**
     * This returns an address where the host should be able to access
     * HTTP.  if HTTP is not reachable, this returns NULL
     */
    InetAddress getInternalHttpAddress( IPSessionDesc session );

    void registerListener( NetworkConfigurationListener networkListener );

    void unregisterListener( NetworkConfigurationListener networkListener );

    void refreshIptablesRules();

    String[] getPossibleInterfaces();
}
