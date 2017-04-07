Ext.define('Ung.config.network.view.PortForwardRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-portforwardrules',
    itemId: 'port_forward_rules',

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

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete', 'reorder'],

        listProperty: 'settings.portForwardRules.list',
        recordJavaClass: 'com.untangle.uvm.network.PortForwardRule',
        ruleJavaClass: 'com.untangle.uvm.network.PortForwardRuleCondition',

        conditions: [
            Condition.dstLocal,
            Condition.dstAddr,
            Condition.dstPort,
            Condition.srcAddr,
            Condition.srcPort,
            Condition.srcIntf,
            Condition.protocol([['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']])
        ],

        actionText: 'Forward to the following location:'.t(),
        emptyRow: {
            ruleId: -1,
            simple: true,
            enabled: true,
            // description: '',
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
            // name: 'portForwardWarnings',
            bind: {
                html: '{portForwardWarnings}'
            }
        }]
    }]
});
