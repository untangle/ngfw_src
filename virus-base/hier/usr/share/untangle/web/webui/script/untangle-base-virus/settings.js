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
        // called when the component is rendered
        initComponent: function() {
            this.vendor=this.getRpcNode().getVendor();
            this.buildWeb();
            this.buildEmail();
            this.buildFtp();
            this.buildWebEventLog();
            this.buildMailEventLog();
            
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelWeb, this.panelEmail, this.panelFtp, this.gridWebEventLog, this.gridMailEventLog]);
            this.callParent(arguments);
        },
        // Web Panel
        buildWeb: function() {
            this.panelWeb = Ext.create('Ext.panel.Panel',{
                name: 'Web',
                helpSource: 'web',
                // private fields
                winExtensions: null,
                winMimeTypes: null,
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
                    items: [
                        {
                            xtype:'fieldset',
                            items:    {
                                xtype: 'button',
                                name: 'File Extensions',
                                text: this.i18n._('File Extensions'),
                                style: 'padding-bottom:10px;',
                                handler: Ext.bind(function() {
                                    this.panelWeb.onManageExtensions();
                                }, this)
                            }
                        }
                    , {
                        xtype:'fieldset',
                        items: {
                            xtype: 'button',
                            name: 'MIME Types',
                            text: this.i18n._('MIME Types'),
                            style: 'padding-bottom:10px;',
                            handler: Ext.bind(function() {
                                this.panelWeb.onManageMimeTypes();
                            }, this)
                        }
                    }]
                }, {
                    cls: 'description',
                    html: this.i18n._("Virus Blocker signatures were last updated") + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                        + ((this.getRpcNode().getLastSignatureUpdate() != null) ? i18n.timestampFormat(this.getRpcNode().getLastSignatureUpdate()): this.i18n._("Unknown"))
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
                beforeDestroy: function() {
                    Ext.destroy( this.winExtensions, this.winMimeTypes);
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
                    fixed: true,
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
                    fixed: true,
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
                helpSource: 'ftp',
                // private fields
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
                    html: this.i18n._("Virus Blocker signatures were last updated") + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                        + ((this.getRpcNode().getLastSignatureUpdate() != null) ? i18n.timestampFormat(this.getRpcNode().getLastSignatureUpdate()): this.i18n._("Unknown"))
                }]

            });
        },
        // Email Panel
        buildEmail: function() {
            this.panelEmail = Ext.create('Ext.panel.Panel',{
                name: 'Email',
                helpSource: 'email',
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
                        columnWidth:.3,
                        border:false,
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
                        }, {
                            xtype: 'checkbox',
                            boxLabel: this.i18n._('Scan POP3'),
                            hideLabel: true,
                            name: 'Scan POP3',
                            checked: this.settings.scanPop,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, checked) {
                                        this.settings.scanPop = checked;
                                    }, this)
                                }
                            }
                        }, {
                            xtype: 'checkbox',
                            boxLabel: this.i18n._('Scan IMAP'),
                            hideLabel: true,
                            name: 'Scan IMAP',
                            checked: this.settings.scanImap,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, checked) {
                                        this.settings.scanImap = checked;
                                    }, this)
                                }
                            }
                        }]
                    },{
                        columnWidth:.7,
                        border:false,
                        items: [{
                            xtype: 'combo',
                            name: 'SMTP Action',
                            editable: false,
                            fieldLabel: this.i18n._('Action'),
                            mode: 'local',
                            triggerAction: 'all',
                            listClass: 'x-combo-list-small',
                            store: [["pass", this.i18n._("pass message")], 
                                    ["remove", this.i18n._("remove infection")],
                                    ["block", this.i18n._("block message")]],
                            displayField: 'name',
                            valueField: 'key',
                            value: this.settings.smtpAction,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, newValue) {
                                        this.settings.smtpAction = newValue;
                                    }, this)
                                }
                            }
                        },{
                            xtype: 'combo',
                            name: 'POP3 Action',
                            editable: false,
                            fieldLabel: this.i18n._('Action'),
                            mode: 'local',
                            triggerAction: 'all',
                            listClass: 'x-combo-list-small',
                            store: [["pass", this.i18n._("pass message")], 
                                    ["remove", this.i18n._("remove infection")]],
                            displayField: 'name',
                            valueField: 'key',
                            value: this.settings.popAction,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, newValue) {
                                        this.settings.popAction = newValue;
                                    }, this)
                                }
                            }
                        },{
                            xtype: 'combo',
                            name: 'IMAP Action',
                            editable: false,
                            fieldLabel: this.i18n._('Action'),
                            mode: 'local',
                            triggerAction: 'all',
                            listClass: 'x-combo-list-small',
                            store: [["PASS", this.i18n._("pass message")], 
                                   ["REMOVE", this.i18n._("remove infection")]],
                            displayField: 'name',
                            valueField: 'key',
                            value: this.settings.imapAction,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, newValue) {
                                        this.settings.imapAction = newValue;
                                    }, this)
                                }
                            }
                        }]
                    }]
                }, {
                    cls: 'description',
                    html: this.i18n._("Virus Blocker signatures were last updated") + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                        + ((this.getRpcNode().getLastSignatureUpdate() != null) ? i18n.timestampFormat(this.getRpcNode().getLastSignatureUpdate()): this.i18n._("Unknown"))
                }]

            });
        },
        // Event Log
        buildWebEventLog: function() {
            this.gridWebEventLog = Ext.create('Ung.GridEventLog',{
                name: 'Web Event Log',
                helpSource: 'Web_Event_Log',
                settingsCmp: this,
                title: this.i18n._("Web Event Log"),
                eventQueriesFn: this.getRpcNode().getWebEventQueries,

                // the list of fields
                fields: [{
                    name: 'time_stamp',
                    sortType: Ung.SortTypes.asTimestamp
                }, {
                    name: 'client',
                    mapping: 'c_client_addr'
                }, {
                    name: 'uid'
                }, {
                    name: 'server',
                    mapping: 'c_server_addr'
                }, {
                    name: 'host',
                    mapping: 'host'
                }, {
                    name: 'uri',
                    mapping: 'uri'
                }, {
                    name: 'location'
                }, {
                    name: 'reason',
                    mapping: 'virus_' + this.vendor + '_name'
                }],
                // the list of columns
                columns: [{
                    header: this.i18n._("Timestamp"),
                    width: Ung.Util.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: this.i18n._("Client"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'client'
                }, {
                    header: this.i18n._("Username"),
                    width: Ung.Util.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'uid'
                }, {
                    header: this.i18n._("Host"),
                    width: Ung.Util.hostnameFieldWidth,
                    dataIndex: 'host'
                }, {
                    header: this.i18n._("Uri"),
                    flex:1,
                    width: Ung.Util.uriFieldWidth,
                    dataIndex: 'uri'
                }, {
                    header: this.i18n._("Virus Name"),
                    width: 140,
                    sortable: true,
                    dataIndex: 'reason'
                }, {
                    header: this.i18n._("Server"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'server'
                }]
            });
        },
        // Event Log
        buildMailEventLog: function() {
            this.gridMailEventLog = Ext.create('Ung.GridEventLog',{
                name: 'Email Event Log',
                helpSource: 'Email_Event_Log',
                settingsCmp: this,
                title: this.i18n._("Email Event Log"),
                eventQueriesFn: this.getRpcNode().getMailEventQueries,

                // the list of fields
                fields: [{
                    name: 'time_stamp',
                    sortType: Ung.SortTypes.asTimestamp
                }, {
                    name: 'client',
                    mapping: 'c_client_addr'
                }, {
                    name: 'uid'
                }, {
                    name: 'server',
                    mapping: 'c_server_addr'
                }, {
                    name: 'subject',
                    type: 'string'
                }, {
                    name: 'addr',
                    type: 'string'
                }, {
                    name: 'sender',
                    type: 'string'
                }, {
                    name: 'reason',
                    mapping: 'virus_' + this.vendor + '_name'
                }],
                // the list of columns
                columns: [{
                    header: this.i18n._("Timestamp"),
                    width: Ung.Util.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: this.i18n._("Client"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'client'
                }, {
                    header: this.i18n._("Receiver"),
                    width: Ung.Util.emailFieldWidth,
                    sortable: true,
                    dataIndex: 'addr'
                }, {
                    header: this.i18n._("Sender"),
                    width: Ung.Util.emailFieldWidth,
                    sortable: true,
                    dataIndex: 'sender'
                }, {
                    header: this.i18n._("Subject"),
                    flex:1,
                    width: 150,
                    sortable: true,
                    dataIndex: 'subject'
                }, {
                    header: this.i18n._("Virus name"),
                    width: 140,
                    sortable: true,
                    dataIndex: 'reason'
                }, {
                    header: this.i18n._("Server"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'server'
                }]
            });
        }
    });
}
//@ sourceURL=virus-settings.js
