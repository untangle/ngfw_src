if (!Ung.hasResource["Ung.Ips"]) {
    Ung.hasResource["Ung.Ips"] = true;
    Ung.NodeWin.registerClassName('untangle-node-ips', 'Ung.Ips');

    Ext.define('Ung.Ips', {
        extend:'Ung.NodeWin',
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
            this.panelStatus = Ext.create('Ext.panel.Panel',{
                name : 'Status',
                helpSource : 'status',
                parentId : this.getId(),

                title : this.i18n._('Status'),
            //    layout : "form",
                cls: 'ung-panel',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Statistics'),
                   // layout:'form',
                    labelWidth: 230,
                    defaults: {
                        xtype: "textfield",
                        disabled: true
                    },
                    items: [{
                        fieldLabel : this.i18n._('Total Signatures Available'),
                        name: 'Total Signatures Available',
                        labelWidth:200,
                        labelAlign:'left',
                        value: this.getBaseSettings().totalAvailable
                    }, {
                        fieldLabel : this.i18n._('Total Signatures Logging'),
                        name: 'Total Signatures Logging',
                        labelWidth:200,
                        labelAlign:'left',
                        value: this.getBaseSettings().totalLogging
                    }, {
                        fieldLabel : this.i18n._('Total Signatures Blocking'),
                        name: 'Total Signatures Blocking',
                        labelWidth:200,
                        labelAlign:'left',
                        value: this.getBaseSettings().totalBlocking
                    }]
                }, {
                    title : this.i18n._('Note'),
                    cls: 'description',
                    html : Ext.String.format(this.i18n._("{0} continues to maintain the default signature settings through automatic updates. You are free to modify and add signatures, however it is not required."),
                                         main.getBrandingManager().getCompanyName())
                }]
            });
        },

        // Rules Panel
        buildRules : function() {
            
            
            this.panelRules = Ext.create('Ext.panel.Panel',{
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
                items : [this.gridRules = Ext.create('Ung.EditorGrid',{
                        name : 'Rules',
                        settingsCmp : this,
                        height : 350,
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
                        dataFn:	Ext.bind(function() {
                        	return this.getRpcNode().getRules(0, Ung.Util.maxRowCount,[]);
                        },this),
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
                            header : this.i18n._("category"),
                            width : 180,
                            dataIndex : 'category',
                            field: {
                                xtype:'texfield',
                                allowBlank : false
                            }
                        }, 
                        {
                            xtype:'checkcolumn',
                            header : this.i18n._("block"),
                            dataIndex : 'live',
                            fixed : true,
                            width:55
                        },
                        {
                            xtype:'checkcolumn',
                            header : this.i18n._("log"),
                            dataIndex : 'log',
                            fixed : true,
                            width:55
                        }, 
                        {
                            header : this.i18n._("description"),
                            width : 200,
                            dataIndex : 'description',
                            flex:1,
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
                            header : this.i18n._("id"),
                            width : 70,
                            dataIndex : 'sid',
                            editor : null
                        }, {
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
                        rowEditorInputLines : [
                        {
                            xtype:'textfield',
                            name : "Category",
                            dataIndex: "category",
                            fieldLabel : this.i18n._("Category"),
                            allowBlank : false,
                            width : 400
                        },
                        {
                            xtype:'textfield',
                            name : "Signature",
                            dataIndex: "text",
                            fieldLabel : this.i18n._("Signature"),
                            allowBlank : false,
                            width : 450
                        },
                        {
                            xtype:'textfield',
                            name : "Name",
                            dataIndex: "name",
                            fieldLabel : this.i18n._("Name"),
                            allowBlank : false,
                            width : 300
                        },
                        {
                            xtype:'textfield',
                            name : "SID",
                            dataIndex: "sid",
                            fieldLabel : this.i18n._("SID"),
                            allowBlank : false,
                            width : 150
                        },
                        {
                            xtype:'checkbox',
                            name : "Block",
                            dataIndex: "live",
                            fieldLabel : this.i18n._("Block")
                        },
                        {
                            xtype:'checkbox',
                            name : "Log",
                            dataIndex: "log",
                            fieldLabel : this.i18n._("Log")
                        },
                        {
                            xtype:'textfield',
                            name : "Description",
                            dataIndex: "description",
                            fieldLabel : this.i18n._("Description"),
                            allowBlank : false,
                            width : 500
                        }]
                    }),  
                    {html : '<br>', border: false}, 
                    this.gridVariables = Ext.create('Ung.EditorGrid',{
                        name : 'Variables',
                        settingsCmp : this,
                        height : 350,
                        emptyRow : {
                            "variable" : this.i18n._("[no name]"),
                            "definition" : this.i18n._("[no definition]"),
                            "description" : this.i18n._("[no description]")
                        },
                        title : this.i18n._("Variables"),
                        recordJavaClass : "com.untangle.node.ips.IpsVariable",
                        dataFn : Ext.bind(function() {
                        	return this.getRpcNode().getVariables(0,this.getBaseSettings().variablesLength,[]);
                        },this),
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
                            header : this.i18n._("name"),
                            width : 170,
                            dataIndex : 'variable',
                            field: {
                                xtype:'textfield',
                                allowBlank : false
                                }
                            },
                            {
                            id : 'definition',
                            header : this.i18n._("pass"),
                            width : 300,
                            dataIndex : 'definition',
                            field: {
                                xtype:'textfield',
                                allowBlank : false
                            }
                        }, {
                            header : this.i18n._("description"),
                            width : 300,
                            dataIndex : 'description',
                            flex:1,
                            field: {
                                xtype:'textfield',
                                allowBlank : false
                            }
                        }],
                        sortField : 'variable',
                        columnsDefaultSortable : true,
                        rowEditorInputLines : [{
                            xtype:'textfield',
                            name : "Name",
                            dataIndex: "variable",
                            fieldLabel : this.i18n._("Name"),
                            allowBlank : false,
                            width : 300
                        }, 
                        {
                            xtype:'textfield',
                            name : "Pass",
                            dataIndex: "definition",
                            fieldLabel : this.i18n._("Pass"),
                            allowBlank : false,
                            width : 400
                        },
                        {
                            xtype:'textfield',
                            name : "Description",
                            dataIndex: "description",
                            fieldLabel : this.i18n._("Description"),
                            allowBlank : false,
                            width : 400
                        }]
                    })
            ]});
        },
        // Event Log
        buildEventLog : function() {
            this.gridEventLog = Ext.create('Ung.GridEventLog',{
                settingsCmp : this,
                fields : [{
                    name : 'timeStamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'blocked',
                    mapping : 'ipsBlocked'
                }, {
                    name : 'name',
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
                    id: 'ruleName',
                    header : this.i18n._('rule id'),
                    width : 60,
                    sortable : true,
                    dataIndex : 'name'
                }, {
                    id: 'ruleDescription',
                    header : this.i18n._('rule description'),
                    width : 150,
                    sortable : true,
                    flex:1,
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
                this.getRpcNode().updateAll(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    // exit settings screen
                    if(keepWindowOpen!== true){
                        Ext.MessageBox.hide();                    
                        this.closeWindow();
                    }else{
                    //refresh the settings
                            this.getRpcNode().getBaseSettings(Ext.bind(function(result2,exception2){
                                Ext.MessageBox.hide();                            
                                this.gridRules.reloadGrid();
                                this.gridVariables.reloadGrid();
                            },this));
                            //this.gridEventLog.reloadGrid();                                     
                    }
                },this), this.getBaseSettings(), this.gridRules ? this.gridRules.getSaveList() : null,
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
