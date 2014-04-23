if (!Ung.hasResource["Ung.Virus"]) {
    Ung.hasResource["Ung.Virus"] = true;
    Ung.NodeWin.registerClassName('untangle-base-virus', 'Ung.Virus');

    Ext.define('Ung.Virus', {
        extend:'Ung.NodeWin',
        panelWeb:null,
        panelEmail: null,
        panelFtp: null,
        gridWebEventLog: null,
        gridMailEventLog: null,
        gridFtpEventLog: null,
        // called when the component is rendered
        initComponent: function() {
            this.lastUpdate = this.getRpcNode().getLastSignatureUpdate();
            this.nodeName=this.getRpcNode().getName();
            this.buildWeb();
            this.buildEmail();
            this.buildFtp();
            this.buildPassSites();
            this.buildWebEventLog();
            this.buildMailEventLog();
            this.buildFtpEventLog();
            
            // builds the tab panel with the tabs
            this.buildTabPanel([
                this.panelWeb, 
                this.panelEmail, 
                this.panelFtp, 
                this.panelPassSites,
                this.gridWebEventLog, 
                this.gridMailEventLog, 
                this.gridFtpEventLog
            ]);
            this.callParent(arguments);
        },

        save: function( isApply ){
            var settingsCmp = this;
            for( var i = 0; i < this.tabs.items.items.length; i++ ){
                var panel = this.tabs.items.items[i];
                for( var j = 0; j < panel.items.items.length; j++ ){
                    var cmp = panel.items.items[j];
                    if( cmp.getList ){
                        cmp.getList( function( saveList ){
                            settingsCmp.settings[cmp.dataProperty] = saveList;
                        }, true);
                    }
                }
            }
            this.callParent( arguments );
        },

        // Web Panel
        buildWeb: function() {
            this.panelWeb = Ext.create('Ext.panel.Panel',{
                name: 'Web',
                //helpSource: 'virus_blocker_web',
                //helpSource: 'virus_blocker_lite_web',
                helpSource: this.helpSourceName + '_web',
                // private fields
                winExtensions: null,
                winMimeTypes: null,
                winPassSites: null,
                parentId: this.getId(),

                title: this.i18n._('Web'),
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    xtype: 'fieldset',
                    buttonAlign: 'left'
                },
                items: [{
                    items: [{
                        xtype: 'checkbox',
                        boxLabel: this.i18n._('Scan HTTP'),
                        hideLabel: true,
                        name: 'Scan HTTP',
                        checked: this.settings.scanHttp,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    this.settings.scanHttp = checked;
                                }, this)
                            }
                        }
                    }]
                }, {
                    title: this.i18n._('Advanced Settings'),
                    collapsible: true,
                    collapsed: true,
                    labelWidth: 370,
                    items: [{
                        xtype:'fieldset',
                        items:    {
                            xtype: 'button',
                            name: 'File Extensions',
                            text: this.i18n._('Edit File Extensions'),
                            handler: Ext.bind(function() {
                                this.panelWeb.onManageExtensions();
                            }, this)
                        }
                    },{
                        xtype:'fieldset',
                        items: {
                            xtype: 'button',
                            name: 'MIME Types',
                            text: this.i18n._('Edit MIME Types'),
                            handler: Ext.bind(function() {
                                this.panelWeb.onManageMimeTypes();
                            }, this)
                        }
                    }]
                }, {
                    cls: 'description',
                    html: this.i18n._("Virus Blocker signatures were last updated") + ":&nbsp;&nbsp;&nbsp;&nbsp;" +
                        (this.lastUpdate != null && this.lastUpdate.time != 0 ? i18n.timestampFormat(this.lastUpdate): i18n._("never"))
                }],

                onManageExtensions: function() {
                    if (!this.winExtensions) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildExtensions();
                        this.winExtensions = Ext.create('Ung.ManageListWindow', {
                            breadcrumbs: [{
                                title: i18n._(rpc.currentPolicy.name),
                                action: Ext.bind(function() {
                                    Ung.Window.cancelAction(
                                       this.gridExtensions.isDirty() || this.isDirty(),
                                       Ext.bind(function() {
                                            this.panelWeb.winExtensions.closeWindow();
                                            this.closeWindow();
                                       }, this)
                                    );
                                },settingsCmp)
                            }, {
                                title: settingsCmp.node.displayName,
                                action: Ext.bind(function() {
                                    this.panelWeb.winExtensions.cancelAction();
                                },settingsCmp)
                            }, {
                                title: settingsCmp.i18n._("Web"),
                                action: Ext.bind(function() {
                                    this.panelWeb.winExtensions.cancelAction();
                                },settingsCmp)
                            }, {
                                title: settingsCmp.i18n._("File Extensions")
                            }],
                            grid: settingsCmp.gridExtensions,
                            applyAction: function(callback) {
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridExtensions.getList(Ext.bind(function(saveList) {
                                    this.getRpcNode().setHttpFileExtensions(Ext.bind(function(result, exception) {
                                        Ext.MessageBox.hide();
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getSettings(Ext.bind(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.settings.httpFileExtensions = result.httpFileExtensions;
                                            this.gridExtensions.clearDirty();
                                            if(callback != null) {
                                                callback();
                                            }
                                        }, this));
                                    }, this), saveList);
                                },settingsCmp));
                            }
                        });
                    }
                    this.winExtensions.show();
                },
                onManageMimeTypes: function() {
                    if (!this.winMimeTypes) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildMimeTypes();
                        this.winMimeTypes = Ext.create('Ung.ManageListWindow',{
                            breadcrumbs: [{
                                title: i18n._(rpc.currentPolicy.name),
                                action: Ext.bind(function() {
                                    Ung.Window.cancelAction(
                                       this.gridMimeTypes.isDirty() || this.isDirty(),
                                       Ext.bind(function() {
                                            this.panelWeb.winMimeTypes.closeWindow();
                                            this.closeWindow();
                                       }, this)
                                    );
                                },settingsCmp)
                            }, {
                                title: settingsCmp.node.displayName,
                                action: Ext.bind(function() {
                                    this.panelWeb.winMimeTypes.cancelAction();
                                },settingsCmp)
                            }, {
                                title: settingsCmp.i18n._("Web"),
                                action: Ext.bind(function() {
                                    this.panelWeb.winMimeTypes.cancelAction();
                                },settingsCmp)
                            }, {
                                title: settingsCmp.i18n._("MIME Types")
                            }],
                            grid: settingsCmp.gridMimeTypes,
                            applyAction: function(callback) {
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridMimeTypes.getList(Ext.bind(function(saveList) {
                                    this.getRpcNode().setHttpMimeTypes(Ext.bind(function(result, exception) {
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getSettings(Ext.bind(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.settings.httpMimeTypes = result.httpMimeTypes;
                                            this.gridMimeTypes.clearDirty();
                                            if(callback != null) {
                                                callback();
                                            }
                                        }, this));
                                    }, this), saveList);
                                },settingsCmp));
                            }
                        });
                    }
                    this.winMimeTypes.show();
                },
                onManagePassSites: function() {
                    if (!this.winPassSites) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildPassSites();
                        this.winPassSites = Ext.create('Ung.ManageListWindow',{
                            breadcrumbs: [{
                                title: i18n._(rpc.currentPolicy.name),
                                action: Ext.bind(function() {
                                    Ung.Window.cancelAction(
                                       this.gridPassSites.isDirty() || this.isDirty(),
                                       Ext.bind(function() {
                                            this.panelWeb.winPassSites.closeWindow();
                                            this.closeWindow();
                                       }, this)
                                    );
                                },settingsCmp)
                            }, {
                                title: settingsCmp.node.displayName,
                                action: Ext.bind(function() {
                                    this.panelWeb.winPassSites.cancelAction();
                                },settingsCmp)
                            }, {
                                title: settingsCmp.i18n._("Web"),
                                action: Ext.bind(function() {
                                    this.panelWeb.winPassSites.cancelAction();
                                },settingsCmp)
                            }, {
                                title: settingsCmp.i18n._("Pass Sites")
                            }],
                            grid: settingsCmp.gridPassSites,
                            applyAction: function(callback) {
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridPassSites.getList(Ext.bind(function(saveList) {
                                    this.getRpcNode().setPassSites(Ext.bind(function(result, exception) {
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getSettings(Ext.bind(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.settings.passSites = result.passSites;
                                            this.gridPassSites.clearDirty();
                                            if(callback != null) {
                                                callback();
                                            }
                                        }, this));
                                    }, this), saveList);
                                },settingsCmp));
                            }
                        });
                    }
                    this.winPassSites.show();
                },
                beforeDestroy: function() {
                    Ext.destroy( 
                        this.winExtensions, 
                        this.winMimeTypes, 
                        this.winPassSites
                    );
                    Ext.Panel.prototype.beforeDestroy.call(this);
                }
            });
        },
        // File Types
        buildExtensions: function() {
            this.gridExtensions = Ext.create('Ung.EditorGrid',{
                name: 'File Extensions',
                settingsCmp: this,
                emptyRow: {
                    "string": "undefined type",
                    "enabled": true,
                    "name": this.i18n._("[no description]")
                },
                title: this.i18n._("File Extensions"),
                recordJavaClass: "com.untangle.uvm.node.GenericRule",
                dataProperty: "httpFileExtensions",
                fields: Ung.Util.getGenericRuleFields(this),
                columns: [{
                    header: this.i18n._("File Type"),
                    width: 200,
                    dataIndex: 'string',
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }, 
                {
                    xtype:'checkcolumn',
                    header: this.i18n._("Scan"),
                    dataIndex: 'enabled',
                    resizable: false,
                    width:55
                }, {
                    header: this.i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex: 1,
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }],
                sortField: 'string',
                columnsDefaultSortable: true,
                rowEditorInputLines: [
                {
                    xtype:'textfield',
                    name: "File Type",
                    dataIndex: "string",
                    fieldLabel: this.i18n._("File Type"),
                    allowBlank: false,
                    width: 400
                },
                {
                    xtype:'checkbox',
                    name: "Scan",
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Scan")
                },
                {
                    xtype:'textarea',
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    width: 400,
                    height: 60
                }]
            });
        },
        // MIME Types
        buildMimeTypes: function() {
            this.gridMimeTypes = Ext.create('Ung.EditorGrid', {
                name: 'MIME Types',
                settingsCmp: this,
                emptyRow: {
                    "string": "undefined type",
                    "enabled": true,
                    "name": this.i18n._("[no description]")
                },
                title: this.i18n._("MIME Types"),
                recordJavaClass: "com.untangle.uvm.node.GenericRule",
                dataProperty: "httpMimeTypes",
                fields: Ung.Util.getGenericRuleFields(this),
                columns: [{
                    header: this.i18n._("MIME Type"),
                    width: 200,
                    dataIndex: 'string',
                    editor:{
                        xtype:'textfield',
                        allowBlank:false
                    }
                }, {
                    xtype:'checkcolumn',
                    header: this.i18n._("Scan"),
                    dataIndex: 'enabled',
                    resizable: false,
                    width:55
                }, {
                    header: this.i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex: 1,
                    field: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }],
                sortField: 'string',
                columnsDefaultSortable: true,
                rowEditorInputLines: [
                {
                    xtype:'textfield',
                    name: "MIME Type",
                    dataIndex: "string",
                    fieldLabel: this.i18n._("MIME Type"),
                    allowBlank: false,
                    width: 400
                },
                {    xtype:'checkbox',
                    name: "Scan",
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Scan")
                },
                {
                    xtype:'textarea',
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    width: 400,
                    height: 60
                }]
            });
        },        
        // Ftp Panel
        buildFtp: function() {
            this.panelFtp = Ext.create('Ext.panel.Panel',{
                name: 'FTP',
                //helpSource: 'virus_blocker_ftp',
                //helpSource: 'virus_blocker_lite_ftp',
                helpSource: this.helpSourceName + '_ftp',
                parentId: this.getId(),
                title: this.i18n._('FTP'),
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    xtype: 'fieldset',
                    buttonAlign: 'left'
                },
                items: [{
                    items: [{
                        xtype: 'checkbox',
                        boxLabel: this.i18n._('Scan FTP'),
                        hideLabel: true,
                        name: 'Scan FTP',
                        checked: this.settings.scanFtp,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    this.settings.scanFtp = checked;
                                }, this)
                            }
                        }
                    }]
                }, {
                    cls: 'description',
                    html: this.i18n._("Virus Blocker signatures were last updated") + ":&nbsp;&nbsp;&nbsp;&nbsp;" +
                        ((this.getRpcNode().getLastSignatureUpdate() != null) ? i18n.timestampFormat(this.getRpcNode().getLastSignatureUpdate()): this.i18n._("Unknown"))
                }]

            });
        },
        // Email Panel
        buildEmail: function() {
            this.panelEmail = Ext.create('Ext.panel.Panel',{
                name: 'Email',
                //helpSource: 'virus_blocker_email',
                //helpSource: 'virus_blocker_lite_email',
                helpSource: this.helpSourceName + '_email',
                // private fields
                parentId: this.getId(),

                title: this.i18n._('Email'),
                layout: "anchor",
                defaults: {
                    anchor: '98%',
                    xtype: 'fieldset',
                    autoScroll: true,
                    buttonAlign: 'left'
                },
                cls: 'ung-panel',
                autoScroll: true,
                items: [{
                    layout:'column',
                    items:[{
                        columnWidth: 0.3,
                        border: false,
                        items: [{
                            xtype: 'checkbox',
                            boxLabel: this.i18n._('Scan SMTP'),
                            hideLabel: true,
                            name: 'Scan SMTP',
                            checked: this.settings.scanSmtp,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, checked) {
                                        this.settings.scanSmtp = checked;
                                    }, this)
                                }
                            }
                        }]
                    },{
                        columnWidth: 0.7,
                        border: false,
                        items: [{
                            xtype: 'combo',
                            name: 'SMTP Action',
                            editable: false,
                            fieldLabel: this.i18n._('Action'),
                            queryMode: 'local',
                            store: [["pass", this.i18n._("pass message")], 
                                    ["remove", this.i18n._("remove infection")],
                                    ["block", this.i18n._("block message")]],
                            value: this.settings.smtpAction,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, newValue) {
                                        this.settings.smtpAction = newValue;
                                    }, this)
                                }
                            }
                        }]
                    }]
                }, {
                    cls: 'description',
                    html: this.i18n._("Virus Blocker signatures were last updated") + ":&nbsp;&nbsp;&nbsp;&nbsp;" +
                        ((this.getRpcNode().getLastSignatureUpdate() != null) ? i18n.timestampFormat(this.getRpcNode().getLastSignatureUpdate()): this.i18n._("Unknown"))
                }]

            });
        },
        // Pass Sites
        buildPassSites: function() {
            this.gridPassSites = Ext.create('Ung.EditorGrid', {
                name: 'Pass Sites',
                settingsCmp: this,
                emptyRow: {
                    "string": "site",
                    "enabled": true,
                    "name": this.i18n._("[no description]")
                },
                flex: 1, 
                title: this.i18n._("Pass Sites"),
                recordJavaClass: "com.untangle.uvm.node.GenericRule",
                dataProperty: "passSites",
                fields: Ung.Util.getGenericRuleFields(this),
                columns: [{
                    header: this.i18n._("Site"),
                    width: 200,
                    dataIndex: 'string',
                    editor:{
                        xtype:'textfield',
                        allowBlank:false
                    }
                },{
                    xtype:'checkcolumn',
                    header: this.i18n._("Pass"),
                    dataIndex: 'enabled',
                    resizable: false,
                    width:55
                },{
                    header: this.i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex: 1,
                    field: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }],
                sortField: 'string',
                columnsDefaultSortable: true,
                rowEditorInputLines: [{
                    xtype:'textfield',
                    name: "Site",
                    dataIndex: "string",
                    fieldLabel: this.i18n._("Site"),
                    allowBlank: false,
                    width: 400
                },{    
                    xtype:'checkbox',
                    name: "Scan",
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Pass")
                },{
                    xtype:'textarea',
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    width: 400,
                    height: 60
                }]
            });

            this.panelPassSites = Ext.create('Ext.panel.Panel',{
                name: 'Pass Sites',
                helpSource: this.helpSourceName + '_pass_sites',
                //helpSource: 'virus_blocker_pass_sites',
                //helpSource: 'virus_blocker_lite_pass_sites',
                parentId: this.getId(),
                title: this.i18n._("Pass Sites"),
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    xtype: 'fieldset',
                    buttonAlign: 'left'
                },
                layout: {
                    type: 'vbox',
                    align: 'stretch'
                },
                items: [{
                    cls: 'description',
                    title: this.i18n . _("Pass Sites"),
                    html: this.i18n . _("Do not scan traffic to the specified sites.  Use caution!"),
                    style: "margin-bottom: 10px;"
                },
                    this.gridPassSites
                ]
            });
        },
        // Event Log
        buildWebEventLog: function() {
            this.gridWebEventLog = Ung.CustomEventLog.buildHttpEventLog (this, 'WebEventLog', this.i18n._('Web Event Log'), 
                    this.helpSourceName + '_web_event_log', 
                    ['time_stamp','c_client_addr','username','c_server_addr','host','uri',this.nodeName + '_name'], 
                    this.getRpcNode().getWebEventQueries);
        },
        // Event Log
        buildMailEventLog: function() {
            this.gridMailEventLog = Ung.CustomEventLog.buildMailEventLog (this, 'EmailEventLog', this.i18n._('Email Event Log'), 
                    this.helpSourceName + '_email_event_log', 
                    ['time_stamp','c_client_addr','username','c_server_addr','subject','addr','sender',this.nodeName + '_name'], 
                    this.getRpcNode().getMailEventQueries);
        },
        buildFtpEventLog: function() {
            this.gridFtpEventLog = Ext.create('Ung.GridEventLog',{
                name: 'Ftp Event Log',
                //helpSource: 'virus_blocker_ftp_event_log',
                //helpSource: 'virus_blocker_lite_ftp_event_log',
                helpSource: this.helpSourceName + '_ftp_event_log',
                settingsCmp: this,
                title: this.i18n._("Ftp Event Log"),
                eventQueriesFn: this.getRpcNode().getFtpEventQueries,

                // the list of fields
                fields: [{
                    name: 'time_stamp',
                    sortType: Ung.SortTypes.asTimestamp
                }, {
                    name: 'c_client_addr',
                    sortType: Ung.SortTypes.asIp
                }, {
                    name: 'username'
                }, {
                    name: 'c_server_addr',
                    sortType: Ung.SortTypes.asIp
                }, {
                    name: 'uri',
                    mapping: 'uri'
                }, {
                    name: 'location'
                }, {
                    name: this.nodeName + '_name'
                }],
                // the list of columns
                columns: [{
                    header: this.i18n._("Timestamp"),
                    width: Ung.Util.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    },
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: this.i18n._("Client"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_addr'
                }, {
                    header: this.i18n._("Username"),
                    width: Ung.Util.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'username'
                }, {
                    header: this.i18n._("File Name"),
                    flex:1,
                    width: Ung.Util.uriFieldWidth,
                    dataIndex: 'uri'
                }, {
                    header: this.i18n._("Virus Name"),
                    width: 140,
                    sortable: true,
                    dataIndex: this.nodeName + '_name'
                }, {
                    header: this.i18n._("Server"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_addr'
                }]
            });
        }
    });
}
//@ sourceURL=virus-settings.js
