Ext.define('Ung.config.network.view.BypassRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-bypass-rules',
    itemId: 'bypass-rules',
    viewModel: true,
    scrollable: true,
    withValidation: false,
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
        itemId: 'bypass-rules-grid',
        srcAddrIsLanCheck: true,
        controller: 'unconfigbypassrulesgridcontroller',

        dockedItems: [{
            xtype: 'container',
            padding: '8 5',
            style: { fontSize: '12px', background: '#DADADA'},
            html: '<i class="fa fa-exclamation-triangle" style="color: red;"></i> ' + 'One or more rules have the condition of the source address a LAN address.'.t(),
            hidden: true,
            bind: {
                hidden: '{!warnBypassRuleSrcAddrIsLan}'
            }
        }],

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'copy', 'delete', 'reorder'],
        copyId: 'ruleId',  
        copyAppendField: 'description',

        emptyText: 'No Bypass Rules defined'.t(),

        listProperty: 'settings.bypassRules.list',

        importValidationJavaClass: true,

        importValidationForComboBox: true,

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
        editorXtype: 'ung.cmp.unconfigbypassrulesrecordeditor',
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
    }]
});
