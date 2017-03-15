// Application Control settings
Ext.define('Webui.application-control.settings', {
    extend: 'Ung.NodeWin',
    nodeStats: null,
    gridProtoRules: null,
    gridLogicRules: null,
    gridEventLog: null,
    gridRuleEventLog: null,
    getAppSummary: function() {
        return i18n._("Application Control scans sessions and identifies the associated applications allowing each to be flagged and/or blocked.");
    },
    initComponent: function() {
        this.nodeStats = this.getRpcNode().getStatistics();

        this.buildGridProtoRules();
        this.buildGridLogicRules();
        // builds the tab panel with the tabs
        this.buildTabPanel([this.gridProtoRules, this.gridLogicRules]);
        this.callParent(arguments);
    },
    getConditions: function () {
        return [
            {name:"DST_ADDR",displayName: i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"DST_PORT",displayName: i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
            {name:"DST_INTF",displayName: i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
            {name:"SRC_ADDR",displayName: i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: i18n._("Source Port"), type: "text", vtype:"portMatcher", visible: rpc.isExpertMode},
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
            {name:"CLIENT_HAS_NO_QUOTA",displayName: i18n._("Client has no Quota"), type: "boolean", visible: true},
            {name:"SERVER_HAS_NO_QUOTA",displayName: i18n._("Server has no Quota"), type: "boolean", visible: true},
            {name:"CLIENT_QUOTA_EXCEEDED",displayName: i18n._("Client has exceeded Quota"), type: "boolean", visible: true},
            {name:"SERVER_QUOTA_EXCEEDED",displayName: i18n._("Server has exceeded Quota"), type: "boolean", visible: true},
            {name:"CLIENT_QUOTA_ATTAINMENT",displayName: i18n._("Client Quota Attainment"), type: "text", visible: true},
            {name:"SERVER_QUOTA_ATTAINMENT",displayName: i18n._("Server Quota Attainment"), type: "text", visible: true},
            {name:"HTTP_HOST",displayName: i18n._("HTTP: Hostname"), type: "text", visible: true},
            {name:"HTTP_REFERER",displayName: i18n._("HTTP: Referer"), type: "text", visible: true},
            {name:"HTTP_URI",displayName: i18n._("HTTP: URI"), type: "text", visible: true},
            {name:"HTTP_URL",displayName: i18n._("HTTP: URL"), type: "text", visible: true},
            {name:"HTTP_CONTENT_TYPE",displayName: i18n._("HTTP: Content Type"), type: "text", visible: true},
            {name:"HTTP_CONTENT_LENGTH",displayName: i18n._("HTTP: Content Length"), type: "text", visible: true},
            {name:"HTTP_USER_AGENT",displayName: i18n._("HTTP: Client User Agent"), type: "text", visible: true},
            {name:"HTTP_USER_AGENT_OS",displayName: i18n._("HTTP: Client User OS"), type: "text", visible: false},
            {name:"APPLICATION_CONTROL_APPLICATION",displayName: i18n._("Application Control: Application"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_CATEGORY",displayName: i18n._("Application Control: Application Category"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_PROTOCHAIN",displayName: i18n._("Application Control: ProtoChain"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_DETAIL",displayName: i18n._("Application Control: Detail"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_CONFIDENCE",displayName: i18n._("Application Control: Confidence"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_PRODUCTIVITY",displayName: i18n._("Application Control: Productivity"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_RISK",displayName: i18n._("Application Control: Risk"), type: "text", visible: true},
            {name:"PROTOCOL_CONTROL_SIGNATURE",displayName: i18n._("Application Control Lite: Signature"), type: "text", visible: true},
            {name:"PROTOCOL_CONTROL_CATEGORY",displayName: i18n._("Application Control Lite: Category"), type: "text", visible: true},
            {name:"PROTOCOL_CONTROL_DESCRIPTION",displayName: i18n._("Application Control Lite: Description"), type: "text", visible: true},
            {name:"WEB_FILTER_CATEGORY",displayName: i18n._("Web Filter: Category"), type: "text", visible: true},
            {name:"WEB_FILTER_CATEGORY_DESCRIPTION",displayName: i18n._("Web Filter: Category Description"), type: "text", visible: true},
            {name:"WEB_FILTER_FLAGGED",displayName: i18n._("Web Filter: Website is Flagged"), type: "boolean", visible: true},
            {name:"DIRECTORY_CONNECTOR_GROUP",displayName: i18n._("Directory Connector: User in Group"), type: "editor", editor: Ext.create('Ung.GroupEditorWindow',{}), visible: true},
            {name:"CLIENT_COUNTRY",displayName: i18n._("Client Country"), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true},
            {name:"SERVER_COUNTRY",displayName: i18n._("Server Country"), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true}
        ];
    },
    statFormat: function(input) {
         var s = input.toString(), l = s.length, o = '';
         while (l > 3) {
             var c = s.substr(l - 3, 3);
             o = ',' + c + o;
             s = s.replace(c, '');
             l -= 3;
         }
         o = s + o;
         return o;
    },

    // Status Panel
    buildStatus: function() {
        this.panelStatus = Ext.create('Ung.panel.Status',{
            settingsCmp: this,
            helpSource: 'application_control_status',
            itemsToAppend: [{
                title: i18n._('Traffic Statistics'),
                defaults: {
                    xtype: "displayfield",
                    labelWidth: 200
                },
                items: [{
                    fieldLabel: i18n._('Sessions Scanned'),
                    name: 'packetCount',
                    value: this.statFormat(this.nodeStats.sessionCount)
                },{
                    fieldLabel: i18n._('Sessions Allowed'),
                    name: 'allowedCount',
                    value: this.statFormat(this.nodeStats.allowedCount)
                },{
                    fieldLabel: i18n._('Sessions Flagged'),
                    name: 'flaggedCount',
                    value: this.statFormat(this.nodeStats.flaggedCount)
                },{
                    fieldLabel: i18n._('Sessions Blocked'),
                    name: 'blockedCount',
                    value: this.statFormat(this.nodeStats.blockedCount)
                }]
            },{
                title: i18n._('Application Statistics'),
                defaults: {
                    xtype: "displayfield",
                    labelWidth: 200
                },
                items: [{
                    fieldLabel: i18n._('Known Applications'),
                    name: 'protoTotalCount',
                    value: this.statFormat(this.nodeStats.protoTotalCount)
                },{
                    fieldLabel: i18n._('Flagged Applications'),
                    name: 'protoFlagCount',
                    value: this.statFormat(this.nodeStats.protoFlagCount)
                },{
                    fieldLabel: i18n._('Blocked Applications'),
                    name: 'protoBlockCount',
                    value: this.statFormat(this.nodeStats.protoBlockCount)
                },{
                    fieldLabel: i18n._('Tarpitted Applications'),
                    name: 'protoTarpitCount',
                    value: this.statFormat(this.nodeStats.protoTarpitCount)
                }]
            },{
                title: i18n._('Rule Statistics'),
                defaults: {
                    xtype: "displayfield",
                    labelWidth: 200
                },
                items: [{
                    fieldLabel: i18n._('Total Rules'),
                    name: 'logicTotalCount',
                    value: this.statFormat(this.nodeStats.logicTotalCount)
                },{
                    fieldLabel: i18n._('Active Rules'),
                    name: 'logicLiveCount',
                    value: this.statFormat(this.nodeStats.logicLiveCount)
                }]
            }]
        });
    },

    buildGridProtoRules: function() {
        this.gridProtoRules = Ext.create('Ung.grid.Panel',{
            settingsCmp: this,
            name: 'gridProtoRules',
            helpSource: 'application_control_applications',
            hasEdit: false,
            hasDelete: false,
            hasAdd: false,
            title: i18n._("Applications"),
            qtip: i18n._("The list of known Applications."),
            dataProperty: "protoRules",
            recordJavaClass: "com.untangle.node.application_control.ApplicationControlProtoRule",
            sortField: "guid",
            fields: [{
                name: 'id'
            },{
                name: 'guid'
            },{
                name: 'block'
            },{
                name: 'tarpit'
            },{
                name: 'flag'
            },{
                name: 'name'
            },{
                name: 'category'
            },{
                name: 'productivity'
            },{
                name: 'risk'
            },{
                name: 'description'
            }],
            columns: [{
                header: i18n._("Application"),
                width: 120,
                dataIndex: "guid"
            }, {
                xtype:'checkcolumn',
                header: "<b>"+i18n._("Block")+"</b>",
                dataIndex: "block",
                name: "block",
                width: 50,
                resizable: false,
                listeners: {
                    checkchange: Ext.bind(function(elem, rowIndex, checked) {
                        if(checked) {
                            var record = elem.getView().getRecord(elem.getView().getRow(rowIndex));
                            record.set('tarpit', false);
                        }
                    }, this)
                },
                checkAll: {
                    handler: function(checkbox, checked) {
                        var records=checkbox.up("grid").getStore().getRange();
                        for(var i=0; i<records.length; i++) {
                            records[i].set('block', checked);
                            if(checked) {
                                records[i].set('tarpit', false);
                            }
                        }
                    }
                }
            }, {
                xtype:'checkcolumn',
                header: "<b>"+i18n._("Tarpit")+"</b>",
                dataIndex: "tarpit",
                name: "tarpit",
                width: 50,
                resizable: false,
                listeners: {
                    checkchange: Ext.bind(function(elem, rowIndex, checked) {
                        if(checked) {
                            var record = elem.getView().getRecord(elem.getView().getRow(rowIndex));
                            record.set('block', false);
                        }
                    }, this)
                },
                checkAll: {
                    handler: function(checkbox, checked) {
                        var records=checkbox.up("grid").getStore().getRange();
                        for(var i=0; i<records.length; i++) {
                            records[i].set('tarpit', checked);
                            if(checked) {
                                records[i].set('block', false);
                            }
                        }
                    }
                }
            }, {
                xtype:'checkcolumn',
                header: "<b>"+i18n._("Flag")+"</b>",
                dataIndex: "flag",
                name: "flag",
                width: 50,
                resizable: false,
                checkAll: {}
            }, {
                header: i18n._("Name"),
                width: 150,
                dataIndex: "name"
            }, {
                header: i18n._("Category"),
                width: 150,
                dataIndex: "category"
            }, {
                header: i18n._("Productivity"),
                width: 80,
                dataIndex: "productivity"
            }, {
                header: i18n._("Risk"),
                width: 80,
                dataIndex: "risk"
            }, {
                header: i18n._("Description (click for full text)"),
                width: 300,
                dataIndex: "description",
                flex:1
            }]
        });

        this.gridProtoRules.addListener('cellclick', function(grid, element, columnIndex, dataRecord) {
            if (columnIndex == 8) Ext.Msg.alert(dataRecord.data.name,dataRecord.data.description);
        }, this.gridProtoRules);
    },

    buildGridLogicRules: function() {
        this.gridLogicRules = Ext.create('Ung.grid.Panel',{
            name: "gridLogicRules",
            helpSource: 'application_control_rules',
            settingsCmp: this,
            height: 500,
            hasReorder: true,
            title: i18n._("Rules"),
            qtip: i18n._("Application Control rules are used to control traffic post-classification."),
            dataProperty: "logicRules",
            useServerIds: true,
            recordJavaClass: "com.untangle.node.application_control.ApplicationControlLogicRule",
            emptyRow: {
                "description": "",
                "action": ""
            },
            fields: [{
                name: 'live'
            },{
                name: "id"
            },{
                name: "description"
            },{
                name: "action"
            },{
                name: "conditions"
            }],
            columns:[{
                xtype:'checkcolumn',
                width:55,
                header: i18n._("Enabled"),
                dataIndex: 'live',
                resizable: false
            }, {
                header: i18n._("Rule ID"),
                dataIndex: 'id',
                width: 50,
                renderer: function(value) {
                    if (value < 0) {
                        return i18n._("new");
                    } else {
                        return value;
                    }
                }
            }, {
                header: i18n._("Description"),
                dataIndex:'description',
                flex:1,
                width: 200
            }, {
                header: i18n._("Action"),
                dataIndex:'action',
                width: 150,
                renderer: Ext.bind(function(value) {
                    switch(value.actionType) {
                        case 'ALLOW': return i18n._("Allow");
                        case 'BLOCK': return i18n._("Block");
                        case 'TARPIT': return i18n._("Tarpit");
                        default: return "Unknown Action: " + value;
                    }
                }, this)
            }],
            rowEditorInputLines: [{
                xtype: "checkbox",
                name: "Enabled",
                dataIndex: "live",
                fieldLabel: i18n._( "Enabled" ),
                width: 360
            }, {
                xtype: "textfield",
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._( "Description" ),
                emptyText: i18n._("[no description]"),
                width: 480
            }, {
                xtype: "fieldset",
                autoScroll: true,
                title: "If all of the following conditions are met:",
                items:[{
                    xtype: 'rulebuilder',
                    settingsCmp: this,
                    javaClass: "com.untangle.node.application_control.ApplicationControlLogicRuleCondition",
                    dataIndex: "conditions",
                    conditions: this.getConditions()
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('Perform the following action:'),
                items:[{
                    xtype: "container",
                    name: "Action",
                    dataIndex: "action",
                    fieldLabel: i18n._("Action"),
                    items: [{
                        xtype: "combo",
                        name: "actionType",
                        allowBlank: false,
                        fieldLabel: i18n._("Action"),
                        editable: false,
                        store: [['ALLOW', i18n._('Allow')],
                                ['BLOCK', i18n._('Block')],
                                ['TARPIT', i18n._('Tarpit')]],
                        valueField: "value",
                        displayField: "displayName",
                        queryMode: "local"
                    }],
                    setValue: function(value) {
                        var actionType  = this.down('combo[name="actionType"]');
                        actionType.setValue(value.actionType);
                    },
                    getValue: function() {
                        var actionType  = this.down('combo[name="actionType"]').getValue();
                        var action = {
                            javaClass: "com.untangle.node.application_control.ApplicationControlLogicRuleAction",
                            actionType: actionType,
                            //must override toString in order for all objects not to appear the same
                            toString: function() {
                                return Ext.encode(this);
                            }
                        };
                        return action;
                    },
                    isValid: function () {
                        var actionType  = this.down('combo[name="actionType"]');
                        var isValid = actionType.isValid();
                        this.activeErrors=actionType.activeErrors;
                        return isValid;
                    }
                }]
            }]
        });
    },
    beforeSave: function(isApply, handler) {
        this.settings.protoRules.list=this.gridProtoRules.getList();
        this.settings.logicRules.list=this.gridLogicRules.getList();
        handler.call(this, isApply);
    }
});
//# sourceURL=application-control-settings.js