Ext.define('Ung.apps.wireguard-vpn.view.Tunnels', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-wireguard-vpn-tunnels',
    itemId: 'tunnels',
    title: 'Tunnels'.t(),
    scrollable: true,

    withValidation: true,
    padding: '8 5',

    controller: {
        listen: {
            global: {
                ungridaddrecord: 'onAddTunnel'
            }
        },

        onAddTunnel: function () {
            var vm = this.getViewModel(),
                grid = this.getView().down('ungrid'),
                store = grid.getStore(),
                addressPool = vm.get('settings.addressPool').split('/')[0];

            var newPeerAddress = Util.incrementIpAddr(addressPool, 1);

            while(store.findRecord('peerAddress', newPeerAddress) !== null) {
                newPeerAddress = Util.incrementIpAddr(newPeerAddress, 1);
            }

            // set the new peer on the empty record which will be edited
            grid.emptyRow.peerAddress = newPeerAddress;
        }
    },

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
        xtype: 'ungrid',
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
            'endpointAddress': '',
            'endpointPort': '',
            'peerAddress': '',
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
            width: Renderer.messageWidth,
            flex: 1,
            dataIndex: 'description',
        }, {
            header: 'Public Key'.t(),
            width: 290,
            dataIndex: 'publicKey',
        }, {
            header: 'Endpoint Address'.t(),
            width: Renderer.ipWidth,
            dataIndex: 'endpointAddress',
        }, {
            header: 'Endpoint Port'.t(),
            width: Renderer.portWidth,
            dataIndex: 'endpointPort',
        }, {
            header: 'Peer Address'.t(),
            width: Renderer.ipWidth,
            dataIndex: 'peerAddress',
        }, {
            header: 'Networks'.t(),
            width: Renderer.messageWidth,
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
            xtype: 'fieldcontainer',
            layout: 'hbox',
            items: [{
                xtype: 'checkbox',
                fieldLabel: 'Dynamic endpoint'.t(),
                labelWidth: 180,
                labelAlign: 'right',
                bind: '{record.endpointDynamic}'
            }, {
                xtype: 'textfield',
                fieldLabel: 'Address'.t(),
                labelAlign: 'right',
                flex: 1,
                disabled: true,
                allowBlank: false,
                vtype: 'isSingleIpValid',
                bind: {
                    value: '{record.endpointAddress}',
                    disabled: '{record.endpointDynamic}'
                }
            }, {
                xtype: 'textfield',
                fieldLabel: 'Port'.t(),
                labelAlign: 'right',
                width: 180,
                vtype: 'isSinglePortValid',
                disabled: true,
                allowBlank: false,
                bind: {
                    value: '{record.endpointPort}',
                    disabled: '{record.endpointDynamic}'
                }
            }]
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
            fieldLabel: 'Networks'.t(),
            vtype: 'cidrBlockArea',
            allowBlank: true,
            height: 60,
            bind: {
                value: '{record.networks}'
            }
        }, {
            xtype: 'fieldcontainer',
            layout: 'hbox',
            items: [{
                xtype: 'textfield',
                fieldLabel: 'Ping Address'.t(),
                labelWidth: 180,
                labelAlign: 'right',
                flex: 1,
                allowBlank: true,
                vtype: 'isSingleIpValid',
                bind: {
                    value: '{record.pingAddress}',
                }
            }, {
                xtype: 'numberfield',
                fieldLabel: 'Ping Interval'.t(),
                labelWidth: 120,
                labelAlign: 'right',
                width: 250,
                allowBlank: false,
                allowDecimals: false,
                emptyText: '0s to 300s ...',
                minValue: 0,
                maxValue: 300,
                bind: {
                    value: '{record.pingInterval}'
                }
            }]
        }, {
            xtype: 'checkbox',
            fieldLabel: 'Tunnel Up/Down Alerts'.t(),
            bind: '{record.pingConnectionEvents}'
        }, {
            xtype: 'checkbox',
            fieldLabel: 'Ping Unreachable Alerts'.t(),
            bind: '{record.pingUnreachableEvents}'
        }]

    }]
});
