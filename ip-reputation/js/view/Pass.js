Ext.define('Ung.apps.ipreputation.view.Pass', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-ip-reputation-pass',
    itemId: 'rules',
    title: 'Pass Rules'.t(),
    scrollable: true,

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete', 'reorder'],

    listProperty: 'settings.passRules.list',

    emptyText: 'No Pass Rules defined'.t(),

    emptyRow: {
        ruleId: 0,
        enabled: true,
        pass: false,
        flag: false,
        description: '',
        conditions: {
            javaClass: 'java.util.LinkedList',
            list: []
        },
        javaClass: 'com.untangle.app.ip_reputation.IpReputationPassRule'
    },

    bind: '{passRules}',

    columns: [
        Column.ruleId,
        Column.enabled,
        Column.description,
        Column.conditions, {
            xtype: 'checkcolumn',
            header: 'Pass'.t(),
            dataIndex: 'pass',
            resizable: false,
            width: Renderer.booleanWidth
        }
    ],

    // todo: continue this stuff
    editorFields: [
        Field.enableRule(),
        Field.description,
        Field.conditions(
            'com.untangle.app.ip_reputation.IpReputationPassRuleCondition', [
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
            // 'DIRECTORY_CONNECTOR_GROUP',
            // 'DIRECTORY_CONNECTOR_DOMAIN',
            // 'HTTP_USER_AGENT',
            // 'HTTP_USER_AGENT_OS',
            // 'CLIENT_COUNTRY',
            // 'SERVER_COUNTRY',
            'IP_REPUTATION_SRC_REPUTATION',
            'IP_REPUTATION_DST_REPUTATION',
            'IP_REPUTATION_SRC_THREATMASK',
            'IP_REPUTATION_DST_THREATMASK'
        ]), {
        xtype: 'checkbox',
        bind: '{record.pass}',
        fieldLabel: 'Pass'.t()
    },{
        xtype: 'checkbox',
        bind: '{record.flag}',
        fieldLabel: 'Flag'.t()
    }]
});
