if (!Ung.hasResource["Ung.Protofilter"]) {
    Ung.hasResource["Ung.Protofilter"] = true;
    Ung.NodeWin.registerClassName('untangle-node-protofilter', 'Ung.Protofilter');

    Ung.Protofilter = Ext.extend(Ung.NodeWin, {
    	panelStatus: null,
        gridProtocolList : null,
        gridEventLog : null,
        initComponent : function() {
            this.buildStatus();
            this.buildProtocolList();
            this.buildEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelStatus, this.gridProtocolList, this.gridEventLog]);
            Ung.Protofilter.superclass.initComponent.call(this);
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
                    title : this.i18n._('Status'),
                    cls: 'description',
                    html : String.format(this.i18n._("Protocol Control uses signatures to detect the protocols of network traffic. It is useful for detecting unwanted or interesting protocols in use on the network."))
                }, {
                    title : this.i18n._(''),
				    layout:'form',
                    labelWidth: 230,
                    defaults: {
                    	xtype: "textfield",
                    	disabled: true
					},
                    items: [{
                        fieldLabel : this.i18n._('Total Signatures Available'),
                        name: 'Total Signatures Available',
                        value: this.getBaseSettings().patternsLength
                    }, {
                        fieldLabel : this.i18n._('Total Signatures Logging'),
                        name: 'Total Signatures Logging',
                        value: this.getBaseSettings().patternsLoggedLength
                    }, {
                        fieldLabel : this.i18n._('Total Signatures Blocking'),
                        name: 'Total Signatures Blocking',
                        value: this.getBaseSettings().patternsBlockedLength
                    }]
                }, {
                    title : this.i18n._('Note'),
                    cls: 'description',
                    html : String.format(this.i18n._("Caution and discretion is advised using block at the the risk of false positives and intelligent applications shifting protocol usage to avoid blocking."))
                }]
            });
        },
        // Protocol list grid
        buildProtocolList : function() {
            // blocked is a check column
            var blockedColumn = new Ext.grid.CheckColumn({
                header : "<b>" + this.i18n._("block") + "</b>",
                dataIndex : 'blocked',
                fixed : true
            });
            // log is a check column
            var logColumn = new Ext.grid.CheckColumn({
                header : "<b>" + this.i18n._("log") + "</b>",
                dataIndex : 'log',
                fixed : true
            });

            this.gridProtocolList = new Ung.EditorGrid({
                settingsCmp : this,
                name : 'Signatures',
                helpSource : 'protocol_list',
                // the total records is set from the base settings
                // patternsLength field
                totalRecords : this.getBaseSettings().patternsLength,
                emptyRow : {
                    "category" : this.i18n._("[no category]"),
                    "protocol" : this.i18n._("[no protocol]"),
                    "blocked" : false,
                    "log" : false,
                    "description" : this.i18n._("[no description]"),
                    "definition" : this.i18n._("[no signature]")
                },
                title : this.i18n._("Signatures"),
                // the column is autoexpanded if the grid width permits
                autoExpandColumn : 'description',
                recordJavaClass : "com.untangle.node.protofilter.ProtoFilterPattern",
                // this is the function used by Ung.RpcProxy to retrive data
                // from the server
                proxyRpcFn : this.getRpcNode().getPatterns,
                // the list of fields
                fields : [{
                    name : 'id'
                },
                // this field is internationalized so a converter was
                // added
                {
                    name : 'category',
                    type : 'string'
                }, {
                    name : 'protocol',
                    type : 'string'
                }, {
                    name : 'blocked'
                }, {
                    name : 'log'
                }, {
                    name : 'description',
                    type : 'string'
                }, {
                    name : 'definition'
                }],
                // the list of columns for the column model
                columns : [{
                    id : 'category',
                    header : this.i18n._("category"),
                    width : 200,
                    dataIndex : 'category',
                    // this is a simple text editor
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, {
                    id : 'protocol',
                    header : this.i18n._("protocol"),
                    width : 200,
                    dataIndex : 'protocol',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, blockedColumn, logColumn, {
                    id : 'description',
                    header : this.i18n._("description"),
                    width : 200,
                    dataIndex : 'description',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }],
                sortField : 'category',
                columnsDefaultSortable : true,
                plugins : [blockedColumn, logColumn],
                // the row input lines used by the row editor window
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "Category",
                    dataIndex : "category",
                    fieldLabel : this.i18n._("Category"),
                    allowBlank : false,
                    width : 200
                }), new Ext.form.TextField({
                    name : "Protocol",
                    dataIndex : "protocol",
                    fieldLabel : this.i18n._("Protocol"),
                    allowBlank : false,
                    width : 200
                }), new Ext.form.Checkbox({
                    name : "Block",
                    dataIndex : "blocked",
                    fieldLabel : this.i18n._("Block")
                }), new Ext.form.Checkbox({
                    name : "Log",
                    dataIndex : "log",
                    fieldLabel : this.i18n._("Log")
                }), new Ext.form.TextArea({
                    name : "Description",
                    dataIndex : "description",
                    fieldLabel : this.i18n._("Description"),
                    width : 200,
                    height : 60
                }), new Ext.form.TextArea({
                    name : "Signature",
                    dataIndex : "definition",
                    fieldLabel : this.i18n._("Signature"),
                    allowBlank : false,
                    width : 200,
                    height : 60
                })]
            });
        },
        // Event Log
        buildEventLog : function() {
        	var asAction = function(value) {
                return value ? this.i18n._("blocked") : this.i18n._("passed");
            }.createDelegate(this);
            var asReason = function(value) {
                return value ? this.i18n._("blocked in block list") : this.i18n._("not blocked in block list");
            }.createDelegate(this);
            
            this.gridEventLog = new Ung.GridEventLog({
                settingsCmp : this,
                fields : [{
                    name : 'timeStamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'action',
                    mapping : 'blocked',
                    convert : asAction,
                    type : 'string'
                }, {
                    name : 'reason',
                    mapping : 'blocked',
                    convert : asReason,
                    type : 'string'
                }, {
                    name : 'client',
                    mapping : 'pipelineEndpoints',
                    sortType : Ung.SortTypes.asClient
                }, {
                    name : 'server',
                    mapping : 'pipelineEndpoints',
                    sortType : Ung.SortTypes.asServer
                }, {
                    name : 'protocol',
                    type : 'string'
                }],
                columns : [{
                    header : this.i18n._("timestamp"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'timeStamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : this.i18n._("action"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'action'
                }, {
                    header : this.i18n._("client"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'client',
                    renderer : Ung.SortTypes.asClient
                }, {
                    header : this.i18n._("request"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'protocol'
                }, {
                    header : this.i18n._("reason for action"),
                    width : 150,
                    sortable : true,
                    dataIndex : 'reason'
                }, {
                    header : this.i18n._("server"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'server',
                    renderer : Ung.SortTypes.asServer
                }]
            });
        },
        isDirty : function() {
        	return this.gridProtocolList.isDirty();
        },
        //apply function 
        applyAction : function(){
            this.saveAction(true);
        },        
        // save function
        saveAction : function(keepWindowOpen) {
            Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
            this.getRpcNode().updateAll(function(result, exception) {
                Ext.MessageBox.hide();
                if(Ung.Util.handleException(exception)) return;
                // exit settings screen
                if(!keepWindowOpen){
                    Ext.MessageBox.hide();                    
                    this.closeWindow();
                }else{
                //refresh the settings
                        Ext.MessageBox.hide();
                        //refresh the settings
                        this.getRpcNode().getBaseSettings(function(result2,exception2){
                            Ext.MessageBox.hide();                            
                            this.initialBaseSettings = Ung.Util.clone(this.getBaseSettings());                                      
                            this.gridProtocolList.setTotalRecords(result2.patternsLength);
                            this.gridProtocolList.reloadGrid();
                        }.createDelegate(this));                        
                        //this.gridEventLog.reloadGrid();                            
                }
            }.createDelegate(this), this.gridProtocolList.getSaveList());
        }
    });
}
