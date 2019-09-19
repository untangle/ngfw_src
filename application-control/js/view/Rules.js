Ext.define('Ung.apps.applicationcontrol.view.Rules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-application-control-rules',
    itemId: 'rules',
    title: 'Rules'.t(),

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Application Control rules are used to control traffic post-classification.'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete', 'reorder'],

    listProperty: 'settings.logicRules.list',

    emptyText: 'No Rules defined'.t(),

    emptyRow: {
        enabled: true,
        description: '',
        action: {
            actionType: 'BLOCK',
            javaClass: 'com.untangle.app.application_control.ApplicationControlLogicRuleAction',
            flag: true
        },
        conditions: {
            javaClass: 'java.util.LinkedList',
            list: []
        },
        javaClass: 'com.untangle.app.application_control.ApplicationControlLogicRule'
    },

    bind: '{logicRules}',

    columns: [{
        header: 'Rule Id'.t(),
        width: Renderer.idWidth,
        align: 'right',
        resizable: false,
        dataIndex: 'id',
        renderer: Renderer.id
    },
    Column.enabled,
    Column.description,
    Column.conditions,
    {
        header: 'Action'.t(),
        dataIndex: 'action',
        width: Renderer.actionWidth,
        renderer: Ung.apps.applicationcontrol.MainController.actionRenderer
    }],

    // todo: continue this stuff
    editorFields: [
        Field.enableRule(),
        Field.description,
        Field.conditions(
            'com.untangle.app.application_control.ApplicationControlLogicRuleCondition',[
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
            'WEB_FILTER_CATEGORY',
            'WEB_FILTER_CATEGORY_DESCRIPTION',
            'WEB_FILTER_FLAGGED',
            'DIRECTORY_CONNECTOR_GROUP',
            'DIRECTORY_CONNECTOR_DOMAIN',
            'CLIENT_COUNTRY',
            'SERVER_COUNTRY',
            'IP_REPUTATION_SRC_REPUTATION',
            'IP_REPUTATION_DST_REPUTATION',
            'IP_REPUTATION_SRC_THREATMASK',
            'IP_REPUTATION_DST_THREATMASK',
        ]), {
            xtype: 'combo',
            fieldLabel: 'Action Type'.t(),
            bind: '{_action.actionType}',
            allowBlank: false,
            editable: false,
            store: [
                ['ALLOW', 'Allow'.t()],
                ['BLOCK', 'Block'.t()],
                ['TARPIT', 'Tarpit'.t()]
            ],
            queryMode: 'local',
        }
    ]
});
