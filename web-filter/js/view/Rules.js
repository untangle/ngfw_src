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

    emptyText: 'No Rules defined'.t(),

    listProperty: 'settings.filterRules.list',

    emptyRow: {
        ruleId: 0,
        enabled: true,
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
        Field.conditions(
            'com.untangle.app.web_filter.WebFilterRuleCondition', [
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
    
            'WEB_FILTER_CATEGORY',
            'WEB_FILTER_CATEGORY_DESCRIPTION',
            'WEB_FILTER_FLAGGED',
            'WEB_FILTER_REQUEST_METHOD',
            'WEB_FILTER_REQUEST_FILE_PATH',
            'WEB_FILTER_REQUEST_FILE_NAME',
            'WEB_FILTER_REQUEST_FILE_EXTENSION',
            'WEB_FILTER_RESPONSE_CONTENT_TYPE',
            'WEB_FILTER_RESPONSE_FILE_NAME',
            'WEB_FILTER_RESPONSE_FILE_EXTENSION',
            
            'APPLICATION_CONTROL_APPLICATION',
            'APPLICATION_CONTROL_CATEGORY',
            'APPLICATION_CONTROL_PROTOCHAIN',
            'APPLICATION_CONTROL_DETAIL',
            'APPLICATION_CONTROL_CONFIDENCE',
            'APPLICATION_CONTROL_PRODUCTIVITY',
            'APPLICATION_CONTROL_RISK',
    
            'PROTOCOL_CONTROL_SIGNATURE',
            'PROTOCOL_CONTROL_CATEGORY',
            'PROTOCOL_CONTROL_DESCRIPTION',
    
            'DIRECTORY_CONNECTOR_GROUP',
            'DIRECTORY_CONNECTOR_DOMAIN',
    
            'SSL_INSPECTOR_SNI_HOSTNAME',
            'SSL_INSPECTOR_SUBJECT_DN',
            'SSL_INSPECTOR_ISSUER_DN',
    
            'CLIENT_COUNTRY',
            'SERVER_COUNTRY',

            'IP_REPUTATION_SRC_REPUTATION',
            'IP_REPUTATION_SRC_THREATMASK',
            'IP_REPUTATION_DST_REPUTATION',
            'IP_REPUTATION_DST_THREATMASK'
        ]),
        Field.flagged,
        Field.blockedCombo
    ]
});
