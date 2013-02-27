/**
 * $Id$
 */
package com.untangle.uvm;

import java.net.InetAddress;

import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.NetworkSettingsListener;

/**
 * NetworkManager interface
 * documentation in NetworkManagerImpl
 */
public interface NetworkManager
{
    NetworkSettings getNetworkSettings();

    void setNetworkSettings( NetworkSettings newSettings );

    void registerListener( NetworkSettingsListener networkListener );

    void unregisterListener( NetworkSettingsListener networkListener );

    InetAddress getFirstWanAddress();

    InetAddress getInterfaceHttpAddress( int clientIntf );
}
