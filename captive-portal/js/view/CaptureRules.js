Ext.define('Ung.apps.captive-portal.view.CaptureRules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-captive-portal-capturerules',
    itemId: 'capture-rules',
    title: 'Capture Rules'.t(),

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Network access is controlled based on the set of rules defined below. To learn more click on the <b>Help</b> button below.'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete', 'reorder'],

    listProperty: 'settings.captureRules.list',

    emptyText: 'No Capture Rules defined'.t(),

    emptyRow: {
        ruleId: -1,
        enabled: true,
        description: '',
        capture: false,
        conditions: {
            javaClass: 'java.util.LinkedList',
            list: []
        },
        javaClass: 'com.untangle.app.captive_portal.CaptureRule'
    },

    bind: '{captureRules}',

    columns: [
        Column.ruleId,
        Column.enabled,
        Column.description,
        Column.conditions, {
            xtype: 'checkcolumn',
            header: 'Capture',
            dataIndex: 'capture',
            resizable: false,
            width: Renderer.booleanWidth
        }

    ],

    // todo: continue this stuff
    editorFields: [
        Field.enableRule(),
        Field.description,
        Field.conditions(
            'com.untangle.app.captive_portal.CaptureRuleCondition', [
            "DST_ADDR",
            "DST_PORT",
            "DST_INTF",
            "SRC_ADDR",
            "SRC_PORT",
            "SRC_INTF",
            "PROTOCOL",
            "TAGGED",
            "USERNAME",
            "HOST_HOSTNAME",
            "CLIENT_HOSTNAME",
            "SERVER_HOSTNAME",
            "SRC_MAC",
            "DST_MAC",
            "CLIENT_MAC_VENDOR",
            "SERVER_MAC_VENDOR",
            "CLIENT_IN_PENALTY_BOX",
            "SERVER_IN_PENALTY_BOX",
            "HOST_HAS_NO_QUOTA",
            "USER_HAS_NO_QUOTA",
            "CLIENT_HAS_NO_QUOTA",
            "SERVER_HAS_NO_QUOTA",
            "HOST_QUOTA_EXCEEDED",
            "USER_QUOTA_EXCEEDED",
            "CLIENT_QUOTA_EXCEEDED",
            "SERVER_QUOTA_EXCEEDED",
            "HOST_QUOTA_ATTAINMENT",
            "USER_QUOTA_ATTAINMENT",
            "CLIENT_QUOTA_ATTAINMENT",
            'HOST_ENTITLED',
            "SERVER_QUOTA_ATTAINMENT",
            "DIRECTORY_CONNECTOR_GROUP",
            'DIRECTORY_CONNECTOR_DOMAIN',
            'HTTP_HOST',
            'HTTP_REFERER', 
            'HTTP_URI',
            'HTTP_URL',
            'HTTP_CONTENT_TYPE',
            'HTTP_CONTENT_LENGTH',
            'HTTP_REQUEST_METHOD',
            'HTTP_REQUEST_FILE_PATH',
            'HTTP_REQUEST_FILE_NAME',
            'HTTP_REQUEST_FILE_EXTENSION',
            'HTTP_RESPONSE_FILE_NAME',
            'HTTP_RESPONSE_FILE_EXTENSION',
            'HTTP_USER_AGENT',
            'HTTP_USER_AGENT_OS',
            'SSL_INSPECTOR_SNI_HOSTNAME',
            'SSL_INSPECTOR_SUBJECT_DN',
            'SSL_INSPECTOR_ISSUER_DN',
            "CLIENT_COUNTRY",
            "SERVER_COUNTRY",
            'THREAT_PREVENTION_SRC_REPUTATION',
            'THREAT_PREVENTION_SRC_CATEGORIES',
            'THREAT_PREVENTION_DST_REPUTATION',
            'THREAT_PREVENTION_DST_CATEGORIES'
        ]), {
            xtype: 'combo',
            allowBlank: false,
            bind: '{record.capture}',
            fieldLabel: 'Action Type'.t(),
            editable: false,
            store: [[true, 'Capture'.t()], [false, 'Pass'.t()]],
            queryMode: 'local'
        }
    ]
});
