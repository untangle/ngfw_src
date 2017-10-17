Ext.define('Ung.config.network.view.PortForwardRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-port-forward-rules',
    itemId: 'port-forward-rules',

    viewModel: true,

    title: 'Port Forward Rules'.t(),

    layout: { type: 'vbox', align: 'stretch' },

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: 'Port Forward rules forward sessions matching the configured criteria from a public IP to an IP on an internal (NAT\'d) network. The rules are evaluated in order.'.t()
    }],

    items: [{
        xtype: 'ungrid',
        name: 'Port Forward Rules',
        flex: 3,

        simpleEditorAlias: 'config-network-portforwardsimple',

        tbar: ['@addSimple', '->', '@import', '@export'],
        recordActions: ['edit', 'delete', 'reorder'],

        listProperty: 'settings.portForwardRules.list',
        recordJavaClass: 'com.untangle.uvm.network.PortForwardRule',
        ruleJavaClass: 'com.untangle.uvm.network.PortForwardRuleCondition',

        conditions: [
            {name:"DST_LOCAL",displayName: "Destined Local".t(), type: "boolean", visible: true},
            {name:"DST_ADDR",displayName: "Destination Address".t(), type: "textfield", visible: true, vtype:"ipMatcher"},
            {name:"DST_PORT",displayName: "Destination Port".t(), type: "textfield",vtype:"portMatcher", visible: true},
            {name:"SRC_ADDR",displayName: "Source Address".t(), type: "textfield", visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: "Source Port".t(), type: "textfield",vtype:"portMatcher", visible: rpc.isExpertMode},
            {name:"SRC_INTF",displayName: "Source Interface".t(), type: "checkboxgroup", values: Util.getInterfaceList(true, true), visible: true},
            {name:"PROTOCOL",displayName: "Protocol".t(), type: "checkboxgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true},
            {name:"CLIENT_TAGGED",displayName: 'Client Tagged'.t(), type: 'textfield', visible: true},
            {name:"SERVER_TAGGED",displayName: 'Server Tagged'.t(), type: 'textfield', visible: true},
        ],

        actionText: 'Forward to the following location:'.t(),
        emptyRow: {
            ruleId: -1,
            simple: true,
            enabled: true,
            description: '',
            javaClass: 'com.untangle.uvm.network.PortForwardRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: [{
                    conditionType: 'DST_LOCAL',
                    invert: false,
                    value: 'true',
                    javaClass: 'com.untangle.uvm.network.PortForwardRuleCondition'
                }, {
                    conditionType: 'PROTOCOL',
                    invert: false,
                    value: 'TCP',
                    javaClass: 'com.untangle.uvm.network.PortForwardRuleCondition'
                }, {
                    conditionType:'DST_PORT',
                    invert: false,
                    value: '80',
                    javaClass: 'com.untangle.uvm.network.PortForwardRuleCondition'
                }]
            },
            newPort: 80
        },

        bind: '{portForwardRules}',

        columns: [
            Column.ruleId,
            Column.enabled,
            Column.description,
            Column.conditions, {
                header: 'New Destination'.t(),
                dataIndex: 'newDestination',
                width: 150
            }, {
                header: 'New Port'.t(),
                dataIndex: 'newPort',
                width: 80
            }],
        editorFields: [
            Field.enableRule('Enable Port Forward Rule'.t()),
            Field.description,
            Field.conditions,
            Field.newDestination,
            Field.newPort
        ]
    }, {
        xtype: 'fieldset',
        flex: 2,
        margin: 10,
        padding: 10,
        // border: true,
        collapsible: false,
        collapsed: false,
        autoScroll: true,
        style: {
            lineHeight: 1.4
        },
        title: 'The following ports are currently reserved and can not be forwarded:'.t(),
        items: [{
            xtype: 'component',
            bind: {
                html: '{portForwardWarnings}'
            }
        }]
    }]
});
