if (!Ung.hasResource["Ung.BaseWebFilter"]) {
    Ung.hasResource["Ung.BaseWebFilter"] = true;
    Ung.NodeWin.registerClassName('untangle-base-webfilter', 'Ung.BaseWebFilter');

    Ext.define('Ung.BaseWebFilter', {
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

        //
        buildUrlValidator: function(){
            this.urlValidator = Ext.bind(function(fieldValue) {
                if (fieldValue.indexOf("https://") == 0) {
                    return this.i18n._("\"URL\" specified cannot be blocked because it uses secure http (https)");
                }
                // strip "http://" from beginning of rule
                if (fieldValue.indexOf("http://") == 0) {
                    fieldValue = fieldValue.substr(7);
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
            this.gridCategories = Ext.create('Ung.EditorGrid',{
                name: 'Categories',
                title: this.i18n._("Categories"),
                sizetoParent: true,
                settingsCmp: this,
                hasAdd: false,
                hasDelete: false,
                dataProperty: "categories",
                recordJavaClass: "com.untangle.uvm.node.GenericRule",
                fields: this.genericRuleFields,
                paginated: false,
                flex: 1,
                columns: [{
                    header: this.i18n._("Category"),
                    width: 200,
                    dataIndex: 'name'
                },{
                    xtype:'checkcolumn',
                    width:55,
                    header: this.i18n._("Block"),
                    dataIndex: 'blocked',
                    resizable: false,
                    listeners: {
                        "checkchange": {
                            fn: Ext.bind(function(elem, rowIndex, checked) {
                                    if (checked) {
                                        var record = this.NEWgridCategories.getStore().getAt(rowIndex);
                                        record.set('flagged', true);
                                    }
                                }, this)
                            }
                    }
                },{
                    xtype:'checkcolumn',
                    width:55,
                    header: this.i18n._("Flag"),
                    dataIndex: 'flagged',
                    resizable: false,
                    tooltip: this.i18n._("Flag as Violation")
                },{
                    header: this.i18n._("Description"),
                    flex:1,
                    width: 400,
                    dataIndex: 'description',
                    editor:{
                        xtype:'textfield',
                        allowBlank: false
                    }
                }],
                sortField: 'name',
                columnsDefaultSortable: true,
                rowEditorInputLines: [
                    {
                        xtype:'textfield',
                        name: "Category",
                        dataIndex: "name",
                        fieldLabel: this.i18n._("Category"),
                        allowBlank: false,
                        width: 400,
                        disabled: true
                    },{
                        xtype:'checkbox',
                        name: "Block",
                        dataIndex: "blocked",
                        fieldLabel: this.i18n._("Block"),
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                        var rowEditor = this.NEWgridCategories.rowEditor;
                                        if (checked) {
                                            rowEditor.down('checkbox[name="Flag"]').setValue(true);
                                        }
                                    }, this)
                                }
                        }
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
                        width: 400,
                        height: 60
                    }]
            });
            
            this.blockedCategoriesPanel = Ext.create('Ext.panel.Panel',{
                name: 'BlockCategories',
                title: this.i18n._('Block Categories'),
                helpSource: this.helpSourceName + '_block_categories',
                parentId: this.getId(),
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
                    title: this.i18n . _("Block Categories"),
                    html: this.i18n . _("Block or flag access to sites associated with the specified category."),
                    style: "margin-bottom: 10px;"
                },
                    this.gridCategories
                ]
            });
            
            return this.blockedCategoriesPanel;
        },
        
        // Blocked sites
        buildPanelBlockedSites: function() {
            this.gridBlockedSites = Ext.create('Ung.EditorGrid',{
                name: 'Sites',
                title: this.i18n._("Sites"),
                settingsCmp: this,
                emptyRow: {
                    "string": this.i18n._("[no site]"),
                    "blocked": true,
                    "flagged": true,
                    "description": this.i18n._("[no description]")
                },
                flex: 1,    
                paginated: false,
                dataProperty: "blockedUrls",
                recordJavaClass: "com.untangle.uvm.node.GenericRule",
                fields: this.genericRuleFields,
                columns: [{
                    header: this.i18n._("Site"),
                    width: 200,
                    dataIndex: 'string',
                    editor: {
                        xtype:'textfield',
                        allowBlank: false,
                        validator: this.urlValidator,
                        blankText: this.i18n._("Invalid \"URL\" specified")
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
                    width: 400,
                    validator: this.urlValidator,
                    blankText: this.i18n._("Invalid \"URL\" specified")
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
                    width: 400,
                     height: 60
                }]
            });
            
            this.blockedSitesPanel = Ext.create('Ext.panel.Panel',{
                name: 'BlockSites',
                title: this.i18n._('Block Sites'),
                helpSource: this.helpSourceName + '_block_sites',
                parentId: this.getId(),
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
                    title: this.i18n . _("Blocked Sites"),
                    html: this.i18n . _("Block or flag access to the specified site."),
                    style: "margin-bottom: 10px;"
                },
                    this.gridBlockedSites
                ]
            });
            
            return this.blockedSitesPanel;
        },
        
        // Blocked File Types
        buildPanelBlockedFileTypes: function() {
            this.gridBlockedFileTypes = Ext.create('Ung.EditorGrid',{
                flex: 1,
                name: 'File Types',
                settingsCmp: this,
                emptyRow: {
                    "string": this.i18n._("[no extension]"),
                    "blocked": true,
                    "flagged": true,
                    "category": this.i18n._("[no category]"),
                    "description": this.i18n._("[no description]")
                },
                title: this.i18n._("File Types"),
                dataProperty: "blockedExtensions",
                recordJavaClass: "com.untangle.uvm.node.GenericRule",
                fields: this.genericRuleFields,
                sizetoParent: true,
                paginated: false,                
                columns: [{
                    header: this.i18n._("File Type"),
                    width: 200,
                    dataIndex: 'string',
                    editor: {
                        xtype: 'textfield',
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
                        allowBlank:false
                    }
                },{
                    header: this.i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex:1,
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
                    width: 300
                },{
                    xtype:'textarea',
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    width: 400,
                    height: 60
                }]
            });
            
            this.blockedFileTypesPanel = Ext.create('Ext.panel.Panel',{
                name: 'BlockFileTypes',
                title: this.i18n._('Block File Types'),
                helpSource: this.helpSourceName + '_block_filetypes',
                parentId: this.getId(),
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
                    title: this.i18n . _("Block File Types"),
                    html: this.i18n . _("Block or flag access to files associated with the specified file type."),
                    style: "margin-bottom: 10px;"
                },
                    this.gridBlockedFileTypes
                ]
            });
            
            return this.blockedFileTypesPanel;
        },
        
        // Blocked MIME Types
        buildPanelBlockedMimeTypes: function() {
            this.gridBlockedMimeTypes = Ext.create('Ung.EditorGrid',{
                name: 'MIME Types',
                paginated: false,
                settingsCmp: this,
                emptyRow: {
                    "string": this.i18n._("[no mime type]"),
                    "blocked": true,
                    "flagged": true,
                    "category": this.i18n._("[no category]"),
                    "description": this.i18n._("[no description]")
                },
                title: this.i18n._("MIME Types"),
                recordJavaClass: "com.untangle.uvm.node.GenericRule",
                fields: this.genericRuleFields,
                dataProperty: "blockedMimeTypes",
                flex: 1,    
                columns: [{
                    header: this.i18n._("MIME type"),
                    width: 200,
                    dataIndex: 'string',
                    editor: {
                        xtype:'textfield',
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
                        allowBlank:true
                    }
                },{
                    header: this.i18n._("Description"),
                    width: 200,
                    flex:1,
                    dataIndex: 'description',
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }],
                sortField: 'string',
                columnsDefaultSortable: true,
                rowEditorInputLines: [{
                    xtype:'textfield',
                    name: "MIME Type",
                    dataIndex: "string",
                    fieldLabel: this.i18n._("MIME Type"),
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
                    width: 300
                },{
                    xtype:'textarea',
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    width: 400,
                    height: 60
                }]
            });
            
            this.blockedMimeTypesPanel = Ext.create('Ext.panel.Panel',{
                name: 'BlockMimeTypes',
                title: this.i18n._('Block Mime Types'),
                helpSource: this.helpSourceName + '_block_mimetypes',
                parentId: this.getId(),
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
                    title: this.i18n . _("Block MIME Types"),
                    html: this.i18n . _("Block or flag access to files associated with the specified MIME type."),
                    style: "margin-bottom: 10px;"
                },
                    this.gridBlockedMimeTypes
                ]
            });
            
            return this.blockedMimeTypesPanel;
        },

        // Allowed Sites
        buildPanelPassedSites: function() {
            this.gridAllowedSites = Ext.create('Ung.EditorGrid',{
                name: 'Sites',
                settingsCmp: this,
                emptyRow: {
                    "string": this.i18n._("[no site]"),
                    "enabled": true,
                    "description": this.i18n._("[no description]")
                },
                paginated: false,                
                flex: 1,    
                title: this.i18n._("Sites"),
                dataProperty: "passedUrls",
                recordJavaClass: "com.untangle.uvm.node.GenericRule",
                fields: this.genericRuleFields,
                columns: [{
                    header: this.i18n._("Site"),
                    width: 200,
                    dataIndex: 'string',
                    editor: {
                        xtype:'textfield',
                        allowBlank: false,
                        validator: this.urlValidator,
                        blankText: this.i18n._("Invalid \"URL\" specified")
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
                    width: 400,
                    validator: this.urlValidator,
                    blankText: this.i18n._("Invalid \"URL\" specified")
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
                    width: 400,
                    height: 60
                }]
            });

            this.allowedSitesPanel = Ext.create('Ext.panel.Panel',{
                name: 'PassSites',
                helpSource: this.helpSourceName + '_pass_sites',
                parentId: this.getId(),

                title: this.i18n._('Pass Sites'),
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
                    html: this.i18n . _("Allow access to the specified site regardless of matching block policies."),
                    style: "margin-bottom: 10px;"
                },
                    this.gridAllowedSites
                ]
            });
            
            return this.allowedSitesPanel;
        },
        
        // Allowed Clients
        buildPanelPassedClients: function() {
            this.gridAllowedClients = Ext.create('Ung.EditorGrid',{
                name: 'Client IP addresses',
                paginated: false,                
                settingsCmp: this,
                emptyRow: {
                    "string": "1.2.3.4",
                    "enabled": true,
                    "description": this.i18n._("[no description]")
                },
                title: this.i18n._("Client IP addresses"),
                dataProperty: "passedClients",
                recordJavaClass: "com.untangle.uvm.node.GenericRule",
                fields: this.genericRuleFields,
                flex: 1,    
                columns: [{
                    header: this.i18n._("IP address/range"),
                    width: 200,
                    dataIndex: 'string',
                    editor: {
                        xtype:'textfield',
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
                        allowBlank:false
                    }
                }],
                sortField: 'string',
                columnsDefaultSortable: true,
                rowEditorInputLines: [
                {
                    xtype:'textfield',
                    name: "IP address/range",
                    dataIndex: "string",
                    fieldLabel: this.i18n._("IP address/range"),
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
                    width: 400,
                    height: 60
                }]
            });

            this.allowedClientsPanel = Ext.create('Ext.panel.Panel',{
                name: 'PassClients',
                helpSource: this.helpSourceName + '_pass_clients',
                parentId: this.getId(),

                title: this.i18n._('Pass Clients'),
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
                    title: this.i18n . _("Pass Clients"),
                    html: this.i18n . _("Allow access for client networks regardless of matching block policies."),
                    style: "margin-bottom: 10px;"
                },
                    this.gridAllowedClients
                ]
            });
            
            return this.allowedClientsPanel;
        },
        // Advanced options
        buildPanelAdvanced: function() {
            this.panelAdvanced = Ext.create('Ext.panel.Panel',{
                name: 'Advanced',
                helpSource: this.helpSourceName + '_advanced',

                title: this.i18n._('Advanced'),
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    xtype: 'fieldset',
                    buttonAlign: 'left'
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
}
//@ sourceURL=base-webfilter-settings.js
