/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.List;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.network.InterfaceStatus;
import com.untangle.uvm.network.DeviceStatus;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.generic.InterfaceStatusGeneric;
import com.untangle.uvm.network.generic.NetworkSettingsGeneric;

/**
 * NetworkManager interface
 * documentation in NetworkManagerImpl
 */
public interface NetworkManager
{
    NetworkSettings getNetworkSettings();
    
    NetworkSettingsGeneric getNetworkSettingsV2();

    void setNetworkSettings( NetworkSettings newSettings );

    void setNetworkSettingsV2( NetworkSettingsGeneric newSettingsGen );

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

    List<InterfaceStatusGeneric> getAllInterfacesStatus();

    List<InterfaceStatus> getInterfaceStatus( );

    List<InterfaceStatus> getLocalInterfaceStatuses();
    
    List<DeviceStatus> getDeviceStatus( );

    boolean isVrrpMaster( int interfaceId );

    boolean isWanInterface( int interfaceId );

    boolean isWirelessRegulatoryCompliant( String systemDev );

    JSONArray getWirelessValidRegulatoryCountryCodes( String systemDev );

    String getWirelessRegulatoryCountryCode( String systemDev );

    JSONArray getWirelessChannels( String systemDev, String region );

    String getUpnpManager(String command, String arguments);

    String getPublicUrl();

    String getFullyQualifiedHostname();

    int getNextFreeInterfaceId(NetworkSettings netSettings);

    List<IPMaskedAddress> getLocalNetworks();

    InetAddress getInterfaceAddressForNetwork(String network, int prefixLength);

    String getNetworkSettingsFilename();

    void updateReservedAccessRulePort(String oldPort, String newPort);

    void setInterfacesOverloadedFlag(boolean value);

    boolean getInterfacesOverloadedFlag();

    String getLogFile(String device);

    ConcurrentMap<String, String> lookupMacVendorList(List<String> macAddressList);

    public static enum StatusCommands {
        INTERFACE_TRANSFER,
        INTERFACE_IP_ADDRESSES,
        INTERFACE_ARP_TABLE,
        DYNAMIC_ROUTING_TABLE,
        DYNAMIC_ROUTING_BGP,
        DYNAMIC_ROUTING_OSPF,
        ROUTING_TABLE,
        QOS,
        DHCP_LEASES
    };
    String getStatus(StatusCommands command, String argument);

    public static enum TroubleshootingCommands {
        CONNECTIVITY,
        REACHABLE,
        DNS,
        CONNECTION,
        PATH,
        DOWNLOAD,
        TRACE
    };
    ExecManagerResultReader runTroubleshooting(TroubleshootingCommands command, JSONObject arguments);

}
