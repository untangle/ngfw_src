Ext.define('Ung.config.network.Network', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.config.network',

    requires: [
        'Ung.config.network.NetworkController',
        'Ung.config.network.NetworkModel',

        'Ung.config.network.RuleEditor',

        'Ung.view.grid.Grid'
    ],

    controller: 'config.network',

    viewModel: {
        type: 'config.network'
    },

    // tabPosition: 'left',
    // tabRotation: 0,
    // tabStretchMax: false,

    dockedItems: [{
        xtype: 'toolbar',
        weight: -10,
        border: false,
        items: [{
            text: 'Back',
            iconCls: 'fa fa-arrow-circle-left fa-lg',
            hrefTarget: '_self',
            href: '#config'
        }, '-', {
            xtype: 'component',
            html: 'Network'
        }],
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        border: false,
        items: ['->', {
            text: 'Apply Changes'.t(),
            iconCls: 'fa fa-floppy-o fa-lg',
            handler: 'saveSettings'
        }]
    }],

    items: [{
        xtype: 'ung.config.network.interfaces'
    }, {
        xtype: 'ung.config.network.hostname'
    }, {
        xtype: 'ung.config.network.services'
    }, {
        xtype: 'ung.config.network.portforwardrules'
    }, {
        title: 'Routes'.t(),
        html: 'routes'
    }, {
        title: 'DNS Server'.t(),
        html: 'dns'
    }, {
        title: 'DHCP Server'.t(),
        html: 'dhcp'
    }, {
        title: 'Advanced'.t(),
        html: 'adv'
    }, {
        title: 'Troubleshooting'.t(),
        html: 'trb'
    }, {
        title: 'Reports'.t(),
        html: 'reports'
    }]
});