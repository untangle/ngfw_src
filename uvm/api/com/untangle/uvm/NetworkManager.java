/**
 * $Id$
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
import com.untangle.uvm.networking.NetworkConfigurationListener;
import com.untangle.uvm.node.IPAddress;
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
    void setSetupSettings( AddressSettings address, InterfaceConfiguration settings ) throws Exception;

    /**
     * Save the network settings during the wizard.
     * This can double for refresh because it returns the new, populated network settings.
     */
    InterfaceConfiguration setSetupSettings( InterfaceConfiguration settings ) throws Exception;

    /**
     * Retrieve the settings related to the hostname and the address
     * used to access to the box.
     */
    AddressSettings getAddressSettings();

    void setAddressSettings( AddressSettings address );

    /**
     * Remap the interfaces
     * @param osArray Array of os names (eth0, eth1, etc)
     * @param userArray Array of system names (External, Internal, etc);
     */
    void remapInterfaces( String[] osArray, String[] userArray ) throws Exception;

    /* Set the access and address settings, used by the Remote Panel */
    void setSettings( AddressSettings address ) throws Exception;

    /** Update the internal representation of the address */
    void refreshNetworkConfig();

    /* Get the external HTTPS port */
    int getPublicHttpsPort();

    /* Get the hostname of the box */
    String getHostname();

    /* Get the public URL of the box */
    String getPublicAddress();

    IPAddress getPrimaryAddress();

    /* Allow the setup wizard to setup NAT properly, or disable it. */
    void setWizardNatEnabled(IPAddress address, IPAddress netmask, boolean enableDhcpServer ) throws Exception;

    void setWizardNatDisabled() throws Exception;

    InterfaceConfiguration getWizardWAN();
    
    /* returns a recommendation for the internal network. */
    /* @param externalAddress The external address, if null, this uses
     * the external address of the box. */
    IPNetwork getWizardInternalAddressSuggestion(IPAddress externalAddress);

    /* Forces the link status to be re-examined, since it is likely to
     * have changed */
    void updateLinkStatus();

    Boolean isQosEnabled();

    JSONArray getWANSettings();

    void setWANDownloadBandwidth(String name, int speed);

    void setWANUploadBandwidth(String name, int speed);
    
    void enableQos();

    /**
     * This returns an address where the host on the given interface should be able to access HTTP.
     # if HTTP is not reachable, this returns NULL
     */
    InetAddress getInternalHttpAddress( int clientIntf );

    void registerListener( NetworkConfigurationListener networkListener );

    void unregisterListener( NetworkConfigurationListener networkListener );

    String[] getPossibleInterfaces();

    String[] getWanInterfaces();

}
