Ext.define('Ung.apps.tunnel-vpn.view.Tunnels', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-tunnel-vpn-tunnels',
    itemId: 'tunnels',
    title: 'Tunnels'.t(),
    viewModel: true,
    autoScroll: true,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'The Tunnels tab is used to configure Tunnel VPN tunnels (connections) to remote VPN services'.t()
    }],

    defaults: {
        labelWidth: 180,
        padding: '10 0 0 10'
    },

    items: [{
        xtype: 'app-tunnel-vpn-tunnel-tab-panel',
        padding: '20 20 0 20',
    }, {
        xtype: 'button',
        anchor: '100%',
        text: 'Configured New Tunnel VPN Connection'.t(),
        iconCls: 'fa fa-magic',
        handler: 'runWizard'
    }]
});

Ext.define('Ung.apps.tunnel-vpn.view.ClientTabs', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.app-tunnel-vpn-tunnel-tab-panel',
    itemId: 'tunnel-vpn-tunnel-tab-panel',
    viewModel: true,
    layout: 'fit',

    items: [{
        title: 'Tunnels'.t(),
        items: [
            { xtype: 'app-tunnel-vpn-tunnels-grid' }
            ]
    }]

});

Ext.define('Ung.apps.tunnel-vpn.view.TunnelsGrid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-tunnel-vpn-tunnels-grid',
    itemId: 'tunnel-vpn-tunnels-grid',

    recordActions: ['delete'],
    listProperty: 'settings.tunnels.list',
    emptyRow: {
        javaClass: 'com.untangle.app.tunnel_vpn.TunnelVpnTunnelSettings',
        'enabled': true,
        'name': '',
    },

    bind: '{tunnels}',

    columns: [{
        xtype: 'checkcolumn',
        header: 'Enabled'.t(),
        width: 80,
        dataIndex: 'enabled',
        resizable: false
    }, {
        header: 'Tunnel Name'.t(),
        width: 150,
        flex: 1,
        dataIndex: 'name',
        editor: {
            xtype: 'textfield',
            bind: '{record.name}'
        }
    }, {
        header: 'Username'.t(),
        width: 150,
        dataIndex: 'username',
        editor: {
            xtype: 'textfield',
            bind: '{record.username}'
        }
    }, {
        header: 'Password'.t(),
        width: 150,
        dataIndex: 'password',
        editor: {
            xtype: 'textfield',
            bind: '{record.password}'
        }
    }],
});
