if (!Ung.hasResource["Ung.Virus"]) {
    Ung.hasResource["Ung.Virus"] = true;
    Ung.NodeWin.registerClassName('untangle-base-virus', 'Ung.Virus');

    Ung.Virus = Ext.extend(Ung.NodeWin, {
        panelWeb:null,
        panelEmail: null,
        panelFtp: null,
        gridWebEventLog : null,
        gridMailEventLog : null,
        // override get base settings object to reload the signature information.
        getBaseSettings : function(forceReload) {
            if (forceReload || this.rpc.baseSettings === undefined) {
                try {
                    this.rpc.baseSettings = this.getRpcNode().getBaseSettings(true);
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
            }
            return this.rpc.baseSettings;
        },
        // called when the component is rendered
        initComponent : function() {
            // keep initial base settings
            this.initialBaseSettings = Ung.Util.clone(this.getBaseSettings());
            
            this.buildWeb();
            this.buildEmail();
            this.buildFtp();
            this.buildWebEventLog();
            this.buildMailEventLog();
            
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelWeb, this.panelEmail, this.panelFtp, this.gridWebEventLog, this.gridMailEventLog]);
            Ung.Virus.superclass.initComponent.call(this);
        },
        // Web Panel
        buildWeb : function() {
            this.panelWeb = new Ext.Panel({
                name : 'Web',
                helpSource : 'web',
                // private fields
                winExtensions : null,
                winMimeTypes : null,
                parentId : this.getId(),

                title : this.i18n._('Web'),
                layout : "form",
                cls: 'ung-panel',
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
                        name : 'Scan HTTP',
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
                    title: this.i18n._('Advanced Settings'),
                    collapsible: true,
                    collapsed: true,
                    labelWidth: 170,
                    items : [{
                        xtype : 'button',
                        name : 'File Extensions',
                        text : this.i18n._('File Extensions'),
                        style : 'padding-bottom:10px;',
                        handler : function() {
                            this.panelWeb.onManageExtensions();
                        }.createDelegate(this)
                    }, {
                        xtype : 'button',
                        name : 'MIME Types',
                        text : this.i18n._('MIME Types'),
                        style : 'padding-bottom:10px;',
                        handler : function() {
                            this.panelWeb.onManageMimeTypes();
                        }.createDelegate(this)
                    }, {
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Disable HTTP Resume'),
                        hideLabel : true,
                        name : 'Disable HTTP Resume',
                        checked : this.getBaseSettings().httpDisableResume,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().httpDisableResume = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Scan trickle rate (1-99)'),
                        name : 'Scan trickle rate',
                        id: 'virus_http_trickle_percent',
                        value : this.getBaseSettings().tricklePercent,
                        width: 25,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 1,                        
                        maxValue: 99,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    var tricklePercentFtpCmp = Ext.getCmp('virus_ftp_trickle_percent');
                                    tricklePercentFtpCmp.setValue(newValue);
                                    this.getBaseSettings().tricklePercent = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    cls: 'description',
                    html : this.i18n._("Virus Blocker signatures were last updated") + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                            + ((this.getBaseSettings().lastUpdate != null) ? i18n.timestampFormat(this.getBaseSettings().lastUpdate) : 
                            this.i18n._("Unknown"))
                }],

                onManageExtensions : function() {
                    if (!this.winExtensions) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildExtensions();
                        this.winExtensions = new Ung.ManageListWindow({
                            breadcrumbs : [{
                                title : i18n._(rpc.currentPolicy.name),
                                action : function() {
                                    Ung.Window.cancelAction(
                                       this.gridExtensions.isDirty() || this.isDirty(),
                                       function() {
                                            this.panelWeb.winExtensions.closeWindow();
                                            this.closeWindow();
                                       }.createDelegate(this)
                                    );
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.node.displayName,
                                action : function() {
                                    this.panelWeb.winExtensions.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Web"),
                                action : function() {
                                    this.panelWeb.winExtensions.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("File Extensions")
                            }],
                            grid : settingsCmp.gridExtensions,
                            applyAction : function(callback){
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                var saveList = settingsCmp.gridExtensions.getSaveList();
                                settingsCmp.getRpcNode().updateExtensions(function(result, exception) {
                                    if(Ung.Util.handleException(exception)){
                                        Ext.MessageBox.hide();
                                        return;
                                    }
                                    this.getRpcNode().getBaseSettings(function(result2,exception2){
                                        Ext.MessageBox.hide();                                                
                                        if(Ung.Util.handleException(exception2)){
                                            return;
                                        }
                                        this.gridExtensions.setTotalRecords(result2.extensionsLength);
                                        this.gridExtensions.reloadGrid();
                                        if(callback != null) {
                                            callback();
                                        }
                                    }.createDelegate(this));
                                }.createDelegate(settingsCmp), saveList[0],saveList[1],saveList[2]);
                            }    
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
                                    Ung.Window.cancelAction(
                                       this.gridMimeTypes.isDirty() || this.isDirty(),
                                       function() {
                                            this.panelWeb.winMimeTypes.closeWindow();
                                            this.closeWindow();
                                       }.createDelegate(this)
                                    );
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.node.displayName,
                                action : function() {
                                    this.panelWeb.winMimeTypes.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Web"),
                                action : function() {
                                    this.panelWeb.winMimeTypes.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("MIME Types")
                            }],
                            grid : settingsCmp.gridMimeTypes,
                            applyAction : function(callback){
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                var saveList = settingsCmp.gridMimeTypes.getSaveList();
                                settingsCmp.getRpcNode().updateHttpMimeTypes(function(result, exception) {
                                    if(Ung.Util.handleException(exception)){
                                        Ext.MessageBox.hide();
                                        return;
                                    }
                                    this.getRpcNode().getBaseSettings(function(result2,exception2){
                                        Ext.MessageBox.hide();                                                
                                        if(Ung.Util.handleException(exception2)){
                                            return;
                                        }
                                        this.gridMimeTypes.setTotalRecords(result2.httpMimeTypesLength);
                                        this.gridMimeTypes.reloadGrid();
                                        if(callback != null) {
                                            callback();
                                        }
                                    }.createDelegate(this));
                                }.createDelegate(settingsCmp), saveList[0],saveList[1],saveList[2]);
                            } 
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
                name : 'File Extensions',
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
                    name : 'string',
                    type : 'string'
                }, {
                    name : 'live'
                }, {
                    name : 'name',
                    type : 'string'
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
                }],
                sortField : 'string',
                columnsDefaultSortable : true,
                autoExpandColumn : 'name',
                plugins : [liveColumn],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "File Type",
                    dataIndex : "string",
                    fieldLabel : this.i18n._("File Type"),
                    allowBlank : false,
                    width : 200
                }), new Ext.form.Checkbox({
                    name : "Scan",
                    dataIndex : "live",
                    fieldLabel : this.i18n._("Scan")
                }), new Ext.form.TextArea({
                    name : "Description",
                    dataIndex : "name",
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
                name : 'MIME Types',
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
                    name : 'mimeType',
                    type : 'string'
                }, {
                    name : 'live'
                }, {
                    name : 'name',
                    type : 'string'
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
                }],
                sortField : 'mimeType',
                columnsDefaultSortable : true,
                autoExpandColumn : 'name',
                plugins : [liveColumn],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "MIME Type",
                    dataIndex : "mimeType",
                    fieldLabel : this.i18n._("MIME Type"),
                    allowBlank : false,
                    width : 200
                }), new Ext.form.Checkbox({
                    name : "Scan",
                    dataIndex : "live",
                    fieldLabel : this.i18n._("Scan")
                }), new Ext.form.TextArea({
                    name : "Description",
                    dataIndex : "name",
                    fieldLabel : this.i18n._("Description"),
                    width : 200,
                    height : 60
                })]
            });
        },        
        // Ftp Panel
        buildFtp : function() {
            this.panelFtp = new Ext.Panel({
                name : 'FTP',
                helpSource : 'ftp',
                // private fields
                parentId : this.getId(),

                title : this.i18n._('FTP'),
                layout : "form",
                cls: 'ung-panel',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Scan FTP'),
                        hideLabel : true,
                        name : 'Scan FTP',
                        checked : this.getBaseSettings().ftpConfig.scan,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().ftpConfig.scan = checked;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    title: this.i18n._('Advanced Settings'),
                    collapsible: true,
                    collapsed: true,
                    labelWidth: 170,
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Disable FTP Resume'),
                        hideLabel : true,
                        name : 'Disable FTP Resume',
                        checked : this.getBaseSettings().ftpDisableResume,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().ftpDisableResume = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Scan trickle rate'),
                        name : 'Scan trickle rate (1-99)',
                        id: 'virus_ftp_trickle_percent',
                        value : this.getBaseSettings().tricklePercent,
                        width: 25,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 1,                        
                        maxValue: 99,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    var tricklePercentHttpCmp = Ext.getCmp('virus_http_trickle_percent');
                                    tricklePercentHttpCmp.setValue(newValue);
                                    this.getBaseSettings().tricklePercent = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    cls: 'description',
                    html : this.i18n._("Virus Blocker signatures were last updated") + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                            + ((this.getBaseSettings().lastUpdate != null) ? i18n.timestampFormat(this.getBaseSettings().lastUpdate) : 
                            this.i18n._("Unknown"))
                }]

            });
        },
        // Email Panel
        buildEmail : function() {
            this.panelEmail = new Ext.Panel({
                name : 'Email',
                helpSource : 'email',
                // private fields
                parentId : this.getId(),

                title : this.i18n._('Email'),
                layout : "anchor",
                defaults: {
                    anchor: '98%',
                    xtype : 'fieldset',
                    autoHeight : true,
                    autoScroll: true,
                    buttonAlign : 'left'
                },
                cls: 'ung-panel',
                autoScroll : true,
                items : [{
                    layout:'column',
                    items:[{
                        columnWidth:.3,
                        layout: 'form',
                        border:false,
                        items: [{
                            xtype : 'checkbox',
                            boxLabel : this.i18n._('Scan SMTP'),
                            hideLabel : true,
                            name : 'Scan SMTP',
                            checked : this.getBaseSettings().smtpConfig.scan,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getBaseSettings().smtpConfig.scan = checked;
                                    }.createDelegate(this)
                                }
                            }
                        }, {
                            xtype : 'checkbox',
                            boxLabel : this.i18n._('Scan POP3'),
                            hideLabel : true,
                            name : 'Scan POP3',
                            checked : this.getBaseSettings().popConfig.scan,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getBaseSettings().popConfig.scan = checked;
                                    }.createDelegate(this)
                                }
                            }
                        }, {
                            xtype : 'checkbox',
                            boxLabel : this.i18n._('Scan IMAP'),
                            hideLabel : true,
                            name : 'Scan IMAP',
                            checked : this.getBaseSettings().imapConfig.scan,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getBaseSettings().imapConfig.scan = checked;
                                    }.createDelegate(this)
                                }
                            }
                        }]
                    },{
                        columnWidth:.7,
                        layout: 'form',
                        border:false,
                        items: [{
                            xtype : 'combo',
                            name : 'SMTP Action',
                            editable : false,
                            fieldLabel : this.i18n._('Action'),
                            mode : 'local',
                            triggerAction : 'all',
                            listClass : 'x-combo-list-small',
                            store : new Ext.data.SimpleStore({
                                fields : ['key', 'name'],
                                data : [["PASS", this.i18n._("pass message")], 
                                        ["REMOVE", this.i18n._("remove infection")],
                                        ["BLOCK", this.i18n._("block message")]]
                            }),
                            displayField : 'name',
                            valueField : 'key',
                            value : this.getBaseSettings().smtpConfig.msgAction,
                            listeners : {
                                "change" : {
                                    fn : function(elem, newValue) {
                                        this.getBaseSettings().smtpConfig.msgAction = newValue;
                                    }.createDelegate(this)
                                }
                            }
                        },{
                            xtype : 'combo',
                            name : 'POP3 Action',
                            editable : false,
                            fieldLabel : this.i18n._('Action'),
                            mode : 'local',
                            triggerAction : 'all',
                            listClass : 'x-combo-list-small',
                            store : new Ext.data.SimpleStore({
                                fields : ['key', 'name'],
                                data : [["PASS", this.i18n._("pass message")], 
                                        ["REMOVE", this.i18n._("remove infection")]]
                            }),
                            displayField : 'name',
                            valueField : 'key',
                            value : this.getBaseSettings().popConfig.msgAction,
                            listeners : {
                                "change" : {
                                    fn : function(elem, newValue) {
                                        this.getBaseSettings().popConfig.msgAction = newValue;
                                    }.createDelegate(this)
                                }
                            }
                        },{
                            xtype : 'combo',
                            name : 'IMAP Action',
                            editable : false,
                            fieldLabel : this.i18n._('Action'),
                            mode : 'local',
                            triggerAction : 'all',
                            listClass : 'x-combo-list-small',
                            store : new Ext.data.SimpleStore({
                                fields : ['key', 'name'],
                                data : [["PASS", this.i18n._("pass message")], 
                                        ["REMOVE", this.i18n._("remove infection")]]
                            }),
                            displayField : 'name',
                            valueField : 'key',
                            value : this.getBaseSettings().imapConfig.msgAction,
                            listeners : {
                                "change" : {
                                    fn : function(elem, newValue) {
                                        this.getBaseSettings().imapConfig.msgAction = newValue;
                                    }.createDelegate(this)
                                }
                            }
                        }]
                    }]
                }, {
                    cls: 'description',
                    html : this.i18n._("Virus Blocker signatures were last updated") + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                            + ((this.getBaseSettings().lastUpdate != null) ? i18n.timestampFormat(this.getBaseSettings().lastUpdate) : 
                            this.i18n._("Unknown"))
                }]

            });
        },
        // Event Log
        buildWebEventLog : function() {
            this.gridWebEventLog = new Ung.GridEventLog({
                name : 'Web Event Log',
                helpSource : 'Web_Event_Log',
                settingsCmp : this,
                title : this.i18n._("Web Event Log"),
                eventQueriesFn : this.getRpcNode().getWebEventQueries,

                // the list of fields
                fields : [{
                    name : 'timeStamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'client',
                    mapping : 'CClientAddr'
                }, {
                    name : 'uid'
                }, {
                    name : 'server',
                    mapping : 'CServerAddr'
                }, {
                    name : 'host',
                    mapping : 'host'
                }, {
                    name : 'uri',
                    mapping : 'uri'
                }, {
                    name : 'location'
                }, {
                    name : 'reason',
                    mapping : 'virus' + main.capitalize(this.getRpcNode().getVendor()) + 'Name'
                }],
                // the list of columns
                autoExpandColumn: 'uri',
                columns : [{
                    header : this.i18n._("timestamp"),
                    width : Ung.Util.timestampFieldWidth,
                    sortable : true,
                    dataIndex : 'timeStamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    id : 'client',
                    header : this.i18n._("client"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'client'
                }, {
                    id : 'username',
                    header : this.i18n._("username"),
                    width : Ung.Util.usernameFieldWidth,
                    sortable : true,
                    dataIndex : 'uid'
                }, {
                    id: 'host',
                    header : this.i18n._("host"),
                    width : Ung.Util.hostnameFieldWidth,
                    dataIndex : 'host'
                }, {
                    id: 'uri',
                    header : this.i18n._("uri"),
                    width : Ung.Util.uriFieldWidth,
                    dataIndex : 'uri'
                }, {
                    id : 'reason',
                    header : this.i18n._("virus name"),
                    width : 140,
                    sortable : true,
                    dataIndex : 'reason'
                }, {
                    id : 'server',
                    header : this.i18n._("server"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'server'
                }]
            });
        },
        // Event Log
        buildMailEventLog : function() {
            this.gridMailEventLog = new Ung.GridEventLog({
                name : 'Email Event Log',
                helpSource : 'Email_Event_Log',
                settingsCmp : this,
                title : this.i18n._("Email Event Log"),
                eventQueriesFn : this.getRpcNode().getMailEventQueries,

                // the list of fields
                fields : [{
                    name : 'timeStamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'client',
                    mapping : 'CClientAddr'
                }, {
                    name : 'uid'
                }, {
                    name : 'server',
                    mapping : 'CServerAddr'
                }, {
                    name : 'subject',
                    type : 'string'
                }, {
                    name : 'addr',
                    type : 'string'
                }, {
                    name : 'sender',
                    type : 'string'
                }, {
                    name : 'reason',
                    mapping : 'virus' + main.capitalize(this.getRpcNode().getVendor()) + 'Name'
                }],
                // the list of columns
                autoExpandColumn: 'subject',
                columns : [{
                    header : this.i18n._("timestamp"),
                    width : Ung.Util.timestampFieldWidth,
                    sortable : true,
                    dataIndex : 'timeStamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    id : 'client',
                    header : this.i18n._("client"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'client'
                }, {
                    header : this.i18n._("receiver"),
                    width : Ung.Util.emailFieldWidth,
                    sortable : true,
                    dataIndex : 'addr'
                }, {
                    header : this.i18n._("sender"),
                    width : Ung.Util.emailFieldWidth,
                    sortable : true,
                    dataIndex : 'sender'
                }, {
                    id : 'subject',
                    header : this.i18n._("subject"),
                    width : 150,
                    sortable : true,
                    dataIndex : 'subject'
                }, {
                    id : 'reason',
                    header : this.i18n._("virus name"),
                    width : 140,
                    sortable : true,
                    dataIndex : 'reason'
                }, {
                    id : 'server',
                    header : this.i18n._("server"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'server'
                }]
            });
        },
        // validation function
        validateClient : function() {
            //validate trickle rate
            var tricklePercentCmp = Ext.getCmp('virus_http_trickle_percent');
            if (tricklePercentCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("Scan trickle rate should be between 1 and 99!"),
                    function () {
                        this.tabs.activate(this.panelWeb);
                        tricklePercentCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.getRpcNode().updateAll(function(result, exception) {
                    Ext.MessageBox.hide();
                    if(Ung.Util.handleException(exception)) return;
                    // exit settings screen
                    this.closeWindow();
                }.createDelegate(this), this.getBaseSettings(), 
                        this.gridMimeTypes ? this.gridMimeTypes.getSaveList() : null,
                        this.gridExtensions ? this.gridExtensions.getSaveList() : null);
            }
        },
        isDirty : function() {
            return !Ung.Util.equals(this.getBaseSettings(), this.initialBaseSettings)
                || (this.gridMimeTypes ? this.gridMimeTypes.isDirty() : false)
                || (this.gridExtensions ? this.gridExtensions.isDirty() : false);
        }
    });
}
