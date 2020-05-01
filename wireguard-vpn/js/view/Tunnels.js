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
        'endpointDynamic': true,
        'endpointAddress' : '',
        'endpointPort': '',
        'peerAddress': '',
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
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'description',
    }, {
        header: 'Public Key'.t(),
        width: 290,
        dataIndex: 'publicKey',
    }, {
        header: 'Dynamic Endpoint?'.t(),
        width: Renderer.messageWidth,
        dataIndex: 'endpointDynamic',
    }, {
        header: 'IP Address'.t(),
        width: Renderer.ipWidth,
        dataIndex: 'endpointAddress',
        renderer: Ung.apps['wireguard-vpn'].Main.dynamicEndpointRenderer
    }, {
        header: 'Port'.t(),
        width: Renderer.portWidth,
        dataIndex: 'endpointPort',
        renderer: Ung.apps['wireguard-vpn'].Main.dynamicEndpointRenderer
    }, {
        header: 'Peer IP Address'.t(),
        width: Renderer.ipWidth,
        dataIndex: 'peerAddress',
    }, {
        header: 'Remote Networks'.t(),
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'networks'
    }],

    editorXtype: 'ung.cmp.unwireguardvpntunnelrecordeditor',
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
        vtype: 'wireguardPublicKey',
        fieldLabel: 'Public Key'.t(),
        allowBlank: false,
        bind: {
            value: '{record.publicKey}'
        }
    },{
        xtype: 'checkbox',
        fieldLabel: 'Dynamic endpoint'.t(),
        bind: '{record.endpointDynamic}'
    }, {
        xtype: 'textfield',
        fieldLabel: 'Endpoint Address'.t(),
        hidden: true,
        disabled: true,
        allowBlank: false,
        vtype: 'isSingleIpValid',
        bind: {
            value: '{record.endpointAddress}',
            hidden: '{record.endpointDynamic}',
            disabled: '{record.endpointDynamic}'
        }
    }, {
        xtype: 'textfield',
        fieldLabel: 'Endpoint Port'.t(),
        vtype: 'isSinglePortValid',
        hidden: true,
        disabled: true,
        allowBlank: false,
        bind: {
            value: '{record.endpointPort}',
            hidden: '{record.endpointDynamic}',
            disabled: '{record.endpointDynamic}'
        }
    }, {
        xtype: 'textfield',
        fieldLabel: 'Peer Address'.t(),
        vtype: 'isSingleIpValidOrEmpty',
        allowBlank: true,
        bind: {
            value: '{record.peerAddress}'
        }
    }, {
        xtype: 'textarea',
        fieldLabel: 'Remote Networks'.t(),
        vtype: 'cidrBlockArea',
        allowBlank: true,
        width: 250,
        height: 50,
        bind: {
            value: '{record.networks}'
        }
    }, {
        xtype: 'textfield',
        fieldLabel: 'Ping Address'.t(),
        allowBlank: true,
        vtype: 'isSingleIpValid',
        bind: {
            value: '{record.pingAddress}',
        }
    }, {
        xtype: 'numberfield',
        fieldLabel: 'Ping Interval'.t(),
        allowBlank: false,
        allowDecimals: false,
        minValue: 0,
        maxValue: 300,
        bind: {
            value: '{record.pingInterval}'
        }
    },{
        xtype: 'checkbox',
        fieldLabel: 'Tunnel Up/Down Alerts'.t(),
        bind: '{record.pingConnectionEvents}'
    },{
        xtype: 'checkbox',
        fieldLabel: 'Ping Unreachable Alerts'.t(),
        bind: '{record.pingUnreachableEvents}'
    }]
});
