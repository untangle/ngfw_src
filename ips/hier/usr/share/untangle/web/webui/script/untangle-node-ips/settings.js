if (!Ung.hasResource["Ung.Ips"]) {
    Ung.hasResource["Ung.Ips"] = true;
    Ung.NodeWin.registerClassName('untangle-node-ips', 'Ung.Ips');

    Ung.Ips = Ext.extend(Ung.NodeWin, {
        panelStatus: null,
        panelRules: null,
        gridRules : null,
        gridVariables : null,
        gridEventLog : null,
        // called when the component is rendered
        initComponent : function() {
            this.buildStatus();
            this.buildRules();
            this.buildEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelStatus, this.panelRules, this.gridEventLog]);
            Ung.Ips.superclass.initComponent.call(this);
        },
        // Status Panel
        buildStatus : function() {
            this.panelStatus = new Ext.Panel({
                name : 'Status',
                helpSource : 'status',
                parentId : this.getId(),

                title : this.i18n._('Status'),
                layout : "form",
                cls: 'ung-panel',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Statistics'),
                    layout:'form',
                    labelWidth: 230,
                    defaults: {
                        xtype: "textfield",
                        disabled: true
                    },
                    items: [{
                        fieldLabel : this.i18n._('Total Signatures Available'),
                        name: 'Total Signatures Available',
                        value: this.getBaseSettings().totalAvailable
                    }, {
                        fieldLabel : this.i18n._('Total Signatures Logging'),
                        name: 'Total Signatures Logging',
                        value: this.getBaseSettings().totalLogging
                    }, {
                        fieldLabel : this.i18n._('Total Signatures Blocking'),
                        name: 'Total Signatures Blocking',
                        value: this.getBaseSettings().totalBlocking
                    }]
                }, {
                    title : this.i18n._('Note'),
                    cls: 'description',
                    html : String.format(this.i18n._("{0} continues to maintain the default signature settings through automatic updates. You are free to modify and add signatures, however it is not required."),
                                         main.getBrandingManager().getCompanyName())
                }]
            });
        },

        // Rules Panel
        buildRules : function() {
            // block is a check column
            var blockColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("block"),
                dataIndex : 'live',
                fixed : true
            });
            
            // log is a check column
            var logColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("log"),
                dataIndex : 'log',
                fixed : true
            });
            
            this.panelRules = new Ext.Panel({
                name : 'panelRules',
                helpSource : 'rules',
                // private fields
                parentId : this.getId(),
                title : this.i18n._('Rules'),
                autoScroll : true,
                border : false,
                layout: 'anchor',
                defaults: {
                    anchor: '98%',
                    autoWidth: true,
                    autoScroll: true
                },
                cls: 'ung-panel',
                items : [this.gridRules = new Ung.EditorGrid({
                        name : 'Rules',
                        settingsCmp : this,
                        height : 350,
                        totalRecords : this.getBaseSettings().rulesLength,
                        emptyRow : {
                            "category" : this.i18n._("[no category]"),
                            "name" : this.i18n._("[no name]"),
                            "text" : this.i18n._("[no signature]"),
                            "category" : this.i18n._("[no category]"),
                            "sid" : "0",
                            "live" : true,
                            "log" : true,
                            "description" : this.i18n._("[no description]")
                        },
                        title : this.i18n._("Rules"),
                        recordJavaClass : "com.untangle.node.ips.IpsRule",
                        proxyRpcFn : this.getRpcNode().getRules,
                        fields : [{
                            name : 'id'
                        }, {
                            name : 'text'
                        }, {
                            name : 'sid'
                        }, {
                            name : 'name'
                        }, {
                            name : 'category',
                            type : 'string'
                        }, {
                            name : 'classification'
                        }, {
                            name : 'URL'
                        }, {
                            name : 'live'
                        }, {
                            name : 'log'
                        }, {
                            name : 'description',
                            type : 'string'
                        }],
                        columns : [{
                            id : 'category',
                            header : this.i18n._("category"),
                            width : 180,
                            dataIndex : 'category',
                            editor : new Ext.form.TextField({
                                allowBlank : false
                            })
                        }, blockColumn, logColumn, {
                            id : 'description',
                            header : this.i18n._("description"),
                            width : 200,
                            dataIndex : 'description',
                            editor : null,
                            renderer : function(value, metadata, record) {
                                var description = "";
                                if (record.data.classification != null) 
                                {
                                    description += record.data.classification + " ";
                                }
                                if (record.data.description != null) {
                                    description += "(" + record.data.description + ")";
                                }    
                                return description;
                            }
                        }, {
                            id : 'id',
                            header : this.i18n._("id"),
                            width : 70,
                            dataIndex : 'sid',
                            editor : null
                        }, {
                            id : 'info',
                            header : this.i18n._("info"),
                            width : 70,
                            dataIndex : 'URL',
                            editor : null,
                            sortable : false,
                            renderer : function(value) {
                                return (value == null || value.length == 0) ? "no info" : "<a href='" + value + "' target='_blank'>info</a>";
                            }
                        }],
                        sortField : 'category',
                        columnsDefaultSortable : true,
                        autoExpandColumn : 'description',
                        plugins : [blockColumn, logColumn],
                        rowEditorInputLines : [new Ext.form.TextField({
                            name : "Category",
                            dataIndex: "category",
                            fieldLabel : this.i18n._("Category"),
                            allowBlank : false,
                            width : 300
                        }), new Ext.form.TextField({
                            name : "Signature",
                            dataIndex: "text",
                            fieldLabel : this.i18n._("Signature"),
                            allowBlank : false,
                            width : 350
                        }), new Ext.form.TextField({
                            name : "Name",
                            dataIndex: "name",
                            fieldLabel : this.i18n._("Name"),
                            allowBlank : false,
                            width : 300
                        }), new Ext.form.TextField({
                            name : "SID",
                            dataIndex: "sid",
                            fieldLabel : this.i18n._("SID"),
                            allowBlank : false,
                            width : 50
                        }), new Ext.form.Checkbox({
                            name : "Block",
                            dataIndex: "live",
                            fieldLabel : this.i18n._("Block")
                        }), new Ext.form.Checkbox({
                            name : "Log",
                            dataIndex: "log",
                            fieldLabel : this.i18n._("Log")
                        }), new Ext.form.TextField({
                            name : "Description",
                            dataIndex: "description",
                            fieldLabel : this.i18n._("Description"),
                            allowBlank : false,
                            width : 400
                        })]
                    }),  
                    {html : '<br>', border: false}, 
                    this.gridVariables = new Ung.EditorGrid({
                        name : 'Variables',
                        settingsCmp : this,
                        totalRecords : this.getBaseSettings().variablesLength,
                        height : 350,
                        emptyRow : {
                            "variable" : this.i18n._("[no name]"),
                            "definition" : this.i18n._("[no definition]"),
                            "description" : this.i18n._("[no description]")
                        },
                        title : this.i18n._("Variables"),
                        autoExpandColumn : 'description',
                        recordJavaClass : "com.untangle.node.ips.IpsVariable",
                        proxyRpcFn : this.getRpcNode().getVariables,
                        fields : [{
                            name : 'id'
                        }, {
                            name : 'variable',
                            type : 'string'
                        }, {
                            name : 'definition',
                            type : 'string'
                        }, {
                            name : 'description',
                            type : 'string'
                        }],
                        columns : [{
                            id : 'variable',
                            header : this.i18n._("name"),
                            width : 170,
                            dataIndex : 'variable',
                            editor : new Ext.form.TextField({
                                allowBlank : false
                            })
                        }, {
                            id : 'definition',
                            header : this.i18n._("pass"),
                            width : 300,
                            dataIndex : 'definition',
                            editor : new Ext.form.TextField({
                                allowBlank : false
                            })
                        }, {
                            id : 'description',
                            header : this.i18n._("description"),
                            width : 300,
                            dataIndex : 'description',
                            editor : new Ext.form.TextField({
                                allowBlank : false
                            })
                        }],
                        sortField : 'variable',
                        columnsDefaultSortable : true,
                        rowEditorInputLines : [new Ext.form.TextField({
                            name : "Name",
                            dataIndex: "variable",
                            fieldLabel : this.i18n._("Name"),
                            allowBlank : false,
                            width : 200
                        }), new Ext.form.TextField({
                            name : "Pass",
                            dataIndex: "definition",
                            fieldLabel : this.i18n._("Pass"),
                            allowBlank : false,
                            width : 300
                        }), new Ext.form.TextField({

                            name : "Description",
                            dataIndex: "description",
                            fieldLabel : this.i18n._("Description"),
                            allowBlank : false,
                            width : 300
                        })]
                    })
            ]});
        },
        // Event Log
        buildEventLog : function() {
            this.gridEventLog = new Ung.GridEventLog({
                settingsCmp : this,
                fields : [{
                    name : 'timeStamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'blocked',
                    mapping : 'ipsBlocked'
                }, {
                    name : 'reason',
                    mapping : 'ipsName',
                    type : 'string'
                }, {
                    name : 'description',
                    mapping : 'ipsDescription',
                    type : 'string'
                }, {
                    name : 'client',
                    mapping : 'CClientAddr'
                }, {
                    name : 'server',
                    mapping : 'CServerAddr'
                }, {
                    name : 'server',
                    mapping : 'CServerAddr'
                }, {
                    name : 'uid'
                }],
                autoExpandColumn : 'ruleDescription',
                columns : [{
                    header : this.i18n._("timestamp"),
                    width : Ung.Util.timestampFieldWidth,
                    sortable : true,
                    dataIndex : 'timeStamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : this.i18n._("client"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'client'
                }, {
                    header : this.i18n._("username"),
                    width : Ung.Util.usernameFieldWidth,
                    sortable : true,
                    dataIndex : 'uid'
                }, {
                    header : this.i18n._("blocked"),
                    width : Ung.Util.booleanFieldWidth,
                    sortable : true,
                    dataIndex : 'blocked'
                }, {
                    id: 'ruleDescription',
                    header : this.i18n._('rule description'),
                    width : 150,
                    sortable : true,
                    dataIndex : 'description'
                }, {
                    header : this.i18n._("server"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'server'
                }]
            });
        },
        //apply function 
        applyAction : function(){
            this.saveAction(true);
        },
        // save function
        saveAction : function(keepWindowOpen) {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.getRpcNode().updateAll(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    // exit settings screen
                    if(keepWindowOpen!== true){
                        Ext.MessageBox.hide();                    
                        this.closeWindow();
                    }else{
                    //refresh the settings
                            this.getRpcNode().getBaseSettings(function(result2,exception2){
                                Ext.MessageBox.hide();                            
                                this.gridRules.setTotalRecords(result2.rulesLength);
                                this.gridVariables.setTotalRecords(result2.variablesLength);
                                this.gridRules.reloadGrid();
                                this.gridVariables.reloadGrid();
                            }.createDelegate(this));
                            //this.gridEventLog.reloadGrid();                                     
                    }
                }.createDelegate(this), this.getBaseSettings(), this.gridRules ? this.gridRules.getSaveList() : null,
                        this.gridVariables ? this.gridVariables.getSaveList() : null,
                        null);
            }
        },
        isDirty : function() {
            return (this.gridRules ? this.gridRules.isDirty() : false)
                || (this.gridVariables ? this.gridVariables.isDirty() : false);
        }        
    });
}
