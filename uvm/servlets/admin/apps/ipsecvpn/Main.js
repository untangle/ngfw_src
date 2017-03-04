Ext.define('Ung.apps.ipsecvpn.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-ipsecvpn',
    controller: 'app-ipsecvpn',

    items: [
        { xtype: 'app-ipsecvpn-status' },
        { xtype: 'app-ipsecvpn-ipsecoptions' },
        { xtype: 'app-ipsecvpn-ipsectunnels' },
        { xtype: 'app-ipsecvpn-vpnconfig' },
        { xtype: 'app-ipsecvpn-grenetworks' },
        { xtype: 'app-ipsecvpn-ipsecstate' },
        { xtype: 'app-ipsecvpn-ipsecpolicy' },
        { xtype: 'app-ipsecvpn-ipseclog' },
        { xtype: 'app-ipsecvpn-l2tplog' }
    ]

});
