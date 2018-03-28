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

    items: [
        { xtype: 'app-wan-failover-status' },
        { xtype: 'app-wan-failover-tests' }
    ]

});
