Ext.define('Ung.apps.wanbalancer.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-wanbalancer',

    items: [
        { xtype: 'app-wanbalancer-status' },
        { xtype: 'app-wanbalancer-trafficallocation' },
        { xtype: 'app-wanbalancer-routerules' }
    ]

});
