Ext.define('Ung.apps.wanfailover.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-wan-failover-status',
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
            html: '<img src="/skins/modern-rack/images/admin/apps/wan-failover_80x80.png" width="80" height="80"/>' +
                '<h3>WAN Failover</h3>' +
                '<p>' + 'WAN Failover detects WAN outages and re-routes traffic to any other available WANs to maximize network uptime.'.t() + '</p>' +
                '<p>' + '<b>NOTE:</b> Tests must be configured using the <i>Tests</i> tab to determine the connectivity of each WAN.'.t() + '</p>'
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
                collapsed: '{instance.targetState !== "RUNNING"}',
                disabled: '{instance.targetState !== "RUNNING"}'
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
                xtype: 'grid',
                itemId: 'wanStatus',
                trackMouseOver: false,
                sortableColumns: false,
                enableColumnHide: false,
                forceFit: true,
                minHeight: 120,
                maxHeight: 250,
                flex: 1,
                viewConfig: {
                    emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>No Active Sessions ...</p>',
                    stripeRows: false
                },

                bind: '{wanStatusStore}',

                columns: [{
                    header: 'Interface ID'.t(),
                    dataIndex: 'interfaceId',
                    width: 100,
                    resizable: false
                }, {
                    header: 'Interface Name'.t(),
                    dataIndex: 'interfaceName',
                    flex: 1,
                }, {
                    header: 'System Name'.t(),
                    dataIndex: 'systemName',
                    width: 100,
                    resizable: false
                }, {
                    header: 'Online Status',
                    dataIndex: 'online',
                    width: 100,
                    resizable: false
                }, {
                    header: 'Current Tests Count'.t(),
                    dataIndex: 'totalTestsRun',
                    width: 150,
                    resizable: false
                }, {
                    header: 'Tests Passed'.t(),
                    dataIndex: 'totalTestsPassed',
                    width: 100,
                    resizable: false
                }, {
                    header: 'Tests Failed'.t(),
                    dataIndex: 'totalTestsFailed',
                    width: 100
                }],
                bbar: [{
                    text: 'Refresh'.t(),
                    iconCls: 'fa fa-refresh',
                    handler: 'getWanStatus'
                }]
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
