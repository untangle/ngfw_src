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

    emptyText: 'No Rules defined'.t(),

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
        Column.conditions,
    {
        header: 'Action'.t(),
        dataIndex: 'action',
        flex: 1,
        width: Renderer.messageWidth,
        renderer: Ung.apps.bandwidthcontrol.MainController.actionRenderer
    }],

    // todo: continue this stuff
    editorFields: [
        Field.enableRule(),
        Field.description,
        Field.conditions(
            'com.untangle.app.bandwidth_control.BandwidthControlRuleCondition', [
            "DST_ADDR",
            "DST_PORT",
            "DST_INTF",
            "SRC_ADDR",
            "SRC_PORT",
            "SRC_INTF",
            "PROTOCOL",
            "TAGGED",
            "USERNAME",
            "HOST_HOSTNAME",
            "CLIENT_HOSTNAME",
            "SERVER_HOSTNAME",
            {
                name:"HOST_MAC",
                displayName: "Host MAC Address".t(),
                type: "textfield"
            },
            "SRC_MAC",
            "DST_MAC",
            {
                name:"HOST_MAC_VENDOR",
                displayName: "Host MAC Vendor".t(),
                type: "textfield"
            },
            "CLIENT_MAC_VENDOR",
            "SERVER_MAC_VENDOR",
            {
                name:"HOST_IN_PENALTY_BOX",
                displayName: "Host in Penalty Box".t(),
                type: "boolean"
            },
            "CLIENT_IN_PENALTY_BOX",
            "SERVER_IN_PENALTY_BOX",
            "HOST_HAS_NO_QUOTA",
            "USER_HAS_NO_QUOTA",
            "CLIENT_HAS_NO_QUOTA",
            "SERVER_HAS_NO_QUOTA",
            "HOST_QUOTA_EXCEEDED",
            "USER_QUOTA_EXCEEDED",
            "CLIENT_QUOTA_EXCEEDED",
            "SERVER_QUOTA_EXCEEDED",
            "HOST_QUOTA_ATTAINMENT",
            "USER_QUOTA_ATTAINMENT",
            "CLIENT_QUOTA_ATTAINMENT",
            "SERVER_QUOTA_ATTAINMENT",
            'HOST_ENTITLED',
            "HTTP_HOST",
            "HTTP_REFERER",
            "HTTP_URI",
            "HTTP_URL",
            "HTTP_CONTENT_TYPE",
            "HTTP_CONTENT_LENGTH",
            "HTTP_USER_AGENT",
            "HTTP_USER_AGENT_OS",
            "APPLICATION_CONTROL_APPLICATION",
            "APPLICATION_CONTROL_CATEGORY",
            "APPLICATION_CONTROL_PROTOCHAIN",
            "APPLICATION_CONTROL_DETAIL",
            "APPLICATION_CONTROL_CONFIDENCE",
            "APPLICATION_CONTROL_PRODUCTIVITY",
            "APPLICATION_CONTROL_RISK",
            "PROTOCOL_CONTROL_SIGNATURE",
            "PROTOCOL_CONTROL_CATEGORY",
            "PROTOCOL_CONTROL_DESCRIPTION",
            "WEB_FILTER_CATEGORY",
            "WEB_FILTER_CATEGORY_DESCRIPTION",
            "WEB_FILTER_FLAGGED",
            "DIRECTORY_CONNECTOR_GROUP",
            'DIRECTORY_CONNECTOR_DOMAIN',
            {
                name:"REMOTE_HOST_COUNTRY",
                displayName: 'Remote Host Country'.t(),
                type: 'countryfield'
            },
            "CLIENT_COUNTRY",
            "SERVER_COUNTRY",
            'IP_REPUTATION_SRC_REPUTATION',
            'IP_REPUTATION_SRC_THREATMASK',
            'IP_REPUTATION_DST_REPUTATION',
            'IP_REPUTATION_DST_THREATMASK'
        ]), {
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
    ]
});
