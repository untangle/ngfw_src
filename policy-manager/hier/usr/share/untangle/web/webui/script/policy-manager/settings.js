Ext.define('Webui.policy-manager.settings', {
    extend:'Ung.AppWin',
    gridRules: null,
    gridEventLog: null,
    getAppSummary: function() {
        return i18n._("Policy Manager enables administrators to create different policies and handle different sessions with different policies based on rules.");
    },
    initComponent: function() {
        this.buildPolicies();
        this.buildRules();
        this.buildTabPanel([this.panelPolicies, this.gridRules]);
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
            {name:"TIME_OF_DAY",displayName: i18n._("Time of Day"), type: "editor", editor: Ext.create('Ung.TimeEditorWindow',{}), visible: true},
            {name:"DAY_OF_WEEK",displayName: i18n._("Day of Week"), type: "checkgroup", values: Ung.Util.getDayOfWeekList(), visible: true},
            {name:"CLIENT_HOSTNAME",displayName: i18n._("Client Hostname"), type: "text", visible: true},
            {name:"SERVER_HOSTNAME",displayName: i18n._("Server Hostname"), type: "text", visible: rpc.isExpertMode},
            {name:"SRC_MAC", displayName: i18n._("Client MAC Address"), type: "text", visible: true },
            {name:"DST_MAC", displayName: i18n._("Server MAC Address"), type: "text", visible: true },
            {name:"CLIENT_MAC_VENDOR",displayName: i18n._("Client MAC Vendor"), type: "text", visible: true},
            {name:"SERVER_MAC_VENDOR",displayName: i18n._("Server MAC Vendor"), type: "text", visible: true},
            {name:"CLIENT_IN_PENALTY_BOX",displayName: i18n._("Client in Penalty Box"), type: "boolean", visible: true},
            {name:"SERVER_IN_PENALTY_BOX",displayName: i18n._("Server in Penalty Box"), type: "boolean", visible: true},
            {name:"CLIENT_HAS_NO_QUOTA",displayName: i18n._("Client has no Quota"), type: "boolean", visible: true},
            {name:"SERVER_HAS_NO_QUOTA",displayName: i18n._("Server has no Quota"), type: "boolean", visible: true},
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
    // Policies Panel
    buildPolicies: function() {
        this.policyStore = [];
        this.parentPolicyStore = [];

        this.policyStore.push([null, ""]);
        this.policyStore.push([0, i18n._("> No Rack")]);
        this.parentPolicyStore.push([null, ""]);
        this.parentPolicyStore.push([0, i18n._("none")]);

        for( var i=0 ; i<this.getSettings().policies.list.length ; i++ ) {
            var policy = this.getSettings().policies.list[i];
            this.policyStore.push([policy.policyId, policy.name]);
            this.parentPolicyStore.push([policy.policyId, policy.name]);
        }
        this.policyNamesMap = Ung.Util.createStoreMap(this.policyStore);
        this.parentPolicyNamesMap = Ung.Util.createStoreMap(this.parentPolicyStore);
        
        this.policyRenderer = Ext.bind( function(policyId) {
            return this.policyNamesMap[policyId];
        }, this);
        this.parentPolicyRenderer = Ext.bind( function(policyId) {
            return this.parentPolicyNamesMap[policyId];
        }, this);
        
        this.panelPolicies = Ext.create('Ext.panel.Panel',{
            name: 'panelPolicies',
            helpSource: 'policy_manager_policies',
            title: i18n._('Policies'),
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                flex: 0,
                title: i18n._('Note'),
                html: i18n._("<b>Policy Manager</b> allows for the creation of multiple policies (also known as virtual racks) and controls which sessions are processed by which policies.") + "<br/>" +
                    i18n._("For each new session the <b>Rules</b> are evaluated in order and the <b>Target Policy</b> for the first matching rule is used to handle the session.") + "<br/>" +
                    i18n._("If no rules match, the First Policy (Id:1) is used to handle the session.")
            }, this.gridPolicies = Ext.create('Ung.grid.Panel', {
                flex: 1,
                name: 'Policies',
                settingsCmp: this,
                addAtTop: false,
                title: i18n._("Racks"),
                dataProperty: "policies",
                recordJavaClass: "com.untangle.app.policy_manager.PolicySettings",
                emptyRow: {
                    "name": i18n._("New Rack"),
                    "description": "",
                    "parentId": null
                },
                sortField: 'policyId',
                fields: [{
                    name: "policyId"
                },{
                    name: "name"
                },{
                    name: "description"
                },{
                    name: "parentId"
                }],
                columns: [{
                    header: i18n._("Id"),
                    width: 55,
                    dataIndex: 'policyId',
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    },
                    renderer: function(value) {
                        if (value == "") {
                            return i18n._("new");
                        } else {
                            return value;
                        }
                    }
                }, {
                    header: i18n._("Name"),
                    width: 200,
                    dataIndex: 'name',
                    editor: {
                        xtype:'textfield',
                        emptyText: i18n._("[enter name]"),
                        allowBlank:false
                    }
                }, {
                    header: "",
                    width: 70,
                    renderer: Ext.bind( function(value, metadata, record) {
                        if (record.data.policyId == 1)
                            return "<b>" + i18n._("Default") + "</b>";
                        else
                            return "";
                    }, this)
                }, {
                    header: i18n._("Description"),
                    width: 200,
                    flex:1,
                    dataIndex: 'description',
                    editor: {
                        xtype:'textfield',
                        emptyText: i18n._("[enter description]"),
                        allowBlank: false
                    }
                },{
                    header: i18n._("Parent"),
                    width: 200,
                    dataIndex: 'parentId',
                    renderer: this.parentPolicyRenderer
                }],
                rowEditorInputLines: [{
                    xtype:'textfield',
                    width: 250,
                    name: "Name",
                    dataIndex: "name",
                    fieldLabel: i18n._("Name"),
                    emptyText: i18n._("[enter name]"),
                    allowBlank: false
                }, {
                    xtype:'textarea',
                    width: 400,
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: i18n._("Description"),
                    emptyText: i18n._("[enter description]"),
                    allowBlank: false
                }, {
                    xtype: "combo",
                    name: "parent",
                    allowBlank: true,
                    dataIndex: "parentId",
                    fieldLabel: i18n._("Parent"),
                    editable: false,
                    store: this.parentPolicyStore,
                    queryMode: 'local'
                }]
            })]
        });
    },
    // Rules Panel
    buildRules: function() {
        this.gridRules= Ext.create('Ung.grid.Panel',{
            name: 'Rules',
            settingsCmp: this,
            helpSource: 'policy_manager_rules',
            height: 500,
            hasReorder: true,
            addAtTop: false,
            title: i18n._("Rules"),
            dataProperty:'rules',
            recordJavaClass: "com.untangle.app.policy_manager.PolicyRule",
            emptyRow: {
                "ruleId": -1,
                "enabled": true,
                "targetPolicy": 1,
                "description": ""
            },
            fields: [{
                name: 'ruleId'
            }, {
                name: 'enabled'
            }, {
                name: 'targetPolicy'
            }, {
                name: 'conditions'
            },{
                name: 'description'
            }, {
                name: 'javaClass'
            }],
            columns: [{
                header: i18n._("Rule ID"),
                dataIndex: 'ruleId',
                width: 50,
                renderer: function(value) {
                    if (value < 0) {
                        return i18n._("new");
                    } else {
                        return value;
                    }
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
                header: i18n._("Target Policy"),
                dataIndex: 'targetPolicy',
                renderer: this.policyRenderer,
                resizable: false,
                width: 200
            }],
            rowEditorInputLines: [{
                xtype:'checkbox',
                name: "Enable Rule",
                dataIndex: "enabled",
                fieldLabel: i18n._("Enable Rule")
            },{
                xtype:'textfield',
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
                width: 500
            },{
                xtype:'fieldset',
                title: i18n._("If all of the following conditions are met:"),
                items:[{
                    xtype:'rulebuilder',
                    settingsCmp: this,
                    javaClass: "com.untangle.app.policy_manager.PolicyRuleCondition",
                    dataIndex: "conditions",
                    conditions: this.getConditions()
                }]
            },{
                xtype: 'fieldset',
                title: i18n._('Perform the following action(s):'),
                items: [{
                    xtype: "combo",
                    name: "targetPolicy",
                    allowBlank: false,
                    dataIndex: "targetPolicy",
                    fieldLabel: i18n._("Target Policy"),
                    editable: false,
                    store: this.policyStore,
                    queryMode: 'local'
                }]
            }]
        });
    },
    beforeSave: function(isApply, handler) {
        this.settings.rules.list = this.gridRules.getList();
        this.settings.policies.list = this.gridPolicies.getList();
        handler.call(this, isApply);
    },
    rebuildTab: function(panelName, buildFn, index) {
        var isPanelActive = (this.tabs.getActiveTab() == this[panelName]);
        this.tabs.remove(this[panelName]);
        buildFn.call(this);
        this.tabs.insert(index, this[panelName]);
        if(isPanelActive) {
            this.tabs.setActiveTab(index);
        }
    },
    afterSave: function() {
        //Rebuilds panelPolicies tab
        this.rebuildTab("panelPolicies", this.buildPolicies, 0);
        this.rebuildTab("gridRules", this.buildRules, 1);
    },
    beforeClose: function() {
        if(!this.toRemove) {
            Ext.defer(Ung.Main.loadPolicies, 1, Ung.Main);
        }
    }
});
//# sourceURL=policy-manager-settings.js