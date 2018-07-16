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
            html: 'Rules determine which sessions will use a tunnel VPN connections. Rules are evaluated in order and the action from the first matching rule is used to route the matching session.<BR>'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete', 'reorder'],

    emptyText: 'No Rules defined'.t(),

    listProperty: 'settings.rules.list',

    emptyRow: {
        ruleId: 0,
        enabled: true,
        description: '',
        conditions: {
            javaClass: 'java.util.LinkedList',
            list: []
        },
        javaClass: 'com.untangle.app.tunnel_vpn.TunnelVpnRule',
        tunnelId: 0
    },

    bind: '{rules}',

    columns: [
        Column.ruleId,
        Column.enabled,
        Column.description,
        Column.conditions, {
            header: 'Destination Tunnel'.t(),
            dataIndex: 'tunnelId',
            width: Renderer.messageWidth,
            flex: 1,
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
        Field.conditions(
            'com.untangle.app.tunnel_vpn.TunnelVpnRuleCondition', [
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
