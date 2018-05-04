Ext.define('Ung.apps.tunnel-vpn.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-tunnel-vpn-status',
    itemId: 'status',
    title: 'Status'.t(),
    viewModel: true,
    scrollable: true,

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/icons/apps/tunnel-vpn.svg" width="80" height="80"/>' +
                '<h3>Tunnel VPN</h3>' +
                '<p>' + 'Tunnel VPN provides connectivity through encrypted tunnels to remote VPN servers and services.'.t() + '</p>'
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
            title: '<i class="fa fa-clock-o"></i> ' + 'Tunnel Status'.t(),
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

                flex: 1,

                emptyText: 'No Tunnels defined'.t(),

                bind: {
                    store: '{tunnelStatusList}'
                },

                plugins: ['gridfilters'],

                columns: [{
                    header: 'Tunnel ID'.t(),
                    dataIndex: 'tunnelId',
                    sortable: true,
                    width: Renderer.idWidth
                }, {
                    header: 'Tunnel Name'.t(),
                    dataIndex: 'tunnelName',
                    sortable: true,
                    width: Renderer.messageWidth,
                    flex: 1
                }, {
                    header: 'Elapsed Time'.t(),
                    dataIndex: 'elapsedTime',
                    sortable: true,
                    width: Renderer.messageWidth,
                    renderer: function(value) {
                        var total = parseInt(value / 1000,10);
                        var hours = (parseInt(total / 3600,10) % 24);
                        var minutes = (parseInt(total / 60,10) % 60);
                        var seconds = parseInt(total % 60,10);
                        var result = (hours < 10 ? "0" + hours : hours) + ":" + (minutes < 10 ? "0" + minutes : minutes) + ":" + (seconds  < 10 ? "0" + seconds : seconds);
                        return result;
                    }
                }, {
                    header: 'Rx Data'.t(),
                    dataIndex: 'recvTotal',
                    sortable: true,
                    width: Renderer.sizeWidth,
                    renderer: Renderer.datasize,
                    filter: Renderer.numericFilter
                }, {
                    header: 'Tx Data'.t(),
                    dataIndex: 'xmitTotal',
                    sortable: true,
                    width: Renderer.sizeWidth,
                    renderer: Renderer.datasize,
                    filter: Renderer.numericFilter
                }, {
                    header: 'Tunnel Status'.t(),
                    sortable: true,
                    dataIndex: 'stateInfo',
                    width: Renderer.messageWidth
                }, {
                    header: 'Recycle'.t(),
                    xtype: 'actioncolumn',
                    width: Renderer.actionWidth,
                    align: 'center',
                    iconCls: 'fa fa-recycle',
                    handler: 'externalAction',
                    action: 'recycleTunnel',
                }],
                bbar: [ '@refresh', '@reset']
            }]
        },{
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
            scrollable: true,
            region: 'center'
        }],
        bbar: [{
            xtype: 'appremove',
            width: '100%'
        }]
    }]
});
