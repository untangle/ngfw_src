Ext.define('Ung.apps.tunnel-vpn.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-tunnel-vpn-status',
    itemId: 'status',
    title: 'Status'.t(),
    viewModel: true,

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/skins/modern-rack/images/admin/apps/tunnel-vpn_80x80.png" width="80" height="80"/>' +
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
            xtype: 'grid',
            title: 'Tunnel Status'.t(),
            itemId: 'tunnelStatus',
            trackMouseOver: false,
            sortableColumns: false,
            enableColumnHide: false,
            minHeight: 150,
            maxHeight: 250,
            margin: '10 0 10 0',
            resizable: true,
            resizeHandles: 's',
            viewConfig: {
                emptyText: '',
                stripeRows: false
            },

            collapsible: true,
            hideCollapseTool: true,
            animCollapse: false,
            recordJavaClass: 'com.untangle.app.tunnel_vpn.TunnelVpnStatusRecord',
            bind: {
                store: '{tunnelStatusList}'
            },

            columns: [{
                header: 'Tunnel ID'.t(),
                dataIndex: 'tunnelId',
                width: 75
            }, {
                header: 'Tunnel Name'.t(),
                dataIndex: 'tunnelName',
                width: 150,
                flex: 1
            }, {
                header: 'Elapsed Time'.t(),
                dataIndex: 'elapsedTime',
                width: 180,
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
                width: Renderer.sizeWidth,
                renderer: Renderer.datasize,
                filter: Renderer.numericFilter
            }, {
                header: 'Tx Data'.t(),
                dataIndex: 'xmitTotal',
                width: Renderer.sizeWidth,
                renderer: Renderer.datasize,
                filter: Renderer.numericFilter
            }, {
                header: 'Tunnel Status'.t(),
                dataIndex: 'stateInfo',
                width: 180
            }, {
                header: 'Recycle'.t(),
                xtype: 'actioncolumn',
                width: 80,
                align: 'center',
                iconCls: 'fa fa-recycle',
                handler: 'recycleTunnel',
            }],
            bbar: [{
                text: 'Refresh'.t(),
                iconCls: 'fa fa-refresh',
                handler: 'getTunnelStatus'
            }]

        }, {
            xtype: 'appreports'
        }]
    }, {
        region: 'west',
        border: false,
        width: 350,
        minWidth: 300,
        split: true,
        layout: 'border',
        items: [{
            xtype: 'appmetrics',
            region: 'center'
        }],
        bbar: [{
            xtype: 'appremove',
            width: '100%'
        }]
    }]
});
