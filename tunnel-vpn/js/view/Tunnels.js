Ext.define('Ung.apps.tunnel-vpn.view.Tunnels', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-tunnel-vpn-tunnels',
    itemId: 'tunnels',
    title: 'Tunnels'.t(),
    viewModel: true,

    controller: 'tunnelgrid',

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Configure Tunnel VPN tunnels (connections) to remote VPN services'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add']
    }],

    recordActions: ['delete'],
    listProperty: 'settings.tunnels.list',

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
