Ext.define('Ung.apps.intrusionprevention.view.BypassRules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-intrusion-prevention-bypass',
    itemId: 'bypass-rules',
    scrollable: true,

    title: 'Bypass Rules'.t(),

    tbar: ['@add', '->', '@import', '@export'],
    recordActions: ['edit', 'delete', 'reorder'],

    emptyText: 'No Bypass Rules defined'.t(),

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
            Field.conditions(
                'com.untangle.uvm.network.BypassRuleCondition',[
                "DST_ADDR",
                "DST_PORT",
                "DST_INTF",
                "SRC_ADDR",
                "SRC_PORT",
                "SRC_INTF",
                "PROTOCOL",
                "CLIENT_TAGGED",
                "SERVER_TAGGED"
            ]),
           Field.bypass
    ]
});
