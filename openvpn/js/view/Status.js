Ext.define('Ung.apps.openvpn.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-openvpn-status',
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
            html: '<img src="/icons/apps/openvpn.svg" width="80" height="80"/>' +
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
            xtype: 'fieldset',
            title: '<i class="fa fa-clock-o"></i> ' + 'Connected Remote Clients'.t(),
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
                itemId: 'activeClients',
                enableColumnHide: true,
                stateful: true,
                trackMouseOver: false,
                resizable: true,
                defaultSortable: true,

                emptyText: 'No Active Clients'.t(),

                bind: {
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
                bbar: ['@refresh', '@reset']
            }]
        }, {
            xtype: 'fieldset',
            title: '<i class="fa fa-clock-o"></i> ' + 'Connected Remote Servers'.t(),
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
                title: 'Remote Server Status'.t(),
                itemId: 'remoteServers',
                enableColumnHide: true,
                stateful: true,
                trackMouseOver: false,
                resizable: true,
                defaultSortable: true,

                emptyText: 'No Remote Servers'.t(),

                bind: {
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
