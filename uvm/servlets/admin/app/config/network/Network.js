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

    portForwardConditions: [
        {name:"DST_LOCAL",displayName: 'Destined Local'.t(), type: "boolean", visible: true},
        {name:"DST_ADDR",displayName: 'Destination Address'.t(), type: "textfield", visible: true, vtype:"ipMatcher"},
        {name:"DST_PORT",displayName: 'Destination Port'.t(), type: "textfield",vtype:"portMatcher", visible: true},
        {name:"SRC_ADDR",displayName: 'Source Address'.t(), type: "textfield", visible: true, vtype:"ipMatcher"},
        {name:"SRC_PORT",displayName: 'Source Port'.t(), type: "textfield",vtype:"portMatcher", visible: rpc.isExpertMode},
        {name:"SRC_INTF",displayName: 'Source Interface'.t(), type: "checkboxgroup", values: [['a', 'a'], ['b', 'b']], visible: true},
        {name:"PROTOCOL",displayName: 'Protocol'.t(), type: "checkboxgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
    ],

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
        xtype: 'ung.config.network.natrules'
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