Ext.define('Ung.apps.wanfailover.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-wan-failover',
    controller: 'app-wan-failover',

    viewModel: {

        data: {
            autoReload: false,
            wanStatusData: []
        },

        stores: {
            tests: {
                data: '{settings.tests.list}'
            },
            wanStatusStore: {
                data: '{wanStatusData}'
            }
        },

    },

    items: [
        { xtype: 'app-wan-failover-status' },
        { xtype: 'app-wan-failover-tests' }
    ]

});
