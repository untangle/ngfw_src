Ext.define('Ung.apps.openvpn.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-openvpn-status',
    itemId: 'status',
    title: 'Status'.t(),

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
            html: '<img src="/skins/modern-rack/images/admin/apps/openvpn_80x80.png" width="80" height="80"/>' +
                '<h3>OpenVPN</h3>' +
                '<p>' + 'OpenVPN provides secure network access and tunneling to remote users and sites using the OpenVPN protocol.'.t() + '</p>'
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'grid',
            title: 'Connected Remote Clients'.t(),
            itemId: 'activeClients',
            trackMouseOver: false,
            sortableColumns: false,
            enableColumnHide: false,
            // forceFit: true,
            minHeight: 150,
            maxHeight: 250,
            margin: '0 0 10 0',
            resizable: true,
            resizeHandles: 's',
            viewConfig: {
                emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/> ' + 'No Active Clients'.t() + ' ...</p>',
                stripeRows: false
            },
            disabled: true,
            collapsible: true,
            hideCollapseTool: true,
            animCollapse: false,
            bind: {
                disabled: '{instance.targetState !== "RUNNING"}',
                store: '{clientStatusList}'
            },

            columns: [{
                header: 'Address'.t(),
                dataIndex: 'address',
                flex: 1
            }, {
                header: 'Client'.t(),
                dataIndex: 'clientName',
                width: 200
            }, {
                header: 'Pool Address'.t(),
                dataIndex: 'poolAddress',
                width: 150
            }, {
                header: 'Start Time'.t(),
                dataIndex: 'start',
                width: 150,
                renderer: function(value) { return Util.timestampFormat(value); }
            }, {
                header: 'Rx Data'.t(),
                dataIndex: 'bytesRxTotal',
                width: 100,
                renderer: function(value) { return (Math.round(value/100000)/10) + ' Mb'; }
            }, {
                header: 'Tx Data'.t(),
                dataIndex: 'bytesTxTotal',
                width: 100,
                renderer: function(value) { return (Math.round(value/100000)/10) + ' Mb'; }
            }],
            bbar: [{
                text: 'Refresh'.t(),
                iconCls: 'fa fa-refresh',
                handler: 'getActiveClients'
            }]

        }, {
            xtype: 'grid',
            title: 'Remote Server Status'.t(),
            itemId: 'remoteServers',
            trackMouseOver: false,
            sortableColumns: false,
            enableColumnHide: false,
            // forceFit: true,
            minHeight: 150,
            maxHeight: 250,
            margin: '10 0 10 0',
            resizable: true,
            resizeHandles: 's',
            viewConfig: {
                emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>' + 'No Remote Servers'.t() + ' ...</p>',
                stripeRows: false
            },

            disabled: true,
            collapsible: true,
            hideCollapseTool: true,
            animCollapse: false,
            bind: {
                disabled: '{instance.targetState !== "RUNNING"}',
                store: '{serverStatusList}'
            },

            columns: [{
                header: 'Name'.t(),
                dataIndex: 'name',
                width: 150,
                flex: 1
            }, {
                header: 'Connected'.t(),
                dataIndex: 'connected',
                width: 75
            }, {
                header: 'Rx Data'.t(),
                dataIndex: 'bytesRead',
                width: 180,
                renderer: function(value) { return (Math.round(value/100000)/10) + ' Mb'; }
            }, {
                header: 'Tx Data'.t(),
                dataIndex:'bytesWritten',
                width: 180,
                renderer: function(value) { return (Math.round(value/100000)/10) + ' Mb'; }
            }],
            bbar: [{
                text: 'Refresh'.t(),
                iconCls: 'fa fa-refresh',
                handler: 'getActiveServers'
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
