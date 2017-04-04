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

    listProperty: 'settings.rules.list',
    ruleJavaClass: 'com.untangle.app.policy_manager.PolicyRuleCondition',

    conditions: [
            { name: 'DST_ADDR',displayName: 'Destination Address'.t(), type: 'textfield', visible: true, vtype: 'ipMatcher' },
            { name: 'DST_PORT',displayName: 'Destination Port'.t(), type: 'textfield',vtype: 'portMatcher', visible: true},
            { name: 'DST_INTF',displayName: 'Destination Interface'.t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, false), visible: true},
            { name: 'SRC_ADDR',displayName: 'Source Address'.t(), type: 'textfield', visible: true, vtype: 'ipMatcher' },
            { name: 'SRC_PORT',displayName: 'Source Port'.t(), type: 'textfield',vtype: 'portMatcher', visible: rpc.isExpertMode},
            { name: 'SRC_INTF',displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, false), visible: true},
            { name: 'PROTOCOL',displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP'],['any', 'any'.t()]], visible: true},
            // { name: 'USERNAME',displayName: 'Username'.t(), type: "editor", editor: Ext.create('Ung.UserEditorWindow',{}), visible: true},
            // { name: 'TIME_OF_DAY',displayName: 'Time of Day'.t(), type: "editor", editor: Ext.create('Ung.TimeEditorWindow',{}), visible: true},
            // { name: 'DAY_OF_WEEK',displayName: 'Day of Week'.t(), type: "checkgroup", values: Ung.Util.getDayOfWeekList(), visible: true},
            { name: 'CLIENT_HOSTNAME',displayName: 'Client Hostname'.t(), type: 'textfield', visible: true},
            { name: 'SERVER_HOSTNAME',displayName: 'Server Hostname'.t(), type: 'textfield', visible: rpc.isExpertMode},
            { name: 'SRC_MAC', displayName: 'Client MAC Address'.t(), type: 'textfield', visible: true },
            { name: 'DST_MAC', displayName: 'Server MAC Address'.t(), type: 'textfield', visible: true },
            { name: 'CLIENT_MAC_VENDOR',displayName: 'Client MAC Vendor'.t(), type: 'textfield', visible: true},
            { name: 'SERVER_MAC_VENDOR',displayName: 'Server MAC Vendor'.t(), type: 'textfield', visible: true},
            { name: 'CLIENT_IN_PENALTY_BOX',displayName: 'Client in Penalty Box'.t(), type: 'boolean', visible: true},
            { name: 'SERVER_IN_PENALTY_BOX',displayName: 'Server in Penalty Box'.t(), type: 'boolean', visible: true},
            { name: 'CLIENT_HAS_NO_QUOTA',displayName: 'Client has no Quota'.t(), type: 'boolean', visible: true},
            { name: 'SERVER_HAS_NO_QUOTA',displayName: 'Server has no Quota'.t(), type: 'boolean', visible: true},
            { name: 'CLIENT_QUOTA_EXCEEDED',displayName: 'Client has exceeded Quota'.t(), type: 'boolean', visible: true},
            { name: 'SERVER_QUOTA_EXCEEDED',displayName: 'Server has exceeded Quota'.t(), type: 'boolean', visible: true},
            { name: 'CLIENT_QUOTA_ATTAINMENT',displayName: 'Client Quota Attainment'.t(), type: 'textfield', visible: true},
            { name: 'SERVER_QUOTA_ATTAINMENT',displayName: 'Server Quota Attainment'.t(), type: 'textfield', visible: true},
            // { name: 'DIRECTORY_CONNECTOR_GROUP',displayName: 'Directory Connector: User in Group'.t(), type: "editor", editor: Ext.create('Ung.GroupEditorWindow',{}), visible: true},
            { name: 'HTTP_USER_AGENT',displayName: 'HTTP: Client User Agent'.t(), type: 'textfield', visible: true},
            { name: 'HTTP_USER_AGENT_OS',displayName: 'HTTP: Client User OS'.t(), type: 'textfield', visible: false},
            // { name: 'CLIENT_COUNTRY',displayName: 'Client Country'.t(), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true},
            // { name: 'SERVER_COUNTRY',displayName: 'Server Country'.t(), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true}
    ],

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
            dataIndex: 'targetPolicy',
            renderer: function (val) {
                var plc = Ext.getStore('policiestree').findNode('policyId', val);
                return plc ? plc.get('name') : '';
            },
            resizable: false,
            width: 200
        }
    ],
    editorFields: [
        Field.enableRule(),
        Field.description,
        Field.conditions, {
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
