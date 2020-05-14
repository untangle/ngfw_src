/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.List;
import java.net.InetAddress;

import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.network.InterfaceStatus;
import com.untangle.uvm.network.DeviceStatus;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.NetworkSettings;

/**
 * NetworkManager interface
 * documentation in NetworkManagerImpl
 */
public interface NetworkManager
{
    NetworkSettings getNetworkSettings();

    void setNetworkSettings( NetworkSettings newSettings );

    void renewDhcpLease( int interfaceId );
    
    List<InterfaceSettings> getEnabledInterfaces();
    
    InetAddress getFirstWanAddress();

    InetAddress getFirstNonWanAddress();

    InetAddress getInterfaceHttpAddress( int clientIntf );

    InetAddress getFirstDnsResolverAddress();

    InterfaceSettings findInterfaceId( int interfaceId );

    InterfaceSettings findInterfaceSystemDev( String systemDev );

    InterfaceSettings findInterfaceFirstWan( );

    InterfaceStatus getInterfaceStatus( int interfaceId );

    List<InterfaceStatus> getInterfaceStatus( );
    
    List<DeviceStatus> getDeviceStatus( );

    boolean isVrrpMaster( int interfaceId );

    boolean isWanInterface( int interfaceId );

    List<Integer> getWirelessChannels( String systemDev );

    String getUpnpManager(String command, String arguments);

    String getPublicUrl();

    String getFullyQualifiedHostname();

    int getNextFreeInterfaceId(NetworkSettings netSettings, int minimum);

    List<IPMaskedAddress> getLocalNetworks();

    String getNetworkSettingsFilename();

}
