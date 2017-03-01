Ext.define('Ung.apps.ipsecvpn.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.ipsecvpn',

    viewModel: {
        data: {
            nodeName: 'untangle-node-ipsec-vpn',
            appName: 'IPsec VPN'
        }
    },

    items: [
        { xtype: 'app.ipsecvpn.status' },
        { xtype: 'app.ipsecvpn.ipsecoptions' },
        { xtype: 'app.ipsecvpn.ipsectunnels' },
        { xtype: 'app.ipsecvpn.vpnconfig' },
        { xtype: 'app.ipsecvpn.grenetworks' },
        { xtype: 'app.ipsecvpn.ipsecstate' },
        { xtype: 'app.ipsecvpn.ipsecpolicy' },
        { xtype: 'app.ipsecvpn.ipseclog' },
        { xtype: 'app.ipsecvpn.l2tplog' }
    ]

});
