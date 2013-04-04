/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.List;
import java.net.InetAddress;

import com.untangle.uvm.network.InterfaceStatus;
import com.untangle.uvm.network.DeviceStatus;
import com.untangle.uvm.network.InterfaceSettings;
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

    /* convenience methods */

    List<InterfaceSettings> getEnabledInterfaces();
    
    InetAddress getFirstWanAddress();

    InetAddress getInterfaceHttpAddress( int clientIntf );

    InterfaceSettings findInterfaceId( int interfaceId );

    InterfaceSettings findInterfaceSystemDev( String systemDev );

    InterfaceSettings findInterfaceFirstWan( );

    InterfaceStatus getInterfaceStatus( int interfaceId );

    List<InterfaceStatus> getInterfaceStatus( );
    
    List<DeviceStatus> getDeviceStatus( );
}
