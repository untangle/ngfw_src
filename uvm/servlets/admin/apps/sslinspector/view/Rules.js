Ext.define('Ung.apps.sslinspector.view.Rules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-sslinspector-rules',
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
    ruleJavaClass: 'com.untangle.node.ssl_inspector.SslInspectorRuleCondition',

    emptyRow: {
        ruleId: null,
        live: true,
        description: '',
        action: {
            actionType: 'IGNORE',
            javaClass: 'com.untangle.node.ssl_inspector.SslInspectorRuleAction',
            flag: true
        },
        conditions: {
            javaClass: 'java.util.LinkedList',
            list: []
        },
        javaClass: 'com.untangle.node.ssl_inspector.SslInspectorRule'
    },

    bind: '{ignoreRules}',

    columns: [
        Column.ruleId,
        Column.live,
        Column.description,
        Column.conditions, {
            header: 'Action'.t(),
            dataIndex: 'action',
            renderer: function (act) {
                switch (act.actionType) {
                case 'INSPECT': return 'Inspect'.t();
                case 'IGNORE': return 'Ignore'.t();
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
            name: 'action',
            fieldLabel: 'Action Type'.t(),
            bind: '{_action.actionType}',
            allowBlank: false,
            editable: false,
            store: [['INSPECT', 'Inspect'.t()], ['IGNORE', 'Ignore'.t()]],
            queryMode: 'local'
        }

    ],

    conditions: [
        { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', visible: true, vtype: 'ipMatcher' },
        { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype: 'portMatcher', visible: true },
        { name: 'DST_INTF', displayName: 'Destination Interface'.t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, false), visible: true },
        { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', visible: true, vtype: 'ipMatcher' },
        { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'textfield', vtype: 'portMatcher', visible: rpc.isExpertMode },
        { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, false), visible: true },
        { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP', 'TCP'],['UDP', 'UDP'],['any', 'any'.t()]], visible: true},
        // { name: 'USERNAME', displayName: 'Username'.t(), type: 'editor', editor: Ext.create('Ung.UserEditorWindow',{}), visible: true},
        { name: 'CLIENT_HOSTNAME', displayName: 'Client Hostname'.t(), type: 'textfield', visible: true },
        { name: 'SERVER_HOSTNAME', displayName: 'Server Hostname'.t(), type: 'textfield', visible: true },
        { name: 'SRC_MAC', displayName: 'Client MAC Address'.t(), type: 'textfield', visible: true },
        { name: 'DST_MAC', displayName: 'Server MAC Address'.t(), type: 'textfield', visible: true },
        { name: 'CLIENT_MAC_VENDOR', displayName: 'Client MAC Vendor'.t(), type: 'textfield', visible: true },
        { name: 'SERVER_MAC_VENDOR', displayName: 'Server MAC Vendor'.t(), type: 'textfield', visible: true },
        { name: 'CLIENT_IN_PENALTY_BOX', displayName: 'Client in Penalty Box'.t(), type: 'boolean', visible: rpc.isExpertMode },
        { name: 'SERVER_IN_PENALTY_BOX', displayName: 'Server in Penalty Box'.t(), type: 'boolean', visible: rpc.isExpertMode },
        { name: 'CLIENT_HAS_NO_QUOTA', displayName: 'Client has no Quota'.t(), type: 'boolean', visible: rpc.isExpertMode },
        { name: 'SERVER_HAS_NO_QUOTA', displayName: 'Server has no Quota'.t(), type: 'boolean', visible: rpc.isExpertMode },
        { name: 'CLIENT_QUOTA_EXCEEDED', displayName: 'Client has exceeded Quota'.t(), type: 'boolean', visible: rpc.isExpertMode },
        { name: 'SERVER_QUOTA_EXCEEDED', displayName: 'Server has exceeded Quota'.t(), type: 'boolean', visible: rpc.isExpertMode },
        { name: 'CLIENT_QUOTA_ATTAINMENT', displayName: 'Client Quota Attainment'.t(), type: 'textfield', visible: rpc.isExpertMode },
        { name: 'SERVER_QUOTA_ATTAINMENT', displayName: 'Server Quota Attainment'.t(), type: 'textfield', visible: rpc.isExpertMode },
        { name: 'HTTP_USER_AGENT', displayName: 'HTTP: Client User Agent'.t(), type: 'textfield', visible: true },
        { name: 'HTTP_USER_AGENT_OS', displayName: 'HTTP: Client User OS'.t(), type: 'textfield', visible: false },
        // { name: 'DIRECTORY_CONNECTOR_GROUP', displayName: 'Directory Connector: User in Group'.t(), type: 'editor', editor: Ext.create('Ung.GroupEditorWindow',{}), visible: true},
        { name: 'SSL_INSPECTOR_SNI_HOSTNAME', displayName: 'SSL: SNI Host Name'.t(), type: 'textfield', visible: true },
        { name: 'SSL_INSPECTOR_SUBJECT_DN', displayName: 'SSL: Certificate Subject'.t(), type: 'textfield', visible: true },
        { name: 'SSL_INSPECTOR_ISSUER_DN', displayName: 'SSL: Certificate Issuer'.t(), type: 'textfield', visible: true },
        // { name: 'CLIENT_COUNTRY', displayName: 'Client Country'.t(), type: 'editor', editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true},
        // { name: 'SERVER_COUNTRY', displayName: 'Server Country'.t(), type: 'editor', editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true}
    ],


});
