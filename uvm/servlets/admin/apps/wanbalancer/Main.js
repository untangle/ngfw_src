Ext.define('Ung.apps.wanbalancer.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.wanbalancer',

    viewModel: {
        data: {
            nodeName: 'untangle-node-wan-balancer',
            appName: 'WAN Balancer'
        }
    },

    items: [
        { xtype: 'app.wanbalancer.status' },
        { xtype: 'app.wanbalancer.trafficallocation' },
        { xtype: 'app.wanbalancer.routerules' }
    ]

});
