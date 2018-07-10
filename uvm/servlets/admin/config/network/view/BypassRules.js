Ext.define('Ung.config.network.view.BypassRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-bypass-rules',
    itemId: 'bypass-rules',
    viewModel: true,
    scrollable: true,

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

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete', 'reorder'],

        emptyText: 'No Bypass Rules defined'.t(),

        listProperty: 'settings.bypassRules.list',
        ruleJavaClass: 'com.untangle.uvm.network.BypassRuleCondition',
        conditions: Ung.cmp.ConditionsEditor.buildConditions(
            "DST_ADDR",
            "DST_PORT",
            "DST_INTF",
            "SRC_ADDR",
            "SRC_PORT",
            "SRC_INTF",
            "PROTOCOL",
            "CLIENT_TAGGED",
            "SERVER_TAGGED"
        ),

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
        {
            header: 'Bypass'.t(),
            xtype: 'checkcolumn',
            dataIndex: 'bypass',
            width: Renderer.booleanWidth
        }],
        editorFields: [
            Field.enableRule(),
            Field.description,
            Field.conditions,
            Field.bypass
        ]
    }]
});
