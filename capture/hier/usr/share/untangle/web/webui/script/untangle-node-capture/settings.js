//TODO: remove getLogicRules and getProtoRules from api since these lists are taken form settings directly now
// Application Control settings
if (!Ung.hasResource["Ung.ClassD"])
{
    Ung.hasResource["Ung.ClassD"] = true;
    Ung.NodeWin.registerClassName('untangle-node-classd', 'Ung.ClassD');

    // ClassD Utilities
    Ung.ClassDUtil={
        getMatchers: function (settingsCmp) {
            return [
                {name:"DST_ADDR",displayName: settingsCmp.i18n._("Destination Address"), type: "text", visible: true, vtype:"ipAddress"},
                {name:"DST_PORT",displayName: settingsCmp.i18n._("Destination Port"), type: "text",vtype:"port", visible: true},
                {name:"DST_INTF",displayName: settingsCmp.i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
                {name:"SRC_ADDR",displayName: settingsCmp.i18n._("Source Address"), type: "text", visible: true, vtype:"ipAddress"},
                {name:"SRC_PORT",displayName: settingsCmp.i18n._("Source Port"), type: "text",vtype:"port", visible: true},
                {name:"SRC_INTF",displayName: settingsCmp.i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
                {name:"PROTOCOL",displayName: settingsCmp.i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"]], visible: true},
                {name:"HTTP_HOST",displayName: settingsCmp.i18n._("HTTP: Hostname"), type: "text", visible: true},
                {name:"HTTP_URI",displayName: settingsCmp.i18n._("HTTP: URI"), type: "text", visible: true},
                {name:"CLASSD_APPLICATION",displayName: settingsCmp.i18n._("Application Control: Application"), type: "text", visible: true},
                {name:"CLASSD_CATEGORY",displayName: settingsCmp.i18n._("Application Control: Category"), type: "text", visible: true},
                {name:"CLASSD_PROTOCHAIN",displayName: settingsCmp.i18n._("Application Control: ProtoChain"), type: "text", visible: true},
                {name:"CLASSD_DETAIL",displayName: settingsCmp.i18n._("Application Control: Detail"), type: "text", visible: true},
                {name:"CLASSD_CONFIDENCE_GREATER_THAN",displayName: settingsCmp.i18n._("Application Control: Confidence >"), type: "text", visible: true},
                {name:"CLASSD_CONFIDENCE_LESS_THAN",displayName: settingsCmp.i18n._("Application Control: Confidence <"), type: "text", visible: true},
                {name:"CLASSD_PRODUCTIVITY_GREATER_THAN",displayName: settingsCmp.i18n._("Application Control: Productivity >"), type: "text", visible: true},
                {name:"CLASSD_PRODUCTIVITY_LESS_THAN",displayName: settingsCmp.i18n._("Application Control: Productivity <"), type: "text", visible: true},
                {name:"CLASSD_RISK_GREATER_THAN",displayName: settingsCmp.i18n._("Application Control: Risk >"), type: "text", visible: true},
                {name:"CLASSD_RISK_LESS_THAN",displayName: settingsCmp.i18n._("Application Control: Risk <"), type: "text", visible: true},
                {name:"PROTOCOL_CONTROL_SIGNATURE",displayName: settingsCmp.i18n._("Application Control Lite: Signature"), type: "text", visible: true},
                {name:"PROTOCOL_CONTROL_CATEGORY",displayName: settingsCmp.i18n._("Application Control Lite: Category"), type: "text", visible: true},
                {name:"PROTOCOL_CONTROL_DESCRIPTION",displayName: settingsCmp.i18n._("Application Control Lite: Description"), type: "text", visible: true},
                {name:"SITEFILTER_CATEGORY",displayName: settingsCmp.i18n._("Web Filter: Category"), type: "text", visible: true},
                {name:"SITEFILTER_CATEGORY_DESCRIPTION",displayName: settingsCmp.i18n._("Web Filter: Category Description"), type: "text", visible: true},
                {name:"SITEFILTER_FLAGGED",displayName: settingsCmp.i18n._("Web Filter: Site is Flagged"), type: "boolean", visible: true},
                {name:"DIRECTORY_CONNECTOR_USERNAME",displayName: settingsCmp.i18n._("Directory Connector: Username"), type: "editor", editor: Ext.create('Ung.UserEditorWindow',{}), visible: true},
                {name:"DIRECTORY_CONNECTOR_GROUP",displayName: settingsCmp.i18n._("Directory Connector: User in Group"), type: "editor", editor: Ext.create('Ung.GroupEditorWindow',{}), visible: true}
            ];
        }
    };

   Ext.define('Ung.ClassD', {
        extend: 'Ung.NodeWin',
        nodeStats: null,
        gridProtoRules: null,
        gridLogicRules: null,
        panelStatus: null,
        gridEventLog: null,
        gridRuleEventLog: null,
        initComponent: function() {
            this.nodeStats = this.getRpcNode().getStatistics();

            this.actionMap = [[this.i18n._('Allow'),'ALLOW'],
                              [this.i18n._('Block'),'BLOCK'],
                              [this.i18n._('Tarpit'),'TARPIT']];

            this.actionStore = Ext.create('Ext.data.ArrayStore',{
                idIndex: 0,
                fields: ['displayName', 'value'],
                data: this.actionMap
            });

            this.buildStatus();
            this.buildGridProtoRules();
            this.buildGridLogicRules();
            this.buildEventLog();
            this.buildRuleEventLog();

            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelStatus, this.gridProtoRules, this.gridLogicRules, this.gridEventLog, this.gridRuleEventLog]);

            this.callParent(arguments);
        },

        stat_format: function(input) {
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
            this.panelStatus = Ext.create('Ext.panel.Panel',{
                name: 'Status',
                helpSource: 'status',
                parentId: this.getId(),

                title: this.i18n._('Status'),
               // layout: "form",
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    xtype: 'fieldset',
                    buttonAlign: 'left'
                },
                items: [{
                    title: this.i18n._('Note'),
                    cls: 'description',
                    html: this.i18n._("Application Control detects many different types of network traffic, allowing each to be flagged and/or blocked.")
                },{
                    title:'<b>'+ this.i18n._('Traffic Statistics') + '</b>',
                    labelWidth: 230,
                    defaults: {
                        xtype: "textfield",
                        readOnly: true,
                        style: "color:#000000;"
                    },
                    items: [{
                        fieldLabel: this.i18n._('Sessions Scanned'),
                        labelWidth:200,
                        labelAlign:'left',
                        name: 'packetCount',
                        value: this.stat_format(this.nodeStats.sessionCount)
                    },{
                        fieldLabel: this.i18n._('Sessions Allowed'),
                        labelWidth:200,
                        labelAlign:'left',
                        name: 'allowedCount',
                        value: this.stat_format(this.nodeStats.allowedCount)
                    },{
                        fieldLabel: this.i18n._('Sessions Flagged'),
                        labelWidth:200,
                        labelAlign:'left',
                        name: 'flaggedCount',
                        value: this.stat_format(this.nodeStats.flaggedCount)
                    },{
                        fieldLabel: this.i18n._('Sessions Blocked'),
                        labelWidth:200,
                        labelAlign:'left',
                        name: 'blockedCount',
                        value: this.stat_format(this.nodeStats.blockedCount)
                    }]
                },{
                    title: '<b>' + this.i18n._('Application Statistics') +'</b>',
                    labelWidth: 230,
                    defaults: {
                        xtype: "textfield",
                        readOnly: true,
                        style: "color:#000000;"
                    },
                    items: [{
                        fieldLabel: this.i18n._('Known Applications'),
                        labelWidth:200,
                        labelAlign:'left',
                        name: 'protoTotalCount',
                        value: this.stat_format(this.nodeStats.protoTotalCount)
                    },{
                        fieldLabel: this.i18n._('Flagged Applications'),
                        labelWidth:200,
                        labelAlign:'left',
                        name: 'protoFlagCount',
                        value: this.stat_format(this.nodeStats.protoFlagCount)
                    },{
                        fieldLabel: this.i18n._('Blocked Applications'),
                        labelWidth:200,
                        labelAlign:'left',
                        name: 'protoBlockCount',
                        value: this.stat_format(this.nodeStats.protoBlockCount)
                    },{
                        fieldLabel: this.i18n._('Tarpitted Applications'),
                        labelWidth:200,
                        labelAlign:'left',
                        name: 'protoTarpitCount',
                        value: this.stat_format(this.nodeStats.protoTarpitCount)
                    }]
                },{
                    title: '<b>' + this.i18n._('Rule Statistics') + '</b>',
                    labelWidth: 230,
                    defaults: {
                        xtype: "textfield",
                        readOnly: true,
                        style: "color:#000000;"
                    },
                    items: [{
                        fieldLabel: this.i18n._('Total Rules'),
                        labelWidth:200,
                        labelAlign:'left',
                        name: 'logicTotalCount',
                        value: this.stat_format(this.nodeStats.logicTotalCount)
                    },{
                        fieldLabel: this.i18n._('Active Rules'),
                        labelWidth:200,
                        labelAlign:'left',
                        name: 'logicLiveCount',
                        value: this.stat_format(this.nodeStats.logicLiveCount)
                    }]
                }]
            });
        },

        buildGridProtoRules: function() {
            this.gridProtoRules = Ext.create('Ung.EditorGrid',{
                loadMask: {msg: i18n._("Loading ...")},
                settingsCmp: this,
                id:'gridRules',
                name: 'gridProtoRules',
                helpSource: 'proto_rules',
                hasEdit: false,
                hasDelete: false,
                hasAdd: false,
                hasReorder: false,
                autoScroll: true,
                columnsDefaultSortable: true,
                title: this.i18n._("Applications"),
                qtip: this.i18n._("The list of known Applications."),
                paginated: false,
                recordJavaClass: "com.untangle.node.classd.ClassDProtoRule",
                dataProperty: "protoRules",
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
                    header: this.i18n._("Application"),
                    width: 120,
                    dataIndex: "guid"
                },
                {
                    xtype:'checkcolumn',
                    header: "<b>Block</b>",
                    dataIndex: "block",
                    name: "block",
                    width: 50,
                    fixed: true,
                    listeners:
                    {
                        checkchange: function(col,idx,chk)
                        {
                            if (chk) {
                                var store = Ext.getCmp('gridRules').getStore();
                                var record = store.getAt(idx);
                                record.set('tarpit',false);
                            }
                        }
                    }
                },
                {
                    xtype:'checkcolumn',
                    header: "<b>Tarpit</b>",
                    dataIndex: "tarpit",
                    name: "tarpit",
                    width: 50,
                    fixed: true,
                    listeners:
                    {
                        checkchange: function(col,idx,chk)
                        {
                            if ( chk) {
                                var store = Ext.getCmp('gridRules').getStore();
                                var record = store.getAt(idx);
                                record.set('block',false);
                            }
                        }
                    }
                },
                {
                    xtype:'checkcolumn',
                    header: "<b>Flag</b>",
                    dataIndex: "flag",
                    name: "flag",
                    width: 50,
                    fixed: true
                },
                {
                    header: this.i18n._("Name"),
                    width: 150,
                    dataIndex: "name"
                },
                {
                    header: this.i18n._("Category"),
                    width: 150,
                    dataIndex: "category"
                },
                {
                    header: this.i18n._("Productivity"),
                    width: 80,
                    dataIndex: "productivity"
                },
                {
                    header: this.i18n._("Risk"),
                    width: 80,
                    dataIndex: "risk"
                },
                {
                    header: this.i18n._("Description (click for full text)"),
                    width: 300,
                    dataIndex: "description",
                    flex:1
                }],
                sortField: "guid",
                columnsDefaultSortable: true
            });

            this.gridProtoRules.addListener('cellclick', function(grid, element, columnIndex, dataRecord)
            {
                if (columnIndex == 8) Ext.Msg.alert(dataRecord.data.name,dataRecord.data.description);
            }, this.gridProtoRules);
        },

        actionRenderer: function(value) {
            switch(value.actionType) {
              case 'ALLOW': return i18n._("Allow");
              case 'BLOCK': return i18n._("Block");
              case 'TARPIT': return i18n._("Tarpit");
            default: return "Unknown Action: " + value;
            }
        },

        buildGridLogicRules: function() {

            var rowEditorInputLines = [{
                xtype: "checkbox",
                name: "Enabled",
                dataIndex: "live",
                fieldLabel: this.i18n._( "Enabled" ),
                width: 360
            }, {
                xtype: "textfield",
                name: "Description",
                dataIndex: "description",
                fieldLabel: this.i18n._( "Description" ),
                width: 480
            }, {
                xtype: "fieldset",
                autoWidth: true,
                autoScroll: true,
                title: "If all of the following conditions are met:",
                items:[{
                    xtype:"rulebuilder",
                    settingsCmp: this,
                    javaClass: "com.untangle.node.classd.ClassDLogicRuleMatcher",
                    anchor:"98%",
                    width: 900,
                    dataIndex: "matchers",
                    matchers: Ung.ClassDUtil.getMatchers(this),
                    id:'builder'
                }]
            }, {
                xtype: 'fieldset',
                cls:'description',
                title: i18n._('Perform the following action:'),
                border: false
            }, {
                xtype: "form",
                name: "Action",
                dataIndex: "action",
                fieldLabel: this.i18n._( "Action" ),
                width: 360,
                border: false,
                items: [{
                    xtype: "combo",
                    name: "actionType",
                    allowBlank: false,
                    fieldLabel: "Action",
                    editable: false,
                    store: this.actionStore,
                    valueField: "value",
                    displayField: "displayName",
                    queryMode: "local",
                    triggerAction: 'all',
                    listClass: 'x-combo-list-small',
                    listeners: {
                        select: Ext.bind(function(combo, ewVal, oldVal) {
                           // var form=this.gridLogicRules.rowEditor.query('form[name="Action"]')[0];
                        }, this )
                    }
                    }],
                setValue: function(value) {
                    var actionType  = this.query('combo[name="actionType"]')[0];

                    actionType.setValue(value.actionType);
                },
                getValue: function() {
                    var actionType  = this.query('combo[name="actionType"]')[0].getValue();

                    var jsonobj = {
                        javaClass: "com.untangle.node.classd.ClassDLogicRuleAction",
                        actionType: actionType,
                        //must override toString in order for all objects not to appear the same
                        toString: function() {
                            return Ext.encode(this);
                        }
                    };

                    return jsonobj;
                }
            }];

            this.gridLogicRules = Ext.create('Ung.EditorGrid',{
                name: "gridLogicRules",
                helpSource: 'logic_rules',
                settingsCmp: this,
                height: 300,
                emptyRow: {
                    "description": "",
                    "action": ""
                },
                height: 500,
                hasEdit: true,
                configEdit: {width:50,fixed:false,tooltip:this.i18n._("Edit")},
                hasDelete: true,
                configDelete: {width:50,fixed:false,tooltip:this.i18n._("Delete")},
                hasReorder: true,
                columnsDefaultSortable: false,
                title: this.i18n._("Rules"),
                qtip: this.i18n._("Application Control rules are used to control traffic post-classification."),
                paginated: false,
                recordJavaClass: "com.untangle.node.classd.ClassDLogicRule",
                dataProperty: "logicRules",
                fields: [{
                    name: 'live'
                },{
                    name: "id"
                },{
                    name: "description"
                },{
                    name: "action"
                },{
                    name: "matchers"
                }],
                columns:[
                {
                    xtype:'checkcolumn',
                    width:55,
                    header: this.i18n._("Enabled"),
                    dataIndex: 'live',
                    fixed: true
                },
                {
                    header: this.i18n._("Rule ID"),
                    dataIndex: 'internalId',
                    width: 50,
                    renderer: function(value) {
                        if (value < 0) {
                            return i18n._("new");
                        } else {
                            return value;
                        }
                    }
                }, {
                    header: this.i18n._("Description"),
                    dataIndex:'description',
                    flex:1,
                    width: 200
                }, {
                    header: this.i18n._("Action"),
                    dataIndex:'action',
                    width: 150,
                    renderer: this.actionRenderer
                }],

                initComponent: function() {
                    this.rowEditor = Ext.create('Ung.RowEditorWindow',{
                        grid: this,
                        sizeToComponent: this.settingsCmp,
                        inputLines: rowEditorInputLines,
                        rowEditorLabelWidth: 100,
                        populate: function(record, addMode) {
                            return this.populateTree(record, addMode);
                        },
                        // updateAction is called to update the record after the edit
                        updateAction: function() {
                            return this.updateActionTree();
                        },
                        isDirty: function() {
                            if (this.record !== null) {
                                if (this.inputLines) {
                                    for (var i = 0; i < this.inputLines.length; i++) {
                                        var inputLine = this.inputLines[i];
                                        if(inputLine.dataIndex!=null) {
                                            var recordData = this.record.get(inputLine.dataIndex);
                                            var inputLineData = inputLine.getValue();
                                            if ( recordData != inputLineData) {
                                                if ( typeof(inputLineData) == 'object') {
                                                    if (recordData.actionType != inputLineData.actionType) {
                                                        return true;
                                                    }
                                                } else {
                                                    return true;
                                                }
                                            }
                                        }
                                        /* for fieldsets */
                                        if(inputLine.items !=null && inputLine.items.dataIndex != null) {
                                            if (this.record.get(inputLine.items.dataIndex) != inputLine.items.getValue()) {
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                            return Ext.getCmp('builder').isDirty();
                        },
                        isFormValid: function() {
                            for (var i = 0; i < this.inputLines.length; i++) {
                                var item = null;
                                if ( this.inputLines.get != null ) {
                                    item = this.inputLines.get(i);
                                } else {
                                    item = this.inputLines[i];
                                }
                                if ( item == null ) {
                                    continue;
                                }

                                if ( item.isValid != null) {
                                    if(!item.isValid()) {
                                        return false;
                                    }
                                } else if(item.items !=null && item.items.getCount()>0) {
                                    /* for fieldsets */
                                    for (var j = 0; j < item.items.getCount(); j++) {
                                        var subitem=item.items.get(j);
                                        if ( subitem == null ) {
                                            continue;
                                        }

                                        if ( subitem.isValid != null && !subitem.isValid()) {
                                            return false;
                                        }
                                    }
                                }

                            }
                            return true;
                        }
                    });
                    Ung.EditorGrid.prototype.initComponent.call(this);
                }
            });
        },

        // Event Log
        buildEventLog: function() {
            this.gridEventLog = Ext.create('Ung.GridEventLog',{
                settingsCmp: this,
                title: this.i18n._("Event Log"),
                fields: [{
                    name: 'time_stamp',
                    sortType: Ung.SortTypes.asTimestamp
                }, {
                    name: 'uid'
                }, {
                    name: 'client_addr',
                    mapping: 'c_client_addr'
                }, {
                    name: 'client_port',
                    mapping: 'c_client_port'
                }, {
                    name: 'server_addr',
                    mapping: 'c_server_addr'
                }, {
                    name: 'server_port',
                    mapping: 'c_server_port'
                }, {
                    name: 'application',
                    mapping: 'classd_application',
                    type: 'string'
                }, {
                    name: 'protochain',
                    mapping: 'classd_protochain',
                    type: 'string'
                }, {
                    name: 'flagged',
                    mapping: 'classd_flagged',
                    type: 'boolean'
                }, {
                    name: 'blocked',
                    mapping: 'classd_blocked',
                    type: 'boolean'
                }, {
                    name: 'confidence',
                    mapping: 'classd_confidence'
                }, {
                    name: 'detail',
                    mapping: 'classd_detail'
                }],
                columns: [{
                    header: this.i18n._("Timestamp"),
                    width: Ung.Util.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) { return i18n.timestampFormat(value); }
                }, {
                    header: this.i18n._("Username"),
                    width: Ung.Util.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'uid'
                }, {
                    header: this.i18n._("Client IP"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'client_addr'
                }, {
                    header: this.i18n._("Client Port"),
                    width: Ung.Util.portFieldWidth,
                    dataIndex: 'client_port'
                }, {
                    header: this.i18n._("Server IP"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'server_addr'
                }, {
                    header: this.i18n._("Server Port"),
                    width: Ung.Util.portFieldWidth,
                    sortable: true,
                    dataIndex: 'server_port'
                }, {
                    header: this.i18n._("Application"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'application'
                }, {
                    header: this.i18n._("ProtoChain"),
                    width: 180,
                    sortable: true,
                    dataIndex: 'protochain'
                }, {
                    header: this.i18n._("Blocked"),
                    width: Ung.Util.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'blocked'
                }, {
                    header: this.i18n._("Flagged"),
                    width: Ung.Util.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'flagged'
                }, {
                    header: this.i18n._("Confidence"),
                    width: Ung.Util.portFieldWidth,
                    sortable: true,
                    dataIndex: 'confidence'
                }, {
                    id: 'detail',
                    header: this.i18n._("Detail"),
                    width: 200,
                    sortable: true,
                    flex:1,
                    dataIndex: 'detail'
                }]
            });
        },

        // Event Log
        buildRuleEventLog: function() {
            this.gridRuleEventLog = Ext.create('Ung.GridEventLog',{
                settingsCmp: this,
                title: this.i18n._("Rule Event Log"),
                eventQueriesFn: this.getRpcNode().getRuleEventQueries,
                fields: [{
                    name: 'time_stamp',
                    sortType: Ung.SortTypes.asTimestamp
                }, {
                    name: 'uid'
                }, {
                    name: 'client_addr',
                    mapping: 'c_client_addr'
                }, {
                    name: 'client_port',
                    mapping: 'c_client_port'
                }, {
                    name: 'server_addr',
                    mapping: 'c_server_addr'
                }, {
                    name: 'server_port',
                    mapping: 'c_server_port'
                }, {
                    name: 'application',
                    mapping: 'classd_application',
                    type: 'string'
                }, {
                    name: 'protochain',
                    mapping: 'classd_protochain',
                    type: 'string'
                }, {
                    name: 'flagged',
                    mapping: 'classd_flagged',
                    type: 'boolean'
                }, {
                    name: 'blocked',
                    mapping: 'classd_blocked',
                    type: 'boolean'
                }, {
                    name: 'confidence',
                    mapping: 'classd_confidence'
                }, {
                    name: 'detail',
                    mapping: 'classd_detail'
                }, {
                    name: 'ruleid',
                    mapping: 'classd_ruleid'

                }],
                columns: [{
                    header: this.i18n._("Timestamp"),
                    width: Ung.Util.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) { return i18n.timestampFormat(value); }
                }, {
                    header: this.i18n._("Username"),
                    width: Ung.Util.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'uid'
                }, {
                    header: this.i18n._("Client IP"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'client_addr'
                }, {
                    header: this.i18n._("Client Port"),
                    width: Ung.Util.portFieldWidth,
                    dataIndex: 'client_port'
                }, {
                    header: this.i18n._("Server IP"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'server_addr'
                }, {
                    header: this.i18n._("Server Port"),
                    width: Ung.Util.portFieldWidth,
                    sortable: true,
                    dataIndex: 'server_port'
                }, {
                    header: this.i18n._("Rule ID"),
                    width: 70,
                    sortable: true,
                    dataIndex: 'ruleid'
                }, {
                    header: this.i18n._("Application"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'application'
                }, {
                    header: this.i18n._("ProtoChain"),
                    width: 180,
                    sortable: true,
                    dataIndex: 'protochain'
                }, {
                    header: this.i18n._("Blocked"),
                    width: Ung.Util.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'blocked'
                }, {
                    header: this.i18n._("Flagged"),
                    width: Ung.Util.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'flagged'
                }, {
                    header: this.i18n._("Confidence"),
                    width: Ung.Util.portFieldWidth,
                    sortable: true,
                    dataIndex: 'confidence'
                }, {
                    header: this.i18n._("Detail"),
                    width: 200,
                    sortable: true,
                    flex:1,
                    dataIndex: 'detail'
                }]
            });
        },
        beforeSave: function(isApply, handler) {
            this.settings.protoRules.list=this.gridProtoRules.getPageList();
            this.settings.logicRules.list=this.gridLogicRules.getPageList();
            handler.call(this, isApply);
        }
    });
}
//@ sourceURL=classd-settings.js
