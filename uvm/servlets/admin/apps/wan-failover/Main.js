Ext.define('Ung.apps.wanfailover.Main', {
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
                data: '{wanStatusData}'
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
