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
    ],

    conditions: [
            {name:'DST_ADDR',displayName: 'Destination Address'.t(), type: 'textfield', visible: true, vtype: 'ipMatcher'},
            {name:'DST_PORT',displayName: 'Destination Port'.t(), type: 'textfield',vtype: 'portMatcher', visible: true},
            {name:'DST_INTF',displayName: 'Destination Interface'.t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, false), visible: true},
            {name:'SRC_ADDR',displayName: 'Source Address'.t(), type: 'textfield', visible: true, vtype: 'ipMatcher' },
            {name:'SRC_PORT',displayName: 'Source Port'.t(), type: 'textfield',vtype: 'portMatcher', visible: rpc.isExpertMode},
            {name:'SRC_INTF',displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, false), visible: true},
            {name:'PROTOCOL',displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP'],['any', 'any'.t()]], visible: true},
            {name:'TAGGED',displayName: 'Tagged'.t(), type: 'textfield', visible: true},
            {name:'USERNAME',displayName: 'Username'.t(), type: "userfield", visible: true},
            {name:'TIME_OF_DAY',displayName: 'Time of Day'.t(), type: "timefield", visible: true},
            {name:'DAY_OF_WEEK',displayName: 'Day of Week'.t(), type: "checkboxgroup", values: [['1', 'Sunday'.t()], ['2', 'Monday'.t()], ['3', 'Tuesday'.t()], ['4', 'Wednesday'.t()], ['5', 'Thursday'.t()], ['6', 'Friday'.t()], ['7', 'Saturday'.t()]], visible: true},
            {name:'HOST_HOSTNAME',displayName: 'Host Hostname'.t(), type: 'textfield', visible: true},
            {name:'CLIENT_HOSTNAME',displayName: 'Client Hostname'.t(), type: 'textfield', visible: false},
            {name:'SERVER_HOSTNAME',displayName: 'Server Hostname'.t(), type: 'textfield', visible: false},
            {name:'SRC_MAC', displayName: 'Client MAC Address'.t(), type: 'textfield', visible: true },
            {name:'DST_MAC', displayName: 'Server MAC Address'.t(), type: 'textfield', visible: true },
            {name:'CLIENT_MAC_VENDOR',displayName: 'Client MAC Vendor'.t(), type: 'textfield', visible: true},
            {name:'SERVER_MAC_VENDOR',displayName: 'Server MAC Vendor'.t(), type: 'textfield', visible: true},
            {name:'CLIENT_IN_PENALTY_BOX',displayName: 'Client in Penalty Box'.t(), type: 'boolean', visible: false},
            {name:'SERVER_IN_PENALTY_BOX',displayName: 'Server in Penalty Box'.t(), type: 'boolean', visible: false},
            {name:'HOST_HAS_NO_QUOTA',displayName: 'Host has no Quota'.t(), type: 'boolean', visible: true},
            {name:'USER_HAS_NO_QUOTA',displayName: 'User has no Quota'.t(), type: 'boolean', visible: true},
            {name:'CLIENT_HAS_NO_QUOTA',displayName: 'Client has no Quota'.t(), type: 'boolean', visible: rpc.isExpertMode},
            {name:'SERVER_HAS_NO_QUOTA',displayName: 'Server has no Quota'.t(), type: 'boolean', visible: rpc.isExpertMode},
            {name:'HOST_QUOTA_EXCEEDED',displayName: 'Host has exceeded Quota'.t(), type: 'boolean', visible: true},
            {name:'USER_QUOTA_EXCEEDED',displayName: 'User has exceeded Quota'.t(), type: 'boolean', visible: true},
            {name:'CLIENT_QUOTA_EXCEEDED',displayName: 'Client has exceeded Quota'.t(), type: 'boolean', visible: rpc.isExpertMode},
            {name:'SERVER_QUOTA_EXCEEDED',displayName: 'Server has exceeded Quota'.t(), type: 'boolean', visible: rpc.isExpertMode},
            {name:'HOST_QUOTA_ATTAINMENT',displayName: 'Host Quota Attainment'.t(), type: 'textfield', visible: true},
            {name:'USER_QUOTA_ATTAINMENT',displayName: 'User Quota Attainment'.t(), type: 'textfield', visible: true},
            {name:'CLIENT_QUOTA_ATTAINMENT',displayName: 'Client Quota Attainment'.t(), type: 'textfield', visible: rpc.isExpertMode},
            {name:'SERVER_QUOTA_ATTAINMENT',displayName: 'Server Quota Attainment'.t(), type: 'textfield', visible: rpc.isExpertMode},
            {name:'HOST_ENTITLED',displayName: 'Host Entitled'.t(), type: 'boolean', visible: true},
            {name:'DIRECTORY_CONNECTOR_GROUP',displayName: 'Directory Connector: User in Group'.t(), type: "directorygroupfield", visible: true},
            {name:'HTTP_USER_AGENT',displayName: 'HTTP: Client User Agent'.t(), type: 'textfield', visible: true},
            {name:'HTTP_USER_AGENT_OS',displayName: 'HTTP: Client User OS'.t(), type: 'textfield', visible: false},
            {name:'CLIENT_COUNTRY',displayName: 'Client Country'.t(), type: "countryfield", visible: true},
            {name:'SERVER_COUNTRY',displayName: 'Server Country'.t(), type: "countryfield", visible: true}
    ],

});
