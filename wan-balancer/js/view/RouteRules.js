Ext.define('Ung.apps.wan-balancer.view.RouteRules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-wan-balancer-routerules',
    itemId: 'route-rules',
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
        {name:"DST_ADDR",displayName: "Destination Address".t(), type: "textfield", visible: true, vtype:"ipMatcher"},
        {name:"DST_PORT",displayName: "Destination Port".t(), type: "textfield",vtype:"portMatcher", visible: true},
        {name:"SRC_ADDR",displayName: "Source Address".t(), type: "textfield", visible: true, vtype:"ipMatcher"},
        {name:"SRC_PORT",displayName: "Source Port".t(), type: "textfield",vtype:"portMatcher", visible: rpc.isExpertMode},
        {name:"SRC_INTF",displayName: "Source Interface".t(), type: "checkboxgroup", values: Util.getInterfaceList(true, false), visible: true},
        {name:"PROTOCOL",displayName: "Protocol".t(), type: "checkboxgroup", values: [["TCP","TCP"],["UDP","UDP"],["any", "any".t()]], visible: true},
        {name:"CLIENT_TAGGED",displayName: 'Client Tagged'.t(), type: 'textfield', visible: true},
        {name:"SERVER_TAGGED",displayName: 'Server Tagged'.t(), type: 'textfield', visible: true}
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
        Column.enabled,
        Column.description,
        Column.conditions, {
            header: 'Destination WAN'.t(),
            dataIndex: 'destinationWan',
            width: 250,
            renderer: function(value, meta, record, row, col, store, grid) {
                var wanlist = this.getViewModel().get('destinationWanList');
                var dstname = 'Unknown'.t();
                wanlist.each(function(record) { if (record.get('index') == value) dstname = record.get('name'); });
                return(dstname);
            }
        }
    ],

    editorFields: [
        Field.enableRule(),
        Field.description,
        Field.conditions, {
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
