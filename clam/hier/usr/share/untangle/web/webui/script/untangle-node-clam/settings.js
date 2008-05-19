if (!Ung.hasResource["Ung.Clam"]) {
    Ung.hasResource["Ung.Clam"] = true;
    Ung.Settings.registerClassName('untangle-node-clam', 'Ung.Clam');

    Ung.Clam = Ext.extend(Ung.Settings, {
        gridExceptions : null,
        gridEventLog : null,
        // called when the component is rendered
        onRender : function(container, position) {
            // call superclass renderer first
            Ung.Clam.superclass.onRender.call(this, container, position);
            // builds the 4 tabs
            this.buildWeb();
//            this.buildEmail();
//            this.buildFtp();
            this.buildEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelWeb, /*this.panelEmail, this.panelFtp,*/ this.gridEventLog]);
        },
        // Web Panel
        buildWeb : function() {
            this.panelWeb = new Ext.Panel({
                info : 'panelWeb',
                // private fields
                winExtensions : null,
                winMimeTypes : null,
                parentId : this.getId(),

                title : this.i18n._('Web'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Scan HTTP'),
                        hideLabel : true,
                        name : 'scanHttp',
                        checked : this.getBaseSettings().httpConfig.scan,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().httpConfig.scan = checked;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    title : this.i18n._('File Extensions'),
                    buttons : [{
                        info : 'manageExtensionsButton',
                        text : this.i18n._("manage list"),
                        handler : function() {
                            this.panelWeb.onManageExtensions();
                        }.createDelegate(this)
                    }]
                }, {
                    title : this.i18n._('MIME Types'),
                    buttons : [{
                        info : 'manageMimeTypesButton',
                        text : this.i18n._("manage list"),
                        handler : function() {
                            this.panelWeb.onManageMimeTypes();
                        }.createDelegate(this)
                    }]
                }, {
                	title: this.i18n._('Advanced Settings'),
                	checkboxToggle:true,
                	collapsed: true,
                	labelWidth: 150,
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Disable HTTP Resume'),
                        hideLabel : true,
                        name : 'httpDisableResume',
                        checked : this.getBaseSettings().httpDisableResume,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().httpDisableResume = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Scan trickle rate (1-99)'), //TODO add validation
                        name : 'tricklePercent',
                        value : this.getBaseSettings().tricklePercent,
                        width: 25,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().tricklePercent = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }],

                onManageExtensions : function() {
                    if (!this.winExtensions) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildExtensions();
                        this.winExtensions = new Ung.ManageListWindow({
                            breadcrumbs : [{
                                title : i18n._(rpc.currentPolicy.name),
                                action : function() {
                                    this.panelWin.winExtensions.cancelAction();
                                    this.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.node.md.displayName,
                                action : function() {
                                    this.panelWin.winExtensions.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("File Extensions")
                            }],
                            grid : settingsCmp.gridExtensions
                        });
                    }
                    this.winExtensions.show();
                },
                onManageMimeTypes : function() {
                    if (!this.winMimeTypes) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildMimeTypes();
                        this.winMimeTypes = new Ung.ManageListWindow({
                            breadcrumbs : [{
                                title : i18n._(rpc.currentPolicy.name),
                                action : function() {
                                    this.panelWin.winMimeTypes.cancelAction();
                                    this.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.node.md.displayName,
                                action : function() {
                                    this.panelWin.winMimeTypes.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("MIME Types")
                            }],
                            grid : settingsCmp.gridMimeTypes
                        });
                    }
                    this.winMimeTypes.show();
                },
                beforeDestroy : function() {
                    Ext.destroy( this.winExtensions, this.winMimeTypes);
                    Ext.Panel.prototype.beforeDestroy.call(this);
                }
            });
        },
        // File Types
        buildExtensions : function() {
            var liveColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("scan"),
                dataIndex : 'live',
                fixed : true
            });

            this.gridExtensions = new Ung.EditorGrid({
                info : 'gridExtensions',
                settingsCmp : this,
                totalRecords : this.getBaseSettings().extensionsLength,
                emptyRow : {
                    "string" : "undefined type",
                    "live" : true,
                    "name" : this.i18n._("[no description]")
                },
                title : this.i18n._("File Extensions"),
                recordJavaClass : "com.untangle.uvm.node.StringRule",
                proxyRpcFn : this.getRpcNode().getExtensions,
                fields : [{
                    name : 'id'
                }, {
                    name : 'string'
                }, {
                    name : 'live'
                }, {
                    name : 'name',
                    convert : function(v) {
                        return this.i18n._(v)
                    }.createDelegate(this)
                }],
                columns : [{
                    id : 'string',
                    header : this.i18n._("file type"),
                    width : 200,
                    dataIndex : 'string',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, liveColumn, {
                    id : 'name',
                    header : this.i18n._("description"),
                    width : 200,
                    dataIndex : 'name',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                },],
                sortField : 'string',
                columnsDefaultSortable : true,
                autoExpandColumn : 'name',
                plugins : [liveColumn],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "string",
                    fieldLabel : this.i18n._("File Type"),
                    allowBlank : false,
                    width : 200
                }), new Ext.form.Checkbox({
                    name : "live",
                    fieldLabel : this.i18n._("Scan")
                }), new Ext.form.TextArea({
                    name : "name",
                    fieldLabel : this.i18n._("Description"),
                    width : 200,
                    height : 60
                })]
            });
        },
        // MIME Types
        buildMimeTypes : function() {
            var liveColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("scan"),
                dataIndex : 'live',
                fixed : true
            });

            this.gridMimeTypes = new Ung.EditorGrid({
                info : 'gridMimeTypes',
                settingsCmp : this,
                totalRecords : this.getBaseSettings().httpMimeTypesLength,
                emptyRow : {
                    "mimeType" : "undefined type",
                    "live" : true,
                    "name" : this.i18n._("[no description]")
                },
                title : this.i18n._("MIME Types"),
                recordJavaClass : "com.untangle.uvm.node.MimeTypeRule",
                proxyRpcFn : this.getRpcNode().getHttpMimeTypes,
                fields : [{
                    name : 'id'
                }, {
                    name : 'mimeType'
                }, {
                    name : 'live'
                }, {
                    name : 'name',
                    convert : function(v) {
                        return this.i18n._(v)
                    }.createDelegate(this)
                }],
                columns : [{
                    id : 'mimeType',
                    header : this.i18n._("MIME type"),
                    width : 200,
                    dataIndex : 'mimeType',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, liveColumn, {
                    id : 'name',
                    header : this.i18n._("description"),
                    width : 200,
                    dataIndex : 'name',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                },],
                sortField : 'mimeType',
                columnsDefaultSortable : true,
                autoExpandColumn : 'name',
                plugins : [liveColumn],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "mimeType",
                    fieldLabel : this.i18n._("MIME Type"),
                    allowBlank : false,
                    width : 200
                }), new Ext.form.Checkbox({
                    name : "live",
                    fieldLabel : this.i18n._("Scan")
                }), new Ext.form.TextArea({
                    name : "name",
                    fieldLabel : this.i18n._("Description"),
                    width : 200,
                    height : 60
                })]
            });
        },        
        // Event Log
        buildEventLog : function() {
            this.gridEventLog = new Ung.GridEventLog({
                settingsCmp : this,
                //autoExpandColumn: 'timeStamp',

                // the list of fields
                fields : [{
                    name : 'timeStamp'
                }, {
                    name : 'type'
                }, {
                    name : 'actionType'
                }, {
                    name : 'pipelineEndpoints'
                }, {
                    name : 'traffic'
                }, {
                    name : 'infected'
                }],
                // the list of columns
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
                    dataIndex : 'actionType',
                    renderer : function(value, metadata, record ) {
						switch (record.data.type) {
							case 'HTTP' :
							case 'FTP' :
								switch (value) {
									case 0 : // PASSED
										return this.i18n._("clean");
									case 1 : // CLEANED
										return this.i18n._("cleaned");
									default :
									case 2 : // BLOCKED
										return this.i18n._("blocked");
								}
								break;
							case 'POP/IMAP' :
								switch (value) {
									case 0 : // PASSED
										return this.i18n._("pass message");
									default :
									case 1 : // CLEANED
										return this.i18n._("remove infection");
								}
								break;
							case 'SMTP' :
								switch (value) {
									case 0 : // PASSED
										return this.i18n._("pass message");
									case 1 : // CLEANED
										return this.i18n._("remove infection");
									default :
									case 2 : // BLOCKED
										return this.i18n._("block message");
								}
                                break;
						}
						return "";
                    }.createDelegate(this)
                }, {
                    header : this.i18n._("client"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'pipelineEndpoints',
                    renderer : function(value) {
                        return value === null ? "" : value.CClientAddr.hostAddress + ":" + value.CClientPort;
                    }
                }, {
                    header : this.i18n._("traffic"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'traffic'
                }, {
                    header : this.i18n._("reason for") + "<br>" + this.i18n._("action"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'infected',
                    renderer : function(value) {
                        return value ? this.i18n._("virus found") : this.i18n._("no virus found");
                    }.createDelegate(this)
                }, {
                    header : this.i18n._("server"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'pipelineEndpoints',
                    renderer : function(value) {
                        return value === null ? "" : value.SServerAddr.hostAddress + ":" + value.SServerPort;
                    }
                }]
            });
        },
        // save function
        save : function() {
            if (this.validate()) {
                // disable tabs during save
                this.tabs.disable();
                this.getRpcNode().updateAll(function(result, exception) {
                    // re-enable tabs
                    this.tabs.enable();
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    // exit settings screen
                    this.cancelAction();
                }.createDelegate(this), this.getBaseSettings(), 
                        this.gridMimeTypes ? this.gridMimeTypes.getSaveList() : null,
                        this.gridExtensions ? this.gridExtensions.getSaveList() : null);
            }
        }
    });
}