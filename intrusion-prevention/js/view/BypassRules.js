Ext.define('Ung.apps.intrusionprevention.view.BypassRules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-intrusion-prevention-bypass',
    itemId: 'bypass-rules',
    scrollable: true,
    withValidation: false,
    editorFieldProtocolTcpUdpOnly: true,
    title: 'Bypass Rules'.t(),
    controller: 'unintrusionbypassrulesgrid',
    srcAddrIsLanCheck: true,

    listeners: {
        afterrender: 'warnSrcAddrIsLan',
        itemclick: 'warnSrcAddrIsLan'
    },

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

    importValidationJavaClass: true,

    importValidationForComboBox: true,

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
        editorXtype: 'ung.cmp.unintrusionbypassrulesrecordeditor',
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
