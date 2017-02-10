Ext.define('Ung.config.network.Network', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.config.network',

    requires: [
        'Ung.config.network.NetworkController',
        'Ung.config.network.NetworkModel',

        'Ung.view.grid.Grid',
        'Ung.store.RuleConditions',
        'Ung.store.Rule',
        'Ung.cmp.Rules'
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
        xtype: 'config.network.interfaces'
    }, {
        xtype: 'config.network.hostname'
    }, {
        xtype: 'config.network.services'
    },
    // {
    //     xtype: 'panel',
    //     layout: 'border',
    //     title: 'Mix',
    //     items: [{
    //         xtype: 'ung.config.network.portforwardrules',
    //         region: 'center'
    //     }, {
    //         xtype: 'ung.config.network.natrules',
    //         region: 'south',
    //         height: 300
    //     }]
    // }
    {
        xtype: 'config.network.portforwardrules'
    }, {
        xtype: 'config.network.natrules'
    }, {
        xtype: 'config.network.bypassrules'
    }, {
        xtype: 'config.network.routes'
    }, {
        xtype: 'config.network.dnsserver'
    }, {
        xtype: 'config.network.dhcpserver'
    }, {
        xtype: 'config.network.advanced'
    }, {
        xtype: 'config.network.troubleshooting'
    }
    ]
});