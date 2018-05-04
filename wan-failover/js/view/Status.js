Ext.define('Ung.apps.wan-failover.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-wan-failover-status',
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
            html: '<img src="/icons/apps/wan-failover.svg" width="80" height="80"/>' +
                '<h3>WAN Failover</h3>' +
                '<p>' + 'WAN Failover detects WAN outages and re-routes traffic to any other available WANs to maximize network uptime.'.t() + '</p>' +
                '<p>' + '<b>NOTE:</b> Tests must be configured using the <i>Tests</i> tab to determine the connectivity of each WAN.'.t() + '</p>'
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
            title: '<i class="fa fa-clock-o"></i> ' + 'WAN Status'.t(),
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
                html: '<i class="fa fa-exclamation-triangle fa-red fa-lg"></i> <strong>' + 'There are currently no tests configured. A test must be configured for each WAN.'.t() + '</strong>',
                margin: '0 0 10 0',
                hidden: true,
                bind: {
                    hidden: '{settings.tests.list.length > 0}'
                }
            }, {
                xtype: 'component',
                margin: '0 0 10 0',
                hidden: true,
                bind: {
                    html: '<ul style="margin: 0;">{wanWarnings}</ul>',
                    hidden: '{!wanWarnings}'
                }
            }, {
                xtype: 'ungrid',
                itemId: 'wanStatus',
                enableColumnHide: true,
                stateful: true,
                trackMouseOver: false,
                resizable: true,
                defaultSortable: true,

                flex: 1,

                emptyText: 'No External Interfaces'.t(),

                bind: {
                    store: '{wanStatusStore}'
                },

                plugins: ['gridfilters'],

                columns: [{
                    header: 'Interface ID'.t(),
                    dataIndex: 'interfaceId',
                    sortable: true,
                    width: Renderer.idWidth,
                    filter: Renderer.booleanFilter,
                }, {
                    header: 'Interface Name'.t(),
                    dataIndex: 'interfaceName',
                    sortable: true,
                    width: Renderer.idWidth,
                    flex: 1,
                    filter: Renderer.stringFilter,
                }, {
                    header: 'System Name'.t(),
                    dataIndex: 'systemName',
                    sortable: true,
                    width: Renderer.idWidth,
                    resizable: false,
                    filter: Renderer.stringFilter,
                }, {
                    header: 'Online Status',
                    dataIndex: 'online',
                    sortable: true,
                    width: Renderer.booeanWidth,
                    resizable: false,
                    filter: Renderer.booleanFilter,
                }, {
                    header: 'Current Tests Count'.t(),
                    dataIndex: 'totalTestsRun',
                    sortable: true,
                    width: Renderer.messageWidth,
                    resizable: false,
                    filter: Renderer.numericFilter,
                    renderer: Renderer.count
                }, {
                    header: 'Tests Passed'.t(),
                    dataIndex: 'totalTestsPassed',
                    sortable: true,
                    width: Renderer.sizeWidth,
                    resizable: false,
                    filter: Renderer.numericFilter,
                    renderer: Renderer.count
                }, {
                    header: 'Tests Failed'.t(),
                    sortable: true,
                    dataIndex: 'totalTestsFailed',
                    width: Renderer.sizeWidth,
                    filter: Renderer.numericFilter,
                    renderer: Renderer.count
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
            xtype: 'appmetrics'
        }],
        bbar: [{
            xtype: 'appremove',
            width: '100%'
        }]
    }]
});
