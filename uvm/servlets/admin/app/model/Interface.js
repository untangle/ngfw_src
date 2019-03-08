Ext.define ('Ung.model.Interface', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'addressed', type: 'boolean', defaultValue: true },
        { name: 'bridged', type: 'boolean', defaultValue: false },
        { name: 'bridgedTo', type: 'auto', defaultValue: null },
        { name: 'configType', type: 'string' },

        { name: 'dhcpDnsOverride', type: 'string' },
        { name: 'dhcpEnabled', type: 'boolean', defaultValue: false },
        { name: 'dhcpGatewayOverride', type: 'auto', defaultValue: null },
        { name: 'dhcpLeaseDuration', type: 'number', defaultValue: null },
        { name: 'dhcpNetmaskDuration', type: 'auto', defaultValue: null },
        { name: 'dhcpOptions', type: 'auto', defaultValue: { javaClass: 'java.util.LinkedList', list: [] } }, // collection
        { name: 'dhcpPrefixOverride', type: 'auto', defaultValue: null },
        { name: 'dhcpRangeEnd', type: 'string', defaultValue: null },
        { name: 'dhcpRangeStart', type: 'string', defaultValue: null },

        { name: 'disabled', type: 'boolean', defaultValue: false },
        { name: 'downloadBandwidthKbps', type: 'number' },
        { name: 'hidden', type: 'boolean', defaultValue: null },
        { name: 'imqDev', type: 'string' },
        { name: 'interfaceId', type: 'number', defaultValue: -1 },

        { name: 'isVlanInterface', type: 'boolean' },
        { name: 'isWan', type: 'boolean' },
        { name: 'isWirelessInterface', type: 'boolean' },

        { name: 'javaClass', type: 'string', defaultValue: 'com.untangle.uvm.network.InterfaceSettings' },
        { name: 'name', type: 'string', defaultValue: '' },
        { name: 'physicalDev', type: 'string' },
        { name: 'reEnabled', type: 'boolean', defaultValue: false },
        { name: 'supportedConfigTypes', type: 'auto', defaultValue: null },
        { name: 'symbolicDev', type: 'string' },
        { name: 'systemDev', type: 'string' },
        { name: 'uploadBandwidthKbps', type: 'number' },

        { name: 'v4Aliases', type: 'auto', defaultValue: { javaClass: 'java.util.LinkedList', list: [] } }, // collection
        { name: 'v4AutoAddressOverride', type: 'string', defaultValue: null },
        { name: 'v4AutoDns1Override', type: 'string', defaultValue: null },
        { name: 'v4AutoDns2Override', type: 'string', defaultValue: null },
        { name: 'v4AutoGatewayOverride', type: 'string', defaultValue: null },
        { name: 'v4AutoNetmaskOverride', type: 'string', defaultValue: null },
        { name: 'v4AutoPrefixOverride', type: 'string', defaultValue: null },
        { name: 'v4ConfigType', type: 'string' },
        { name: 'v4NatEgressTraffic', type: 'boolean', defaultValue: true },
        { name: 'v4NatIngressTraffic', type: 'boolean', defaultValue: false },
        { name: 'v4PPPoEDns1', type: 'string', defaultValue: null },
        { name: 'v4PPPoEDns2', type: 'string', defaultValue: null },
        { name: 'v4PPPoEPassword', type: 'string', defaultValue: '' },
        { name: 'v4PPPoERootDev', type: 'string', defaultValue: null },
        { name: 'v4PPPoEUsePeerDns', type: 'boolean', defaultValue: false },
        { name: 'v4PPPoEUsername', type: 'string', defaultValue: '' },
        { name: 'v4StaticAddress', type: 'string', defaultValue: null },
        { name: 'v4StaticDns1', type: 'string', defaultValue: null },
        { name: 'v4StaticDns2', type: 'string', defaultValue: null },
        { name: 'v4StaticGateway', type: 'string', defaultValue: null },
        { name: 'v4StaticNetmask', type: 'string', defaultValue: null },
        { name: 'v4StaticPrefix', type: 'string', defaultValue: null },

        { name: 'v6Aliases', type: 'auto', defaultValue: { javaClass: 'java.util.LinkedList', list: [] } }, // collection
        { name: 'v6ConfigType', type: 'string' },
        { name: 'v6StaticAddress', type: 'string', defaultValue: null },
        { name: 'v6StaticDns1', type: 'string', defaultValue: null },
        { name: 'v6StaticDns2', type: 'string', defaultValue: null },
        { name: 'v6StaticGateway', type: 'string', defaultValue: null },
        { name: 'v6StaticPrefixLength', type: 'string', defaultValue: null },

        { name: 'vlanParent', type: 'number', defaultValue: null },
        { name: 'vlanTag', type: 'number', defaultValue: null },

        { name: 'vrrpAliases', type: 'auto', defaultValue: { javaClass: 'java.util.LinkedList', list: [] } }, // collection
        { name: 'vrrpEnabled', type: 'boolean', defaultValue: false },
        { name: 'vrrpId', type: 'auto', defaultValue: null },
        { name: 'vrrpPriority', type: 'auto', defaultValue: null },

        { name: 'wirelessChannel', type: 'auto', defaultValue: null },
        { name: 'wirelessEncryption', type: 'auto', defaultValue: null },
        { name: 'wirelessPassword', type: 'string', defaultValue: '' },
        { name: 'wirelessSsid', type: 'string', defaultValue: '' },

        // status fields
        { name: 'v4Address', type: 'string', defaultValue: null },
        { name: 'v4Dns1', type: 'string', defaultValue: null },
        { name: 'v4Dns2', type: 'string', defaultValue: null },
        { name: 'v4Gateway', type: 'string', defaultValue: null },
        { name: 'v4Netmask', type: 'string', defaultValue: null },
        { name: 'v4PrefixLength', type: 'string', defaultValue: null },
        { name: 'v6Address', type: 'string', defaultValue: null },
        { name: 'v6Gateway', type: 'string', defaultValue: null },
        { name: 'v6PrefixLength', type: 'string', defaultValue: null },

        // device fields
        { name: 'connected', type: 'string', defaultValue: null },
        { name: 'deviceName', type: 'string', defaultValue: null },
        { name: 'duplex', type: 'string' },
        { name: 'macAddress', type: 'string', defaultValue: null },
        { name: 'mbit', type: 'number', defaultValue: null },
        { name: 'vendor', type: 'string', defaultValue: null },
    ],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});
