Ext.define('Ung.apps.firewall.view.Rules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-firewall-rules',
    itemId: 'rules',
    title: 'Rules'.t(),
    scrollable: true,

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Routing and Port Forwarding functionality can be found elsewhere in Config->Networking.'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete', 'reorder'],

    listProperty: 'settings.rules.list',

    emptyText: 'No Rules defined'.t(),

    emptyRow: {
        ruleId: 0,
        enabled: true,
        block: false,
        flag: false,
        description: '',
        conditions: {
            javaClass: 'java.util.LinkedList',
            list: []
        },
        javaClass: 'com.untangle.app.firewall.FirewallRule'
    },

    bind: '{rules}',

    columns: [
        Column.ruleId,
        Column.enabled,
        Column.description,
        Column.conditions, {
            xtype: 'checkcolumn',
            header: 'Block'.t(),
            dataIndex: 'block',
            resizable: false,
            width: Renderer.booleanWidth
        }, {
            xtype: 'checkcolumn',
            header: 'Flag'.t(),
            dataIndex: 'flag',
            resizable: false,
            width: Renderer.booleanWidth
        }
    ],

    // todo: continue this stuff
    editorFields: [
        Field.enableRule(),
        Field.description,
        Field.conditions(
            'com.untangle.app.firewall.FirewallRuleCondition', [
            'DST_ADDR',
            'DST_PORT',
            'DST_INTF',
            'SRC_ADDR',
            'SRC_PORT',
            'SRC_INTF',
            'PROTOCOL',
            'TAGGED',
            'USERNAME',
            'HOST_HOSTNAME',
            'CLIENT_HOSTNAME',
            'SERVER_HOSTNAME',
            'SRC_MAC',
            'DST_MAC',
            'CLIENT_MAC_VENDOR',
            'SERVER_MAC_VENDOR',
            'CLIENT_IN_PENALTY_BOX',
            'SERVER_IN_PENALTY_BOX',
            'HOST_HAS_NO_QUOTA',
            'USER_HAS_NO_QUOTA',
            'CLIENT_HAS_NO_QUOTA',
            'SERVER_HAS_NO_QUOTA',
            'HOST_QUOTA_EXCEEDED',
            'USER_QUOTA_EXCEEDED',
            'CLIENT_QUOTA_EXCEEDED',
            'SERVER_QUOTA_EXCEEDED',
            'HOST_QUOTA_ATTAINMENT',
            'USER_QUOTA_ATTAINMENT',
            'CLIENT_QUOTA_ATTAINMENT',
            'SERVER_QUOTA_ATTAINMENT',
            'HOST_ENTITLED',
            'DIRECTORY_CONNECTOR_GROUP',
            'DIRECTORY_CONNECTOR_DOMAIN',
            'HTTP_USER_AGENT',
            'HTTP_USER_AGENT_OS',
            'CLIENT_COUNTRY',
            'SERVER_COUNTRY',
            'THREAT_PREVENTION_SRC_REPUTATION',
            'THREAT_PREVENTION_DST_REPUTATION',
            'THREAT_PREVENTION_SRC_THREATMASK',
            'THREAT_PREVENTION_DST_THREATMASK',
        ]), {
            xtype: 'combo',
            allowBlank: false,
            bind: '{record.block}',
            fieldLabel: 'Action Type',
            editable: false,
            store: [[true, 'Block'.t()], [false, 'Pass'.t()]],
            queryMode: 'local'
        }, {
            xtype: 'checkbox',
            bind: '{record.flag}',
            fieldLabel: 'Flag'.t()
        }
    ]
});
