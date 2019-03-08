Ext.define('Ung.apps.ipsecvpn.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ipsec-vpn-status',
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
            html: '<img src="/icons/apps/ipsec-vpn.svg" width="80" height="80"/>' +
                '<h3>IPsec VPN</h3>' +
                '<p>' + 'IPsec VPN provides secure network access and tunneling to remote users and sites using IPsec, GRE, L2TP, Xauth, and IKEv2 protocols.'.t() + '</p>'
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
            title: '<i class="fa fa-clock-o"></i> ' + 'Enabled IPsec Tunnels'.t(),
            padding: 10,
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

                emptyText: 'No Enabled IPsec VPN Tunnels'.t(),

                bind: {
                    store: '{tunnelStatusStore}'
                },

                plugins: ['gridfilters'],

                columns: [{
                    header: 'Status'.t(),
                    dataIndex: 'mode',
                    sortable: true,
                    width: Renderer.messagwWidth,
                    renderer: Ung.apps.ipsecvpn.MainController.modeRenderer,
                    filter: Renderer.stringFilter
                }, {
                    header: 'Local IP'.t(),
                    dataIndex: 'src',
                    sortable: true,
                    width: Renderer.ipWidth,
                    filter: Renderer.stringFilter
                }, {
                    header: 'Remote Host'.t(),
                    dataIndex: 'dst',
                    sortable: true,
                    width: Renderer.hostnameWidth,
                    filter: Renderer.stringFilter
                }, {
                    header: 'Local Network'.t(),
                    dataIndex: 'tmplSrc',
                    sortable: true,
                    width: Renderer.networkWidth,
                    filter: Renderer.stringFilter
                }, {
                    header: 'Remote Network'.t(),
                    dataIndex: 'tmplDst',
                    sortable: true,
                    width: Renderer.networkWidth,
                    filter: Renderer.stringFilter
                }, {
                    header: 'Description'.t(),
                    dataIndex: 'proto',
                    sortable: true,
                    width: Renderer.messageWidth,
                    flex: 1,
                    filter: Renderer.stringFilter
                }, {
                    header: 'Bytes In'.t(),
                    dataIndex: 'inBytes',
                    sortable: true,
                    width: Renderer.sizeWidth,
                    filter: Renderer.numericFilter,
                    renderer: Renderer.count
                }, {
                    header: 'Bytes Out'.t(),
                    dataIndex: 'outBytes',
                    sortable: true,
                    width: Renderer.sizeWidth,
                    filter: Renderer.numericFilter,
                    renderer: Renderer.count
                }],
                bbar: [ '@refresh', '@reset']
            }]
        }, {
            xtype: 'fieldset',
            title: '<i class="fa fa-clock-o"></i> ' + 'Active VPN Sessions'.t(),
            padding: 10,
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
                itemId: 'virtualUsers',
                enableColumnHide: true,
                stateful: true,
                trackMouseOver: false,
                resizable: true,
                defaultSortable: true,

                flex: 1,

                emptyText: 'No Active VPN Sessions'.t(),

                bind: {
                    store: '{virtualUserStore}'
                },

                plugins: ['gridfilters'],

                columns: [{
                    header: 'IP Address'.t(),
                    dataIndex: 'clientAddress',
                    sortable: true,
                    width: Renderer.ipWidth,
                    filter: Renderer.stringFilter
                }, {
                    header: 'Protocol'.t(),
                    dataIndex: 'clientProtocol',
                    sortable: true,
                    width: Renderer.protocolWidth,
                    filter: Renderer.stringFilter
                }, {
                    header: 'Username'.t(),
                    dataIndex: 'clientUsername',
                    sortable: true,
                    width: Renderer.usernameWidth,
                    flex: 1,
                    filter: Renderer.stringFilter
                }, {
                    header: 'Interface'.t(),
                    dataIndex: 'netInterface',
                    sortable: true,
                    width: Renderer.idWidth,
                    filter: Renderer.stringFilter
                }, {
                    header: 'Connect Time'.t(),
                    dataIndex: 'sessionCreation',
                    sortable: true,
                    width: Renderer.timestampWidth,
                    filter: Renderer.timestampFilter,
                    renderer: Renderer.timestamp
                }, {
                    header: 'Elapsed Time'.t(),
                    dataIndex: 'sessionElapsed',
                    width: Renderer.timestampWidth,
                    sortable: true,
                    renderer: Renderer.elapsedTime,
                    filter: Renderer.numericFilter,
                },{
                    header: 'Disconnect'.t(),
                    xtype: 'actioncolumn',
                    width: Renderer.actionWidth + 20,
                    align: 'center',
                    iconCls: 'fa fa-minus-circle',
                    handler: 'externalAction',
                    action: 'disconnectUser',
                }],
                bbar: [ '@refresh', '@reset']

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
