Ext.define('Webui.untangle-base-webfilter.settings', {
    extend:'Ung.NodeWin',
    // called when the component is rendered
    initComponent: function() {
        this.buildUrlValidator();
        this.genericRuleFields = Ung.Util.getGenericRuleFields(this);
        this.buildTabPanel([
            this.buildPanelBlockedCategories(),
            this.buildPanelBlockedSites(),
            this.buildPanelBlockedFileTypes(),
            this.buildPanelBlockedMimeTypes(),
            this.buildPanelPassedSites(),
            this.buildPanelPassedClients(),
            this.buildPanelAdvanced(),
            this.buildEventLog()
        ]);
        this.callParent(arguments);
    },
    beforeSave: function(isApply, handler) {
        this.settings.categories.list=this.gridCategories.getList();
        this.settings.blockedUrls.list=this.gridBlockedSites.getList();
        this.settings.blockedExtensions.list=this.gridBlockedFileTypes.getList();
        this.settings.blockedMimeTypes.list=this.gridBlockedMimeTypes.getList();
        this.settings.passedUrls.list=this.gridAllowedSites.getList();
        this.settings.passedClients.list=this.gridAllowedClients.getList();
        handler.call(this, isApply);
    },
    buildUrlValidator: function(){
        this.urlValidator = Ext.bind(function(fieldValue) {
            if (fieldValue.match( /^([^:]+):\/\// ) != null ){
                return this.i18n._("Site cannot contain URL protocol.");
            }
            if (fieldValue.match( /^([^:]+):\d+\// ) != null ){
                return this.i18n._("Site cannot contain port.");
            }
            // strip "www." from beginning of rule
            if (fieldValue.indexOf("www.") == 0) {
                fieldValue = fieldValue.substr(4);
            }
            // strip "*." from beginning of rule
            if (fieldValue.indexOf("*.") == 0) {
                fieldValue = fieldValue.substr(2);
            }
            // strip "/" from the end
            if (fieldValue.indexOf("/") == fieldValue.length - 1) {
                fieldValue = fieldValue.substring(0, fieldValue.length - 1);
            }
            if (fieldValue.trim().length == 0) {
                return this.i18n._("Invalid \"URL\" specified");
            }
            return true;
        }, this);
    },

    // Block Lists Panel
    buildPanelBlockedCategories: function() {
        this.gridCategories = Ext.create('Ung.grid.Panel',{
            flex: 1,
            name: 'Categories',
            title: this.i18n._("Categories"),
            sizetoParent: true,
            settingsCmp: this,
            hasAdd: false,
            hasDelete: false,
            dataProperty: "categories",
            recordJavaClass: "com.untangle.uvm.node.GenericRule",
            sortField: 'name',
            fields: this.genericRuleFields,
            columns: [{
                header: this.i18n._("Category"),
                width: 200,
                dataIndex: 'name'
            }, {
                xtype:'checkcolumn',
                width:55,
                header: this.i18n._("Block"),
                dataIndex: 'blocked',
                resizable: false,
                listeners: {
                    "checkchange": {
                        fn: Ext.bind(function(elem, rowIndex, checked) {
                                if (checked) {
                                    var record = this.gridCategories.getStore().getAt(rowIndex);
                                    record.set('flagged', true);
                                }
                            }, this)
                        }
                }
            }, {
                xtype:'checkcolumn',
                width:55,
                header: this.i18n._("Flag"),
                dataIndex: 'flagged',
                resizable: false,
                tooltip: this.i18n._("Flag as Violation")
            }, {
                header: this.i18n._("Description"),
                flex:1,
                width: 400,
                dataIndex: 'description',
                editor:{
                    xtype:'textfield',
                    allowBlank: false
                }
            }],
            rowEditorInputLines: [{
                xtype:'textfield',
                name: "Category",
                dataIndex: "name",
                fieldLabel: this.i18n._("Category"),
                allowBlank: false,
                width: 400,
                disabled: true
            }, {
                xtype:'checkbox',
                name: "Block",
                dataIndex: "blocked",
                fieldLabel: this.i18n._("Block"),
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, checked) {
                            var rowEditor = this.gridCategories.rowEditor;
                            if (checked) {
                                rowEditor.down('checkbox[name="Flag"]').setValue(true);
                            }
                        }, this)
                    }
                }
            }, {
                xtype:'checkbox',
                name: "Flag",
                dataIndex: "flagged",
                fieldLabel: this.i18n._("Flag"),
                tooltip: this.i18n._("Flag as Violation")
            }, {
                xtype:'textarea',
                name: "Description",
                dataIndex: "description",
                fieldLabel: this.i18n._("Description"),
                width: 400,
                height: 60
            }]
        });
        this.blockedCategoriesPanel = Ext.create('Ext.panel.Panel',{
            name: 'BlockCategories',
            title: this.i18n._('Block Categories'),
            //helpSource: 'web_filter_block_categories',
            //helpSource: 'web_filter_lite_block_categories',
            helpSource: this.helpSourceName + '_block_categories',
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
                title: this.i18n . _("Block Categories"),
                html: this.i18n . _("Block or flag access to sites associated with the specified category.")
            }, this.gridCategories ]
        });
        return this.blockedCategoriesPanel;
    },
    // Blocked sites
    buildPanelBlockedSites: function() {
        this.gridBlockedSites = Ext.create('Ung.grid.Panel',{
            name: 'Sites',
            title: this.i18n._("Sites"),
            settingsCmp: this,
            flex: 1,
            dataProperty: "blockedUrls",
            recordJavaClass: "com.untangle.uvm.node.GenericRule",
            emptyRow: {
                "string": "",
                "blocked": true,
                "flagged": true,
                "description": ""
            },
            sortField: 'string',
            fields: this.genericRuleFields,
            columns: [{
                header: this.i18n._("Site"),
                width: 200,
                dataIndex: 'string',
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[enter site]"),
                    allowBlank: false,
                    validator: this.urlValidator
                }
            },{
                xtype:'checkcolumn',
                width:55,
                header: this.i18n._("Block"),
                dataIndex: 'blocked',
                resizable: false
            },{
                xtype:'checkcolumn',
                width:55,
                header: this.i18n._("Flag"),
                dataIndex: 'flagged',
                resizable: false,
                tooltip: this.i18n._("Flag as Violation")
            },{
                header: this.i18n._("Description"),
                width: 200,
                flex:1,
                dataIndex: 'description',
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[no description]")
                }
            }],
            rowEditorInputLines: [{
                xtype:'textfield',
                name: "Site",
                dataIndex: "string",
                fieldLabel: this.i18n._("Site"),
                emptyText: this.i18n._("[enter site]"),
                allowBlank: false,
                width: 400,
                validator: this.urlValidator
            },{
                xtype:'checkbox',
                name: "Block",
                dataIndex: "blocked",
                fieldLabel: this.i18n._("Block")
            },{
                xtype:'checkbox',
                name: "Flag",
                dataIndex: "flagged",
                fieldLabel: this.i18n._("Flag"),
                tooltip: this.i18n._("Flag as Violation")
            },{
                xtype:'textarea',
                name: "Description",
                dataIndex: "description",
                fieldLabel: this.i18n._("Description"),
                emptyText: this.i18n._("[no description]"),
                width: 400,
                height: 60
            }]
        });
        this.blockedSitesPanel = Ext.create('Ext.panel.Panel',{
            name: 'BlockSites',
            title: this.i18n._('Block Sites'),
            //helpSource: 'web_filter_block_sites',
            //helpSource: 'web_filter_lite_block_sites',
            helpSource: this.helpSourceName + '_block_sites',
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
                title: this.i18n . _("Blocked Sites"),
                html: this.i18n . _("Block or flag access to the specified site.")
            }, this.gridBlockedSites ]
        });
        return this.blockedSitesPanel;
    },
    // Blocked File Types
    buildPanelBlockedFileTypes: function() {
        this.gridBlockedFileTypes = Ext.create('Ung.grid.Panel',{
            flex: 1,
            name: 'File Types',
            sizetoParent: true,
            settingsCmp: this,
            title: this.i18n._("File Types"),
            dataProperty: "blockedExtensions",
            recordJavaClass: "com.untangle.uvm.node.GenericRule",
            emptyRow: {
                "string": "",
                "blocked": true,
                "flagged": true,
                "category": "",
                "description": ""
            },
            sortField: 'string',
            fields: this.genericRuleFields,
            columns: [{
                header: this.i18n._("File Type"),
                width: 200,
                dataIndex: 'string',
                editor: {
                    xtype: 'textfield',
                    emptyText: this.i18n._("[enter extension]"),
                    allowBlank: false
                }
            },{
                xtype:'checkcolumn',
                width:55,
                header: this.i18n._("Block"),
                dataIndex: 'blocked',
                resizable: false
            },{
                xtype:'checkcolumn',
                width:55,
                header: this.i18n._("Flag"),
                dataIndex: 'flagged',
                resizable: false,
                tooltip: this.i18n._("Flag as Violation")
            },{
                header: this.i18n._("Category"),
                width: 200,
                dataIndex: 'category',
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[no category]")
                }
            },{
                header: this.i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex:1,
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[no description]")
                }
            }],
            rowEditorInputLines: [{
                xtype:'textfield',
                name: "File Type",
                dataIndex: "string",
                fieldLabel: this.i18n._("File Type"),
                emptyText: this.i18n._("[enter extension]"),
                allowBlank: false,
                width: 300
            },{
                xtype:'checkbox',
                name: "Block",
                dataIndex: "blocked",
                fieldLabel: this.i18n._("Block")
            },{
                xtype:'checkbox',
                name: "Flag",
                dataIndex: "flagged",
                fieldLabel: this.i18n._("Flag"),
                tooltip: this.i18n._("Flag as Violation")
            },{
                xtype:'textarea',
                name: "Category",
                dataIndex: "category",
                fieldLabel: this.i18n._("Category"),
                emptyText: this.i18n._("[no category]"),
                width: 300
            },{
                xtype:'textarea',
                name: "Description",
                dataIndex: "description",
                fieldLabel: this.i18n._("Description"),
                emptyText: this.i18n._("[no description]"),
                width: 400,
                height: 60
            }]
        });
        this.blockedFileTypesPanel = Ext.create('Ext.panel.Panel',{
            name: 'BlockFileTypes',
            title: this.i18n._('Block File Types'),
            //helpSource: 'web_filter_block_filetypes',
            //helpSource: 'web_filter_lite_block_filetypes',
            helpSource: this.helpSourceName + '_block_filetypes',
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
                title: this.i18n . _("Block File Types"),
                html: this.i18n . _("Block or flag access to files associated with the specified file type.")
            }, this.gridBlockedFileTypes ]
        });
        return this.blockedFileTypesPanel;
    },
    // Blocked MIME Types
    buildPanelBlockedMimeTypes: function() {
        this.gridBlockedMimeTypes = Ext.create('Ung.grid.Panel',{
            flex: 1,
            name: 'MIME Types',
            settingsCmp: this,
            title: this.i18n._("MIME Types"),
            dataProperty: "blockedMimeTypes",
            recordJavaClass: "com.untangle.uvm.node.GenericRule",
            emptyRow: {
                "string": "",
                "blocked": true,
                "flagged": true,
                "category": "",
                "description": ""
            },
            sortField: 'string',
            fields: this.genericRuleFields,
            columns: [{
                header: this.i18n._("MIME type"),
                width: 200,
                dataIndex: 'string',
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[enter mime type]"),
                    allowBlank:false
                }
            },{
                xtype:'checkcolumn',
                width:55,
                header: this.i18n._("Block"),
                dataIndex: 'blocked',
                resizable: false
            },{
                xtype:'checkcolumn',
                width:55,
                header: this.i18n._("Flag"),
                dataIndex: 'flagged',
                resizable: false,
                tooltip: this.i18n._("Flag as Violation")
            },{
                header: this.i18n._("Category"),
                width: 100,
                dataIndex: 'category',
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[no category]")
                }
            },{
                header: this.i18n._("Description"),
                width: 200,
                flex:1,
                dataIndex: 'description',
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[no description]")
                }
            }],
            rowEditorInputLines: [{
                xtype:'textfield',
                name: "MIME Type",
                dataIndex: "string",
                fieldLabel: this.i18n._("MIME Type"),
                emptyText: this.i18n._("[enter mime type]"),
                allowBlank: false,
                width: 400
            },{
                xtype:'checkbox',
                name: "Block",
                dataIndex: "blocked",
                fieldLabel: this.i18n._("Block")
            },{
                xtype:'checkbox',
                name: "Flag",
                dataIndex: "flagged",
                fieldLabel: this.i18n._("Flag"),
                tooltip: this.i18n._("Flag as Violation")
            },{
                xtype:'textarea',
                name: "Category",
                dataIndex: "category",
                fieldLabel: this.i18n._("Category"),
                emptyText: this.i18n._("[no category]"),
                width: 300
            },{
                xtype:'textarea',
                name: "Description",
                dataIndex: "description",
                fieldLabel: this.i18n._("Description"),
                emptyText: this.i18n._("[no description]"),
                width: 400,
                height: 60
            }]
        });
        this.blockedMimeTypesPanel = Ext.create('Ext.panel.Panel',{
            name: 'BlockMimeTypes',
            title: this.i18n._('Block Mime Types'),
            //helpSource: 'web_filter_block_mimetypes',
            //helpSource: 'web_filter_lite_block_mimetypes',
            helpSource: this.helpSourceName + '_block_mimetypes',
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
                title: this.i18n . _("Block MIME Types"),
                html: this.i18n . _("Block or flag access to files associated with the specified MIME type.")
            }, this.gridBlockedMimeTypes ]
        });
        return this.blockedMimeTypesPanel;
    },
    // Allowed Sites
    buildPanelPassedSites: function() {
        this.gridAllowedSites = Ext.create('Ung.grid.Panel',{
            name: 'Sites',
            settingsCmp: this,
            flex: 1,
            title: this.i18n._("Sites"),
            dataProperty: "passedUrls",
            recordJavaClass: "com.untangle.uvm.node.GenericRule",
            emptyRow: {
                "string": "",
                "enabled": true,
                "description": ""
            },
            sortField: 'string',
            fields: this.genericRuleFields,
            columns: [{
                header: this.i18n._("Site"),
                width: 200,
                dataIndex: 'string',
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[enter site]"),
                    allowBlank: false,
                    validator: this.urlValidator
                }
            },{
                xtype:'checkcolumn',
                width:55,
                header: this.i18n._("Pass"),
                dataIndex: 'enabled',
                resizable: false
            },{
                header: this.i18n._("Description"),
                flex:1,
                width: 200,
                dataIndex: 'description',
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[no description]")
                }
            }],
            rowEditorInputLines: [{
                xtype:'textfield',
                name: "Site",
                dataIndex: "string",
                fieldLabel: this.i18n._("Site"),
                emptyText: this.i18n._("[enter site]"),
                allowBlank: false,
                width: 400,
                validator: this.urlValidator
            },{
                xtype:'checkbox',
                name: "Pass",
                dataIndex: "enabled",
                fieldLabel: this.i18n._("Pass")
            },{
                xtype:'textarea',
                name: "Description",
                dataIndex: "description",
                fieldLabel: this.i18n._("Description"),
                emptyText: this.i18n._("[no description]"),
                width: 400,
                height: 60
            }]
        });
        this.allowedSitesPanel = Ext.create('Ext.panel.Panel',{
            name: 'PassSites',
            //helpSource: 'web_filter_pass_sites',
            //helpSource: 'web_filter_lite_pass_sites',
            helpSource: this.helpSourceName + '_pass_sites',
            title: this.i18n._('Pass Sites'),
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
                title: this.i18n . _("Pass Sites"),
                html: this.i18n . _("Allow access to the specified site regardless of matching block policies.")
            }, this.gridAllowedSites ]
        });
        return this.allowedSitesPanel;
    },
    // Allowed Clients
    buildPanelPassedClients: function() {
        this.gridAllowedClients = Ext.create('Ung.grid.Panel',{
            flex: 1,
            name: 'Client IP addresses',
            settingsCmp: this,
            title: this.i18n._("Client IP addresses"),
            dataProperty: "passedClients",
            recordJavaClass: "com.untangle.uvm.node.GenericRule",
            emptyRow: {
                "string": "1.2.3.4",
                "enabled": true,
                "description": ""
            },
            sortField: 'string',
            fields: this.genericRuleFields,
            columns: [{
                header: this.i18n._("IP address/range"),
                width: 200,
                dataIndex: 'string',
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[enter IP address/range]"),
                    vtype:"ipMatcher",
                    allowBlank:false
                }
            },{
                xtype:'checkcolumn',
                width:55,
                header: this.i18n._("Pass"),
                dataIndex: 'enabled',
                resizable: false
            },{
                header: this.i18n._("Description"),
                flex:1,
                width: 200,
                dataIndex: 'description',
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[no description]")
                }
            }],
            rowEditorInputLines: [{
                xtype:'textfield',
                name: "IP address/range",
                dataIndex: "string",
                fieldLabel: this.i18n._("IP address/range"),
                emptyText: this.i18n._("[enter IP address/range]"),
                vtype:"ipMatcher",
                allowBlank: false,
                width: 400
            },{
                xtype:'checkbox',
                name: "Pass",
                dataIndex: "enabled",
                fieldLabel: this.i18n._("Pass")
            },{
                xtype:'textarea',
                name: "Description",
                dataIndex: "description",
                fieldLabel: this.i18n._("Description"),
                emptyText: this.i18n._("[no description]"),
                width: 400,
                height: 60
            }]
        });
        this.allowedClientsPanel = Ext.create('Ext.panel.Panel',{
            name: 'PassClients',
            //helpSource: 'web_filter_pass_clients',
            //helpSource: 'web_filter_lite_pass_clients',
            helpSource: this.helpSourceName + '_pass_clients',
            title: this.i18n._('Pass Clients'),
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
                title: this.i18n . _("Pass Clients"),
                html: this.i18n . _("Allow access for client networks regardless of matching block policies.")
            }, this.gridAllowedClients ]
        });
        return this.allowedClientsPanel;
    },
    // Advanced options
    buildPanelAdvanced: function() {
        this.panelAdvanced = Ext.create('Ext.panel.Panel',{
            name: 'Advanced',
            //helpSource: 'web_filter_advanced',
            //helpSource: 'web_filter_lite_advanced',
            helpSource: this.helpSourceName + '_advanced',

            title: this.i18n._('Advanced'),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                name: "fieldset_miscellaneous",
                title: this.i18n._("Advanced Options"),
                items: [{
                    xtype: "checkbox",
                    boxLabel: this.i18n._("Block pages from IP only hosts"),
                    hideLabel: true,
                    name: 'Block IPHost',
                    checked: this.settings.blockAllIpHosts,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                this.settings.blockAllIpHosts = checked;
                            }, this)
                        }
                    }
                },{
                    xtype: "checkbox",
                    boxLabel: this.i18n._("Pass if referers match Pass Sites"),
                    hideLabel: true,
                    name: 'Pass Referers',
                    checked: this.settings.passReferers,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                this.settings.passReferers = checked;
                            }, this)
                        }
                    }
                },{
                    xtype: "checkbox",
                    boxLabel: this.i18n._("Restrict Google applications"),
                    hideLabel: true,
                    name: 'restrictGoogleApps',
                    checked: this.settings.restrictGoogleApps,
                    listeners: {
                        "change": {
                            fn: Ext.bind( function( elem, checked ){
                                this.settings.restrictGoogleApps = checked;
                                this.panelAdvanced.query('fieldset[name="restrictGoogleAppsFieldset"]')[0].setVisible( checked );
                            }, this )
                        }
                    }
                },{
                    xtype: 'fieldset',
                    name: 'restrictGoogleAppsFieldset',
                    hidden: !this.settings.restrictGoogleApps,
                    items: [{
                        xtype: 'displayfield',
                        value: this.i18n._("NOTE:") + "&nbsp;" + "<i>HTTPS Inspector</i> " + this.i18n._("must be installed and running with the Inspect Google Traffic configured to Inspect."),
                        style: {
                            marginBottom: '10px'
                        }
                    },{
                        xtype: "textfield",
                        fieldLabel: this.i18n._("Allowed Domain(s)"),
                        labelWidth: 120,
                        tooltip: i18n._("Specify the comma separated list of domains allowed to access non-search Google applications"),
                        passwordField: true,
                        name: "restrictGoogleAppsDomain",
                        value: this.settings.restrictGoogleAppsDomain,
                        validator: Ext.bind(function(fieldValue) {
                                var domains = fieldValue.split(/,/);
                                for( var i = 0; i < domains.length; i++ ){
                                    var domain = domains[i];
                                    if ( domain.match( /^([^:]+):\/\// ) != null ){
                                        return this.i18n._("Domain cannot contain URL protocol.");
                                    }
                                    if( domain.match( /^([^:]+):\d+\// ) != null ){
                                        return this.i18n._("Domain cannot contain port.");
                                    }
                                    if (domain.trim().length == 0) {
                                        return this.i18n._("Invalid domain specified");
                                    }
                                }
                                return true;
                            }, this),
                        listeners: {
                            "change": {
                                fn: function(elem,newValue,oldValue) {
                                    this.settings.restrictGoogleAppsDomain = newValue;
                                },
                                scope: this
                            }
                        }
                    }]
                },{
                    xtype: "combo",
                    editable: false,
                    queryMode: 'local',
                    fieldLabel: this.i18n._("Unblock"),
                    name: "user_bypass",
                    store: [["None", this.i18n._("None")],
                            ["Host", this.i18n._("Temporary")],
                            ["Global", this.i18n._("Permanent and Global")]],
                    value: this.settings.unblockMode,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.unblockMode = newValue;
                            }, this)
                        }
                    }
                }]
            }],
        });
        return this.panelAdvanced;
    },
    // Event Log
    buildEventLog: function() {
        //helpSource: 'web_filter_event_log',
        //helpSource: 'web_filter_lite_event_log',
        this.gridEventLog = Ung.CustomEventLog.buildHttpEventLog (this, 'EventLog', i18n._('Event Log'),
                this.helpSourceName + '_event_log',
                ['time_stamp','username','c_client_addr','c_server_addr','s_server_port','host','uri',this.getRpcNode().getName() + '_blocked',
                 this.getRpcNode().getName() + '_flagged',this.getRpcNode().getName() + '_category',this.getRpcNode().getName() + '_reason'],
                this.getRpcNode().getEventQueries);
        return this.gridEventLog;
    },
    // private method
    alterUrls: function(saveList) {
        if (saveList != null) {
            var list = saveList.list;
            for (var i = 0; i < list.length; i++) {
                list[i]["string"] = this.alterUrl(list[i]["string"]);
            }
        }
    },
    // private method
    alterUrl: function(value) {
        // strip "http://" from beginning of rule
        // strip "www." from beginning of rule
        // strip "*." from beginning of rule
        // strip "/" from the end
        if (value.indexOf("http://") == 0) {
            value = value.substr(7);
        }
        if (value.indexOf("www.") == 0) {
            value = value.substr(4);
        }
        if (value.indexOf("*.") == 0) {
            value = value.substr(2);
        }
        if (value.indexOf("/") == value.length - 1) {
            value = value.substring(0, value.length - 1);
        }
        return value.trim();
    }
});
//# sourceURL=base-webfilter-settings.js