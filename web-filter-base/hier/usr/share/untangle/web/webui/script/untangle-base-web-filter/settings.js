Ext.define('Webui.web-filter-base.settings', {
    extend:'Ung.AppWin',
    // called when the component is rendered
    initComponent: function() {
        this.buildUrlValidator();
        this.genericRuleFields = Ung.Util.getGenericRuleFields(this);
        this.buildTabPanel([
            this.buildPanelCategories(),
            this.buildPanelBlockedSites(),
            this.buildPanelPassedSites(),
            this.buildPanelPassedClients(),
            this.buildGridFilterRules(),
            this.buildPanelAdvanced()
        ]);
        this.callParent(arguments);
    },
    getConditions: function () {
        return [
            {name:"DST_ADDR",displayName: i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"DST_PORT",displayName: i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
            {name:"DST_INTF",displayName: i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
            {name:"SRC_ADDR",displayName: i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: i18n._("Source Port"), type: "text", vtype:"portMatcher", visible: rpc.isExpertMode},
            {name:"SRC_INTF",displayName: i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
            {name:"PROTOCOL",displayName: i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["any", i18n._("any")]], visible: true},
            {name:"USERNAME",displayName: i18n._("Username"), type: "editor", editor: Ext.create('Ung.UserEditorWindow',{}), visible: true},
            {name:"CLIENT_HOSTNAME",displayName: i18n._("Client Hostname"), type: "text", visible: true},
            {name:"SERVER_HOSTNAME",displayName: i18n._("Server Hostname"), type: "text", visible: rpc.isExpertMode},
            {name:"SRC_MAC", displayName: i18n._("Client MAC Address"), type: "text", visible: true },
            {name:"DST_MAC", displayName: i18n._("Server MAC Address"), type: "text", visible: true },
            {name:"CLIENT_MAC_VENDOR",displayName: i18n._("Client MAC Vendor"), type: "text", visible: true},
            {name:"SERVER_MAC_VENDOR",displayName: i18n._("Server MAC Vendor"), type: "text", visible: true},
            {name:"CLIENT_IN_PENALTY_BOX",displayName: i18n._("Client in Penalty Box"), type: "boolean", visible: true},
            {name:"SERVER_IN_PENALTY_BOX",displayName: i18n._("Server in Penalty Box"), type: "boolean", visible: true},
            {name:"CLIENT_HAS_NO_QUOTA",displayName: i18n._("Client has no Quota"), type: "boolean", visible: true},
            {name:"SERVER_HAS_NO_QUOTA",displayName: i18n._("Server has no Quota"), type: "boolean", visible: true},
            {name:"CLIENT_QUOTA_EXCEEDED",displayName: i18n._("Client has exceeded Quota"), type: "boolean", visible: true},
            {name:"SERVER_QUOTA_EXCEEDED",displayName: i18n._("Server has exceeded Quota"), type: "boolean", visible: true},
            {name:"CLIENT_QUOTA_ATTAINMENT",displayName: i18n._("Client Quota Attainment"), type: "text", visible: true},
            {name:"SERVER_QUOTA_ATTAINMENT",displayName: i18n._("Server Quota Attainment"), type: "text", visible: true},
            {name:"HTTP_HOST",displayName: i18n._("HTTP: Hostname"), type: "text", visible: true},
            {name:"HTTP_REFERER",displayName: i18n._("HTTP: Referer"), type: "text", visible: true},
            {name:"HTTP_URI",displayName: i18n._("HTTP: URI"), type: "text", visible: true},
            {name:"HTTP_URL",displayName: i18n._("HTTP: URL"), type: "text", visible: true},
            {name:"HTTP_CONTENT_LENGTH",displayName: i18n._("HTTP: Content Length"), type: "text", visible: true},
            {name:"HTTP_REQUEST_METHOD",displayName: i18n._("HTTP: Request Method"), type: "text", visible: true},
            {name:"WEB_FILTER_REQUEST_METHOD",displayName: i18n._("HTTP: Request Method"), type: "text", visible: false},
            {name:"HTTP_REQUEST_FILE_PATH",displayName: i18n._("HTTP: Request File Path"), type: "text", visible: true},
            {name:"WEB_FILTER_REQUEST_FILE_PATH",displayName: i18n._("HTTP: Request File Path"), type: "text", visible: false},
            {name:"HTTP_REQUEST_FILE_NAME",displayName: i18n._("HTTP: Request File Name"), type: "text", visible: true},
            {name:"WEB_FILTER_REQUEST_FILE_NAME",displayName: i18n._("HTTP: Request File Name"), type: "text", visible: false},
            {name:"HTTP_REQUEST_FILE_EXTENSION",displayName: i18n._("HTTP: Request File Extension"), type: "text", visible: true},
            {name:"WEB_FILTER_REQUEST_FILE_EXTENSION",displayName: i18n._("HTTP: Request File Extension"), type: "text", visible: false},
            {name:"HTTP_CONTENT_TYPE",displayName: i18n._("HTTP: Content Type"), type: "text", visible: true},
            {name:"WEB_FILTER_RESPONSE_CONTENT_TYPE",displayName: i18n._("HTTP: Content Type"), type: "text", visible: false},
            {name:"HTTP_RESPONSE_FILE_NAME",displayName: i18n._("HTTP: Response File Name"), type: "text", visible: true},
            {name:"WEB_FILTER_RESPONSE_FILE_NAME",displayName: i18n._("HTTP: Response File Name"), type: "text", visible: false},
            {name:"HTTP_RESPONSE_FILE_EXTENSION",displayName: i18n._("HTTP: Response File Extension"), type: "text", visible: true},
            {name:"WEB_FILTER_RESPONSE_FILE_EXTENSION",displayName: i18n._("HTTP: Response File Extension"), type: "text", visible: false},
            {name:"HTTP_USER_AGENT",displayName: i18n._("HTTP: Client User Agent"), type: "text", visible: true},
            {name:"HTTP_USER_AGENT_OS",displayName: i18n._("HTTP: Client User OS"), type: "text", visible: false},
            {name:"APPLICATION_CONTROL_APPLICATION",displayName: i18n._("Application Control: Application"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_CATEGORY",displayName: i18n._("Application Control: Application Category"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_PROTOCHAIN",displayName: i18n._("Application Control: ProtoChain"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_DETAIL",displayName: i18n._("Application Control: Detail"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_CONFIDENCE",displayName: i18n._("Application Control: Confidence"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_PRODUCTIVITY",displayName: i18n._("Application Control: Productivity"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_RISK",displayName: i18n._("Application Control: Risk"), type: "text", visible: true},
            {name:"PROTOCOL_CONTROL_SIGNATURE",displayName: i18n._("Application Control Lite: Signature"), type: "text", visible: true},
            {name:"PROTOCOL_CONTROL_CATEGORY",displayName: i18n._("Application Control Lite: Category"), type: "text", visible: true},
            {name:"PROTOCOL_CONTROL_DESCRIPTION",displayName: i18n._("Application Control Lite: Description"), type: "text", visible: true},
            {name:"WEB_FILTER_CATEGORY",displayName: i18n._("Web Filter: Category"), type: "text", visible: true},
            {name:"WEB_FILTER_CATEGORY_DESCRIPTION",displayName: i18n._("Web Filter: Category Description"), type: "text", visible: true},
            {name:"WEB_FILTER_FLAGGED",displayName: i18n._("Web Filter: Website is Flagged"), type: "boolean", visible: true},
            {name:"DIRECTORY_CONNECTOR_GROUP",displayName: i18n._("Directory Connector: User in Group"), type: "editor", editor: Ext.create('Ung.GroupEditorWindow',{}), visible: true},
            {name:"CLIENT_COUNTRY",displayName: i18n._("Client Country"), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true},
            {name:"SERVER_COUNTRY",displayName: i18n._("Server Country"), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true}
        ];
    },
    beforeSave: function(isApply, handler) {
        this.settings.categories.list=this.gridCategories.getList();
        this.settings.blockedUrls.list=this.gridBlockedSites.getList();
        this.settings.passedUrls.list=this.gridAllowedSites.getList();
        this.settings.passedClients.list=this.gridAllowedClients.getList();
        this.settings.filterRules.list=this.gridFilterRules.getList();
        handler.call(this, isApply);
    },
    buildUrlValidator: function(){
        this.urlValidator = Ext.bind(function(fieldValue) {
            if (fieldValue.match( /^([^:]+):\/\// ) != null ){
                return i18n._("Site cannot contain URL protocol.");
            }
            if (fieldValue.match( /^([^:]+):\d+\// ) != null ){
                return i18n._("Site cannot contain port.");
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
                return i18n._("Invalid URL specified");
            }
            return true;
        }, this);
    },

    // Block Lists Panel
    buildPanelCategories: function() {
        this.gridCategories = Ext.create('Ung.grid.Panel',{
            flex: 1,
            name: 'Categories',
            title: i18n._("Categories"),
            sizetoParent: true,
            settingsCmp: this,
            hasAdd: false,
            hasDelete: false,
            dataProperty: "categories",
            recordJavaClass: "com.untangle.uvm.app.GenericRule",
            sortField: 'name',
            fields: this.genericRuleFields,
            columns: [{
                header: i18n._("Category"),
                width: 200,
                dataIndex: 'name'
            }, {
                xtype:'checkcolumn',
                width:55,
                header: i18n._("Block"),
                dataIndex: 'blocked',
                resizable: false,
                listeners: {
                    "checkchange": {
                        fn: Ext.bind(function(elem, rowIndex, checked) {
                            if (checked) {
                                var record = elem.getView().getRecord(elem.getView().getRow(rowIndex));
                                record.set('flagged', true);
                            }
                        }, this)
                    }
                },
                checkAll: {
                    handler: function(checkbox, checked) {
                        var records=checkbox.up("grid").getStore().getRange();
                        for(var i=0; i<records.length; i++) {
                            records[i].set('blocked', checked);
                            if(checked) {
                                records[i].set('flagged', true);
                            }
                        }
                    }
                }
            }, {
                xtype:'checkcolumn',
                width:55,
                header: i18n._("Flag"),
                dataIndex: 'flagged',
                resizable: false,
                tooltip: i18n._("Flag as Violation"),
                checkAll: {}
            }, {
                header: i18n._("Description"),
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
                fieldLabel: i18n._("Category"),
                allowBlank: false,
                width: 400,
                disabled: true
            }, {
                xtype:'checkbox',
                name: "Block",
                dataIndex: "blocked",
                fieldLabel: i18n._("Block"),
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
                fieldLabel: i18n._("Flag"),
                tooltip: i18n._("Flag as Violation")
            }, {
                xtype:'textarea',
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                width: 400,
                height: 60
            }]
        });
        this.categoriesPanel = Ext.create('Ext.panel.Panel',{
            name: 'Categories',
            title: i18n._('Categories'),
            //helpSource: 'web_filter_block_categories',
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
                name: 'categoriesPanelHeader',
                title: i18n . _("Categories"),
                html: i18n . _("Block or flag access to sites associated with the specified category.")
            }, this.gridCategories ]
        });
        return this.categoriesPanel;
    },

    // Blocked sites
    buildPanelBlockedSites: function() {
        this.gridBlockedSites = Ext.create('Ung.grid.Panel',{
            name: 'Sites',
            title: i18n._("Sites"),
            settingsCmp: this,
            flex: 1,
            dataProperty: "blockedUrls",
            recordJavaClass: "com.untangle.uvm.app.GenericRule",
            emptyRow: {
                "string": "",
                "blocked": true,
                "flagged": true,
                "description": ""
            },
            sortField: 'string',
            fields: this.genericRuleFields,
            columns: [{
                header: i18n._("Site"),
                width: 200,
                dataIndex: 'string',
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter site]"),
                    allowBlank: false,
                    validator: this.urlValidator
                }
            },{
                xtype:'checkcolumn',
                width:55,
                header: i18n._("Block"),
                dataIndex: 'blocked',
                resizable: false,
                checkAll: {}
            },{
                xtype:'checkcolumn',
                width:55,
                header: i18n._("Flag"),
                dataIndex: 'flagged',
                resizable: false,
                tooltip: i18n._("Flag as Violation"),
                checkAll: {}
            },{
                header: i18n._("Description"),
                width: 200,
                flex:1,
                dataIndex: 'description',
                editor: {
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
                width: 400,
                validator: this.urlValidator
            },{
                xtype:'checkbox',
                name: "Block",
                dataIndex: "blocked",
                fieldLabel: i18n._("Block")
            },{
                xtype:'checkbox',
                name: "Flag",
                dataIndex: "flagged",
                fieldLabel: i18n._("Flag"),
                tooltip: i18n._("Flag as Violation")
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
        this.blockedSitesPanel = Ext.create('Ext.panel.Panel',{
            name: 'BlockSites',
            title: i18n._('Block Sites'),
            //helpSource: 'web_filter_block_sites',
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
                name: 'blockedSitesPanelHeader',
                title: i18n . _("Blocked Sites"),
                html: i18n . _("Block or flag access to the specified site.")
            }, this.gridBlockedSites ]
        });
        return this.blockedSitesPanel;
    },

    // Allowed Sites
    buildPanelPassedSites: function() {
        this.gridAllowedSites = Ext.create('Ung.grid.Panel',{
            name: 'Sites',
            settingsCmp: this,
            flex: 1,
            title: i18n._("Sites"),
            dataProperty: "passedUrls",
            recordJavaClass: "com.untangle.uvm.app.GenericRule",
            emptyRow: {
                "string": "",
                "enabled": true,
                "description": ""
            },
            sortField: 'string',
            fields: this.genericRuleFields,
            columns: [{
                header: i18n._("Site"),
                width: 200,
                dataIndex: 'string',
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter site]"),
                    allowBlank: false,
                    validator: this.urlValidator
                }
            },{
                xtype:'checkcolumn',
                width:55,
                header: i18n._("Pass"),
                dataIndex: 'enabled',
                resizable: false
            },{
                header: i18n._("Description"),
                flex:1,
                width: 200,
                dataIndex: 'description',
                editor: {
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
                width: 400,
                validator: this.urlValidator
            },{
                xtype:'checkbox',
                name: "Pass",
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
        this.allowedSitesPanel = Ext.create('Ext.panel.Panel',{
            name: 'PassSites',
            //helpSource: 'web_filter_pass_sites',
            helpSource: this.helpSourceName + '_pass_sites',
            title: i18n._('Pass Sites'),
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
                name: 'allowedSitesPanelHeader',
                title: i18n . _("Pass Sites"),
                html: i18n . _("Allow access to the specified site regardless of matching block policies.")
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
            title: i18n._("Client IP addresses"),
            dataProperty: "passedClients",
            recordJavaClass: "com.untangle.uvm.app.GenericRule",
            emptyRow: {
                "string": "1.2.3.4",
                "enabled": true,
                "description": ""
            },
            sortField: 'string',
            fields: this.genericRuleFields,
            columns: [{
                header: i18n._("IP address/range"),
                width: 200,
                dataIndex: 'string',
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter IP address/range]"),
                    vtype:"ipMatcher",
                    allowBlank:false
                }
            },{
                xtype:'checkcolumn',
                width:55,
                header: i18n._("Pass"),
                dataIndex: 'enabled',
                resizable: false
            },{
                header: i18n._("Description"),
                flex:1,
                width: 200,
                dataIndex: 'description',
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[no description]")
                }
            }],
            rowEditorInputLines: [{
                xtype:'textfield',
                name: "IP address/range",
                dataIndex: "string",
                fieldLabel: i18n._("IP address/range"),
                emptyText: i18n._("[enter IP address/range]"),
                vtype:"ipMatcher",
                allowBlank: false,
                width: 400
            },{
                xtype:'checkbox',
                name: "Pass",
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
        this.allowedClientsPanel = Ext.create('Ext.panel.Panel',{
            name: 'PassClients',
            //helpSource: 'web_filter_pass_clients',
            helpSource: this.helpSourceName + '_pass_clients',
            title: i18n._('Pass Clients'),
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
                name: 'allowedClientsPanelHeader',
                title: i18n . _("Pass Clients"),
                html: i18n . _("Allow access for client networks regardless of matching block policies.")
            }, this.gridAllowedClients ]
        });
        return this.allowedClientsPanel;
    },

    // Rules
    buildGridFilterRules: function() {
        this.gridFilterRules = Ext.create('Ung.grid.Panel',{
            name: "gridFilterRules",
            //helpSource: 'web_filter_rules',
            helpSource: this.helpSourceName + '_rules',
            settingsCmp: this,
            height: 500,
            hasReorder: true,
            title: i18n._("Rules"),
            qtip: i18n._("Web Filter rules allow creating flexible block and pass conditions."),
            dataProperty: "filterRules",
            recordJavaClass: "com.untangle.app.web_filter.WebFilterRule",
            emptyRow: {
                "enabled": true,
                "ruleId": 0,
                "flagged": true,
                "blocked": false,
                "description": ""
            },
            fields: [{
                name: 'enabled'
            },{
                name: 'ruleId'
            },{
                name: 'flagged'
            },{
                name: 'blocked'
            },{
                name: 'description'
            },{
                name: 'conditions'
            }],
            columns:[{
                xtype:'checkcolumn',
                width:65,
                header: i18n._("Enabled"),
                dataIndex: 'enabled',
                resizable: false
            }, {
                header: i18n._("Rule ID"),
                dataIndex: 'ruleId',
                width: 70,
                renderer: function(value) {
                    if (value < 0) {
                        return i18n._("new");
                    } else {
                        return value;
                    }
                }
            }, {
                xtype:'checkcolumn',
                width:65,
                header: i18n._("Flagged"),
                dataIndex: 'flagged',
                resizable: false
            }, {
                xtype:'checkcolumn',
                width:65,
                header: i18n._("Blocked"),
                dataIndex: 'blocked',
                resizable: false
            }, {
                header: i18n._("Description"),
                dataIndex:'description',
                flex:1,
                width: 200
            }],
            rowEditorInputLines: [{
                xtype: "checkbox",
                name: "Enabled",
                dataIndex: "enabled",
                fieldLabel: i18n._( "Enabled" ),
                width: 100
            }, {
                xtype: "textfield",
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._( "Description" ),
                emptyText: i18n._("[no description]"),
                width: 480
            }, {
                xtype: "fieldset",
                autoScroll: true,
                title: "If all of the following conditions are met:",
                items:[{
                    xtype: 'rulebuilder',
                    settingsCmp: this,
                    javaClass: "com.untangle.app.web_filter.WebFilterRuleCondition",
                    dataIndex: "conditions",
                    conditions: this.getConditions()
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('Perform the following actions:'),
                items:[{
                    xtype: "checkbox",
                    name: "Flag",
                    dataIndex: "flagged",
                    fieldLabel: i18n._( "Flag" ),
                    width: 100
                }, {
                    xtype: "checkbox",
                    name: "Block",
                    dataIndex: "blocked",
                    fieldLabel: i18n._( "Block" ),
                    width: 100
                }]
            }]
        });
        return this.gridFilterRules;
    },

    // Advanced options
    buildPanelAdvanced: function() {
        this.panelAdvanced = Ext.create('Ext.panel.Panel',{
            name: 'Advanced',
            //helpSource: 'web_filter_advanced',
            helpSource: this.helpSourceName + '_advanced',
            title: i18n._('Advanced'),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                name: "fieldset_miscellaneous",
                title: i18n._("Advanced Options"),
                items: [{
                    xtype: "checkbox",
                    boxLabel: i18n._("Block pages from IP only hosts"),
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
                    boxLabel: i18n._("Pass if referers matches Pass Sites"),
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
                    boxLabel: i18n._("Restrict Google applications"),
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
                        value: i18n._("NOTE:") + "&nbsp;" + "<i>SSL Inspector</i> " + i18n._("must be installed and running with the Inspect Google Traffic configured to Inspect."),
                        style: {
                            marginBottom: '10px'
                        }
                    },{
                        xtype: "textfield",
                        fieldLabel: i18n._("Allowed Domain(s)"),
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
                                        return i18n._("Domain cannot contain URL protocol.");
                                    }
                                    if( domain.match( /^([^:]+):\d+\// ) != null ){
                                        return i18n._("Domain cannot contain port.");
                                    }
                                    if (domain.trim().length == 0) {
                                        return i18n._("Invalid domain specified");
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
                    fieldLabel: i18n._("Unblock"),
                    name: "user_bypass",
                    store: [["None", i18n._("None")],
                            ["Host", i18n._("Temporary")],
                            ["Global", i18n._("Permanent and Global")]],
                    value: this.settings.unblockMode,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.unblockMode = newValue;
                            }, this)
                        }
                    }
                }]
            }]
        });
        return this.panelAdvanced;
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
//# sourceURL=base-web-filter-settings.js
