Ext.define('Ung.config.network.view.NatRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-nat-rules',
    itemId: 'nat-rules',
    scrollable: true,

    viewModel: true,

    title: 'NAT Rules'.t(),

    layout: 'fit',

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: 'NAT Rules control the rewriting of the IP source address of traffic (Network Address Translation). The rules are evaluated in order.'.t()
    }],

    items: [{
        xtype: 'ungrid',
        flex: 3,

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete', 'reorder'],

        emptyText: 'No NAT Rules defined'.t(),

        listProperty: 'settings.natRules.list',
        ruleJavaClass: 'com.untangle.uvm.network.NatRuleCondition',

        conditions: [
            {name:"DST_ADDR",displayName: "Destination Address".t(), type: 'textfield', visible: true, vtype:"ipMatcher"},
            {name:"DST_PORT",displayName: "Destination Port".t(), type: 'textfield',vtype:"portMatcher", visible: true},
            {name:"DST_INTF",displayName: "Destination Interface".t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, true), visible: true},
            {name:"SRC_ADDR",displayName: "Source Address".t(), type: 'textfield', visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: "Source Port".t(), type: 'textfield',vtype:"portMatcher", visible: Rpc.directData('rpc.isExpertMode')},
            {name:"SRC_INTF",displayName: "Source Interface".t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, true), visible: true},
            {name:"PROTOCOL",displayName: "Protocol".t(), type: 'checkboxgroup', values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"],["OSPF","OSPF"]], visible: true},
            {name:"CLIENT_TAGGED",displayName: 'Client Tagged'.t(), type: 'textfield', visible: true},
            {name:"SERVER_TAGGED",displayName: 'Server Tagged'.t(), type: 'textfield', visible: true},
        ],

        emptyRow: {
            ruleId: -1,
            enabled: true,
            auto: true,
            javaClass: 'com.untangle.uvm.network.NatRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: []
            },
            description: ''
        },

        bind: '{natRules}',

        columns: [{
            header: 'Rule Id'.t(),
            width: Renderer.idWidth,
            align: 'right',
            resizable: false,
            dataIndex: 'ruleId',
            renderer: Renderer.id
        }, {
            xtype: 'checkcolumn',
            header: 'Enable'.t(),
            dataIndex: 'enabled',
            resizable: false,
            width: Renderer.booleanWidth,
        }, {
            header: 'Description',
            width: Renderer.messageWidth,
            dataIndex: 'description'
        }, {
            header: 'Conditions'.t(),
            width: Renderer.messageWidth,
            flex: 1,
            dataIndex: 'conditions',
            renderer: 'conditionsRenderer'
        }, {
            header: 'NAT Type'.t(),
            dataIndex: 'auto',
            width: Renderer.idWidth,
            renderer: Ung.config.network.MainController.natTypeRenderer
        }, {
            header: 'New Source'.t(),
            dataIndex: 'newSource',
            width: Renderer.networkWidth,
            renderer: Ung.config.network.MainController.natNewSourceRenderer
        }],
        editorFields: [
            Field.enableRule('Enable NAT Rule'.t()),
            Field.description,
            Field.conditions,
            Field.natType,
            Field.natSource
        ]
    }]
});
