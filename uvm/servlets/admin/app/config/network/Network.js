Ext.define('Ung.config.network.Network', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.config.network',

    requires: [
        'Ung.config.network.NetworkController',
        'Ung.config.network.NetworkModel',
        'Ung.config.network.Interface',

        // 'Ung.view.grid.Grid',
        // 'Ung.store.RuleConditions',
        'Ung.store.Rule',
        'Ung.model.Rule',
        'Ung.cmp.Grid'
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
            xtype: 'tbtext',
            html: '<strong>' + 'Network'.t() + '</strong>'
        }],
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        border: false,
        items: ['->', {
            text: 'Apply Changes'.t(),
            scale: 'large',
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
    }, {
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
    }]
});