Ext.define('Ung.apps.wan-failover.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-wan-failover',
    controller: 'app-wan-failover',

    viewModel: {

        data: {
            autoReload: false,
            wanStatusData: [],
            pingListData: []
        },

        stores: {
            tests: {
                data: '{settings.tests.list}'
            },
            wanStatusStore: {
                data: '{wanStatusData}',
                fields: [{
                    name: 'systemName'
                },{
                    name: 'totalTestsPassed',
                    sortType: 'asInt'
                },{
                    name: 'totalTestsRun',
                    sortType: 'asInt'
                },{
                    name: 'online'
                },{
                    name: 'interfaceId',
                    sortType: 'asInt'
                },{
                    name: 'interfaceName'
                },{
                    name: 'totalTestsFailed',
                    sortType: 'asInt'
                }]
            },
            pingListStore: {
                fields: [ 'name' , 'addr' ],
                data: '{pingListData}'
            },
        },

    },

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/wan-failover',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            },
            hidden: true,
            bind: {
                hidden: '{!reportsEnabled || instance.runState !== "RUNNING"}'
            }
        }]
    },

    items: [
        { xtype: 'app-wan-failover-status' },
        { xtype: 'app-wan-failover-tests' }
    ]

});
