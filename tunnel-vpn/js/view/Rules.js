Ext.define('Ung.apps.tunnel-vpn.view.Rules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-tunnel-vpn-rules',
    itemId: 'rules',
    title: 'Rules'.t(),
    viewModel: true,

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Rules are determine which sessions will use a tunnel VPN. Rules are evaluated in order and the action from the first matching rule is used to route the matching session.<BR>'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete', 'reorder'],

    listProperty: 'settings.rules.list',
    ruleJavaClass: 'com.untangle.app.tunnel_vpn.TunnelVpnRuleCondition',

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
        javaClass: 'com.untangle.app.tunnel_vpn.TunnelVpnRule'
    },

    bind: '{rules}',

    columns: [
        Column.ruleId,
        Column.enabled,
        Column.description,
        Column.conditions, {
            header: 'Destination Tunnel'.t(),
            dataIndex: 'tunnelId',
            width: 250,
            renderer: function(value, meta, record, row, col, store, grid) {
                var tunnellist = this.getViewModel().get('destinationTunnelList');
                var dstname = 'Unknown'.t();
                tunnellist.each(function(record) { if (record.get('index') == value) dstname = record.get('name'); });
                return(dstname);
            }
        }
    ],

    editorFields: [
        Field.enableRule(),
        Field.description,
        Field.conditions, {
            xtype: 'combo',
            fieldLabel: 'Destination Tunnel'.t(),
            bind: {
                value: '{record.tunnelId}',
                store: '{destinationTunnelList}'
            },
            allowBlank: false,
            editable: false,
            queryMode: 'local',
            displayField: 'name',
            valueField: 'index'
        }
    ]

});
