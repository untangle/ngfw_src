Ext.define('Ung.apps.wireguard-vpn.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-wireguard-vpn-status',
    itemId: 'status',
    title: 'Status'.t(),
    scrollable: true,

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,

        layout: {
            type: 'vbox',
            align: 'stretch'
        },

        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/icons/apps/wireguard-vpn.svg" width="80" height="80"/>' +
                '<h3>WireGuard VPN</h3>' +
                '<p>' + 'WireGuard VPN provides secure network access and tunneling to remote users and sites using the WireGuard VPN protocol.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appstate',
        },
        Ung.apps['wireguard-vpn'].Main.hostDisplayFields(false),
        {
            xtype: 'fieldset',
            title: '<i class="fa fa-clock-o"></i> ' + 'Connected Tunnels'.t(),
            padding: 10,
            margin: '20 0',
            cls: 'app-section',

            layout: {
                type: 'vbox',
                align: 'stretch'
            },

            collapsed: true,
            disabled: true,
            bind: {
                collapsed: '{!state.on}',
                disabled: '{!state.on}'
            },
            items: [{
                xtype: 'component',
                margin: '0 0 10 0',
                hidden: true,
                bind: {
                    html: '{warning}',
                    hidden: '{!warning}'
                }
            },{
                xtype: 'ungrid',
                itemId: 'tunnelStatus',
                enableColumnHide: true,
                stateful: true,
                trackMouseOver: false,
                resizable: true,
                defaultSortable: true,

                emptyText: 'No Active Wireguard Tunnels'.t(),

                bind: {
                    store: '{tunnelStatusList}'
                },

                plugins: ['gridfilters'],

                columns: [{
                    header: 'Interface'.t(),
                    dataIndex: 'interface',
                    width: Renderer.idWidth,
                    filter: Renderer.stringFilter,
                    hidden: true
                }, {
                    header: 'Description'.t(),
                    dataIndex: 'tunnel-description',
                    width: Renderer.messageWidth,
                    filter: Renderer.stringFilter,
                    flex: 1
                }, {
                    header: 'Peer Public Key'.t(),
                    dataIndex: 'peer-key',
                    width: Renderer.messageWidth,
                    filter: Renderer.stringFilter,
                    flex: 1,
                    hidden: true
                }, {
                    header: 'Allowed IPs'.t(),
                    dataIndex: 'allowed-ips',
                    width: Renderer.networkWidth,
                }, {
                    header: 'Endpoint'.t(),
                    dataIndex: 'endpoint',
                    width: Renderer.ipWidth + Renderer.portWidth
                }, {
                    header: 'Last Handshake'.t(),
                    dataIndex: 'latest-handshake',
                    width: Renderer.timestampWidth,
                    renderer: Ung.apps['wireguard-vpn'].Main.statusHandshakeRenderer,
                    filter: Renderer.timestampFilter
                }, {
                    header: 'Keepalive'.t(),
                    dataIndex: 'persistent-keepalive',
                    width: Renderer.messageWidth,
                    hidden: true
                }, {
                    header: 'Bytes In'.t(),
                    dataIndex: 'transfer-rx',
                    width: Renderer.sizeWidth,
                    renderer: Renderer.datasize
                }, {
                    header: 'Bytes Out'.t(),
                    dataIndex: 'transfer-tx',
                    width: Renderer.sizeWidth,
                    renderer: Renderer.datasize
                }],
                bbar: ['@refresh', '@reset']
            }]
        }, {
            xtype: 'appreports'
        }]
    }, {
        region: 'west',
        border: false,
        width: Math.ceil(Ext.getBody().getViewSize().width / 4),
        split: true,
        layout: 'fit',
        items: [{
            xtype: 'appmetrics',
        }],
        bbar: [{
            xtype: 'appremove',
            width: '100%'
        }]
    }]

});
