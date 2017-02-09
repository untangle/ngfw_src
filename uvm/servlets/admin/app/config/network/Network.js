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
        xtype: 'ung.config.network.interfaces'
    }, {
        xtype: 'ung.config.network.hostname'
    }, {
        xtype: 'ung.config.network.services'
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
        xtype: 'ung.config.network.portforwardrules'
    }, {
        xtype: 'ung.config.network.natrules'
    }, {
        xtype: 'ung.config.network.bypassrules'
    }, {
        xtype: 'ung.config.network.routes'
    }, {
        xtype: 'ung.config.network.dnsserver'
    }, {
        xtype: 'ung.config.network.dhcpserver'
    }, {
        xtype: 'ung.config.network.advanced'
    }, {
        xtype: 'ung.config.network.troubleshooting'
    }
    ]
});