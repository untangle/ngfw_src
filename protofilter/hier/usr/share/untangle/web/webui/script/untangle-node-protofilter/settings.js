if (!Ung.hasResource["Ung.Protofilter"]) {
    Ung.hasResource["Ung.Protofilter"] = true;
    Ung.NodeWin.registerClassName('untangle-node-protofilter', 'Ung.Protofilter');

    Ung.Protofilter = Ext.extend(Ung.NodeWin, {
        nodeData: null,
        panelStatus: null,
        gridProtocolList : null,
        gridEventLog : null,
        nodeData : null,
        initComponent : function() {
            this.nodeData = this.getRpcNode().getNodeSettings();
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
                    html : String.format(this.i18n._("Application Control Lite uses signatures to detect the protocols of network traffic. It is useful for detecting unwanted or interesting protocols in use on the network."))
                }, {
                    title : this.i18n._(' '),
                    layout:'form',
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
                paginated : false,
                autoGenerateId: true,
                // the total records is set from the base settings
                // patternsLength field
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
                autoExpandColumn : 'description',
                recordJavaClass : "com.untangle.node.protofilter.ProtoFilterPattern",
                // this is the function used by Ung.RpcProxy to retrive data
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
                    id : 'protocol',
                    header : this.i18n._("protocol"),
                    width : 200,
                    dataIndex : 'protocol',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, {
                    id : 'category',
                    header : this.i18n._("category"),
                    width : 200,
                    dataIndex : 'category',
                    // this is a simple text editor
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
                plugins : [logColumn, blockedColumn],
                // the row input lines used by the row editor window
                rowEditorInputLines : [ new Ext.form.TextField({
                    name : "Protocol",
                    dataIndex : "protocol",
                    fieldLabel : this.i18n._("Protocol"),
                    allowBlank : false,
                    width : 200
                }), new Ext.form.TextField({
                    name : "Category",
                    dataIndex : "category",
                    fieldLabel : this.i18n._("Category"),
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
            this.gridEventLog = new Ung.GridEventLog({
                settingsCmp : this,
                fields : [{
                    name : 'timeStamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'blocked',
                    mapping : 'pfBlocked'
                }, {
                    name : 'client',
                    mapping : 'CClientAddr'
                }, {
                    name : 'uid'
                }, {
                    name : 'server',
                    mapping : 'CServerAddr'
                }, {
                    name : 'protocol',
                    type : 'string',
                    mapping : 'pfProtocol'
                }],
                autoExpandColumn : 'protocol',
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
                    id : 'protocol',
                    header : this.i18n._("protocol"),
                    width : 120,
                    sortable : true,
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

            this.getRpcNode().setNodeSettings(function(result, exception)
            {
                Ext.MessageBox.hide();
                    if (!keepWindowOpen)
                    {
                    this.closeWindow();
                    return;
                    }
                this.gridProtocolList.reloadGrid({ data: this.nodeData.patterns.list } );
            }.createDelegate(this), this.nodeData);
        }
    });
}

