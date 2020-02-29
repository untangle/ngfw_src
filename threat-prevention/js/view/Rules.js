Ext.define('Ung.apps.threatprevention.view.Rules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-threat-prevention-rules',
    itemId: 'rules',
    title: 'Rules'.t(),
    scrollable: true,

    dockedItems: [{
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
        action: "pass",
        flag: true,
        description: '',
        conditions: {
            javaClass: 'java.util.LinkedList',
            list: []
        },
        javaClass: 'com.untangle.app.threat_prevention.ThreatPreventionRule'
    },

    bind: '{rules}',

    columns: [
        Column.ruleId,
        Column.enabled,
        Column.description,
        Column.conditions,
        {
        header: "Action".t(),
        dataIndex: 'action',
        width: Renderer.messageWidth,
        flex: 2,
        editor: {
            xtype: 'combo',
            editable: false,
            matchFieldWidth: false,
            queryMode: 'local',
            valueField: 'value',
            displayField: 'description',
            store: Ung.common.threatprevention.references.actions
        }
    }],

    // todo: continue this stuff
    editorFields: [
        Field.enableRule(),
        Field.description,
        Field.conditions(
            'com.untangle.app.threat_prevention.ThreatPreventionRuleCondition', [
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
            'THREAT_PREVENTION_SRC_REPUTATION',
            'THREAT_PREVENTION_DST_REPUTATION',
            'THREAT_PREVENTION_SRC_CATEGORIES',
            'THREAT_PREVENTION_DST_CATEGORIES',
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
        ]),
    {
        xtype: 'combo',
        fieldLabel: 'Action'.t(),
        editable: false,
        matchFieldWidth: false,
        queryMode: 'local',
        valueField: 'value',
        displayField: 'description',
        bind:{
            value: '{record.action}'
        },
        store: Ung.common.threatprevention.references.actions
    },{
        xtype: 'checkbox',
        bind: '{record.flag}',
        fieldLabel: 'Flag'.t()
    }]
});
