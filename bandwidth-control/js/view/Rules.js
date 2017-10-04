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
        Column.enabled,
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
                  //case 'SET_PRIORITY': return 'Set Priority' + ' [' + this.priorityRenderer(value.priority) + ']';
                  case 'SET_PRIORITY':
                    var priostr = value.priority;
                    switch(value.priority) {
                      case 1: priostr = 'Very High'.t(); break;
                      case 2: priostr = 'High'.t(); break;
                      case 3: priostr = 'Medium'.t(); break;
                      case 4: priostr = 'Low'.t(); break;
                      case 5: priostr = 'Limited'.t(); break;
                      case 6: priostr = 'Limited More'.t(); break;
                      case 7: priostr = 'Limited Severely'.t(); break;
                    default: priostr = 'Unknown Priority'.t() + ': ' + value.priority; break;
                    }
                    return 'Set Priority'.t() + ' [' + priostr + ']';
                  case 'TAG_HOST': return 'Tag Host'.t();
                  case 'APPLY_PENALTY_PRIORITY': return 'Apply Penalty Priority'.t(); // DEPRECATED
                  case 'GIVE_CLIENT_HOST_QUOTA': return 'Give Client a Quota'.t();
                  case 'GIVE_HOST_QUOTA': return 'Give Host a Quota'.t();
                  case 'GIVE_USER_QUOTA': return 'Give User a Quota'.t();
                default: return 'Unknown Action'.t() + ': ' + value;
                }
            }
        }
    ],

    // todo: continue this stuff
    editorFields: [
        Field.enableRule(),
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
            hidden: true,
            disabled: true,
            bind: {
                value: '{_action.priority}',
                hidden: '{actionType.value !== "SET_PRIORITY"}',
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
        }, {
            xtype: 'combo',
            fieldLabel: 'Quota Expiration'.t(),
            hidden: true,
            disabled: true,
            bind: {
                value: '{_action.quotaTime}',
                hidden: '{actionType.value !== "GIVE_HOST_QUOTA" && actionType.value !== "GIVE_USER_QUOTA"}',
                disabled: '{actionType.value !== "GIVE_HOST_QUOTA" && actionType.value !== "GIVE_USER_QUOTA"}'
            },
            allowBlank: false,
            editable: true,
            store:[
             [-4, "End of Month".t()], //END_OF_MONTH from QuotaBoxEntry
             [-3, "End of Week".t()], //END_OF_WEEK from QuotaBoxEntry
             [-2, "End of Day".t()], //END_OF_DAY from QuotaBoxEntry
             [-1, "End of Hour".t()] //END_OF_HOUR from QuotaBoxEntry
            ],
            queryMode: 'local',
        }, {
            xtype: 'numberfield',
            fieldLabel: 'Quota Bytes'.t(),
            hidden: true,
            disabled: true,
            bind: {
                value: '{_action.quotaBytes}',
                hidden: '{actionType.value !== "GIVE_HOST_QUOTA" && actionType.value !== "GIVE_USER_QUOTA"}',
                disabled: '{actionType.value !== "GIVE_HOST_QUOTA" && actionType.value !== "GIVE_USER_QUOTA"}'
            },
            allowBlank: false,
            editable: true,
        }, {
            xtype: 'textfield',
            fieldLabel: 'Tag Name'.t(),
            hidden: true,
            disabled: true,
            bind: {
                value: '{_action.tagName}',
                hidden: '{actionType.value !== "TAG_HOST"}',
                disabled: '{actionType.value !== "TAG_HOST"}'
            },
            allowBlank: false,
            editable: true,
        }, {
            xtype: 'numberfield',
            fieldLabel: 'Tag Time (Seconds)'.t(),
            hidden: true,
            disabled: true,
            bind: {
                value: '{_action.tagTime}',
                hidden: '{actionType.value !== "TAG_HOST"}',
                disabled: '{actionType.value !== "TAG_HOST"}'
            },
            allowBlank: false,
            editable: true,
        }
    ],

    conditions: [
        {name:"DST_ADDR",displayName: "Destination Address".t(), type: "textfield", visible: true, vtype:"ipMatcher"},
        {name:"DST_PORT",displayName: "Destination Port".t(), type: "textfield",vtype:"portMatcher", visible: true},
        {name:"DST_INTF",displayName: "Destination Interface".t(), type: "checkboxgroup", values: Util.getInterfaceList(true, false), visible: true},
        {name:"SRC_ADDR",displayName: "Source Address".t(), type: "textfield", visible: true, vtype:"ipMatcher"},
        {name:"SRC_PORT",displayName: "Source Port".t(), type: "textfield", vtype:"portMatcher", visible: rpc.isExpertMode},
        {name:"SRC_INTF",displayName: "Source Interface".t(), type: "checkboxgroup", values: Util.getInterfaceList(true, false), visible: true},
        {name:"PROTOCOL",displayName: "Protocol".t(), type: "checkboxgroup", values: [["TCP","TCP"],["UDP","UDP"],["any","any".t()]], visible: true},
        {name:"TAGGED",displayName: "Tagged".t(), type: "textfield", visible: true},
        {name:"USERNAME",displayName: "Username".t(), type: "userfield", visible: true},
        {name:"HOST_HOSTNAME",displayName: "Host Hostname".t(), type: "textfield", visible: true},
        {name:"CLIENT_HOSTNAME",displayName: "Client Hostname".t(), type: "textfield", visible: false},
        {name:"SERVER_HOSTNAME",displayName: "Server Hostname".t(), type: "textfield", visible: false},
        {name:"HOST_MAC", displayName: "Host MAC Address".t(), type: "textfield", visible: true },
        {name:"SRC_MAC", displayName: "Client MAC Address".t(), type: "textfield", visible: true },
        {name:"DST_MAC", displayName: "Server MAC Address".t(), type: "textfield", visible: true },
        {name:"HOST_MAC_VENDOR",displayName: "Host MAC Vendor".t(), type: "textfield", visible: true},
        {name:"CLIENT_MAC_VENDOR",displayName: "Client MAC Vendor".t(), type: "textfield", visible: true},
        {name:"SERVER_MAC_VENDOR",displayName: "Server MAC Vendor".t(), type: "textfield", visible: true},
        {name:"HOST_IN_PENALTY_BOX",displayName: "Host in Penalty Box".t(), type: "boolean", visible: false},
        {name:"CLIENT_IN_PENALTY_BOX",displayName: "Client in Penalty Box".t(), type: "boolean", visible: false},
        {name:"SERVER_IN_PENALTY_BOX",displayName: "Server in Penalty Box".t(), type: "boolean", visible: false},
        {name:"HOST_HAS_NO_QUOTA",displayName: "Host has no Quota".t(), type: "boolean", visible: true},
        {name:"USER_HAS_NO_QUOTA",displayName: "User has no Quota".t(), type: "boolean", visible: true},
        {name:"CLIENT_HAS_NO_QUOTA",displayName: "Client has no Quota".t(), type: "boolean", visible: false},
        {name:"SERVER_HAS_NO_QUOTA",displayName: "Server has no Quota".t(), type: "boolean", visible: false},
        {name:"HOST_QUOTA_EXCEEDED",displayName: "Host has exceeded Quota".t(), type: "boolean", visible: true},
        {name:"USER_QUOTA_EXCEEDED",displayName: "User has exceeded Quota".t(), type: "boolean", visible: true},
        {name:"CLIENT_QUOTA_EXCEEDED",displayName: "Client has exceeded Quota".t(), type: "boolean", visible: false},
        {name:"SERVER_QUOTA_EXCEEDED",displayName: "Server has exceeded Quota".t(), type: "boolean", visible: false},
        {name:"HOST_QUOTA_ATTAINMENT",displayName: "Host Quota Attainment".t(), type: "textfield", visible: true},
        {name:"USER_QUOTA_ATTAINMENT",displayName: "User Quota Attainment".t(), type: "textfield", visible: true},
        {name:"CLIENT_QUOTA_ATTAINMENT",displayName: "Client Quota Attainment".t(), type: "textfield", visible: false},
        {name:"SERVER_QUOTA_ATTAINMENT",displayName: "Server Quota Attainment".t(), type: "textfield", visible: false},
        {name:'HOST_ENTITLED',displayName: 'Host Entitled'.t(), type: 'boolean', visible: true},
        {name:"HTTP_HOST",displayName: "HTTP: Hostname".t(), type: "textfield", visible: true},
        {name:"HTTP_REFERER",displayName: "HTTP: Referer".t(), type: "textfield", visible: true},
        {name:"HTTP_URI",displayName: "HTTP: URI".t(), type: "textfield", visible: true},
        {name:"HTTP_URL",displayName: "HTTP: URL".t(), type: "textfield", visible: true},
        {name:"HTTP_CONTENT_TYPE",displayName: "HTTP: Content Type".t(), type: "textfield", visible: true},
        {name:"HTTP_CONTENT_LENGTH",displayName: "HTTP: Content Length".t(), type: "textfield", visible: true},
        {name:"HTTP_USER_AGENT",displayName: "HTTP: Client User Agent".t(), type: "textfield", visible: true},
        {name:"HTTP_USER_AGENT_OS",displayName: "HTTP: Client User OS".t(), type: "textfield", visible: false},
        {name:"APPLICATION_CONTROL_APPLICATION",displayName: "Application Control: Application".t(), type: "textfield", visible: true},
        {name:"APPLICATION_CONTROL_CATEGORY",displayName: "Application Control: Application Category".t(), type: "textfield", visible: true},
        {name:"APPLICATION_CONTROL_PROTOCHAIN",displayName: "Application Control: Protochain".t(), type: "textfield", visible: true},
        {name:"APPLICATION_CONTROL_DETAIL",displayName: "Application Control: Detail".t(), type: "textfield", visible: true},
        {name:"APPLICATION_CONTROL_CONFIDENCE",displayName: "Application Control: Confidence".t(), type: "textfield", visible: true},
        {name:"APPLICATION_CONTROL_PRODUCTIVITY",displayName: "Application Control: Productivity".t(), type: "textfield", visible: true},
        {name:"APPLICATION_CONTROL_RISK",displayName: "Application Control: Risk".t(), type: "textfield", visible: true},
        {name:"PROTOCOL_CONTROL_SIGNATURE",displayName: "Application Control Lite: Signature".t(), type: "textfield", visible: true},
        {name:"PROTOCOL_CONTROL_CATEGORY",displayName: "Application Control Lite: Category".t(), type: "textfield", visible: true},
        {name:"PROTOCOL_CONTROL_DESCRIPTION",displayName: "Application Control Lite: Description".t(), type: "textfield", visible: true},
        {name:"WEB_FILTER_CATEGORY",displayName: "Web Filter: Category".t(), type: "textfield", visible: true},
        {name:"WEB_FILTER_CATEGORY_DESCRIPTION",displayName: "Web Filter: Category Description".t(), type: "textfield", visible: true},
        {name:"WEB_FILTER_FLAGGED",displayName: "Web Filter: Website is Flagged".t(), type: "boolean", visible: true},
        {name:"DIRECTORY_CONNECTOR_GROUP",displayName: 'Directory Connector: User in Group'.t(), type: 'directorygroupfield', visible: true},
        {name:'DIRECTORY_CONNECTOR_DOMAIN',displayName: 'Directory Connector: User in Domain'.t(), type: 'directorydomainfield', visible: true},
        {name:"REMOTE_HOST_COUNTRY",displayName: 'Remote Host Country'.t(), type: 'countryfield', visible: true},
        {name:"CLIENT_COUNTRY",displayName: 'Client Country'.t(), type: 'countryfield', visible: true},
        {name:"SERVER_COUNTRY",displayName: 'Server Country'.t(), type: 'countryfield', visible: true}
    ],

});
