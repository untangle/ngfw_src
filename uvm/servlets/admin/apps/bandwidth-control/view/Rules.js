Ext.define('Ung.apps.bandwidthcontrol.view.Rules', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-bandwidth-control-rules',
    itemId: 'rules',
    title: 'Rules'.t(),

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Rules are evaluated in-order on network traffic.'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete', 'reorder'],

    listProperty: 'settings.rules.list',
    ruleJavaClass: 'com.untangle.app.bandwidth_control.BandwidthControlRuleCondition',

    conditions: [
        {name:"DST_ADDR",displayName: "Destination Address".t(), type: "text", visible: true, vtype:"ipMatcher"},
        {name:"DST_PORT",displayName: "Destination Port".t(), type: "text",vtype:"portMatcher", visible: true},
        {name:"DST_INTF",displayName: "Destination Interface".t(), type: "checkgroup", values: Util.getInterfaceList(true, false), visible: true},
        {name:"SRC_ADDR",displayName: "Source Address".t(), type: "text", visible: true, vtype:"ipMatcher"},
        {name:"SRC_PORT",displayName: "Source Port".t(), type: "text", vtype:"portMatcher", visible: rpc.isExpertMode},
        {name:"SRC_INTF",displayName: "Source Interface".t(), type: "checkgroup", values: Util.getInterfaceList(true, false), visible: true},
        {name:"PROTOCOL",displayName: "Protocol".t(), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["any","any".t()]], visible: true},
        {name:"TAGGED",displayName: "Tagged".t(), type: "text", visible: true},
        //{name:"USERNAME",displayName: "Username".t(), type: "editor", editor: Ext.create('Ung.UserEditorWindow',{}), visible: true}, //FIXME
        {name:"HOST_HOSTNAME",displayName: "Host Hostname".t(), type: "text", visible: true},
        {name:"CLIENT_HOSTNAME",displayName: "Client Hostname".t(), type: "text", visible: rpc.isExpertMode},
        {name:"SERVER_HOSTNAME",displayName: "Server Hostname".t(), type: "text", visible: rpc.isExpertMode},
        {name:"HOST_MAC", displayName: "Host MAC Address".t(), type: "text", visible: true },
        {name:"SRC_MAC", displayName: "Client MAC Address".t(), type: "text", visible: true },
        {name:"DST_MAC", displayName: "Server MAC Address".t(), type: "text", visible: true },
        {name:"HOST_MAC_VENDOR",displayName: "Host MAC Vendor".t(), type: "text", visible: true},
        {name:"CLIENT_MAC_VENDOR",displayName: "Client MAC Vendor".t(), type: "text", visible: true},
        {name:"SERVER_MAC_VENDOR",displayName: "Server MAC Vendor".t(), type: "text", visible: true},
        {name:"HOST_IN_PENALTY_BOX",displayName: "Host in Penalty Box".t(), type: "boolean", visible: true},
        {name:"CLIENT_IN_PENALTY_BOX",displayName: "Client in Penalty Box".t(), type: "boolean", visible: rpc.isExpertMode},
        {name:"SERVER_IN_PENALTY_BOX",displayName: "Server in Penalty Box".t(), type: "boolean", visible: rpc.isExpertMode},
        {name:"HOST_HAS_NO_QUOTA",displayName: "Host has no Quota".t(), type: "boolean", visible: true},
        {name:"CLIENT_HAS_NO_QUOTA",displayName: "Client has no Quota".t(), type: "boolean", visible: rpc.isExpertMode},
        {name:"SERVER_HAS_NO_QUOTA",displayName: "Server has no Quota".t(), type: "boolean", visible: rpc.isExpertMode},
        {name:"USER_HAS_NO_QUOTA",displayName: "User has no Quota".t(), type: "boolean", visible: true},
        {name:"HOST_QUOTA_EXCEEDED",displayName: "Host has exceeded Quota".t(), type: "boolean", visible: true},
        {name:"CLIENT_QUOTA_EXCEEDED",displayName: "Client has exceeded Quota".t(), type: "boolean", visible: rpc.isExpertMode},
        {name:"SERVER_QUOTA_EXCEEDED",displayName: "Server has exceeded Quota".t(), type: "boolean", visible: rpc.isExpertMode},
        {name:"USER_QUOTA_EXCEEDED",displayName: "User has exceeded Quota".t(), type: "boolean", visible: true},
        {name:"HOST_QUOTA_ATTAINMENT",displayName: "Host Quota Attainment".t(), type: "text", visible: true},
        {name:"CLIENT_QUOTA_ATTAINMENT",displayName: "Client Quota Attainment".t(), type: "text", visible: rpc.isExpertMode},
        {name:"SERVER_QUOTA_ATTAINMENT",displayName: "Server Quota Attainment".t(), type: "text", visible: rpc.isExpertMode},
        {name:"USER_QUOTA_ATTAINMENT",displayName: "User Quota Attainment".t(), type: "text", visible: true},
        {name:"HTTP_HOST",displayName: "HTTP: Hostname".t(), type: "text", visible: true},
        {name:"HTTP_REFERER",displayName: "HTTP: Referer".t(), type: "text", visible: true},
        {name:"HTTP_URI",displayName: "HTTP: URI".t(), type: "text", visible: true},
        {name:"HTTP_URL",displayName: "HTTP: URL".t(), type: "text", visible: true},
        {name:"HTTP_CONTENT_TYPE",displayName: "HTTP: Content Type".t(), type: "text", visible: true},
        {name:"HTTP_CONTENT_LENGTH",displayName: "HTTP: Content Length".t(), type: "text", visible: true},
        {name:"HTTP_USER_AGENT",displayName: "HTTP: Client User Agent".t(), type: "text", visible: true},
        {name:"HTTP_USER_AGENT_OS",displayName: "HTTP: Client User OS".t(), type: "text", visible: false},
        {name:"APPLICATION_CONTROL_APPLICATION",displayName: "Application Control: Application".t(), type: "text", visible: true},
        {name:"APPLICATION_CONTROL_CATEGORY",displayName: "Application Control: Application Category".t(), type: "text", visible: true},
        {name:"APPLICATION_CONTROL_PROTOCHAIN",displayName: "Application Control: Protochain".t(), type: "text", visible: true},
        {name:"APPLICATION_CONTROL_DETAIL",displayName: "Application Control: Detail".t(), type: "text", visible: true},
        {name:"APPLICATION_CONTROL_CONFIDENCE",displayName: "Application Control: Confidence".t(), type: "text", visible: true},
        {name:"APPLICATION_CONTROL_PRODUCTIVITY",displayName: "Application Control: Productivity".t(), type: "text", visible: true},
        {name:"APPLICATION_CONTROL_RISK",displayName: "Application Control: Risk".t(), type: "text", visible: true},
        {name:"PROTOCOL_CONTROL_SIGNATURE",displayName: "Application Control Lite: Signature".t(), type: "text", visible: true},
        {name:"PROTOCOL_CONTROL_CATEGORY",displayName: "Application Control Lite: Category".t(), type: "text", visible: true},
        {name:"PROTOCOL_CONTROL_DESCRIPTION",displayName: "Application Control Lite: Description".t(), type: "text", visible: true},
        {name:"WEB_FILTER_CATEGORY",displayName: "Web Filter: Category".t(), type: "text", visible: true},
        {name:"WEB_FILTER_CATEGORY_DESCRIPTION",displayName: "Web Filter: Category Description".t(), type: "text", visible: true},
        {name:"WEB_FILTER_FLAGGED",displayName: "Web Filter: Website is Flagged".t(), type: "boolean", visible: true},
        //{name:"DIRECTORY_CONNECTOR_GROUP",displayName: "Directory Connector: User in Group".t(), type: "editor", editor: Ext.create('Ung.GroupEditorWindow',{}), visible: true}, //FIXME
        //{name:"REMOTE_HOST_COUNTRY",displayName: "Client Country".t(), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true}, //FIXME
        //{name:"CLIENT_COUNTRY",displayName: "Client Country".t(), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: rpc.isExpertMode}, //FIXME
        //{name:"SERVER_COUNTRY",displayName: "Server Country".t(), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: rpc.isExpertMode}, //FIXME
    ],

    emptyRow: {
        ruleId: 0,
        enabled: true,
        description: '',
        action: {
            actionType: 'SET_PRIORITY',
            javaClass: 'com.untangle.app.bandwidth_control.BandwidthControlRuleAction',
            priority: 7,
            quotaBytes: null,
            quotaTime: null,
            tagName: null,
            tagTime: null
        },
        conditions: {
            javaClass: 'java.util.LinkedList',
            list: []
        },
        javaClass: 'com.untangle.app.bandwidth_control.BandwidthControlRule'
    },

    bind: '{rules}',

    columns: [
        Column.ruleId,
        Column.live,
        Column.description,
        Column.conditions, {
            header: 'Action'.t(),
            dataIndex: 'action',
            width: 250,
            renderer: function (value, metaData, record) {
                if (typeof value === 'undefined') {
                    return 'Unknown action'.t();
                }
                switch(value.actionType) {
                  case 'SET_PRIORITY': return 'Set Priority' + ' [' + this.priorityRenderer(value.priority) + ']';
                  case 'TAG_HOST': return 'Tag Host'.t();
                  case 'APPLY_PENALTY_PRIORITY': return 'Apply Penalty Priority'.t(); // DEPRECATED
                  case 'GIVE_CLIENT_HOST_QUOTA': return 'Give Client a Quota'.t();
                  case 'GIVE_HOST_QUOTA': return 'Give Host a Quota'.t();
                  case 'GIVE_USER_QUOTA': return 'Give User a Quota'.t();
                default: return 'Unknown Action: ' + value;
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
            reference: 'actionType',
            publishes: 'value',
            fieldLabel: 'Action Type'.t(),
            bind: '{_action.actionType}',
            allowBlank: false,
            editable: false,
            store: [
                ['SET_PRIORITY', 'Set Priority'.t()],
                ['TAG_HOST', 'Tag Host'.t()],
                ['GIVE_HOST_QUOTA', 'Give Host a Quota'.t()],
                ['GIVE_USER_QUOTA', 'Give User a Quota'.t()]
            ],
            queryMode: 'local',
        }, {
            xtype: 'combo',
            fieldLabel: 'Priority'.t(),
            disabled: true,
            bind: {
                value: '{_action.priority}',
                disabled: '{actionType.value !== "SET_PRIORITY"}'
            },
            allowBlank: false,
            editable: false,
            store:[
                [1, 'Very High'.t()],
                [2, 'High'.t()],
                [3, 'Medium'.t()],
                [4, 'Low'.t()],
                [5, 'Limited'.t()],
                [6, 'Limited More'.t()],
                [7, 'Limited Severely'.t()]
            ],
            queryMode: 'local',
        },
        // {
        //     xtype: 'container',
        //     layout: {
        //         type: 'hbox'
        //     },
        //     items: [{
        //         xtype: 'numberfield',
        //         bind: {
        //             value: '{_action.tagTime}'
        //         },
        //         fieldLabel: 'Penalty Time'.t(),
        //         allowBlank: false,
        //         width: 350,
        //         labelWidth: 150
        //     }, {
        //         xtype: 'displayfield',
        //         html: 'seconds'.t()
        //     }]
        // }
    ]
});
