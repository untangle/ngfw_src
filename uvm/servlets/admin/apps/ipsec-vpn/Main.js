Ext.define('Ung.apps.ipsecvpn.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-ipsec-vpn',
    controller: 'app-ipsec-vpn',

    items: [
        { xtype: 'app-ipsec-vpn-status' },
        { xtype: 'app-ipsec-vpn-ipsecoptions' },
        { xtype: 'app-ipsec-vpn-ipsectunnels' },
        { xtype: 'app-ipsec-vpn-vpnconfig' },
        { xtype: 'app-ipsec-vpn-grenetworks' },
        { xtype: 'app-ipsec-vpn-ipsecstate' },
        { xtype: 'app-ipsec-vpn-ipsecpolicy' },
        { xtype: 'app-ipsec-vpn-ipseclog' },
        { xtype: 'app-ipsec-vpn-l2tplog' }
    ]

});
