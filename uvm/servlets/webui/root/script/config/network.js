if (!Ung.hasResource["Ung.Network"]) {
    Ung.hasResource["Ung.Network"] = true;

    Ung.NetworkUtil={
        getPortForwardMatchers: function (settingsCmp) {
            return [
                {name:"DST_LOCAL",displayName: settingsCmp.i18n._("Destined Local"), type: "boolean", visible: true},
                {name:"DST_ADDR",displayName: settingsCmp.i18n._("Destination Address"), type: "text", visible: true, vtype:"ipAddress"},
                {name:"DST_PORT",displayName: settingsCmp.i18n._("Destination Port"), type: "text",vtype:"port", visible: true},
                {name:"DST_INTF",displayName: settingsCmp.i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
                {name:"SRC_ADDR",displayName: settingsCmp.i18n._("Source Address"), type: "text", visible: true, vtype:"ipAddress"},
                {name:"SRC_PORT",displayName: settingsCmp.i18n._("Source Port"), type: "text",vtype:"port", visible: false},
                {name:"SRC_INTF",displayName: settingsCmp.i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
                {name:"PROTOCOL",displayName: settingsCmp.i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["any","any"]], visible: true}
            ];
        },
        getNatRuleMatchers: function (settingsCmp) {
            return [
                {name:"DST_ADDR",displayName: settingsCmp.i18n._("Destination Address"), type: "text", visible: true, vtype:"ipAddress"},
                {name:"DST_PORT",displayName: settingsCmp.i18n._("Destination Port"), type: "text",vtype:"port", visible: true},
                {name:"DST_INTF",displayName: settingsCmp.i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
                {name:"SRC_ADDR",displayName: settingsCmp.i18n._("Source Address"), type: "text", visible: true, vtype:"ipAddress"},
                {name:"SRC_PORT",displayName: settingsCmp.i18n._("Source Port"), type: "text",vtype:"port", visible: false},
                {name:"SRC_INTF",displayName: settingsCmp.i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
                {name:"PROTOCOL",displayName: settingsCmp.i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["any","any"]], visible: true}
            ];
        }

    };
    
    Ext.define("Ung.Network", {
        extend: "Ung.ConfigWin",
        gridPortForwards: null,
        gridNatRules: null,
        panelInterfaces: null,
        panelPortForwards: null,
        panelNatRules: null,
        initComponent: function() {
            this.breadcrumbs = [{
                title: i18n._("Configuration"),
                action: Ext.bind(function() {
                    this.cancelAction();
                }, this)
            }, {
                title: i18n._('Network')
            }];

            this.refreshSettings();
            
            // builds the tabs
            this.buildInterfaces();
            this.buildPortForwards();
            this.buildNatRules();
            
            // builds the tab panel with the tabs
            var pageTabs = [this.panelInterfaces, this.panelPortForwards, this.panelNatRules];
            this.buildTabPanel(pageTabs);
            this.callParent(arguments);
        },
        // PortForwards Panel
        buildPortForwards: function() {
            this.panelPortForwards = Ext.create('Ext.panel.Panel',{
                name: 'panelPortForwards',
                helpSource: 'port_forwards',
                parentId: this.getId(),
                title: this.i18n._('Port Forwards'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Note'),
                    html: this.i18n._(" <b>Port Forwards</b> are sweet. Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
                },  this.gridPortForwards = Ext.create( 'Ung.EditorGrid', {
                    anchor: '100% -80',
                    name: 'Port Forward Rules',
                    settingsCmp: this,
                    paginated: false,
                    hasReorder: true,
                    addAtTop: false,
                    emptyRow: {
                        "ruleId": 0,
                        "enabled": true,
                        "block": false,
                        "flag": false,
                        "description": this.i18n._("[no description]"),
                        "javaClass": "com.untangle.uvm.network.PortForwardRule"
                    },
                    title: this.i18n._("Port Forwards"),
                    recordJavaClass: "com.untangle.uvm.network.PortForwardRule",
                    dataProperty:'portForwards',
                    fields: [{
                        name: 'ruleId'
                    }, {
                        name: 'enabled'
                    }, {
                        name: 'newDestination'
                    }, {
                        name: 'newPort'
                    }, {
                        name: 'matchers'
                    },{
                        name: 'description'
                    }, {
                        name: 'javaClass'
                    }],
                    columns: [{
                        header: this.i18n._("Rule Id"),
                        width: 50,
                        dataIndex: 'ruleId',
                        renderer: function(value) {
                            if (value < 0) {
                                return i18n._("new");
                            } else {
                                return value;
                            }
                        }
                    }, {
                        xtype:'checkcolumn',
                        header: this.i18n._("Enable"),
                        dataIndex: 'enabled',
                        fixed: true,
                        width:55
                    }, {
                        header: this.i18n._("Description"),
                        width: 200,
                        dataIndex: 'description',
                        flex:1
                    }, {
                        xtype:'checkcolumn',
                        header: this.i18n._("New Destination"),
                        dataIndex: 'newDestination',
                        fixed: true,
                        width:55
                    }, {
                        xtype:'checkcolumn',
                        header: this.i18n._("New Port"),
                        dataIndex: 'newPort',
                        fixed: true,
                        width:55
                    }],
                    columnsDefaultSortable: false,

                    initComponent: function() {
                        this.rowEditor = Ext.create('Ung.RowEditorWindow',{
                            grid: this,
                            sizeToComponent: this.settingsCmp,
                            inputLines: this.rowEditorInputLines,
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
                                return Ext.getCmp('portForwardBuilder').isDirty();
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
                    },

                    rowEditorInputLines: [{
                            xtype:'checkbox',
                            name: "Enable Port Forward",
                            dataIndex: "enabled",
                            fieldLabel: this.i18n._("Enable Port Forward")
                        }, {
                            xtype:'textfield',
                            name: "Description",
                            dataIndex: "description",
                            fieldLabel: this.i18n._("Description"),
                            width: 500
                        }, {
                            xtype:'fieldset',
                            title: this.i18n._("Rule"),
                            title: "If all of the following conditions are met:",
                            items:[{
                                xtype:'rulebuilder',
                                settingsCmp: this,
                                javaClass: "com.untangle.uvm.network.PortForwardRuleMatcher",
                                anchor:"98%",
                                width: 900,
                                dataIndex: "matchers",
                                matchers: Ung.NetworkUtil.getPortForwardMatchers(this),
                                id:'portForwardBuilder'
                            }]
                        }, {
                            xtype: 'fieldset',
                            cls:'description',
                            title: i18n._('Perform the following action(s):'),
                            border: false
                        }, {
                            xtype:'textfield',
                            name: "newDestination",
                            allowBlank: false,
                            dataIndex: "newDestination",
                            fieldLabel: this.i18n._("New Destination"),
                            vtype: 'ipAddress'
                        }, {
                            xtype:'textfield',
                            name: "newPort",
                            allowBlank: false,
                            dataIndex: "newPort",
                            fieldLabel: this.i18n._("New Port"),
                            vtype: 'port'
                        }]
                })]
            });
        },
        // NatRules Panel
        buildNatRules: function() {
            this.panelNatRules = Ext.create('Ext.panel.Panel',{
                name: 'panelNatRules',
                helpSource: 'nat_rules',
                parentId: this.getId(),
                title: this.i18n._('NAT Rules'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Note'),
                    html: this.i18n._(" <b>NAT Rules</b> are pretty cool. Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
                },  this.gridNatRules = Ext.create( 'Ung.EditorGrid', {
                    anchor: '100% -80',
                    name: 'NAT Rules',
                    settingsCmp: this,
                    paginated: false,
                    hasReorder: true,
                    addAtTop: false,
                    emptyRow: {
                        "ruleId": 0,
                        "enabled": true,
                        "block": false,
                        "flag": false,
                        "description": this.i18n._("[no description]"),
                        "javaClass": "com.untangle.uvm.network.NatRule"
                    },
                    title: this.i18n._("NAT Rules"),
                    recordJavaClass: "com.untangle.uvm.network.NatRule",
                    dataProperty:'natRules',
                    fields: [{
                        name: 'ruleId'
                    }, {
                        name: 'enabled'
                    }, {
                        name: 'auto'
                    }, {
                        name: 'newSource'
                    }, {
                        name: 'matchers'
                    },{
                        name: 'description'
                    }, {
                        name: 'javaClass'
                    }],
                    columns: [{
                        header: this.i18n._("Rule Id"),
                        width: 50,
                        dataIndex: 'ruleId',
                        renderer: function(value) {
                            if (value < 0) {
                                return i18n._("new");
                            } else {
                                return value;
                            }
                        }
                    }, {
                        xtype:'checkcolumn',
                        header: this.i18n._("Enable"),
                        dataIndex: 'enabled',
                        fixed: true,
                        width:55
                    }, {
                        header: this.i18n._("Description"),
                        width: 200,
                        dataIndex: 'description',
                        flex:1
                    }, {
                        xtype:'checkcolumn',
                        header: this.i18n._("New Source"),
                        dataIndex: 'newSource',
                        fixed: true,
                        width:55
                    }],
                    columnsDefaultSortable: false,

                    initComponent: function() {
                        this.rowEditor = Ext.create('Ung.RowEditorWindow',{
                            grid: this,
                            sizeToComponent: this.settingsCmp,
                            inputLines: this.rowEditorInputLines,
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
                                return Ext.getCmp('natRuleBuilder').isDirty();
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
                    },

                    rowEditorInputLines: [{
                            xtype:'checkbox',
                            name: "Enable NAT Rule",
                            dataIndex: "enabled",
                            fieldLabel: this.i18n._("Enable NAT Rule")
                        }, {
                            xtype:'textfield',
                            name: "Description",
                            dataIndex: "description",
                            fieldLabel: this.i18n._("Description"),
                            width: 500
                        }, {
                            xtype:'fieldset',
                            title: this.i18n._("Rule"),
                            title: "If all of the following conditions are met:",
                            items:[{
                                xtype:'rulebuilder',
                                settingsCmp: this,
                                javaClass: "com.untangle.uvm.network.NatRuleMatcher",
                                anchor:"98%",
                                width: 900,
                                dataIndex: "matchers",
                                matchers: Ung.NetworkUtil.getNatRuleMatchers(this),
                                id:'natRuleBuilder'
                            }]
                        }, {
                            xtype: 'fieldset',
                            cls:'description',
                            title: i18n._('Perform the following action(s):'),
                            border: false
                        }, {
                            xtype: "combo",
                            name: "auto",
                            allowBlank: false,
                            dataIndex: "auto",
                            fieldLabel: this.i18n._("New Source"),
                            editable: false,
                            store: [[true,i18n._('Auto')], [false,i18n._('Custom')]],
                            valueField: "value",
                            displayField: "displayName",
                            queryMode: 'local',
                            triggerAction: 'all',
                            listClass: 'x-combo-list-small',
                            listeners: {
                                select: Ext.bind(function(combo, ewVal, oldVal) {
                                    if (combo.value == true) /* Auto */ {
                                        Ext.getCmp('newSourceField').disable();
                                        Ext.getCmp('newSourceField').setVisible(false);
                                    } else {
                                        Ext.getCmp('newSourceField').enable();
                                        Ext.getCmp('newSourceField').setVisible(true);
                                    }
                                }, this )
                            }
                        }, {
                            id: 'newSourceField',
                            xtype:'textfield',
                            name: "newSource",
                            allowBlank: false,
                            dataIndex: "newSource",
                            fieldLabel: this.i18n._("New Source"),
                            hidden: true,
                            vtype: 'ipAddress'
                        }],
                    
                    syncRuleEditorComponents: function () {
                        var natType  = this.query('combo[name="auto"]')[0];
                        var newSource = this.query('textfield[name="newSource"]')[0];

                        newSource.disable();
                        
                        switch(natType.value) {
                          case true:
                            break;
                          case false:
                            newSource.enable();
                            break;
                        }
                        newSource.setVisible(!newSource.disabled); 
                    }
                })]
            });
        },
        buildInterfaces: function() {
            this.panelInterfaces = Ext.create('Ext.panel.Panel',{
                name: 'panelInterfaces',
                helpSource: 'network_interfaces',
                parentId: this.getId(),
                title: this.i18n._('Interfaces'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Note'),
                    html: this.i18n._(" <b>Interfaces</b> are legit. Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
                },  this.gridInterfaces = Ext.create('Ung.EditorGrid',{
                    anchor: '100% -80',
                    name: 'Interfaces',
                    settingsCmp: this,
                    paginated: false,
                    hasReorder: false,
                    hasDelete: false,
                    hasAdd: false,
                    title: this.i18n._("Interfaces"),
                    recordJavaClass: "com.untangle.uvm.network.InterfaceSettings",
                    columnsDefaultSortable: false,
                    dataProperty:'interfaces',
                    fields: [{
                        name: 'interfaceId'
                    }, {
                        name: 'name'
                    }, {
                        name: 'physicalDev'
                    }, {
                        name: 'symbolicDev'
                    }, {
                        name: 'configType'
                    },{
                        name: 'isWan'
                    }, {
                        name: 'javaClass'
                    }],
                    columns: [{
                        header: this.i18n._("Interface Id"),
                        width: 75,
                        dataIndex: 'interfaceId',
                        renderer: function(value) {
                            if (value < 0) {
                                return i18n._("new");
                            } else {
                                return value;
                            }
                        }
                    }, {
                        header: this.i18n._("Name"),
                        dataIndex: 'name',
                        width:100
                    }, {
                        header: this.i18n._("Physical Dev"),
                        dataIndex: 'physicalDev',
                        width:75
                    }, {
                        header: this.i18n._("Symbolic Dev"),
                        dataIndex: 'symbolicDev',
                        width:75
                    }, {
                        header: this.i18n._("Config"),
                        dataIndex: 'configType',
                        width:75
                    }, {
                        header: this.i18n._("is WAN"),
                        dataIndex: 'isWan',
                        width:55
                    }],
                    initComponent: function() {
                        Ung.EditorGrid.prototype.initComponent.call(this);
                    }
                })]
            });
        },
        save: function (isApply) {
            this.saveSemaphore = 1;
            // save language settings
            rpc.newNetworkManager.setNetworkSettings(Ext.bind(function(result, exception) {
                this.afterSave(exception, isApply);
            }, this), this.settings);
        },
        refreshSettings: function() {
            this.settings = rpc.newNetworkManager.getNetworkSettings();
        },
        beforeSave: function(isApply, handler) {
            this.beforeSaveCount = 2;

            this.gridPortForwards.getList(Ext.bind(function(saveList) {
                this.settings.portForwards = saveList;
                this.beforeSaveCount--;
                if (this.beforeSaveCount <= 0)
                    handler.call(this, isApply);
            }, this));

            this.gridNatRules.getList(Ext.bind(function(saveList) {
                this.settings.natRules = saveList;
                this.beforeSaveCount--;
                if (this.beforeSaveCount <= 0)
                    handler.call(this, isApply);
            }, this));
        },
        afterSave: function(exception, isApply) {
            if(Ung.Util.handleException(exception)) return;

            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
                this.refreshSettings();
                if(isApply) {
                    this.clearDirty();
                    Ext.MessageBox.hide();
                } else {
                    Ext.MessageBox.hide();
                    this.closeWindow();
                }
            }
        }
        
    });
}
//@ sourceURL=network.js
