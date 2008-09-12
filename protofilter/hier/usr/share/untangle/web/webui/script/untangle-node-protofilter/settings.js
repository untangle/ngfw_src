if (!Ung.hasResource["Ung.Protofilter"]) {
    Ung.hasResource["Ung.Protofilter"] = true;
    Ung.Settings.registerClassName('untangle-node-protofilter', 'Ung.Protofilter');

    Ung.Protofilter = Ext.extend(Ung.Settings, {
        gridProtocolList : null,
        gridEventLog : null,
        // called when the component is rendered
        onRender : function(container, position) {
            // call superclass renderer first
            Ung.Protofilter.superclass.onRender.call(this, container, position);
            // builds the 2 tabs
            this.buildProtocolList();
            this.buildEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.gridProtocolList, this.gridEventLog]);
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
                name : 'Protocol List',
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
                title : this.i18n._("Protocol List"),
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
                            convert : function(v) {
                                return this.i18n._(v)
                            }.createDelegate(this)
                        }, {
                            name : 'protocol'
                        }, {
                            name : 'blocked'
                        }, {
                            name : 'log'
                        },
                        // this field is internationalized so a converter was
                        // added
                        {
                            name : 'description',
                            convert : function(v) {
                                return this.i18n._(v)
                            }.createDelegate(this)
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
                }, {
                    id : 'definition',
                    header : this.i18n._("signature"),
                    width : 200,
                    dataIndex : 'definition',
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
                    name : 'timeStamp'
                }, {
                    name : 'blocked'
                }, {
                    name : 'pipelineEndpoints'
                }, {
                    name : 'protocol'
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
                    width : 70,
                    sortable : true,
                    dataIndex : 'blocked',
                    renderer : function(value) {
                        return value ? this.i18n._("blocked") : this.i18n._("passed");
                    }.createDelegate(this)
                }, {
                    header : this.i18n._("client"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'pipelineEndpoints',
                    renderer : function(value) {
                        return value === null ? "" : value.CClientAddr + ":" + value.CClientPort;
                    }
                }, {
                    header : this.i18n._("request"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'protocol'
                }, {
                    header : this.i18n._("reason for action"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'blocked',
                    renderer : function(value) {
                        return value ? this.i18n._("blocked in block list") : this.i18n._("not blocked in block list");
                    }.createDelegate(this)
                }, {
                    header : this.i18n._("server"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'pipelineEndpoints',
                    renderer : function(value) {
                        return value === null ? "" : value.SServerAddr + ":" + value.SServerPort;
                    }
                }]
            });
        },
        // save function
        saveAction : function() {
            Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
            this.getRpcNode().updateAll(function(result, exception) {
                Ext.MessageBox.hide();
                if (exception) {
                    Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                    return;
                }
                // exit settings screen
                this.cancelAction();
            }.createDelegate(this), this.gridProtocolList.getSaveList());
        }
    });
}