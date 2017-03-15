Ext.define('Webui.firewall.settings', {
    extend:'Ung.NodeWin',
    panelRules: null,
    gridRules: null,
    gridEventLog: null,
    getAppSummary: function() {
        return i18n._("Firewall is a simple application that flags and blocks sessions based on rules.");
    },
    initComponent: function() {
        this.buildRules();
        this.buildTabPanel([this.panelRules]);
        this.callParent(arguments);
    },
    getConditions: function () {
        return [
            {name:"DST_ADDR",displayName: i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"DST_PORT",displayName: i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
            {name:"DST_INTF",displayName: i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
            {name:"SRC_ADDR",displayName: i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: rpc.isExpertMode},
            {name:"SRC_INTF",displayName: i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
            {name:"PROTOCOL",displayName: i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["any", i18n._("any")]], visible: true},
            {name:"USERNAME",displayName: i18n._("Username"), type: "editor", editor: Ext.create('Ung.UserEditorWindow',{}), visible: true},
            {name:"CLIENT_HOSTNAME",displayName: i18n._("Client Hostname"), type: "text", visible: true},
            {name:"SERVER_HOSTNAME",displayName: i18n._("Server Hostname"), type: "text", visible: rpc.isExpertMode},
            {name:"SRC_MAC", displayName: i18n._("Client MAC Address"), type: "text", visible: true },
            {name:"DST_MAC", displayName: i18n._("Server MAC Address"), type: "text", visible: true },
            {name:"CLIENT_MAC_VENDOR",displayName: i18n._("Client MAC Vendor"), type: "text", visible: true},
            {name:"SERVER_MAC_VENDOR",displayName: i18n._("Server MAC Vendor"), type: "text", visible: true},
            {name:"CLIENT_IN_PENALTY_BOX",displayName: i18n._("Client in Penalty Box"), type: "boolean", visible: true},
            {name:"SERVER_IN_PENALTY_BOX",displayName: i18n._("Server in Penalty Box"), type: "boolean", visible: true},
            {name:"CLIENT_HAS_NO_QUOTA",displayName: i18n._("Client has no Quota"), type: "boolean", visible: rpc.isExpertMode},
            {name:"SERVER_HAS_NO_QUOTA",displayName: i18n._("Server has no Quota"), type: "boolean", visible: rpc.isExpertMode},
            {name:"CLIENT_QUOTA_EXCEEDED",displayName: i18n._("Client has exceeded Quota"), type: "boolean", visible: true},
            {name:"SERVER_QUOTA_EXCEEDED",displayName: i18n._("Server has exceeded Quota"), type: "boolean", visible: true},
            {name:"CLIENT_QUOTA_ATTAINMENT",displayName: i18n._("Client Quota Attainment"), type: "text", visible: true},
            {name:"SERVER_QUOTA_ATTAINMENT",displayName: i18n._("Server Quota Attainment"), type: "text", visible: true},
            {name:"DIRECTORY_CONNECTOR_GROUP",displayName: i18n._("Directory Connector: User in Group"), type: "editor", editor: Ext.create('Ung.GroupEditorWindow',{}), visible: true},
            {name:"HTTP_USER_AGENT",displayName: i18n._("HTTP: Client User Agent"), type: "text", visible: true},
            {name:"HTTP_USER_AGENT_OS",displayName: i18n._("HTTP: Client User OS"), type: "text", visible: false},
            {name:"CLIENT_COUNTRY",displayName: i18n._("Client Country"), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true},
            {name:"SERVER_COUNTRY",displayName: i18n._("Server Country"), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true}
        ];
    },
    // Rules Panel
    buildRules: function() {
        this.panelRules = Ext.create('Ext.panel.Panel',{
            name: 'panelRules',
            helpSource: 'firewall_rules',
            title: i18n._('Rules'),
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                title: i18n._('Note'),
                flex: 0,
                html: i18n._("Routing and Port Forwarding functionality can be found elsewhere in Config->Networking.")
            },  this.gridRules= Ext.create('Ung.grid.Panel',{
                flex: 1,
                name: 'Rules',
                settingsCmp: this,
                hasReorder: true,
                addAtTop: false,
                title: i18n._("Rules"),
                dataProperty:'rules',
                recordJavaClass: "com.untangle.node.firewall.FirewallRule",
                emptyRow: {
                    "ruleId": 0,
                    "enabled": true,
                    "block": false,
                    "flag": false,
                    "description": ""
                },
                fields: [{
                    name: 'ruleId'
                }, {
                    name: 'enabled'
                }, {
                    name: 'block'
                }, {
                    name: 'flag'
                }, {
                    name: 'conditions'
                },{
                    name: 'description'
                }, {
                    name: 'javaClass'
                }],
                columns: [{
                    header: i18n._("Rule Id"),
                    width: 50,
                    dataIndex: 'ruleId',
                    renderer: function(value) {
                        return (value < 0) ? i18n._("new") : value;
                    }
                }, {
                    xtype:'checkcolumn',
                    header: i18n._("Enable"),
                    dataIndex: 'enabled',
                    resizable: false,
                    width:55
                }, {
                    header: i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex:1
                }, {
                    xtype:'checkcolumn',
                    header: i18n._("Block"),
                    dataIndex: 'block',
                    resizable: false,
                    width:55,
                    checkAll: {}
                }, {
                    xtype:'checkcolumn',
                    header: i18n._("Flag"),
                    dataIndex: 'flag',
                    resizable: false,
                    width:55,
                    checkAll: {}
                }],
                rowEditorInputLines: [{
                    xtype:'checkbox',
                    name: "Enable Rule",
                    dataIndex: "enabled",
                    fieldLabel: i18n._("Enable Rule")
                }, {
                    xtype:'textfield',
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: i18n._("Description"),
                    emptyText: i18n._("[no description]"),
                    width: 500
                }, {
                    xtype:'fieldset',
                    title: i18n._("If all of the following conditions are met:"),
                    items:[{
                        xtype:'rulebuilder',
                        settingsCmp: this,
                        javaClass: "com.untangle.node.firewall.FirewallRuleCondition",
                        dataIndex: "conditions",
                        conditions: this.getConditions()
                    }]
                }, {
                    xtype: 'fieldset',
                    title: i18n._('Perform the following action(s):'),
                    items:[{
                        xtype: "combo",
                        name: "actionType",
                        allowBlank: false,
                        dataIndex: "block",
                        fieldLabel: i18n._("Action Type"),
                        editable: false,
                        store: [[true, i18n._('Block')], [false, i18n._('Pass')]],
                        queryMode: 'local'
                    }, {
                        xtype:'checkbox',
                        name: "Flag",
                        dataIndex: "flag",
                        fieldLabel: i18n._("Flag")
                    }]
                }]
            })]
        });
    },
    beforeSave: function(isApply, handler) {
        this.settings.rules.list = this.gridRules.getList();
        handler.call(this, isApply);
    }
});
//# sourceURL=firewall-settings.js