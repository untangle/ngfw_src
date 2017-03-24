Ext.define('Ung.apps.wanbalancer.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-wan-balancer',
    controller: 'app-wan-balancer',

    viewModel: {
        stores: {
            routeRules: {
                data: '{settings.routeRules.list}'
            }
        }
    },

    items: [
        { xtype: 'app-wan-balancer-status' },
        { xtype: 'app-wan-balancer-trafficallocation' },
        { xtype: 'app-wan-balancer-routerules' }
    ]

});
