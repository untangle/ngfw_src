Ext.define('Ung.apps.wanfailover.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-wan-failover',
    controller: 'app-wan-failover',

    viewModel: {
        stores: {
            tests: {
                data: '{settings.tests.list}'
            }
        }
    },

    items: [
        { xtype: 'app-wan-failover-status' },
        { xtype: 'app-wan-failover-tests' }
    ]

});
