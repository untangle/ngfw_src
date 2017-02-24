Ext.define('Ung.config.network.view.BypassRules', {
    extend: 'Ext.panel.Panel',
    // xtype: 'ung.config.network.bypassrules',
    alias: 'widget.config.network.bypassrules',

    viewModel: true,

    title: 'Bypass Rules'.t(),

    layout: 'fit',

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: 'Bypass Rules control what traffic is scanned by the applications. Bypassed traffic skips application processing. The rules are evaluated in order. Sessions that meet no rule are not bypassed.'.t()
    }],

    items: [{
        xtype: 'ungrid',

        tbar: ['@add'],
        recordActions: ['@edit', '@delete', '@reorder'],
        listProperty: 'settings.bypassRules.list',
        ruleJavaClass: 'com.untangle.uvm.network.BypassRuleCondition',
        conditions: [
            Condition.dstAddr,
            Condition.dstPort,
            Condition.dstIntf,
            Condition.srcAddr,
            Condition.srcPort,
            Condition.srcIntf,
            Condition.protocol([['TCP','TCP'],['UDP','UDP']])
        ],
        emptyRow: {
            ruleId: -1,
            enabled: true,
            bypass: true,
            javaClass: 'com.untangle.uvm.network.BypassRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: []
            },
            description: ''
        },

        bind: '{bypassRules}',

        columns: [
            Column.ruleId,
            Column.enabled,
            Column.description,
            Column.conditions,
            Column.bypass
        ],
        editorFields: [
            Field.enableRule(),
            Field.description,
            Field.conditions,
            Field.bypass
        ]
    }]
});
