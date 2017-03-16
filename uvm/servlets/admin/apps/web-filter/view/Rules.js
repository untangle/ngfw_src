Ext.define('Ung.apps.webfilter.view.Rules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-web-filter-rules',
    itemId: 'rules',
    title: 'Rules'.t(),

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Web Filter rules allow creating flexible block and pass conditions.'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete', 'reorder'],

    listProperty: 'settings.filterRules.list',
    ruleJavaClass: 'com.untangle.app.web_filter.WebFilterRuleCondition',

    conditions: [
        Condition.dstLocal,
        Condition.dstAddr,
        Condition.dstPort,
        Condition.srcAddr,
        Condition.srcPort,
        Condition.srcIntf,
        Condition.protocol([['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']])
    ],

    emptyRow: {
        ruleId: 0,
        flagged: true,
        blocked: false,
        description: '',
        conditions: {
            javaClass: 'java.util.LinkedList',
            list: []
        },
        javaClass: 'com.untangle.app.web_filter.WebFilterRule'
    },

    bind: '{filterRules}',

    columns: [
        Column.ruleId,
        Column.enabled,
        Column.flagged,
        Column.blocked,
        Column.description,
        Column.conditions
    ],
    editorFields: [
        Field.enableRule(),
        Field.description,
        Field.conditions,
        Field.flagged,
        Field.blocked
    ]

});
