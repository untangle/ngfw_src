Ext.define('Ung.apps.policymanager.view.Rules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-policy-manager-rules',
    itemId: 'rules',
    title: 'Rules'.t(),

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Policy Manager enables administrators to create different policies and handle different sessions with different policies based on rules.'.t()
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
        ruleId: -1,
        enabled: true,
        targetPolicy: 1,
        description: '',
        conditions: {
            javaClass: 'java.util.LinkedList',
            list: []
        },
        javaClass: 'com.untangle.app.policy_manager.PolicyRule'
    },

    bind: '{rules}',

    columns: [
        Column.ruleId,
        Column.enabled,
        Column.description,
        Column.conditions, {
            header: 'Target Policy'.t(),
            width: Renderer.messageWidth,
            dataIndex: 'targetPolicy',
            renderer: function (val) {
                var plc = Ext.getStore('policiestree').findNode('policyId', val);
                return plc ? plc.get('name') : '';
            },
            resizable: false,
        }
    ],
    editorFields: [
        Field.enableRule(),
        Field.description,
        Field.conditions(
            'com.untangle.app.policy_manager.PolicyRuleCondition', [
            'DST_ADDR',
            'DST_PORT',
            'DST_INTF',
            'SRC_ADDR',
            'SRC_PORT',
            'SRC_INTF',
            'PROTOCOL',
            'TAGGED',
            'USERNAME',
            {
                name:'TIME_OF_DAY',
                displayName: 'Time of Day'.t(),
                type: "timefield"
            },{
                name:'DAY_OF_WEEK',
                displayName: 'Day of Week'.t(),
                type: "checkboxgroup",
                values: [[
                    '1', 'Sunday'.t()
                ],[
                    '2', 'Monday'.t()
                ],[
                    '3', 'Tuesday'.t()
                ],[
                    '4', 'Wednesday'.t()
                ],[
                    '5', 'Thursday'.t()
                ], [
                    '6', 'Friday'.t()
                ],[
                    '7', 'Saturday'.t()
                ]]
            },
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
            'SERVER_COUNTRY'
        ]), {
            xtype: 'combo',
            allowBlank: false,
            bind: '{record.targetPolicy}',
            fieldLabel: 'Target Policy'.t(),
            editable: false,
            displayField: 'name',
            valueField: 'policyId',
            store: 'policiestree',
            queryMode: 'local'
        }
    ]

});
