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
                '<p>' + 'Tunnel VPN Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.'.t() + '</p>'
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
            itemId: 'tunnelStates',
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
            bind: {
                store: '{tunnelStatesList}'
            },

            columns: [{
                header: 'Tunnel ID'.t(),
                dataIndex: 'tunnelId',
                width: 75
            }, {
                header: 'Tunnel Name'.t(),
                dataIndex: 'name',
                width: 150,
                flex: 1
            }, {
                header: 'Tunnel Status'.t(),
                dataIndex: 'state',
                width: 180,
            }],
            bbar: [{
                text: 'Refresh'.t(),
                iconCls: 'fa fa-refresh',
                handler: 'getTunnelStates'
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
