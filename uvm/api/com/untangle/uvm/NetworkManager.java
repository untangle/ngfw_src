/**
 * $Id$
 */
package com.untangle.uvm;

import java.net.InetAddress;

import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.NetworkSettingsListener;

public interface NetworkManager
{
    /**
     * Get the network settings
     */
    NetworkSettings getNetworkSettings();

    /**
     * Set the network settings
     */
    void setNetworkSettings( NetworkSettings newSettings );

    void registerListener( NetworkSettingsListener networkListener );
    void unregisterListener( NetworkSettingsListener networkListener );

    InetAddress getFirstWanAddress();

    InetAddress getInternalHttpAddress( int clientIntf );
    
}
