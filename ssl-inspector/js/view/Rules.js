Ext.define('Ung.apps.sslinspector.view.Rules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-ssl-inspector-rules',
    itemId: 'rules',
    title: 'Rules'.t(),

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Ignore rules are used to configure traffic which should be ignored by the inspection engine.'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete', 'reorder'],

    listProperty: 'settings.ignoreRules.list',

    emptyText: 'No Rules defined'.t(),

    emptyRow: {
        ruleId: 0,
        enabled: true,
        description: '',
        action: {
            actionType: 'IGNORE',
            javaClass: 'com.untangle.app.ssl_inspector.SslInspectorRuleAction',
            flag: true
        },
        conditions: {
            javaClass: 'java.util.LinkedList',
            list: []
        },
        javaClass: 'com.untangle.app.ssl_inspector.SslInspectorRule'
    },

    bind: '{ignoreRules}',

    columns: [
        Column.ruleId,
        Column.enabled,
        Column.description,
        Column.conditions,
    {
        header: 'Action'.t(),
        dataIndex: 'action',
        width: Renderer.actionWidth,
        renderer: Ung.apps.sslinspector.MainController.actionRenderer
    }],

    // todo: continue this stuff
    editorFields: [
        Field.enableRule(),
        Field.description,
        Field.conditions(
            'com.untangle.app.ssl_inspector.SslInspectorRuleCondition', [
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
            'HTTP_USER_AGENT',
            'HTTP_USER_AGENT_OS',
            'DIRECTORY_CONNECTOR_GROUP',
            'DIRECTORY_CONNECTOR_DOMAIN',
            'SSL_INSPECTOR_SNI_HOSTNAME',
            'SSL_INSPECTOR_SUBJECT_DN',
            'SSL_INSPECTOR_ISSUER_DN',
            'CLIENT_COUNTRY',
            'SERVER_COUNTRY',
            'THREAT_PREVENTION_SRC_REPUTATION',
            'THREAT_PREVENTION_DST_REPUTATION',
            'THREAT_PREVENTION_SRC_CATEGORIES',
            'THREAT_PREVENTION_DST_CATEGORIES',
        ]), {
            xtype: 'combo',
            name: 'action',
            fieldLabel: 'Action Type'.t(),
            bind: '{_action.actionType}',
            allowBlank: false,
            editable: false,
            store: [['INSPECT', 'Inspect'.t()], ['IGNORE', 'Ignore'.t()]],
            queryMode: 'local'
        }
    ]
});
