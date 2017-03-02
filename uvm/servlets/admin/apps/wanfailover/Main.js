Ext.define('Ung.apps.wanfailover.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.wanfailover',

    viewModel: {
        data: {
            nodeName: 'untangle-node-wan-failover',
            appName: 'WAN Failover'
        }
    },

    items: [
        { xtype: 'app.wanfailover.status' },
        { xtype: 'app.wanfailover.tests' }
    ]

});
