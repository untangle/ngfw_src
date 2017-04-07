Ext.define('Ung.config.network.Main', {
    extend: 'Ung.cmp.ConfigPanel',
    alias: 'widget.config-network',
    itemId: 'network',
    /* requires-start */
    requires: [
        'Ung.config.network.MainController',
        'Ung.config.network.MainModel',
        'Ung.config.network.Interface',

        // 'Ung.view.grid.Grid',
        // 'Ung.store.RuleConditions',
        'Ung.store.Rule',
        'Ung.model.Rule',
        'Ung.cmp.Grid'
    ],
    /* requires-end */
    controller: 'config-network',

    viewModel: {
        type: 'config-network'
    },

    // tabPosition: 'left',
    // tabRotation: 0,
    // tabStretchMax: false,

    items: [
        { xtype: 'config-network-interfaces' },
        { xtype: 'config-network-hostname' },
        { xtype: 'config-network-services' },
        { xtype: 'config-network-portforwardrules' },
        { xtype: 'config-network-natrules' },
        { xtype: 'config-network-bypassrules' },
        { xtype: 'config-network-routes' },
        { xtype: 'config-network-dnsserver' },
        { xtype: 'config-network-dhcpserver' },
        { xtype: 'config-network-advanced' },
        { xtype: 'config-network-troubleshooting' }
    ]
});
