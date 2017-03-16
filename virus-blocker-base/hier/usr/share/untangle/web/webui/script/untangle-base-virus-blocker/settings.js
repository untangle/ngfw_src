Ext.define('Webui.virus-blocker-base.settings', {
    extend:'Ung.AppWin',
    panelWeb:null,
    panelEmail: null,
    panelFtp: null,
    panelAdvanced: null,
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
        this.buildAdvanced();

        // builds the tab panel with the tabs
        this.buildTabPanel([
            this.panelWeb,
            this.panelEmail,
            this.panelFtp,
            this.panelPassSites,
            this.panelAdvanced
        ]);
        this.callParent(arguments);
    },
    beforeSave: function(isApply, handler) {
        this.settings.passSites.list=this.gridPassSites.getList();
        this.settings.httpFileExtensions.list=this.gridExtensions.getList();
        this.settings.httpMimeTypes.list=this.gridMimeTypes.getList();
        handler.call(this, isApply);
    },
    // Web Panel
    buildWeb: function() {
        this.panelWeb = Ext.create('Ext.panel.Panel',{
            name: 'Web',
            //helpSource: 'virus_blocker_web',
            //helpSource: 'virus_blocker_lite_web',
            helpSource: this.helpSourceName + '_web',
            winExtensions: null,
            winMimeTypes: null,
            title: i18n._('Web'),
            cls: 'ung-panel',
            autoScroll: true,
            items: [{
                xtype: 'fieldset',
                items: [{
                    xtype: 'checkbox',
                    boxLabel: i18n._('Scan HTTP'),
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
                }, {
                    xtype: 'component',
                    name: 'signatureStatus',
                    margin: '30 0 0 0'
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('About'),
                html: this.aboutInfo,
                hidden: (this.aboutInfo==null)
            }]
            
        });

        if ( this.lastUpdate != null && this.lastUpdate.time != 0 ) {
            var sigString = "<i>" + i18n._("Signatures were last updated") + "</i>" + ":&nbsp;&nbsp;&nbsp;&nbsp;" + i18n.timestampFormat(this.lastUpdate);
            var signatureStatusField = this.panelWeb.down('component[name=signatureStatus]');
            signatureStatusField.setHtml(sigString);
        }
    },
    // File Types
    buildExtensions: function() {
        this.gridExtensions = Ext.create('Ung.grid.Panel',{
            name: 'File Extensions',
            settingsCmp: this,
            title: i18n._("File Extensions"),
            dataProperty: "httpFileExtensions",
            recordJavaClass: "com.untangle.uvm.app.GenericRule",
            emptyRow: {
                "string": "",
                "enabled": true,
                "description": ""
            },
            sortField: 'string',
            fields: Ung.Util.getGenericRuleFields(this),
            columns: [{
                header: i18n._("File Type"),
                width: 200,
                dataIndex: 'string',
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter file type]"),
                    allowBlank: false
                }
            },
            {
                xtype:'checkcolumn',
                header: i18n._("Scan"),
                dataIndex: 'enabled',
                resizable: false,
                width:55
            }, {
                header: i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex: 1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[no description]")
                }
            }],
            rowEditorInputLines: [
            {
                xtype:'textfield',
                name: "File Type",
                dataIndex: "string",
                fieldLabel: i18n._("File Type"),
                emptyText: i18n._("[enter file type]"),
                allowBlank: false,
                width: 400
            },
            {
                xtype:'checkbox',
                name: "Scan",
                dataIndex: "enabled",
                fieldLabel: i18n._("Scan")
            },
            {
                xtype:'textarea',
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
                width: 400,
                height: 60
            }]
        });
    },
    // MIME Types
    buildMimeTypes: function() {
        this.gridMimeTypes = Ext.create('Ung.grid.Panel', {
            name: 'MIME Types',
            settingsCmp: this,
            title: i18n._("MIME Types"),
            dataProperty: "httpMimeTypes",
            recordJavaClass: "com.untangle.uvm.app.GenericRule",
            emptyRow: {
                "string": "",
                "enabled": true,
                "description": ""
            },
            sortField: 'string',
            fields: Ung.Util.getGenericRuleFields(this),
            columns: [{
                header: i18n._("MIME Type"),
                width: 200,
                dataIndex: 'string',
                editor:{
                    xtype:'textfield',
                    emptyText: i18n._("[enter MIME type]"),
                    allowBlank:false
                }
            }, {
                xtype:'checkcolumn',
                header: i18n._("Scan"),
                dataIndex: 'enabled',
                resizable: false,
                width:55
            }, {
                header: i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex: 1,
                field: {
                    xtype:'textfield',
                    emptyText: i18n._("[no description]"),
                    allowBlank:false
                }
            }],
            rowEditorInputLines: [
            {
                xtype:'textfield',
                name: "MIME Type",
                dataIndex: "string",
                fieldLabel: i18n._("MIME Type"),
                emptyText: i18n._("[enter MIME type]"),
                allowBlank: false,
                width: 400
            }, {
                xtype:'checkbox',
                name: "Scan",
                dataIndex: "enabled",
                fieldLabel: i18n._("Scan")
            }, {
                xtype:'textarea',
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
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
            title: i18n._('FTP'),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                items: [{
                    xtype: 'checkbox',
                    boxLabel: i18n._('Scan FTP'),
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
                html: i18n._("Virus Blocker signatures were last updated") + ":&nbsp;&nbsp;&nbsp;&nbsp;" +
                    ((this.getRpcNode().getLastSignatureUpdate() != null) ? i18n.timestampFormat(this.getRpcNode().getLastSignatureUpdate()): i18n._("Unknown"))
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
            title: i18n._('Email'),
            defaults: {
                xtype: 'fieldset'
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
                        boxLabel: i18n._('Scan SMTP'),
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
                        fieldLabel: i18n._('Action'),
                        queryMode: 'local',
                        store: [["pass", i18n._("pass message")],
                                ["remove", i18n._("remove infection")],
                                ["block", i18n._("block message")]],
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
                html: i18n._("Virus Blocker signatures were last updated") + ":&nbsp;&nbsp;&nbsp;&nbsp;" +
                    ((this.getRpcNode().getLastSignatureUpdate() != null) ? i18n.timestampFormat(this.getRpcNode().getLastSignatureUpdate()): i18n._("Unknown"))
            }]
        });
    },
    // Pass Sites
    buildPassSites: function() {
        this.gridPassSites = Ext.create('Ung.grid.Panel', {
            name: 'Pass Sites',
            settingsCmp: this,
            flex: 1,
            title: i18n._("Pass Sites"),
            dataProperty: "passSites",
            recordJavaClass: "com.untangle.uvm.app.GenericRule",
            emptyRow: {
                "string": "",
                "enabled": true,
                "description": ""
            },
            sortField: 'string',
            fields: Ung.Util.getGenericRuleFields(this),
            columns: [{
                header: i18n._("Site"),
                width: 200,
                dataIndex: 'string',
                editor:{
                    xtype:'textfield',
                    emptyText: i18n._("[enter site]"),
                    allowBlank:false
                }
            },{
                xtype:'checkcolumn',
                header: i18n._("Pass"),
                dataIndex: 'enabled',
                resizable: false,
                width:55
            },{
                header: i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex: 1,
                field: {
                    xtype:'textfield',
                    emptyText: i18n._("[no description]")
                }
            }],
            rowEditorInputLines: [{
                xtype:'textfield',
                name: "Site",
                dataIndex: "string",
                fieldLabel: i18n._("Site"),
                emptyText: i18n._("[enter site]"),
                allowBlank: false,
                width: 400
            },{
                xtype:'checkbox',
                name: "Scan",
                dataIndex: "enabled",
                fieldLabel: i18n._("Pass")
            },{
                xtype:'textarea',
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
                width: 400,
                height: 60
            }]
        });

        this.panelPassSites = Ext.create('Ext.panel.Panel',{
            name: 'Pass Sites',
            helpSource: this.helpSourceName + '_pass_sites',
            //helpSource: 'virus_blocker_pass_sites',
            //helpSource: 'virus_blocker_lite_pass_sites',
            title: i18n._("Pass Sites"),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset'
            },
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            items: [{
                title: i18n . _("Pass Sites"),
                html: i18n . _("Do not scan traffic to the specified sites.  Use caution!")
            },
            this.gridPassSites
            ]
        });
    },
    buildAdvanced: function() {
        this.buildExtensions();
        this.buildMimeTypes();

        this.panelAdvanced = Ext.create('Ext.panel.Panel',{
            name: 'Advanced',
            title: i18n._('Advanced'),
            cls: 'ung-panel',
            layout: { type: 'vbox', pack: 'start', align: 'stretch' },
            items: [{
                xtype: 'fieldset',
                flex: 0,
                title: i18n._("Advanced"),
                items: this.getExtraAdvancedOptions()
            }, {
                xtype: 'fieldset',
                flex: 0,
                title: i18n._("HTTP Advanced"),
            }, {
                    xtype: 'tabpanel',
                    activeTab: 0,
                    deferredRender: false,
                    flex: 1,
                    items: [this.gridExtensions, this.gridMimeTypes]
            }]
        });
    }
});
//# sourceURL=base-virus-blocker-settings.js