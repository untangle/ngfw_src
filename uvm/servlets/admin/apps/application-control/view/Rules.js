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
    ruleJavaClass: 'com.untangle.app.application_control.ApplicationControlLogicRuleCondition',


    emptyRow: {
        id: null,
        live: true,
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
        width: 70,
        align: 'right',
        resizable: false,
        dataIndex: 'id',
        renderer: function(value) {
            return value < 0 ? 'new'.t() : value;
        }
    }, Column.live,
        Column.description,
        Column.conditions, {
            header: 'Action'.t(),
            dataIndex: 'action',
            width: 250,
            renderer: function (act) {
                switch(act.actionType) {
                case 'ALLOW': return 'Allow'.t();
                case 'BLOCK': return 'Block'.t();
                case 'TARPIT': return 'Tarpit'.t();
                default: return 'Unknown Action: '.t() + act;
                }
            }
        }
    ],

    // todo: continue this stuff
    editorFields: [
        Field.live,
        Field.description,
        Field.conditions, {
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
    ],

    conditions: [
        { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', visible: true, vtype: 'ipMatcher' },
        { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield',vtype: 'portMatcher', visible: true },
        { name: 'DST_INTF', displayName: 'Destination Interface'.t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, false), visible: true },
        { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', visible: true, vtype: 'ipMatcher' },
        { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'textfield', vtype: 'portMatcher', visible: rpc.isExpertMode },
        { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, false), visible: true },
        { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP'],['any', 'any'.t()]], visible: true },
        // { name: 'USERNAME', displayName: 'Username'.t(), type: 'editor', editor: Ext.create('Ung.UserEditorWindow',{}), visible: true},
        { name: 'CLIENT_HOSTNAME', displayName: 'Client Hostname'.t(), type: 'textfield', visible: true },
        { name: 'SERVER_HOSTNAME', displayName: 'Server Hostname'.t(), type: 'textfield', visible: rpc.isExpertMode },
        { name: 'SRC_MAC', displayName: 'Client MAC Address'.t(), type: 'textfield', visible: true },
        { name: 'DST_MAC', displayName: 'Server MAC Address'.t(), type: 'textfield', visible: true },
        { name: 'CLIENT_MAC_VENDOR', displayName: 'Client MAC Vendor'.t(), type: 'textfield', visible: true },
        { name: 'SERVER_MAC_VENDOR', displayName: 'Server MAC Vendor'.t(), type: 'textfield', visible: true },
        { name: 'CLIENT_IN_PENALTY_BOX', displayName: 'Client in Penalty Box'.t(), type: 'boolean', visible: true },
        { name: 'SERVER_IN_PENALTY_BOX', displayName: 'Server in Penalty Box'.t(), type: 'boolean', visible: true },
        { name: 'CLIENT_HAS_NO_QUOTA', displayName: 'Client has no Quota'.t(), type: 'boolean', visible: true },
        { name: 'SERVER_HAS_NO_QUOTA', displayName: 'Server has no Quota'.t(), type: 'boolean', visible: true },
        { name: 'CLIENT_QUOTA_EXCEEDED', displayName: 'Client has exceeded Quota'.t(), type: 'boolean', visible: true },
        { name: 'SERVER_QUOTA_EXCEEDED', displayName: 'Server has exceeded Quota'.t(), type: 'boolean', visible: true },
        { name: 'CLIENT_QUOTA_ATTAINMENT', displayName: 'Client Quota Attainment'.t(), type: 'textfield', visible: true },
        { name: 'SERVER_QUOTA_ATTAINMENT', displayName: 'Server Quota Attainment'.t(), type: 'textfield', visible: true },
        { name: 'HTTP_HOST', displayName: 'HTTP: Hostname'.t(), type: 'textfield', visible: true },
        { name: 'HTTP_REFERER', displayName: 'HTTP: Referer'.t(), type: 'textfield', visible: true },
        { name: 'HTTP_URI', displayName: 'HTTP: URI'.t(), type: 'textfield', visible: true },
        { name: 'HTTP_URL', displayName: 'HTTP: URL'.t(), type: 'textfield', visible: true },
        { name: 'HTTP_CONTENT_TYPE', displayName: 'HTTP: Content Type'.t(), type: 'textfield', visible: true },
        { name: 'HTTP_CONTENT_LENGTH', displayName: 'HTTP: Content Length'.t(), type: 'textfield', visible: true },
        { name: 'HTTP_USER_AGENT', displayName: 'HTTP: Client User Agent'.t(), type: 'textfield', visible: true },
        { name: 'HTTP_USER_AGENT_OS', displayName: 'HTTP: Client User OS'.t(), type: 'textfield', visible: false },
        { name: 'APPLICATION_CONTROL_APPLICATION', displayName: 'Application Control: Application'.t(), type: 'textfield', visible: true },
        { name: 'APPLICATION_CONTROL_CATEGORY', displayName: 'Application Control: Application Category'.t(), type: 'textfield', visible: true },
        { name: 'APPLICATION_CONTROL_PROTOCHAIN', displayName: 'Application Control: ProtoChain'.t(), type: 'textfield', visible: true },
        { name: 'APPLICATION_CONTROL_DETAIL', displayName: 'Application Control: Detail'.t(), type: 'textfield', visible: true },
        { name: 'APPLICATION_CONTROL_CONFIDENCE', displayName: 'Application Control: Confidence'.t(), type: 'textfield', visible: true },
        { name: 'APPLICATION_CONTROL_PRODUCTIVITY', displayName: 'Application Control: Productivity'.t(), type: 'textfield', visible: true },
        { name: 'APPLICATION_CONTROL_RISK', displayName: 'Application Control: Risk'.t(), type: 'textfield', visible: true },
        { name: 'PROTOCOL_CONTROL_SIGNATURE', displayName: 'Application Control Lite: Signature'.t(), type: 'textfield', visible: true },
        { name: 'PROTOCOL_CONTROL_CATEGORY', displayName: 'Application Control Lite: Category'.t(), type: 'textfield', visible: true },
        { name: 'PROTOCOL_CONTROL_DESCRIPTION', displayName: 'Application Control Lite: Description'.t(), type: 'textfield', visible: true },
        { name: 'WEB_FILTER_CATEGORY', displayName: 'Web Filter: Category'.t(), type: 'textfield', visible: true },
        { name: 'WEB_FILTER_CATEGORY_DESCRIPTION', displayName: 'Web Filter: Category Description'.t(), type: 'textfield', visible: true },
        { name: 'WEB_FILTER_FLAGGED', displayName: 'Web Filter: Website is Flagged'.t(), type: 'boolean', visible: true },
        // { name: 'DIRECTORY_CONNECTOR_GROUP', displayName: 'Directory Connector: User in Group'.t(), type: 'editor', editor: Ext.create('Ung.GroupEditorWindow',{}), visible: true },
        // { name: 'CLIENT_COUNTRY', displayName: 'Client Country'.t(), type: 'editor', editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true },
        // { name: 'SERVER_COUNTRY', displayName: 'Server Country'.t(), type: 'editor', editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true }

    ]

});
