/**
 * $Id$
 */
package com.untangle.uvm.network.generic;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.network.DhcpOption;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.InterfaceSettings.ConfigType;
import com.untangle.uvm.network.InterfaceSettings.DhcpType;
import com.untangle.uvm.network.InterfaceSettings.InterfaceAlias;
import com.untangle.uvm.network.InterfaceSettings.V4ConfigType;
import com.untangle.uvm.network.InterfaceSettings.V6ConfigType;
import com.untangle.uvm.network.InterfaceSettings.WirelessEncryption;
import com.untangle.uvm.network.InterfaceSettings.WirelessMode;

/**
 * Interface settings v2.
 */
@SuppressWarnings("serial")
public class InterfaceSettingsGeneric implements Serializable, JSONString {

    private int interfaceId;        /* the ID of the physical interface (1-254) */
    private String name;            /* human name: ie External, Internal, Wireless */

    private String device;          /* physical interface name: eth0, etc */

    private boolean wan = false;    /* is a WAN interface? */
    public static enum Type { NIC, VLAN, BRIDGE, WIFI };
    private Type type = Type.NIC;   // TODO Investigate and add all possible types

    private Integer vlanId = null;                  /* vlan 802.1q tag */
    private Integer boundInterfaceId = null;        /* The parent interface of this vlan alias */

    private ConfigType configType = ConfigType.DISABLED;    /* config type */

    private Integer bridgedTo;      /* device to bridge to in "bridged" case */

    private V4ConfigType v4ConfigType = V4ConfigType.AUTO;  /* IPv4 config type */
    private InetAddress v4StaticAddress;                    /* the address  of this interface if configured static, or dhcp override */
    private Integer v4StaticPrefix;                         /* the netmask of this interface if configured static, or dhcp override */
    private InetAddress v4StaticGateway;                    /* the gateway  of this interface if configured static, or dhcp override */
    private InetAddress v4StaticDNS1;                       /* the dns1  of this interface if configured static */
    private InetAddress v4StaticDNS2;                       /* the dns2  of this interface if configured static */
    private InetAddress v4DhcpAddressOverride;              /* the dhcp override address (null means don't override) */ 
    private Integer v4DhcpPrefixOverride;                   /* the dhcp override netmask (null means don't override) */ 
    private InetAddress v4DhcpGatewayOverride;              /* the dhcp override gateway (null means don't override) */ 
    private InetAddress v4DhcpDNS1Override;                 /* the dhcp override dns1 (null means don't override) */
    private InetAddress v4DhcpDNS2Override;                 /* the dhcp override dns2 (null means don't override) */

    private List<V4Alias> v4Aliases = new LinkedList<>();
    private List<V6Alias> v6Aliases = new LinkedList<>();

    private String v4PPPoEUsername;             /* PPPoE Username */
    private String v4PPPoEPassword;             /* PPPoE Password */
    private Boolean v4PPPoEUsePeerDNS;          /* If the DNS should be determined via PPP */
    private InetAddress v4PPPoEOverrideDNS1;    /* the dns1  of this interface if configured static */
    private InetAddress v4PPPoEOverrideDNS2;    /* the dns2  of this interface if configured static */
    private String v4PPPoEPasswordEncrypted;    /* Encrypted PPPoE Password */

    private Boolean natEgress;
    private Boolean natIngress;

    private V6ConfigType v6ConfigType = V6ConfigType.DISABLED;  /* IPv6 config type */
    private InetAddress v6StaticAddress;                        /* the address  of this interface if configured static, or dhcp override */ 
    private Integer v6StaticPrefix;                             /* the netmask  of this interface if configured static, or dhcp override */
    private InetAddress v6StaticGateway;                        /* the gateway  of this interface if configured static, or dhcp override */
    private InetAddress v6StaticDNS1;                           /* the dns1  of this interface if configured static, or dhcp override */
    private InetAddress v6StaticDNS2;                           /* the dns2  of this interface if configured static, or dhcp override */

    private boolean dhcpEnabled;                /* is DHCP Server enabled on this interface? */
    private boolean dhcpRelayEnabled;           /* is DHCP Relay enabled on this interface? */

    private InetAddress dhcpRangeStart;         /* where do DHCP leases start? example: 192.168.2.100*/
    private InetAddress dhcpRangeEnd;           /* where do DHCP leases end? example: 192.168.2.200 */
    private Integer dhcpLeaseDuration;          /* DHCP lease duration in seconds */
    private InetAddress dhcpGatewayOverride;    /* DHCP gateway override, if null defaults to this interface's IP */
    private Integer dhcpPrefixOverride;         /* DHCP netmask override, if null defaults to this interface's netmask */
    private String dhcpDNSOverride;             /* DHCP DNS override, if null defaults to this interface's IP */
    private List<DhcpOption> dhcpOptions;       /* DHCP dnsmasq options */

    private InetAddress dhcpRelayAddress;       /* DHCP relay server IP address */

    private Boolean routerAdvertisements;       /* are IPv6 router advertisements available? */

    private Integer downloadKbps;       /* Download Bandwidth available on this WAN interface (for QoS) */
    private Integer uploadKbps;         /* Upload Bandwidth available on this WAN interface (for QoS) */

    private Boolean vrrpEnabled;            /* Is VRRP enabled */
    private Integer vrrpId;                 /* VRRP ID 1-255 */
    private Integer vrrpPriority;           /* VRRP priority 1-255, highest priority is master */
    private List<V4Alias> vrrpV4Aliases = new LinkedList<>();

    private String wirelessSsid;
    private WirelessEncryption wirelessEncryption = WirelessEncryption.NONE;
    private WirelessMode wirelessMode = WirelessMode.AP;
    private String wirelessPassword;
    private int wirelessLoglevel = 2;
    private Integer wirelessChannel;
    private boolean hidden;
    private String wirelessCountryCode = "";


    public int getInterfaceId() { return interfaceId; }
    public void setInterfaceId(int interfaceId) { this.interfaceId = interfaceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }

    public boolean isWan() { return wan; }
    public void setWan(boolean wan) { this.wan = wan; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public Integer getVlanId() { return vlanId; }
    public void setVlanId(Integer vlanId) { this.vlanId = vlanId; }

    public Integer getBoundInterfaceId() { return boundInterfaceId; }
    public void setBoundInterfaceId(Integer boundInterfaceId) { this.boundInterfaceId = boundInterfaceId; }

    public ConfigType getConfigType() { return configType; }
    public void setConfigType(ConfigType configType) { this.configType = configType; }

    public Integer getBridgedTo() { return bridgedTo; }
    public void setBridgedTo(Integer bridgedTo) { this.bridgedTo = bridgedTo; }

    public V4ConfigType getV4ConfigType() { return v4ConfigType; }
    public void setV4ConfigType(V4ConfigType v4ConfigType) { this.v4ConfigType = v4ConfigType; }

    public InetAddress getV4StaticAddress() { return v4StaticAddress; }
    public void setV4StaticAddress(InetAddress v4StaticAddress) { this.v4StaticAddress = v4StaticAddress; }

    public Integer getV4StaticPrefix() { return v4StaticPrefix; }
    public void setV4StaticPrefix(Integer v4StaticPrefix) { this.v4StaticPrefix = v4StaticPrefix; }

    public InetAddress getV4StaticGateway() { return v4StaticGateway; }
    public void setV4StaticGateway(InetAddress v4StaticGateway) { this.v4StaticGateway = v4StaticGateway; }

    public InetAddress getV4StaticDNS1() { return v4StaticDNS1; }
    public void setV4StaticDNS1(InetAddress v4StaticDNS1) { this.v4StaticDNS1 = v4StaticDNS1; }

    public InetAddress getV4StaticDNS2() { return v4StaticDNS2; }
    public void setV4StaticDNS2(InetAddress v4StaticDNS2) { this.v4StaticDNS2 = v4StaticDNS2; }

    public InetAddress getV4DhcpAddressOverride() { return v4DhcpAddressOverride; }
    public void setV4DhcpAddressOverride(InetAddress v4DhcpAddressOverride) { this.v4DhcpAddressOverride = v4DhcpAddressOverride; }

    public Integer getV4DhcpPrefixOverride() { return v4DhcpPrefixOverride; }
    public void setV4DhcpPrefixOverride(Integer v4DhcpPrefixOverride) { this.v4DhcpPrefixOverride = v4DhcpPrefixOverride; }

    public InetAddress getV4DhcpGatewayOverride() { return v4DhcpGatewayOverride; }
    public void setV4DhcpGatewayOverride(InetAddress v4DhcpGatewayOverride) { this.v4DhcpGatewayOverride = v4DhcpGatewayOverride; }

    public InetAddress getV4DhcpDNS1Override() { return v4DhcpDNS1Override; }
    public void setV4DhcpDNS1Override(InetAddress v4DhcpDNS1Override) { this.v4DhcpDNS1Override = v4DhcpDNS1Override; }

    public InetAddress getV4DhcpDNS2Override() { return v4DhcpDNS2Override; }
    public void setV4DhcpDNS2Override(InetAddress v4DhcpDNS2Override) { this.v4DhcpDNS2Override = v4DhcpDNS2Override; }

    public List<V4Alias> getV4Aliases() { return v4Aliases; }
    public void setV4Aliases(List<V4Alias> v4Aliases) { this.v4Aliases = v4Aliases; }

    public List<V6Alias> getV6Aliases() { return v6Aliases; }
    public void setV6Aliases(List<V6Alias> v6Aliases) { this.v6Aliases = v6Aliases; }

    public String getV4PPPoEUsername() { return v4PPPoEUsername; }
    public void setV4PPPoEUsername(String v4ppPoEUsername) { v4PPPoEUsername = v4ppPoEUsername; }

    public String getV4PPPoEPassword() { return v4PPPoEPassword; }
    public void setV4PPPoEPassword(String v4ppPoEPassword) { v4PPPoEPassword = v4ppPoEPassword; }

    public Boolean getV4PPPoEUsePeerDNS() { return v4PPPoEUsePeerDNS; }
    public void setV4PPPoEUsePeerDNS(Boolean v4ppPoEUsePeerDNS) { v4PPPoEUsePeerDNS = v4ppPoEUsePeerDNS; }

    public InetAddress getV4PPPoEOverrideDNS1() { return v4PPPoEOverrideDNS1; }
    public void setV4PPPoEOverrideDNS1(InetAddress v4ppPoEOverrideDNS1) { v4PPPoEOverrideDNS1 = v4ppPoEOverrideDNS1; }

    public InetAddress getV4PPPoEOverrideDNS2() { return v4PPPoEOverrideDNS2; }
    public void setV4PPPoEOverrideDNS2(InetAddress v4ppPoEOverrideDNS2) { v4PPPoEOverrideDNS2 = v4ppPoEOverrideDNS2; }

    public String getV4PPPoEPasswordEncrypted() { return v4PPPoEPasswordEncrypted; }
    public void setV4PPPoEPasswordEncrypted(String v4ppPoEPasswordEncrypted) { v4PPPoEPasswordEncrypted = v4ppPoEPasswordEncrypted; }

    public Boolean getNatEgress() { return natEgress; }
    public void setNatEgress(Boolean natEgress) { this.natEgress = natEgress; }

    public Boolean getNatIngress() { return natIngress; }
    public void setNatIngress(Boolean natIngress) { this.natIngress = natIngress; }

    public V6ConfigType getV6ConfigType() { return v6ConfigType; }
    public void setV6ConfigType(V6ConfigType v6ConfigType) { this.v6ConfigType = v6ConfigType; }

    public InetAddress getV6StaticAddress() { return v6StaticAddress; }
    public void setV6StaticAddress(InetAddress v6StaticAddress) { this.v6StaticAddress = v6StaticAddress; }

    public Integer getV6StaticPrefix() { return v6StaticPrefix; }
    public void setV6StaticPrefix(Integer v6StaticPrefix) { this.v6StaticPrefix = v6StaticPrefix; }

    public InetAddress getV6StaticGateway() { return v6StaticGateway; }
    public void setV6StaticGateway(InetAddress v6StaticGateway) { this.v6StaticGateway = v6StaticGateway; }

    public InetAddress getV6StaticDNS1() { return v6StaticDNS1; }
    public void setV6StaticDNS1(InetAddress v6StaticDNS1) { this.v6StaticDNS1 = v6StaticDNS1; }

    public InetAddress getV6StaticDNS2() { return v6StaticDNS2; }
    public void setV6StaticDNS2(InetAddress v6StaticDNS2) { this.v6StaticDNS2 = v6StaticDNS2; }

    public boolean isDhcpEnabled() { return dhcpEnabled; }
    public void setDhcpEnabled(boolean dhcpEnabled) { this.dhcpEnabled = dhcpEnabled; }

    public boolean isDhcpRelayEnabled() { return dhcpRelayEnabled; }
    public void setDhcpRelayEnabled(boolean dhcpRelayEnabled) { this.dhcpRelayEnabled = dhcpRelayEnabled; }

    public InetAddress getDhcpRangeStart() { return dhcpRangeStart; }
    public void setDhcpRangeStart(InetAddress dhcpRangeStart) { this.dhcpRangeStart = dhcpRangeStart; }

    public InetAddress getDhcpRangeEnd() { return dhcpRangeEnd; }
    public void setDhcpRangeEnd(InetAddress dhcpRangeEnd) { this.dhcpRangeEnd = dhcpRangeEnd; }

    public Integer getDhcpLeaseDuration() { return dhcpLeaseDuration; }
    public void setDhcpLeaseDuration(Integer dhcpLeaseDuration) { this.dhcpLeaseDuration = dhcpLeaseDuration; }

    public InetAddress getDhcpGatewayOverride() { return dhcpGatewayOverride; }
    public void setDhcpGatewayOverride(InetAddress dhcpGatewayOverride) { this.dhcpGatewayOverride = dhcpGatewayOverride; }

    public Integer getDhcpPrefixOverride() { return dhcpPrefixOverride; }
    public void setDhcpPrefixOverride(Integer dhcpPrefixOverride) { this.dhcpPrefixOverride = dhcpPrefixOverride; }

    public String getDhcpDNSOverride() { return dhcpDNSOverride; }
    public void setDhcpDNSOverride(String dhcpDNSOverride) { this.dhcpDNSOverride = dhcpDNSOverride; }

    public List<DhcpOption> getDhcpOptions() { return dhcpOptions; }
    public void setDhcpOptions(List<DhcpOption> dhcpOptions) { this.dhcpOptions = dhcpOptions; }

    public InetAddress getDhcpRelayAddress() { return dhcpRelayAddress; }
    public void setDhcpRelayAddress(InetAddress dhcpRelayAddress) { this.dhcpRelayAddress = dhcpRelayAddress; }

    public Boolean getRouterAdvertisements() { return routerAdvertisements; }
    public void setRouterAdvertisements(Boolean routerAdvertisements) { this.routerAdvertisements = routerAdvertisements; }

    public Integer getDownloadKbps() { return downloadKbps; }
    public void setDownloadKbps(Integer downloadKbps) { this.downloadKbps = downloadKbps; }

    public Integer getUploadKbps() { return uploadKbps; }
    public void setUploadKbps(Integer uploadKbps) { this.uploadKbps = uploadKbps; }

    public Boolean getVrrpEnabled() { return vrrpEnabled; }
    public void setVrrpEnabled(Boolean vrrpEnabled) { this.vrrpEnabled = vrrpEnabled; }

    public Integer getVrrpId() { return vrrpId; }
    public void setVrrpId(Integer vrrpId) { this.vrrpId = vrrpId; }

    public Integer getVrrpPriority() { return vrrpPriority; }
    public void setVrrpPriority(Integer vrrpPriority) { this.vrrpPriority = vrrpPriority; }

    public List<V4Alias> getVrrpV4Aliases() { return vrrpV4Aliases; }
    public void setVrrpV4Aliases(List<V4Alias> vrrpV4Aliases) { this.vrrpV4Aliases = vrrpV4Aliases; }

    public String getWirelessSsid() { return wirelessSsid; }
    public void setWirelessSsid(String wirelessSsid) { this.wirelessSsid = wirelessSsid; }

    public WirelessEncryption getWirelessEncryption() { return wirelessEncryption; }
    public void setWirelessEncryption(WirelessEncryption wirelessEncryption) { this.wirelessEncryption = wirelessEncryption; }

    public WirelessMode getWirelessMode() { return wirelessMode; }
    public void setWirelessMode(WirelessMode wirelessMode) { this.wirelessMode = wirelessMode; }

    public String getWirelessPassword() { return wirelessPassword; }
    public void setWirelessPassword(String wirelessPassword) { this.wirelessPassword = wirelessPassword; }

    public int getWirelessLoglevel() { return wirelessLoglevel; }
    public void setWirelessLoglevel(int wirelessLoglevel) { this.wirelessLoglevel = wirelessLoglevel; }

    public Integer getWirelessChannel() { return wirelessChannel; }
    public void setWirelessChannel(Integer wirelessChannel) { this.wirelessChannel = wirelessChannel; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public String getWirelessCountryCode() { return wirelessCountryCode; }
    public void setWirelessCountryCode(String wirelessCountryCode) { this.wirelessCountryCode = wirelessCountryCode; }


    /**
     * Transforms the current InterfaceSettingsGeneric instance into the original InterfaceSettings instance.
     * This performs a field-to-field mapping, including IPv4/IPv6 addressing, NAT settings,
     * DHCP/PPPoE configuration, VRRP, and wireless parameters.
     *
     * @param intfSettings the original InterfaceSettings instance to be populated.
     */
    public void transformGenericToInterfaceSettings(InterfaceSettings intfSettings) {
        intfSettings.setInterfaceId(this.interfaceId);
        intfSettings.setName(this.name);

        intfSettings.setPhysicalDev(this.device);

        intfSettings.setIsWan(this.wan);

        intfSettings.setIsVlanInterface(this.type == InterfaceSettingsGeneric.Type.VLAN);
        intfSettings.setIsWirelessInterface(this.type == InterfaceSettingsGeneric.Type.WIFI);

        intfSettings.setVlanTag(this.vlanId);
        intfSettings.setVlanParent(this.boundInterfaceId);

        intfSettings.setConfigType(this.configType);
        intfSettings.setBridgedTo(this.bridgedTo);

        intfSettings.setV4ConfigType(this.v4ConfigType);
        intfSettings.setV4StaticAddress(this.v4StaticAddress);
        intfSettings.setV4StaticPrefix(this.v4StaticPrefix);
        intfSettings.setV4StaticGateway(this.v4StaticGateway);
        intfSettings.setV4StaticDns1(this.v4StaticDNS1);
        intfSettings.setV4StaticDns2(this.v4StaticDNS2);

        intfSettings.setV4AutoAddressOverride(this.v4DhcpAddressOverride);
        intfSettings.setV4AutoPrefixOverride(this.v4DhcpPrefixOverride);
        intfSettings.setV4AutoGatewayOverride(this.v4DhcpGatewayOverride);
        intfSettings.setV4AutoDns1Override(this.v4DhcpDNS1Override);
        intfSettings.setV4AutoDns2Override(this.v4DhcpDNS2Override);

        intfSettings.setV4Aliases(convertToOriginalAliases(this.v4Aliases));
        intfSettings.setV6Aliases(convertToOriginalAliasesV6(this.v6Aliases));

        intfSettings.setV4PPPoEUsername(this.v4PPPoEUsername);
        intfSettings.setV4PPPoEPassword(this.v4PPPoEPassword);
        intfSettings.setV4PPPoEUsePeerDns(this.v4PPPoEUsePeerDNS);
        intfSettings.setV4PPPoEDns1(this.v4PPPoEOverrideDNS1);
        intfSettings.setV4PPPoEDns2(this.v4PPPoEOverrideDNS2);
        intfSettings.setV4PPPoEPasswordEncrypted(this.v4PPPoEPasswordEncrypted);

        intfSettings.setV4NatEgressTraffic(this.natEgress);
        intfSettings.setV4NatIngressTraffic(this.natIngress);

        intfSettings.setV6ConfigType(this.v6ConfigType);
        intfSettings.setV6StaticAddress(this.v6StaticAddress);
        intfSettings.setV6StaticPrefixLength(this.v6StaticPrefix);
        intfSettings.setV6StaticGateway(this.v6StaticGateway);
        intfSettings.setV6StaticDns1(this.v6StaticDNS1);
        intfSettings.setV6StaticDns2(this.v6StaticDNS2);

        intfSettings.setDhcpEnabled(this.dhcpEnabled || this.dhcpRelayEnabled);
        intfSettings.setDhcpType(resolveDhcpType(this));
        intfSettings.setDhcpRangeStart(this.dhcpRangeStart);
        intfSettings.setDhcpRangeEnd(this.dhcpRangeEnd);
        intfSettings.setDhcpLeaseDuration(this.dhcpLeaseDuration);
        intfSettings.setDhcpGatewayOverride(this.dhcpGatewayOverride);
        intfSettings.setDhcpPrefixOverride(this.dhcpPrefixOverride);
        intfSettings.setDhcpDnsOverride(this.dhcpDNSOverride);
        intfSettings.setDhcpOptions(this.dhcpOptions);
        intfSettings.setDhcpRelayAddress(this.dhcpRelayAddress);

        intfSettings.setRaEnabled(this.routerAdvertisements);

        intfSettings.setDownloadBandwidthKbps(this.downloadKbps);
        intfSettings.setUploadBandwidthKbps(this.uploadKbps);

        intfSettings.setVrrpEnabled(this.vrrpEnabled);
        intfSettings.setVrrpId(this.vrrpId);
        intfSettings.setVrrpPriority(this.vrrpPriority);
        intfSettings.setVrrpAliases(convertToOriginalAliases(this.vrrpV4Aliases));

        intfSettings.setWirelessSsid(this.wirelessSsid);
        intfSettings.setWirelessEncryption(this.wirelessEncryption);
        intfSettings.setWirelessMode(this.wirelessMode);
        intfSettings.setWirelessPassword(this.wirelessPassword);
        intfSettings.setWirelessLogLevel(this.wirelessLoglevel);
        intfSettings.setWirelessChannel(this.wirelessChannel);
        intfSettings.setWirelessVisibility(this.hidden ? 1 : 0);
        intfSettings.setWirelessCountryCode(this.wirelessCountryCode);
    }

    private List<InterfaceAlias> convertToOriginalAliases(List<V4Alias> aliases) {
        if (aliases == null) return null;
        return aliases.stream().map(a -> {
            InterfaceAlias alias = new InterfaceAlias();
            alias.setStaticAddress(a.getV4Address());
            alias.setStaticPrefix(a.getV4Prefix());
            return alias;
        }).collect(Collectors.toList());
    }

    private List<InterfaceAlias> convertToOriginalAliasesV6(List<V6Alias> aliases) {
        if (aliases == null) return null;
        return aliases.stream().map(a -> {
            InterfaceAlias alias = new InterfaceAlias();
            alias.setStaticAddress(a.getV6Address());
            alias.setStaticPrefix(a.getV6Prefix());
            return alias;
        }).collect(Collectors.toList());
    }

    private DhcpType resolveDhcpType(InterfaceSettingsGeneric generic) {
        if (generic.dhcpRelayEnabled) return DhcpType.RELAY;
        if (generic.dhcpEnabled) return DhcpType.SERVER;
        return DhcpType.DISABLED;
    }

    /**
     * Interface V4 alias.
     */
    public static class V4Alias {
        private InetAddress v4Address;      /* the address  of this interface if configured static, or dhcp override */ 
        private Integer     v4Prefix;       /* the netmask of this interface if configured static, or dhcp override */

        public V4Alias() {}

        public InetAddress getV4Address() { return v4Address; }
        public void setV4Address(InetAddress v4Address) { this.v4Address = v4Address; }

        public Integer getV4Prefix() { return v4Prefix; }
        public void setV4Prefix(Integer v4Prefix) { this.v4Prefix = v4Prefix; }
    }

    /**
     * Interface V6 alias.
     */
    public static class V6Alias {
        private InetAddress v6Address;      /* the address  of this interface if configured static, or dhcp override */ 
        private Integer     v6Prefix;       /* the netmask of this interface if configured static, or dhcp override */

        public V6Alias() {}

        public InetAddress getV6Address() { return v6Address; }
        public void setV6Address(InetAddress v6Address) { this.v6Address = v6Address; }

        public Integer getV6Prefix() { return v6Prefix; }
        public void setV6Prefix(Integer v6Prefix) { this.v6Prefix = v6Prefix; }
    }


    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}
