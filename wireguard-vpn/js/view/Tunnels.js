Ext.define('Ung.apps.wireguard-vpn.view.Tunnels', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-wireguard-vpn-tunnels',
    itemId: 'tunnels',
    title: 'Tunnels'.t(),
    scrollable: true,

    withValidation: true,
    padding: '8 5',

    items: [{
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
        'javaClass': 'com.untangle.app.wireguard_vpn.WireGuardVpnTunnel',
        'enabled': true,
        'description': '',
        'publicKey': '',
        'endpointDynamic': true,
        'endpointAddress' : '',
        'endpointPort': 51820,
        'peerAddress': '',
        'pingInterval': 60,
        'pingConnectionEvents': true,
        'pingUnreachableEvents': false
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
        header: 'Remote Public Key'.t(),
        width: 290,
        dataIndex: 'publicKey',
    }, {
        header: 'Remote Peer IP Address'.t(),
        width: Renderer.ipWidth,
        dataIndex: 'peerAddress',
    }, {
        header: 'Remote Networks'.t(),
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'networks'
    }, {
        header: 'Remote Endpoint'.t(),
        width: Renderer.messageWidth,
        dataIndex: 'endpointDynamic',
        renderer: Ung.apps['wireguard-vpn'].Main.dynamicEndpointRenderer
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
    }],

    editorXtype: 'ung.cmp.unwireguardvpntunnelrecordeditor',
    editorFields: [{
        xtype: 'checkbox',
        fieldLabel: 'Enabled'.t(),
        bind: '{record.enabled}'
    }, {
        xtype: 'textfield',
        fieldLabel: 'Description'.t(),
        allowBlank: false,
        bind: {
            value: '{record.description}'
        }
    }, {
        xtype: 'textfield',
        vtype: 'wireguardPublicKey',
        fieldLabel: 'Remote Public Key'.t(),
        allowBlank: false,
        bind: {
            value: '{record.publicKey}'
        }
    }, {
        xtype: 'fieldset',
        title: 'Remote Endpoint'.t(),
        layout: {
            type: 'vbox'
        },
        defaults: {
            labelWidth: 170,
            labelAlign: 'right'
        },
        items:[{
            fieldLabel: 'Type'.t(),
            xtype: 'combobox',
            editable: false,
            bind: {
                value: '{record.endpointDynamic}'
            },
            queryMode: 'local',
            store: [
                [true, 'Roaming'.t()],
                [false, 'Static'.t()]
            ],
            forceSelection: true,
            listeners: {
                change: 'endpointTypeComboChange'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'IP Address'.t(),
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
            fieldLabel: 'Port'.t(),
            vtype: 'isSinglePortValid',
            hidden: true,
            disabled: true,
            allowBlank: false,
            bind: {
                value: '{record.endpointPort}',
                hidden: '{record.endpointDynamic}',
                disabled: '{record.endpointDynamic}'
            }
        }]
    }, {
        xtype: 'textfield',
        fieldLabel: 'Remote Peer IP Address'.t(),
        vtype: 'isSingleIpValid',
        allowBlank: false,
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
        xtype: 'fieldset',
        title: 'Monitor'.t(),
        padding: 10,
        layout: {
            type: 'vbox'
        },
        defaults: {
            labelWidth: 170,
            labelAlign: 'right'
        },
        items:[{
            xtype: 'textfield',
            fieldLabel: 'Ping IP Address'.t(),
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
                value: '{record.pingInterval}',
                disabled: '{!record.pingAddress}'
            }
        },{
            xtype: 'checkbox',
            fieldLabel: 'Alert on Tunnel Up/Down'.t(),
            bind: {
                value: '{record.pingConnectionEvents}',
                disabled: '{!record.pingAddress}'
            }
        },{
            xtype: 'checkbox',
            fieldLabel: 'Alert on Ping Unreachable'.t(),
            bind: {
                value: '{record.pingUnreachableEvents}',
                disabled: '{!record.pingAddress}'
            }
        }]
    }]
});
