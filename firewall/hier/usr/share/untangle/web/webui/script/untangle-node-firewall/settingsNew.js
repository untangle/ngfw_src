if (!Ung.hasResource["Ung.Firewall"]) {
    Ung.hasResource["Ung.Firewall"] = true;
    Ung.NodeWin.registerClassName('untangle-node-firewall', 'Ung.Firewall');

    Ung.FirewallUtil={
        getMatchers : function (settingsCmp) {
            return [
                {name:"DST_ADDR",displayName: settingsCmp.i18n._("Destination Address"), type: "text", visible: true, vtype:"ipAddress"},
                {name:"DST_PORT",displayName: settingsCmp.i18n._("Destination Port"), type: "text",vtype:"port", visible: true},
                {name:"DST_INTF",displayName: settingsCmp.i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
                {name:"SRC_ADDR",displayName: settingsCmp.i18n._("Source Address"), type: "text", visible: true, vtype:"ipAddress"},
                {name:"SRC_PORT",displayName: settingsCmp.i18n._("Source Port"), type: "text",vtype:"port", visible: false},
                {name:"SRC_INTF",displayName: settingsCmp.i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
                {name:"PROTOCOL",displayName: settingsCmp.i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["TCP,UDP","TCP,UDP"],["any","any"]], visible: true},
                {name:"DIRECTORY_CONNECTOR_USERNAME",displayName: settingsCmp.i18n._("Directory Connector: Username"), type: "text", visible: true},
                {name:"DIRECTORY_CONNECTOR_GROUP",displayName: settingsCmp.i18n._("Directory Connector: User in Group"), type: "text", visible: true}
            ];
        }
    };

    Ext.define('Ung.Firewall', {
		extend:'Ung.NodeWin',
        panelRules: null,
        gridRules : null,
        gridEventLog : null,
        initComponent : function() {
            //Ung.Util.clearInterfaceStore();
            this.getSettings();
         
            // builds the tabs
            this.buildRules();
            this.buildEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelRules, this.gridEventLog]);
            
            Ung.Firewall.superclass.initComponent.call(this);
        },
        // Rules Panel
        buildRules : function() {

            

            this.panelRules = Ext.create('Ext.panel.Panel',{
                name : 'panelRules',
                helpSource : 'rules',
                // private fields
                gridRulesList : null,
                parentId : this.getId(),
                title : this.i18n._('Rules'),
                layout : 'anchor',
                defaults: {
                    anchor: '98%',
                    autoWidth: true,
                    autoScroll: true
                },
                autoScroll : true,
                border : false,
                cls: 'ung-panel',
                items : [{
                    title : this.i18n._('Note'),
                    cls: 'description',
                    bodyStyle : 'padding:5px 5px 5px; 5px;',
                    html : Ext.String.format(this.i18n._(" <b>Firewall</b> is a simple application designed to block and log network traffic based on a set of rules. To learn more click on the <b>Help</b> button below.<br/> Routing and Port Forwarding functionality can be found elsewhere in Config->Networking."),main.getBrandingManager().getCompanyName())
                },  this.gridRules= Ext.create('Ung.EditorGrid',{
                    name : 'Rules',
                    settingsCmp : this,
                    height : 500,
                    paginated : false,
                    hasReorder : true,
                    addAtTop : false,
                    emptyRow : {
                        "id" : 0,
                        "enabled" : true,
                        "block" : false,
                        "log" : true,
                        "description" : this.i18n._("[no description]"),
                        "javaClass" : "com.untangle.node.firewall.FirewallRule"
                    },
                    title : this.i18n._("Rules"),
                    recordJavaClass : "com.untangle.node.firewall.FirewallRule",
                    dataProperty:'rules',
                    fields : [{
                        name : 'id'
                    }, {
                        name : 'enabled'
                    }, {
                        name : 'block'
                    }, {
                        name : 'log'
                    }, {
                        name : 'matchers'
                    },{
                        name : 'description'
                    }, {
                        name : 'javaClass'
                    }],
                    columns : [{
								header : this.i18n._("Rule Id"),
								width : 50,
								dataIndex : 'id'
							}, 
							{
								xtype:'checkcolumn',
								header : this.i18n._("Enable"),
								dataIndex : 'enabled',
								fixed : true,
								width:55
							},
							{
								header : this.i18n._("Description"),
								width : 200,
								dataIndex : 'description',
								flex:1
							},
							{
								xtype:'checkcolumn',
								header : this.i18n._("Block"),
								dataIndex : 'block',
								fixed : true,
								width:55
							},
							{
								xtype:'checkcolumn',
								header : this.i18n._("Log"),
								dataIndex : 'log',
								fixed : true,
								width:55
							}],
                    columnsDefaultSortable : false,

                    initComponent : function() {
                        this.rowEditor = Ext.create('Ung.RowEditorWindow',{
                            grid : this,
                            sizeToComponent : this.settingsCmp,
                            inputLines : this.rowEditorInputLines,
                            rowEditorLabelWidth : 100,
                            populate : function(record, addMode) {
                                return this.populateTree(record, addMode);
                            },
                            // updateAction is called to update the record after the edit
                            updateAction : function() {
                                return this.updateActionTree();
                            },
                            isDirty : function() {
                                if (this.record !== null) {
                                    if (this.inputLines) {
                                        for (var i = 0; i < this.inputLines.length; i++) {
                                            var inputLine = this.inputLines[i];
                                            if(inputLine.dataIndex!=null) {
                                                if (this.record.get(inputLine.dataIndex) != inputLine.getValue()) {
                                                    return true;
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
                            isFormValid : function() {
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
                    },

                    rowEditorInputLines : [
						{
							xtype:'checkbox',
                            name : "Enable Rule",
                            dataIndex: "enabled",
                            fieldLabel : this.i18n._("Enable Rule"),
                            itemCls:'firewall-spacing-1'
                        }
						,
						{
							xtype:'textfield',
                            name : "Description",
                            dataIndex: "description",
                            fieldLabel : this.i18n._("Description"),
                            itemCls:'firewall-spacing-1',
                            width : 500
                        },
						{
							xtype:'fieldset',
                            title : this.i18n._("Rule") ,
                            cls:'firewall-spacing-2',
                            autoHeight : true,
                            title: "If all of the following conditions are met:",
                            items:[{
                                xtype:'rulebuilder',
                                settingsCmp: this,
                                javaClass: "com.untangle.node.firewall.FirewallRuleMatcher",
                                anchor:"98%",
                                width: 900,
                                dataIndex: "matchers",
                                matchers : Ung.FirewallUtil.getMatchers(this),
                                id:'builder'
                            }]
                        },
						{
                            xtype : 'fieldset',
                            autoHeight: true,
                            cls:'description',
                            title : i18n._('Perform the following action(s):'),
                            border: false
                        }, 
						{
                            xtype: "combo",
                            name: "actionType",
                            allowBlank: false,
                            dataIndex: "block",
                            fieldLabel: this.i18n._("Action Type"),
                            editable : false,
                            store: [[true,i18n._('Block')], [false,i18n._('Pass')]],
                            valueField: "value",
                            displayField: "displayName",
                            mode: "local",
                            triggerAction : 'all',
                            listClass : 'x-combo-list-small'
                        }, 
						{
							xtype:'checkbox',
                            name : "Log",
                            dataIndex: "log",
                            itemCls:'firewall-spacing-1',
                            fieldLabel : this.i18n._("Log")
                        }]
                })]
            });
        },
        // Event Log
        buildEventLog : function() {
            this.gridEventLog = Ext.create('Ung.GridEventLog',{
                settingsCmp : this,
                fields : [{
                    name : 'id'
                }, {
                    name : 'time_stamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'blocked',
                    mapping : 'firewall_was_blocked'
                }, {
                    name : 'firewall_rule_index'
                }, {
                    name : 'uid'
                }, {
                    name : 'client',
                    mapping : 'c_client_addr'
                }, {
                    name : 'client_port',
                    mapping : 'c_client_port'
                }, {
                    name : 'server',
                    mapping : 's_server_addr'
                }, {
                    name : 'server_port',
                    mapping : 's_server_port'
                }],
                columns : [{
                    header : this.i18n._("Timestamp"),
                    width : Ung.Util.timestampFieldWidth,
                    sortable : true,
                    dataIndex : 'time_stamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : this.i18n._("Client"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'client'
                }, {
                    header : this.i18n._("Client Port"),
                    width : Ung.Util.portFieldWidth,
                    sortable : true,
                    dataIndex : 'client_port'
                }, {
                    header : this.i18n._("Username"),
                    width : Ung.Util.usernameFieldWidth,
                    sortable : true,
                    dataIndex : 'uid'
                }, {
                    header : this.i18n._("Blocked"),
                    width : Ung.Util.booleanFieldWidth,
                    sortable : true,
                    dataIndex : 'blocked'
                }, {
                    header : this.i18n._('Rule Id'),
                    width : 60,
                    sortable : true,
                    flex:1,
                    dataIndex : 'firewall_rule_index'
                }, {
                    header : this.i18n._("Server") ,
                    width : Ung.Util.ipFieldWidth + 40, // +40 for column header
                    sortable : true,
                    dataIndex : 'server'
                }, {
                    header : this.i18n._("Server Port"),
                    width : Ung.Util.portFieldWidth + 40, // +40 for column header
                    sortable : true,
                    dataIndex : 'server_port'
                }]
            });
        },

         beforeSave: function(isApply, handler) {
            this.gridRules.getGridSaveList(Ext.bind(function(saveList) {
                this.settings.rules = saveList;
                handler.call(this, isApply);
            },this));
        }
    });
}
//@ sourceURL=firewall-settingsNew.js
