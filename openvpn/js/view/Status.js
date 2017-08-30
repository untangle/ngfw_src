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
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'ungrid',
            title: 'Connected Remote Clients'.t(),
            itemId: 'activeClients',
            trackMouseOver: false,
            enableColumnHide: false,

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
                disabled: '{instance.runState !== "RUNNING"}',
                store: '{clientStatusList}'
            },

            plugins: ['gridfilters'],

            columns: [{
                header: 'Address'.t(),
                dataIndex: 'address',
                width: Renderer.ipWidth,
                filter: Renderer.stringFilter,
                flex: 1
            }, {
                header: 'Client'.t(),
                dataIndex: 'clientName',
                width: Renderer.usernameWidth,
                filter: Renderer.stringFilter
            }, {
                header: 'Pool Address'.t(),
                dataIndex: 'poolAddress',
                width: Renderer.networkWidth,
                filter: Renderer.stringFilter
            }, {
                header: 'Start Time'.t(),
                dataIndex: 'start',
                renderer: Renderer.timestamp,
                width: Renderer.timestampWidth,
                filter: Renderer.timestampFilter
            }, {
                header: 'Rx Data'.t(),
                dataIndex: 'bytesRxTotal',
                width: Renderer.sizeWidth,
                renderer: Renderer.datasize,
                filter: Renderer.numericFilter
            }, {
                header: 'Tx Data'.t(),
                dataIndex: 'bytesTxTotal',
                width: Renderer.sizeWidth,
                renderer: Renderer.datasize,
                filter: Renderer.numericFilter
            }],
            bbar: [{
                text: 'Refresh'.t(),
                iconCls: 'fa fa-refresh',
                handler: 'externalAction',
                action: 'getActiveClients'
            }]

        }, {
            xtype: 'ungrid',
            title: 'Remote Server Status'.t(),
            itemId: 'remoteServers',
            trackMouseOver: false,
            enableColumnHide: false,

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
                disabled: '{instance.runState !== "RUNNING"}',
                store: '{serverStatusList}'
            },

            plugins: ['gridfilters'],

            columns: [{
                header: 'Name'.t(),
                dataIndex: 'name',
                width: Renderer.hostnameWidth,
                filter: Renderer.stringFilter,
                flex: 1
            }, {
                header: 'Connected'.t(),
                dataIndex: 'connected',
                width: Renderer.messageWidth,
                filter: Renderer.booleanFilter
            }, {
                header: 'Rx Data'.t(),
                dataIndex: 'bytesRead',
                width: Renderer.sizeWidth,
                renderer: Renderer.datasize,
                filter: Renderer.numericFilter
            }, {
                header: 'Tx Data'.t(),
                dataIndex:'bytesWritten',
                width: Renderer.sizeWidth,
                renderer: Renderer.datasize,
                filter: Renderer.numericFilter
            }],
            bbar: [{
                text: 'Refresh'.t(),
                iconCls: 'fa fa-refresh',
                handler: 'externalAction',
                action: 'getActiveServers'
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
