Ext.define('Ung.apps.wanbalancer.view.RouteRules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-wan-balancer-routerules',
    itemId: 'route_rules',
    title: 'Route Rules'.t(),
    viewModel: true,

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Route Rules are used to assign specific sessions to a specific WAN interface. Rules are evaluated in order and the WAN interface of the first matching rule is used to route the matching session.<BR>If there is no matching rule or the rule is set to Balance the session will be routed according to the Traffic Allocation settings.'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete', 'reorder'],

    listProperty: 'settings.routeRules.list',
    ruleJavaClass: 'com.untangle.app.wan_balancer.RouteRuleCondition',

    conditions: [
        Condition.dstAddr,
        Condition.dstPort,
        Condition.srcAddr,
        Condition.srcIntf,
        Condition.protocol([['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']])
    ],

    emptyRow: {
        ruleId: 0,
        enabled: true,
        description: '',
        conditions: {
            javaClass: 'java.util.LinkedList',
            list: []
        },
        javaClass: 'com.untangle.app.wan_balancer.RouteRule'
    },

    bind: '{routeRules}',

    columns: [
        Column.ruleId,
        Column.live,
        Column.description,
        Column.conditions, {
            header: 'Destination WAN'.t(),
            dataIndex: 'destinationWan',
            width: 250
        }
    ],

    editorFields: [
        Field.live,
        Field.description,
        Field.conditions, {
            xtype: 'combo',
            fieldLabel: 'Destination WAN'.t(),
            bind: '{record.destinationWan}',
            allowBlank: false,
            editable: false,
            store: [
                ['0', 'Balance'.t()]
            ],
            queryMode: 'local',
        }
    ]

});
