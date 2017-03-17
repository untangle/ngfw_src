Ext.define('Ung.apps.wanbalancer.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-wan-balancer',

    items: [
        { xtype: 'app-wan-balancer-status' },
        { xtype: 'app-wan-balancer-trafficallocation' },
        { xtype: 'app-wan-balancer-routerules' }
    ]

});
