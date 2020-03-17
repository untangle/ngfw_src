Ext.define('Ung.apps.wireguard-vpn.view.Tunnels', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-wireguard-vpn-tunnels',
    itemId: 'tunnels',
    title: 'Tunnels'.t(),
    scrollable: true,

    withValidation: true,
    padding: '8 5',

    items: [{
        fieldLabel: 'Site URL'.t(),
        xtype: 'displayfield',
        bind: {
            value: '{getSiteUrl}',
        },
    },{
        fieldLabel: 'Public Key'.t(),
        xtype: 'displayfield',
        bind: {
            value: '{settings.publicKey}',
        },
    },{
        xtype: 'app-wireguard-vpn-server-tunnels-grid',
    }]
});

Ext.define('Ung.apps.wireguard-vpn.cmp.TunnelsGrid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-wireguard-vpn-server-tunnels-grid',
    itemId: 'server-tunnels-grid',
    viewModel: true,

    emptyText: 'No tunnels defined'.t(),

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete'],
    listProperty: 'settings.tunnels.list',
    emptyRow: {
        'javaClass': 'com.untangle.app.wireguard_vpn.WireguardVpnTunnel',
        'enabled': true,
        'description': '',
        'publicKey': '',
        'peerAddress': '',
        'endpointAddress' : '',
        'endpointPort': '',
        'networks': {
            'javaClass': 'java.util.LinkedList',
            'list': []
        }
    },

    bind: '{tunnels}',

    columns: [{
        xtype: 'checkcolumn',
        header: 'Enabled'.t(),
        width: Renderer.booleanWidth,
        dataIndex: 'enabled',
        resizable: false
    }, {
        header: 'Description'.t(),
        width: Renderer.usernameWidth,
        flex: 1,
        dataIndex: 'description',
    }, {
        header: 'Public Key'.t(),
        width: Renderer.usernameWidth,
        flex: 1,
        dataIndex: 'publicKey',
    }, {
        header: 'Endpoint Address'.t(),
        width: Renderer.usernameWidth,
        flex: 1,
        dataIndex: 'endpointAddress',
    }, {
        header: 'Endpoint Port'.t(),
        width: Renderer.usernameWidth,
        flex: 1,
        dataIndex: 'endpointPort',
    }, {
        header: 'Peer Address'.t(),
        width: Renderer.usernameWidth,
        flex: 1,
        dataIndex: 'peerAddress',
    }, {
        header: 'Networks'.t(),
        width: Renderer.usernameWidth,
        flex: 1,
        dataIndex: 'networks',
    }],

    editorFields: [{
        xtype: 'checkbox',
        fieldLabel: 'Enabled'.t(),
        bind: '{record.enabled}'
    }, {
        xtype: 'textfield',
        // vtype: 'wireguard-vpnName',
        fieldLabel: 'Description'.t(),
        allowBlank: false,
        bind: {
            value: '{record.description}'
        }
    }, {
        xtype: 'textfield',
        // vtype: 'wireguard-vpnName',
        fieldLabel: 'Public Key'.t(),
        allowBlank: false,
        bind: {
            value: '{record.publicKey}'
        }
    }, {
        xtype: 'textfield',
        fieldLabel: 'Endpoint Address'.t(),
        // allowBlank: false,
        bind: {
            value: '{record.endpointAddress}'
        }
    }, {
        xtype: 'textfield',
        fieldLabel: 'Endpoint Port'.t(),
        // allowBlank: false,
        bind: {
            value: '{record.endpointPort}'
        }
    }, {
        xtype: 'textfield',
        fieldLabel: 'Peer Address'.t(),
        // allowBlank: false,
        bind: {
            value: '{record.peerAddress}'
        }
    // }, {
    //     xtype: 'textfield',
    //     fieldLabel: 'Networks'.t(),
    //     // allowBlank: false,
    //     bind: {
    //         value: '{record.networks}'
    //     }
    }]

});

