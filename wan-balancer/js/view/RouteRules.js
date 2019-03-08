Ext.define('Ung.apps.wan-balancer.view.RouteRules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-wan-balancer-routerules',
    itemId: 'route-rules',
    title: 'Route Rules'.t(),
    viewModel: true,
    scrollable: true,

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

    emptyText: 'No Route Rules defined'.t(),

    columns: [
        Column.ruleId,
        Column.enabled,
        Column.description,
        Column.conditions, {
            header: 'Destination WAN'.t(),
            dataIndex: 'destinationWan',
            width: Renderer.messageWidth,
            renderer: Ung.apps['wan-balancer'].MainController.destinationWanRenderer
        }
    ],

    editorFields: [
        Field.enableRule(),
        Field.description,
        Field.conditions(
            'com.untangle.app.wan_balancer.RouteRuleCondition', [
            "DST_ADDR",
            "DST_PORT",
            "SRC_ADDR",
            "SRC_PORT",
            "SRC_INTF",
            "PROTOCOL",
            "CLIENT_TAGGED",
            "SERVER_TAGGED"
        ]), {
            xtype: 'combo',
            fieldLabel: 'Destination WAN'.t(),
            bind: {
                value: '{record.destinationWan}',
                store: '{destinationWanList}'
            },
            allowBlank: false,
            editable: false,
            queryMode: 'local',
            displayField: 'name',
            valueField: 'index'
        }
    ]

});
