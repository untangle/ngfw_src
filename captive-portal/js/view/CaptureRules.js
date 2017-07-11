Ext.define('Ung.apps.captiveportal.view.CaptureRules', {
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
    ruleJavaClass: 'com.untangle.app.captive_portal.CaptureRuleCondition',

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
            width: 70
        }

    ],

    // todo: continue this stuff
    editorFields: [
        Field.enableRule(),
        Field.description,
        Field.conditions, {
            xtype: 'combo',
            allowBlank: false,
            bind: '{record.capture}',
            fieldLabel: 'Action Type'.t(),
            editable: false,
            store: [[true, 'Capture'.t()], [false, 'Pass'.t()]],
            queryMode: 'local'
        }
    ],

    conditions: [
        {name:"DST_ADDR",displayName: "Destination Address".t(), type: "textfield", visible: true, vtype:"ipMatcher"},
        {name:"DST_PORT",displayName: "Destination Port".t(), type: "textfield",vtype:"portMatcher", visible: true},
        {name:"DST_INTF",displayName: "Destination Interface".t(), type: "checkboxgroup", values: Util.getInterfaceList(true, false), visible: true},
        {name:"SRC_ADDR",displayName: "Source Address".t(), type: "textfield", visible: true, vtype:"ipMatcher"},
        {name:"SRC_PORT",displayName: "Source Port".t(), type: "textfield",vtype:"portMatcher", visible: rpc.isExpertMode},
        {name:"SRC_INTF",displayName: "Source Interface".t(), type: "checkboxgroup", values: Util.getInterfaceList(true, false), visible: true},
        {name:"PROTOCOL",displayName: "Protocol".t(), type: "checkboxgroup", values: [["TCP","TCP"],["UDP","UDP"],["any", "any".t()]], visible: true},
        {name:"TAGGED",displayName: "Tagged".t(), type: "textfield", visible: true},
        {name:"USERNAME",displayName: "Username".t(), type: "userfield", visible: true},
        {name:"HOST_HOSTNAME",displayName: "Client Hostname".t(), type: "textfield", visible: true},
        {name:"CLIENT_HOSTNAME",displayName: "Client Hostname".t(), type: "textfield", visible: false},
        {name:"SERVER_HOSTNAME",displayName: "Server Hostname".t(), type: "textfield", visible: false},
        {name:"SRC_MAC", displayName: "Client MAC Address".t(), type: "textfield", visible: true },
        {name:"DST_MAC", displayName: "Server MAC Address".t(), type: "textfield", visible: true },
        {name:"CLIENT_MAC_VENDOR",displayName: "Client MAC Vendor".t(), type: "textfield", visible: true},
        {name:"SERVER_MAC_VENDOR",displayName: "Server MAC Vendor".t(), type: "textfield", visible: true},
        {name:"CLIENT_IN_PENALTY_BOX",displayName: "Client in Penalty Box".t(), type: "boolean", visible: false},
        {name:"SERVER_IN_PENALTY_BOX",displayName: "Server in Penalty Box".t(), type: "boolean", visible: false},
        {name:"HOST_HAS_NO_QUOTA",displayName: "Host has no Quota".t(), type: "boolean", visible: true},
        {name:"USER_HAS_NO_QUOTA",displayName: "User has no Quota".t(), type: "boolean", visible: true},
        {name:"CLIENT_HAS_NO_QUOTA",displayName: "Client has no Quota".t(), type: "boolean", visible: rpc.isExpertMode},
        {name:"SERVER_HAS_NO_QUOTA",displayName: "Server has no Quota".t(), type: "boolean", visible: rpc.isExpertMode},
        {name:"HOST_QUOTA_EXCEEDED",displayName: "Host has exceeded Quota".t(), type: "boolean", visible: true},
        {name:"USER_QUOTA_EXCEEDED",displayName: "User has exceeded Quota".t(), type: "boolean", visible: true},
        {name:"CLIENT_QUOTA_EXCEEDED",displayName: "Client has exceeded Quota".t(), type: "boolean", visible: rpc.isExpertMode},
        {name:"SERVER_QUOTA_EXCEEDED",displayName: "Server has exceeded Quota".t(), type: "boolean", visible: rpc.isExpertMode},
        {name:"HOST_QUOTA_ATTAINMENT",displayName: "Host Quota Attainment".t(), type: "textfield", visible: true},
        {name:"USER_QUOTA_ATTAINMENT",displayName: "User Quota Attainment".t(), type: "textfield", visible: true},
        {name:"CLIENT_QUOTA_ATTAINMENT",displayName: "Client Quota Attainment".t(), type: "textfield", visible: rpc.isExpertMode},
        {name:'HOST_ENTITLED',displayName: 'Host Entitled'.t(), type: 'boolean', visible: true},
        {name:"SERVER_QUOTA_ATTAINMENT",displayName: "Server Quota Attainment".t(), type: "textfield", visible: rpc.isExpertMode},
        {name:"DIRECTORY_CONNECTOR_GROUP",displayName: "Directory Connector: User in Group".t(), type: "directorygroupfield", visible: true},
        {name:'HTTP_HOST', displayName: 'HTTP: Hostname'.t(), type: 'textfield', visible: true },
        {name:'HTTP_REFERER', displayName: 'HTTP: Referer'.t(), type: 'textfield', visible: true },
        {name:'HTTP_URI', displayName: 'HTTP: URI'.t(), type: 'textfield', visible: true },
        {name:'HTTP_URL', displayName: 'HTTP: URL'.t(), type: 'textfield', visible: true },
        {name:'HTTP_CONTENT_TYPE',displayName: 'HTTP: Content Type'.t(), type: 'textfield', visible: true},
        {name:'HTTP_CONTENT_LENGTH',displayName: 'HTTP: Content Length'.t(), type: 'textfield', visible: true},
        {name:'HTTP_REQUEST_METHOD',displayName: 'HTTP: Request Method'.t(), type: 'textfield', visible: true},
        {name:'HTTP_REQUEST_FILE_PATH',displayName: 'HTTP: Request File Path'.t(), type: 'textfield', visible: true},
        {name:'HTTP_REQUEST_FILE_NAME',displayName: 'HTTP: Request File Name'.t(), type: 'textfield', visible: true},
        {name:'HTTP_REQUEST_FILE_EXTENSION',displayName: 'HTTP: Request File Extension'.t(), type: 'textfield', visible: true},
        {name:'HTTP_RESPONSE_FILE_NAME',displayName: 'HTTP: Response File Name'.t(), type: 'textfield', visible: true},
        {name:'HTTP_RESPONSE_FILE_EXTENSION',displayName: 'HTTP: Response File Extension'.t(), type: 'textfield', visible: true},
        {name:'HTTP_USER_AGENT',displayName: 'HTTP: Client User Agent'.t(), type: 'textfield', visible: true},
        {name:'HTTP_USER_AGENT_OS',displayName: 'HTTP: Client User OS'.t(), type: 'textfield', visible: false},
        {name:'HTTP_USER_AGENT', displayName: 'HTTP: Client User Agent'.t(), type: 'textfield', visible: true },
        {name:'HTTP_USER_AGENT_OS', displayName: 'HTTP: Client User OS'.t(), type: 'textfield', visible: false },
        {name:"CLIENT_COUNTRY",displayName: "Client Country".t(), type: "countryfield", visible: true},
        {name:"SERVER_COUNTRY",displayName: "Server Country".t(), type: "countryfield", visible: true}
    ]

});
