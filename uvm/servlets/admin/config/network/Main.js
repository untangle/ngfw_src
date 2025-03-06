Ext.define('Ung.config.network.Main', {
    extend: 'Ung.cmp.ConfigPanel',
    alias: 'widget.config-network',
    itemId: 'network',
    /* requires-start */
    requires: [
        'Ung.config.network.MainController',
        'Ung.config.network.MainModel',
        'Ung.config.network.Interface',

        'Ung.store.Rule',
        'Ung.model.Rule',
        'Ung.cmp.Grid',
        'Ung.util.Renderer'
    ],
    /* requires-end */
    controller: 'config-network',

    viewModel: {
        type: 'config-network',
        data: {
            allWanInterfaceNames: [],
        },
        stores: {
            allWanInterfaceNamesStore: {
                data: '{allWanInterfaceNames}',
            },
        },
    },

    // tabPosition: 'left',
    // tabRotation: 0,
    // tabStretchMax: false,

    items: [
        { xtype: 'config-network-interfaces' },
        { xtype: 'config-network-hostname' },
        { xtype: 'config-network-services' },
        { xtype: 'config-network-port-forward-rules' },
        { xtype: 'config-network-nat-rules' },
        { xtype: 'config-network-bypass-rules' },
        { xtype: 'config-network-filter-rules' },
        { xtype: 'config-network-routes' },
        { xtype: 'config-network-dynamicrouting' },
        { xtype: 'config-network-dns-server' },
        { xtype: 'config-network-dhcp-server' },
        { xtype: 'config-network-advanced' },
        { xtype: 'config-network-troubleshooting' }
    ]
});
