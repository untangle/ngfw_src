/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.io.Serializable;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.network.PortForwardRule;
import com.untangle.uvm.network.NatRule;
import com.untangle.uvm.network.BypassRule;
import com.untangle.uvm.network.StaticRoute;

/**
 * Network settings.
 */
@SuppressWarnings("serial")
public class NetworkSettings implements Serializable, JSONString
{
    public static final String PUBLIC_URL_EXTERNAL_IP = "external";
    public static final String PUBLIC_URL_HOSTNAME = "hostname";
    public static final String PUBLIC_URL_ADDRESS_AND_PORT = "address_and_port";

    private Integer version;

    private List<InterfaceSettings> interfaces = null;
    private List<InterfaceSettings> virtualInterfaces = null;
    private List<DeviceSettings> devices = null;
    private List<PortForwardRule> portForwardRules = null;
    private List<NatRule> natRules = null;
    private List<BypassRule> bypassRules = null;
    private List<StaticRoute> staticRoutes = null;
    private List<DhcpStaticEntry> staticDhcpEntries = null;
    private List<FilterRule> accessRules = null;
    private List<FilterRule> filterRules = null;

    private String hostName;
    private String domainName;

    private boolean dynamicDnsServiceEnabled = false;
    private String  dynamicDnsServiceName = null;
    private String  dynamicDnsServiceUsername = null;
    private String  dynamicDnsServicePassword = null;
    private String  dynamicDnsServiceHostnames = null;

    private boolean enableSipNatHelper = false;
    private boolean sendIcmpRedirects = true;
    private boolean blockInvalidPackets = true;
    private boolean blockReplayPackets = false;
    private boolean strictArpMode = true;
    private boolean stpEnabled = false;
    private boolean dhcpAuthoritative = true;
    private boolean blockDuringRestarts = false;
    private boolean logBypassedSessions = true;
    private boolean logLocalOutboundSessions = true;
    private boolean logLocalInboundSessions = false;
    private boolean logBlockedSessions = false;
    private boolean vlansEnabled = true;
    private int     lxcInterfaceId = 0;

    private int httpPort  = 80;
    private int httpsPort = 443;

    private QosSettings qosSettings;
    private UpnpSettings upnpSettings;
    private DnsSettings dnsSettings;
    private NetflowSettings netflowSettings;
    private DynamicRoutingSettings dynamicRoutingSettings;

    private String dnsmasqOptions;

    private String  publicUrlMethod;
    private String  publicUrlAddress;
    private Integer publicUrlPort;

    private boolean globalCustomBlockPageEnabled = false;
    private String globalCustomBlockPageUrl = null;

    public NetworkSettings() { }

    public Integer getVersion() { return this.version; }
    public void setVersion( Integer newValue ) { this.version = newValue ; }

    public List<InterfaceSettings> getInterfaces() { return this.interfaces; }
    public void setInterfaces( List<InterfaceSettings> newValue ) { this.interfaces = newValue; }

    public List<InterfaceSettings> getVirtualInterfaces() { return this.virtualInterfaces; }
    public void setVirtualInterfaces( List<InterfaceSettings> newValue ) { this.virtualInterfaces = newValue; }

    public List<DeviceSettings> getDevices() { return this.devices; }
    public void setDevices( List<DeviceSettings> newValue ) { this.devices = newValue; }

    public List<PortForwardRule> getPortForwardRules() { return this.portForwardRules; }
    public void setPortForwardRules( List<PortForwardRule> newValue ) { this.portForwardRules = newValue; }

    public List<NatRule> getNatRules() { return this.natRules; }
    public void setNatRules( List<NatRule> newValue ) { this.natRules = newValue; }

    public List<BypassRule> getBypassRules() { return this.bypassRules; }
    public void setBypassRules( List<BypassRule> newValue ) { this.bypassRules = newValue; }

    public List<FilterRule> getFilterRules() { return this.filterRules; }
    public void setFilterRules( List<FilterRule> newValue ) { this.filterRules = newValue; }

    public List<FilterRule> getAccessRules() { return this.accessRules; }
    public void setAccessRules( List<FilterRule> newValue ) { this.accessRules = newValue; }

    public List<StaticRoute> getStaticRoutes() { return this.staticRoutes; }
    public void setStaticRoutes( List<StaticRoute> newValue ) { this.staticRoutes = newValue; }

    public List<DhcpStaticEntry> getStaticDhcpEntries() { return this.staticDhcpEntries; }
    public void setStaticDhcpEntries( List<DhcpStaticEntry> newValue ) { this.staticDhcpEntries = newValue; }

    public String getHostName() { return this.hostName; }
    public void setHostName( String newValue ) { this.hostName = newValue; }

    public String getDomainName() { return this.domainName; }
    public void setDomainName( String newValue ) { this.domainName = newValue; }

    public boolean getDynamicDnsServiceEnabled() { return this.dynamicDnsServiceEnabled; }
    public void setDynamicDnsServiceEnabled( boolean newValue ) { this.dynamicDnsServiceEnabled = newValue; }

    public String getDynamicDnsServiceName() { return this.dynamicDnsServiceName; }
    public void setDynamicDnsServiceName( String newValue ) { this.dynamicDnsServiceName = newValue; }

    public String getDynamicDnsServiceUsername() { return this.dynamicDnsServiceUsername; }
    public void setDynamicDnsServiceUsername( String newValue ) { this.dynamicDnsServiceUsername = newValue; }

    public String getDynamicDnsServicePassword() { return this.dynamicDnsServicePassword; }
    public void setDynamicDnsServicePassword( String newValue ) { this.dynamicDnsServicePassword = newValue; }

    public String getDynamicDnsServiceHostnames() { return this.dynamicDnsServiceHostnames; }
    public void setDynamicDnsServiceHostnames( String newValue ) { this.dynamicDnsServiceHostnames = newValue; }

    public int getHttpsPort() { return this.httpsPort; }
    public void setHttpsPort( int newValue ) { this.httpsPort = newValue ; }

    public int getHttpPort() { return this.httpPort; }
    public void setHttpPort( int newValue ) { this.httpPort = newValue ; }

    public boolean getEnableSipNatHelper() { return this.enableSipNatHelper; }
    public void setEnableSipNatHelper( boolean newValue ) { this.enableSipNatHelper = newValue; }

    public boolean getSendIcmpRedirects() { return this.sendIcmpRedirects; }
    public void setSendIcmpRedirects( boolean newValue ) { this.sendIcmpRedirects = newValue; }

    public boolean getBlockInvalidPackets() { return this.blockInvalidPackets; }
    public void setBlockInvalidPackets( boolean newValue ) { this.blockInvalidPackets = newValue; }

    public boolean getBlockReplayPackets() { return this.blockReplayPackets; }
    public void setBlockReplayPackets( boolean newValue ) { this.blockReplayPackets = newValue; }

    public boolean getStrictArpMode() { return this.strictArpMode; }
    public void setStrictArpMode( boolean newValue ) { this.strictArpMode = newValue; }

    public boolean getStpEnabled() { return this.stpEnabled; }
    public void setStpEnabled( boolean newValue ) { this.stpEnabled = newValue; }

    public boolean getDhcpAuthoritative() { return this.dhcpAuthoritative; }
    public void setDhcpAuthoritative( boolean newValue ) { this.dhcpAuthoritative = newValue; }

    public boolean getBlockDuringRestarts() { return this.blockDuringRestarts; }
    public void setBlockDuringRestarts( boolean newValue ) { this.blockDuringRestarts = newValue; }

    public boolean getLogBypassedSessions() { return this.logBypassedSessions; }
    public void setLogBypassedSessions( boolean newValue ) { this.logBypassedSessions = newValue; }

    public boolean getLogLocalOutboundSessions() { return this.logLocalOutboundSessions; }
    public void setLogLocalOutboundSessions( boolean newValue ) { this.logLocalOutboundSessions = newValue; }

    public boolean getLogLocalInboundSessions() { return this.logLocalInboundSessions; }
    public void setLogLocalInboundSessions( boolean newValue ) { this.logLocalInboundSessions = newValue; }

    public boolean getLogBlockedSessions() { return this.logBlockedSessions; }
    public void setLogBlockedSessions( boolean newValue ) { this.logBlockedSessions = newValue; }

    public QosSettings getQosSettings() { return this.qosSettings; }
    public void setQosSettings( QosSettings newValue ) { this.qosSettings = newValue; }

    public UpnpSettings getUpnpSettings() { return this.upnpSettings; }
    public void setUpnpSettings( UpnpSettings newValue ) { this.upnpSettings = newValue; }

    public DnsSettings getDnsSettings() { return this.dnsSettings; }
    public void setDnsSettings( DnsSettings newValue ) { this.dnsSettings = newValue; }

    public NetflowSettings getNetflowSettings() { return this.netflowSettings; }
    public void setNetflowSettings( NetflowSettings newValue ) { this.netflowSettings = newValue; }

    public DynamicRoutingSettings getDynamicRoutingSettings() { return this.dynamicRoutingSettings; }
    public void setDynamicRoutingSettings( DynamicRoutingSettings newValue ) { this.dynamicRoutingSettings = newValue; }

    public String getDnsmasqOptions() { return this.dnsmasqOptions; }
    public void setDnsmasqOptions( String newValue ) { this.dnsmasqOptions = newValue; }

    public boolean getVlansEnabled() { return this.vlansEnabled; }
    public void setVlansEnabled( boolean newValue ) { this.vlansEnabled = newValue; }

    public int getLxcInterfaceId() { return this.lxcInterfaceId; }
    public void setLxcInterfaceId( int newValue ) { this.lxcInterfaceId = newValue; }

    /**
     * This determines the method used to calculate the publicy available URL used to reach Untangle resources
     */
    public String getPublicUrlMethod() { return this.publicUrlMethod; }
    public void setPublicUrlMethod( String newValue ) { this.publicUrlMethod = newValue; }

    /**
     * This stores the hostname/IP used to reach Untangle publicly (if specified)
     */
    public String getPublicUrlAddress() { return this.publicUrlAddress; }
    public void setPublicUrlAddress( String newValue ) { this.publicUrlAddress = newValue; }

    /**
     * This stores the port used to reach Untangle publicly (if specified)
     */
    public Integer getPublicUrlPort() { return this.publicUrlPort; }
    public void setPublicUrlPort( Integer newValue ) { this.publicUrlPort = newValue; }

    /**
     * This enables a global custom block page that overrides the existing app specific block page settings
     */
    public boolean getGlobalCustomBlockPageEnabled() { return this.globalCustomBlockPageEnabled; }
    public void setGlobalCustomBlockPageEnabled(boolean newValue) { this.globalCustomBlockPageEnabled = newValue; }

    /**
     * This stores the global custom block page url
     */
    public String getGlobalCustomBlockPageUrl() { return this.globalCustomBlockPageUrl; }
    public void setGlobalCustomBlockPageUrl(String newValue) { this.globalCustomBlockPageUrl = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
