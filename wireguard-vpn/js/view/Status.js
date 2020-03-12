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
                '<h3>Wireguard VPN</h3>' +
                '<p>' + 'Wireguard VPN provides secure network access and tunneling to remote users and sites using the Wireguard VPN protocol.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appstate',
        }, {
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
                    width: Renderer.messageWidth,
                    filter: Renderer.stringFilter
                }, {
                    header: 'Public Key'.t(),
                    dataIndex: 'public-key',
                    width: Renderer.messageWidth,
                    filter: Renderer.stringFilter,
                    flex:1
                }, {
                    header: 'Private Key'.t(),
                    dataIndex: 'private-key',
                    width: Renderer.messageWidth,
                    filter: Renderer.stringFilter,
                    flex:1
                }, {
                    header: 'Listen Port'.t(),
                    dataIndex: 'listen-port',
                    width: Renderer.portWidth,
                    filter: Renderer.portFilter
                }, {
                    header: 'Firewall Mark'.t(),
                    dataIndex: 'fwmark',
                    width: Renderer.messageWidth,
                    filter: Renderer.stringFilter
                }, {
                    header: 'Peers'.t(),
                    dataIndex: 'peers',
                    width: Renderer.messageWidth,
                    flex:1
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
