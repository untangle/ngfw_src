Ext.define('Ung.apps.wanbalancer.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-wan-balancer',
    controller: 'app-wan-balancer',

    viewModel: {

        data: {
            autoRefresh: false,
            interfaceWeightData: []
        },

        stores: {
            routeRules: {
                data: '{settings.routeRules.list}'
            },
            interfaceWeightList: {
                data: '{interfaceWeightData}'
            }
        }
    },

    items: [
        { xtype: 'app-wan-balancer-status' },
        { xtype: 'app-wan-balancer-trafficallocation' },
        { xtype: 'app-wan-balancer-routerules' }
    ]

});
