if (!Ung.hasResource["Ung.Protofilter"]) {
    Ung.hasResource["Ung.Protofilter"] = true;
    Ung.NodeWin.registerClassName('untangle-node-protofilter', 'Ung.Protofilter');

	Ext.define('Ung.Protofilter',{
		extend:'Ung.NodeWin',
        nodeData: null,
        panelStatus: null,
        gridProtocolList : null,
        gridEventLog : null,
        nodeData : null,
        initComponent : function() {
            this.nodeData = this.getRpcNode().getSettings();
            this.buildStatus();
            this.buildProtocolList();
            this.buildEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelStatus, this.gridProtocolList, this.gridEventLog]);
            Ung.Protofilter.superclass.initComponent.call(this);
        },
        // Status Panel
        buildStatus : function() {
            this.panelStatus = Ext.create('Ext.panel.Panel',{
                name : 'Status',
                helpSource : 'status',
                parentId : this.getId(),

                title : this.i18n._('Status'),
                //layout : "form",
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
                    html : Ext.String.format(this.i18n._("Application Control Lite uses signatures to detect the protocols of network traffic. It is useful for detecting unwanted or interesting protocols in use on the network."))
                }, {
                    title : this.i18n._(' '),
                    //layout:'form',
                    labelWidth: 230,
                    defaults: {
                        xtype: "textfield",
                        disabled: true
                    },
                    items: [{
                        fieldLabel : this.i18n._('Total Signatures Available'),
                        name: 'Total Signatures Available',
                        value: this.getRpcNode().getPatternsTotal()
                    }, {
                        fieldLabel : this.i18n._('Total Signatures Logging'),
                        name: 'Total Signatures Logging',
                        value: this.getRpcNode().getPatternsLogged()
                    }, {
                        fieldLabel : this.i18n._('Total Signatures Blocking'),
                        name: 'Total Signatures Blocking',
                        value: this.getRpcNode().getPatternsBlocked()
                    }]
                }, {
                    title : this.i18n._('Note'),
                    cls: 'description',
                    html : Ext.String.format(this.i18n._("Caution and discretion is advised using block at the the risk of false positives and intelligent applications shifting protocol usage to avoid blocking."))
                }]
            });
        },
        // Protocol list grid
        buildProtocolList : function() {

            this.gridProtocolList = Ext.create('Ung.EditorGrid',{
                settingsCmp : this,
                name : 'Signatures',
                helpSource : 'protocol_list',
                autoGenerateId: true,
                paginated : false,
                data : this.nodeData.patterns.list,
                emptyRow : {
                    "protocol" : this.i18n._("[no protocol]"),
                    "category" : this.i18n._("[no category]"),
                    "log" : false,
                    "blocked" : false,
                    "description" : this.i18n._("[no description]"),
                    "definition" : this.i18n._("[no signature]")
                },
                title : this.i18n._("Signatures"),
                // the column is autoexpanded if the grid width permits
                recordJavaClass : "com.untangle.node.protofilter.ProtoFilterPattern",
                // from the server
                // the list of fields
                fields : [{
                    name : 'id',
                    type : 'int'
                },{
                    name : 'alert',
                    type : 'boolean'
                },{
                    name : 'metavizeId',
                    type : 'int'
                },{
                    name : 'quality',
                    type : 'string'
                },{
                    name : 'readOnly',
                    type : 'boolean'
                },
                // this field is internationalized so a converter was
                // added
                {
                    name : 'protocol',
                    type : 'string'
                },{
                    name : 'category',
                    type : 'string'
                }, {
                    name : 'log',
                    type : 'boolean'
                }, {
                    name : 'blocked',
                    type : 'boolean'
                }, {
                    name : 'description',
                    type : 'string'
                }, {
                    name : 'definition',
                    type : 'string'
                }],
                // the list of columns for the column model
                columns : [{
                    header : this.i18n._("protocol"),
                    width : 200,
                    dataIndex : 'protocol',
					editor: {
						xtype:'textfield',
						allowBlank:false
					}
                }, 
				{
                    header : this.i18n._("category"),
                    width : 200,
                    dataIndex : 'category',
					editor: {
						xtype:'textfield',
						allowBlank:false
					}
                }, 
				{
					xtype:'checkcolumn',
					header : "<b>" + this.i18n._("block") + "</b>",
					dataIndex : 'blocked',
					fixed : true,
					width:55
				},
				{
					xtype:'checkcolumn',
					header : "<b>" + this.i18n._("log") + "</b>",
					dataIndex : 'log',
					fixed : true,
					width:55
				},
				{
                    header : this.i18n._("description"),
                    width : 200,
                    dataIndex : 'description',
                    flex: 1,
					editor:{
						xtype:'textfield',
                        allowBlank : false
						}
                }],
                sortField : 'category',
                columnsDefaultSortable : true,
                // the row input lines used by the row editor window
                rowEditorInputLines : [{
					xtype:'textfield',
                    name : "Protocol",
                    dataIndex : "protocol",
                    fieldLabel : this.i18n._("Protocol"),
                    allowBlank : false,
                    width : 400
                }, {
					xtype:'textfield',
                    name : "Category",
                    dataIndex : "category",
                    fieldLabel : this.i18n._("Category"),
                    allowBlank : false,
                    width : 400
                }, 
				{
					xtype:'checkbox',
                    name : "Block",
                    dataIndex : "blocked",
                    fieldLabel : this.i18n._("Block")
                },
				{
					xtype:'checkbox',
                    name : "Log",
                    dataIndex : "log",
                    fieldLabel : this.i18n._("Log")
                },
				{
					xtype:'textarea',
                    name : "Description",
                    dataIndex : "description",
                    fieldLabel : this.i18n._("Description"),
                    width : 400,
                    height : 60
                },
				{  
					xtype:'textarea',
                    name : "Signature",
                    dataIndex : "definition",
                    fieldLabel : this.i18n._("Signature"),
                    allowBlank : false,
                    width : 400,
                    height : 60
                }]
            });
        },
        // Event Log
        buildEventLog : function() {
            this.gridEventLog = Ext.create('Ung.GridEventLog',{
                settingsCmp : this,
                fields : [{
                    name : 'time_stamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'blocked',
                    mapping : 'pf_blocked'
                }, {
                    name : 'client',
                    mapping : 'c_client_addr'
                }, {
                    name : 'uid'
                }, {
                    name : 'server',
                    mapping : 'c_server_addr'
                }, {
                    name : 'protocol',
                    type : 'string',
                    mapping : 'pf_protocol'
                }],
                columns : [{
                    header : this.i18n._("timestamp"),
                    width : Ung.Util.timestampFieldWidth,
                    sortable : true,
                    dataIndex : 'time_stamp',
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
                    header : this.i18n._("protocol"),
                    width : 120,
                    sortable : true,
                    flex:1,
                    dataIndex : 'protocol'
                }, {
                    header : this.i18n._("blocked"),
                    width : Ung.Util.booleanFieldWidth,
                    sortable : true,
                    dataIndex : 'blocked'
                }, {
                    header : this.i18n._("server"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'server'
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
        saveAction : function(keepWindowOpen)
        {
            if (this.isDirty() === false)
            {
                if (!keepWindowOpen) { this.closeWindow(); }
                return;
            }

            Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
            this.nodeData.patterns.list = this.gridProtocolList.getFullSaveList();

            this.getRpcNode().setSettings(Ext.bind(function(result, exception)
            {
                Ext.MessageBox.hide();
                    if (!keepWindowOpen)
                    {
                    this.closeWindow();
                    return;
                    }
                this.gridProtocolList.reloadGrid({ data: this.nodeData.patterns.list } );
            },this), this.nodeData);
        }
    });
}

//@ sourceURL=protofilter-settingsNew.js
