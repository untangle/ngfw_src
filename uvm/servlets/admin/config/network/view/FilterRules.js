Ext.define('Ung.config.network.view.FilterRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-filter-rules',
    itemId: 'filter-rules',
    viewModel: true,

    title: 'Filter Rules'.t(),

    layout: 'fit',

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: 'Filter Rules control what sessions are passed/blocked. Filter rules process all sessions including bypassed sessions. The rules are evaluated in order.'.t()
    }],

    items: [{
        xtype: 'ungrid',
        region: 'center',
        title: 'Filter Rules'.t(),

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete', 'reorder'],

        listProperty: 'settings.filterRules.list',
        ruleJavaClass: 'com.untangle.uvm.network.FilterRuleCondition',

        conditions: [
            {name:"DST_ADDR",displayName: "Destination Address".t(), type: 'textfield', visible: true, vtype:"ipMatcher"},
            {name:"DST_PORT",displayName: "Destination Port".t(), type: 'textfield',vtype:"portMatcher", visible: true},
            {name:"DST_INTF",displayName: "Destination Interface".t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, true), visible: true},
            {name:"SRC_MAC" ,displayName: "Source MAC".t(), type: 'textfield', visible: true},
            {name:"SRC_ADDR",displayName: "Source Address".t(), type: 'textfield', visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: "Source Port".t(), type: 'textfield',vtype:"portMatcher", visible: rpc.isExpertMode},
            {name:"SRC_INTF",displayName: "Source Interface".t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, true), visible: true},
            {name:"PROTOCOL",displayName: "Protocol".t(), type: 'checkboxgroup', values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
        ],

        emptyRow: {
            ruleId: -1,
            enabled: true,
            ipvsEnabled: false,
            description: '',
            javaClass: 'com.untangle.uvm.network.FilterRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: []
            },
            blocked: false
        },

        bind: '{filterRules}',

        columns: [{
            header: 'Rule Id'.t(),
            width: 70,
            align: 'right',
            resizable: false,
            dataIndex: 'ruleId',
            renderer: function (value) {
                return value < 0 ? 'new'.t() : value;
            }
        }, {
            xtype: 'checkcolumn',
            header: 'Enable'.t(),
            dataIndex: 'enabled',
            resizable: false,
            width: 70
        }, {
            xtype: 'checkcolumn',
            header: 'IPv6'.t(),
            dataIndex: 'ipv6Enabled',
            resizable: false,
            width: 70
        }, {
            header: 'Description',
            width: 200,
            dataIndex: 'description',
            renderer: function (value) {
                return value || '<em>no description<em>';
            }
        }, {
            header: 'Conditions'.t(),
            flex: 1,
            dataIndex: 'conditions',
            renderer: 'conditionsRenderer'
        }, {
            xtype: 'checkcolumn',
            header: 'Block'.t(),
            dataIndex: 'blocked',
            resizable: false,
            width: 70
        }],
        editorFields: [
            Field.enableRule('Enable Filter Rule'.t()),
            Field.enableIpv6,
            Field.description,
            Field.conditions,
            Field.blockedCombo
        ]
    }]
});
