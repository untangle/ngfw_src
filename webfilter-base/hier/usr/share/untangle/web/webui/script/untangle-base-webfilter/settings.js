if (!Ung.hasResource["Ung.BaseWebFilter"]) {
    Ung.hasResource["Ung.BaseWebFilter"] = true;
    Ung.NodeWin.registerClassName('untangle-base-webfilter', 'Ung.BaseWebFilter');

    Ext.define('Ung.BaseWebFilter', {
        extend:'Ung.NodeWin',
        gridExceptions: null,
        gridEventLog: null,
        // called when the component is rendered
        initComponent: function() {
            this.genericRuleFields = Ung.Util.getGenericRuleFields(this);
            this.buildBlockLists();
            this.buildPassLists();
            this.buildEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelBlockLists, this.panelPassLists, this.gridEventLog]);
            this.callParent(arguments);
        },
        // Block Lists Panel
        buildBlockLists: function() {
            this.panelBlockLists = Ext.create('Ext.panel.Panel',{
                name: 'Block Lists',
                helpSource: 'block_lists',
                // private fields
                winCategories: null,
                winBlockedUrls: null,
                winBlockedExtensions: null,
                winBlockedMimeTypes: null,
                parentId: this.getId(),

                title: this.i18n._('Block Lists'),
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    xtype: 'fieldset',
                    buttonAlign: 'left'
                },
                items: [{
                    name: "fieldset_manage_categories",
                    items: [{
                        xtype: "button",
                        name: "manage_categories",
                        text: this.i18n._("Edit Categories"),
                        handler: Ext.bind(function() {
                            this.panelBlockLists.onManageCategories();
                        }, this)
                    }]
                },{
                    items: [{
                        xtype: "button",
                        name: 'manage_sites',
                        text: this.i18n._("Edit Sites"),
                        handler: Ext.bind(function() {
                            this.panelBlockLists.onManageBlockedUrls();
                        }, this)
                    }]
                },{
                    items: [{
                        xtype: "button",
                        name: "manage_file_types",
                        text: this.i18n._("Edit File Types"),
                        handler: Ext.bind(function() {
                            this.panelBlockLists.onManageBlockedExtensions();
                        }, this)
                    }]
                },{
                    items: [{
                        xtype: "button",
                        name: "manage_mime_types",
                        text: this.i18n._("Edit MIME Types"),
                        handler: Ext.bind(function() {
                            this.panelBlockLists.onManageBlockedMimeTypes();
                        }, this)
                    }]
                },{
                    name: "fieldset_miscellaneous",
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
                        xtype: "combo",
                        editable: false,
                        queryMode: 'local',
                        fieldLabel: this.i18n._("Unblock"),
                        name: "user_bypass",
                        store: [["None", this.i18n._("None")],
                                    ["Host", this.i18n._("Temporary")],
                                    ["Global", this.i18n._("Permanent and Global")]],
                        displayField: "unblockModeName",
                        valueField: "unblockModeValue",
                        value: this.settings.unblockMode,
                        triggerAction: "all",
                        listClass: 'x-combo-list-small',
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.settings.unblockMode = newValue;
                                }, this)
                            }
                        }
                    }]
                }],

                onManageCategories: function() {
                    if (!this.winCategories) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildCategories();
                        this.winCategories = Ext.create('Ung.ManageListWindow',{
                            breadcrumbs: [{
                                title: i18n._(rpc.currentPolicy.name),
                                action: Ext.bind(function() {
                                    Ung.Window.cancelAction(
                                       this.gridCategories.isDirty() || this.isDirty(),
                                       Ext.bind(function() {
                                            this.panelBlockLists.winCategories.closeWindow();
                                            this.closeWindow();
                                       }, this)
                                    );
                                },settingsCmp)
                            }, {
                                title: settingsCmp.node.displayName,
                                action: Ext.bind(function() {
                                    this.panelBlockLists.winCategories.cancelAction();
                                },settingsCmp)
                            }, {
                                title: settingsCmp.i18n._("Categories")
                            }],
                            grid: settingsCmp.gridCategories,
                            applyAction: function(callback) {
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridCategories.getList(Ext.bind(function(saveList) {
                                    this.getRpcNode().setCategories(Ext.bind(function(result, exception) {
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getCategories(Ext.bind(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.settings.categories = result;
                                            this.gridCategories.clearDirty();
                                            if(callback != null) {
                                                callback();
                                            }
                                        }, this));
                                    }, this), saveList);
                                },settingsCmp));
                            }                                                        
                        });
                    }
                    this.winCategories.show();
                },
                onManageBlockedUrls: function() {
                    if (!this.winBlockedUrls) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildBlockedUrls();
                        this.winBlockedUrls = Ext.create('Ung.ManageListWindow',{
                            breadcrumbs: [{
                                title: i18n._(rpc.currentPolicy.name),
                                action: Ext.bind(function() {
                                    Ung.Window.cancelAction(
                                       this.gridBlockedUrls.isDirty() || this.isDirty(),
                                       Ext.bind(function() {
                                            this.panelBlockLists.winBlockedUrls.closeWindow();
                                            this.closeWindow();
                                       }, this)
                                    );
                                },settingsCmp)
                            }, {
                                title: settingsCmp.node.displayName,
                                action: Ext.bind(function() {
                                    this.panelBlockLists.winBlockedUrls.cancelAction();
                                },settingsCmp)
                            }, {
                                title: settingsCmp.i18n._("Sites")
                            }],
                            grid: settingsCmp.gridBlockedUrls,
                            applyAction: function(callback) {
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridBlockedUrls.getList(Ext.bind(function(saveList) {
                                    this.alterUrls(saveList);
                                    this.getRpcNode().setBlockedUrls(Ext.bind(function(result, exception) {
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getBlockedUrls(Ext.bind(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.settings.blockedUrls = result;
                                            this.gridBlockedUrls.clearDirty();
                                            if(callback != null) {
                                                callback();
                                            }
                                        }, this));
                                    }, this), saveList);
                                },settingsCmp));
                            }                                                        
                        });
                    }
                    this.winBlockedUrls.show();
                },
                onManageBlockedExtensions: function() {
                    if (!this.winBlockedExtensions) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildBlockedExtensions();
                        this.winBlockedExtensions = Ext.create('Ung.ManageListWindow',{
                            breadcrumbs: [{
                                title: i18n._(rpc.currentPolicy.name),
                                action: Ext.bind(function() {
                                    Ung.Window.cancelAction(
                                       this.gridBlockedExtensions.isDirty() || this.isDirty(),
                                       Ext.bind(function() {
                                            this.panelBlockLists.winBlockedExtensions.closeWindow();
                                            this.closeWindow();
                                       }, this)
                                    );
                                },settingsCmp)
                            }, {
                                title: settingsCmp.node.displayName,
                                action: Ext.bind(function() {
                                    this.panelBlockLists.winBlockedExtensions.cancelAction();
                                },settingsCmp)
                            }, {
                                title: settingsCmp.i18n._("File Types")
                            }],
                            grid: settingsCmp.gridBlockedExtensions,
                            applyAction: function(callback) {
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridBlockedExtensions.getList(Ext.bind(function(saveList) {
                                    this.getRpcNode().setBlockedExtensions(Ext.bind(function(result, exception) {
                                        Ext.MessageBox.hide();
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getBlockedExtensions(Ext.bind(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.settings.blockedExtensions = result;
                                            this.gridBlockedExtensions.clearDirty();
                                            if(callback != null) {
                                                callback();
                                            }
                                        }, this));
                                    }, this), saveList);
                                },settingsCmp));
                            }                                                        
                        });
                    }
                    this.winBlockedExtensions.show();
                },
                onManageBlockedMimeTypes: function() {
                    if (!this.winBlockedMimeTypes) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildBlockedMimeTypes();
                        this.winBlockedMimeTypes = Ext.create('Ung.ManageListWindow',{
                            breadcrumbs: [{
                                title: i18n._(rpc.currentPolicy.name),
                                action: Ext.bind(function() {
                                    Ung.Window.cancelAction(
                                       this.gridBlockedMimeTypes.isDirty() || this.isDirty(),
                                       Ext.bind(function() {
                                            this.panelBlockLists.winBlockedMimeTypes.closeWindow();
                                            this.closeWindow();
                                       }, this)
                                    );
                                },settingsCmp)
                            }, {
                                title: settingsCmp.node.displayName,
                                action: Ext.bind(function() {
                                    this.panelBlockLists.winBlockedMimeTypes.cancelAction();
                                },settingsCmp)
                            }, {
                                title: settingsCmp.i18n._("MIME Types")
                            }],
                            grid: settingsCmp.gridBlockedMimeTypes,
                            applyAction: function(callback) {
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridBlockedMimeTypes.getList(Ext.bind(function(saveList) {
                                    this.getRpcNode().setBlockedMimeTypes(Ext.bind(function(result, exception) {
                                        Ext.MessageBox.hide();
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getBlockedMimeTypes(Ext.bind(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.settings.blockedMimeTypes = result;
                                            this.gridBlockedMimeTypes.clearDirty();
                                            if(callback != null) {
                                                callback();
                                            }
                                        }, this));
                                    }, this), saveList);
                                },settingsCmp));
                            }
                        });
                    }
                    this.winBlockedMimeTypes.show();
                },
                beforeDestroy: function() {
                    Ext.destroy(this.winCategories, this.winBlockedUrls, this.winBlockedExtensions, this.winBlockedMimeTypes);
                    Ext.Panel.prototype.beforeDestroy.call(this);
                }
            });
        },
        // Block Categories
        buildCategories: function() {

        this.gridCategories = Ext.create('Ung.EditorGrid',{
                name: 'Categories',
                settingsCmp: this,
                hasAdd: false,
                hasDelete: false,
                title: this.i18n._("Categories"),
                dataProperty: "categories",
                recordJavaClass: "com.untangle.uvm.node.GenericRule",
                fields: this.genericRuleFields,
                paginated: false,
                columns: [{
                    header: this.i18n._("Category"),
                    width: 200,
                    dataIndex: 'name'
                }, 
                {
                    xtype:'checkcolumn',
                    width:55,
                    header: this.i18n._("Block"),
                    dataIndex: 'blocked',
                    fixed: true,
                    changeRecord: function(record) {
                        Ext.ux.CheckColumn.prototype.changeRecord.call(this, record);
                        var blocked = record.get(this.dataIndex);
                        if (blocked) {
                            record.set('flagged', true);
                        }
                    }
                },
                {
                    xtype:'checkcolumn',
                    width:55,
                    header: this.i18n._("Flag"),
                    dataIndex: 'flagged',
                    fixed: true,
                    tooltip: this.i18n._("Flag as Violation")
                },
                {
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
                        disabled: true,
                        //ctCls: "fixed-pos"
                    },
                    {
                        xtype:'checkbox',
                        name: "Block",
                        dataIndex: "blocked",
                        fieldLabel: this.i18n._("Block"),
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                        var rowEditor = this.gridCategories.rowEditor;
                                        if (checked) {
                                            rowEditor.inputLines[2].setValue(true);
                                        }
                                    }, this)
                                }
                        }
                    },
                    {
                        xtype:'checkbox',
                        name: "Flag",
                        dataIndex: "flagged",
                        fieldLabel: this.i18n._("Flag"),
                        tooltip: this.i18n._("Flag as Violation")
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
        // Block Sites
        buildBlockedUrls: function() {
            var urlValidator = Ext.bind(function(fieldValue) {
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

            this.gridBlockedUrls = Ext.create('Ung.EditorGrid',{
                name: 'Sites',
                settingsCmp: this,
                emptyRow: {
                    "string": this.i18n._("[no site]"),
                    "blocked": true,
                    "flagged": true,
                    "description": this.i18n._("[no description]")
                },
                title: this.i18n._("Sites"),
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
                        validator: urlValidator,
                        blankText: this.i18n._("Invalid \"URL\" specified")
                    }
                },
                {
                    xtype:'checkcolumn',
                    width:55,
                    header: this.i18n._("Block"),
                    dataIndex: 'blocked',
                    fixed: true
                },
                { 
                    xtype:'checkcolumn',
                    width:55,
                    header: this.i18n._("Flag"),
                    dataIndex: 'flagged',
                    fixed: true,
                    tooltip: this.i18n._("Flag as Violation")
                },
                {
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
                rowEditorInputLines: [
                    {
                        xtype:'textfield',
                        name: "Site",
                        dataIndex: "string",
                        fieldLabel: this.i18n._("Site"),
                        allowBlank: false,
                        width: 400,
                        validator: urlValidator,
                        blankText: this.i18n._("Invalid \"URL\" specified")
                    },
                    {
                        xtype:'checkbox',
                        name: "Block",
                        dataIndex: "blocked",
                        fieldLabel: this.i18n._("Block")
                    },
                    {
                        xtype:'checkbox',
                        name: "Flag",
                        dataIndex: "flagged",
                        fieldLabel: this.i18n._("Flag"),
                        tooltip: this.i18n._("Flag as Violation")
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
        // Block File Types
        buildBlockedExtensions: function() {

            this.gridBlockedExtensions = Ext.create('Ung.EditorGrid',{
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
                    width:55,
                    header: this.i18n._("Block"),
                    dataIndex: 'blocked',
                    fixed: true
                },
                { 
                    xtype:'checkcolumn',
                    width:55,
                    header: this.i18n._("Flag"),
                    dataIndex: 'flagged',
                    fixed: true,
                    tooltip: this.i18n._("Flag as Violation")
                },
                {
                    header: this.i18n._("Category"),
                    width: 200,
                    dataIndex: 'category',
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }, 
                {
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
                },
                {
                    xtype:'checkbox',
                    name: "Block",
                    dataIndex: "blocked",
                    fieldLabel: this.i18n._("Block")
                },
                {
                    xtype:'checkbox',
                    name: "Flag",
                    dataIndex: "flagged",
                    fieldLabel: this.i18n._("Flag"),
                    tooltip: this.i18n._("Flag as Violation")
                },
                {
                    xtype:'textarea',
                    name: "Category",
                    dataIndex: "category",
                    fieldLabel: this.i18n._("Category"),
                    width: 300
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
        // Block MIME Types
        buildBlockedMimeTypes: function() {

            this.gridBlockedMimeTypes = Ext.create('Ung.EditorGrid',{
                name: 'MIME Types',
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
                columns: [{
                    header: this.i18n._("MIME type"),
                    width: 200,
                    dataIndex: 'string',
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }, 
                { 
                    xtype:'checkcolumn',
                    width:55,
                    header: this.i18n._("Block"),
                    dataIndex: 'blocked',
                    fixed: true
                },
                { 
                    xtype:'checkcolumn',
                    width:55,
                    header: this.i18n._("Flag"),
                    dataIndex: 'flagged',
                    fixed: true,
                    tooltip: this.i18n._("Flag as Violation")
                }, 
                {
                    header: this.i18n._("Category"),
                    width: 100,
                    dataIndex: 'category',
                    editor: {
                        xtype:'textfield',
                        allowBlank:true
                    }
                }, 
                {
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
                rowEditorInputLines: [
                {
                    xtype:'textfield',
                    name: "MIME Type",
                    dataIndex: "string",
                    fieldLabel: this.i18n._("MIME Type"),
                    allowBlank: false,
                    width: 400
                },
                {
                    xtype:'checkbox',
                    name: "Block",
                    dataIndex: "blocked",
                    fieldLabel: this.i18n._("Block")
                },
                {
                    xtype:'checkbox',
                    name: "Flag",
                    dataIndex: "flagged",
                    fieldLabel: this.i18n._("Flag"),
                    tooltip: this.i18n._("Flag as Violation")
                },
                {
                    xtype:'textarea',
                    name: "Category",
                    dataIndex: "category",
                    fieldLabel: this.i18n._("Category"),
                    width: 300
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

        // Pass Lists Panel
        buildPassLists: function() {
            this.panelPassLists = Ext.create('Ext.panel.Panel',{
            // private fields
                name: 'Pass Lists',
                helpSource: 'pass_lists',
                winPassedUrls: null,
                winPassedClients: null,
                parentId: this.getId(),
                autoScroll: true,
                title: this.i18n._('Pass Lists'),
                bodyStyle: 'padding:5px 5px 0px; 5px;',
                defaults: {
                    xtype: 'fieldset',
                    buttonAlign: 'left'
                },
                items: [
                    {
                        xtype:'fieldset',
                        items:{
                            xtype:'button',
                            name: 'Sites manage list',
                            text: this.i18n._("Edit Passed Sites"),
                            handler: Ext.bind(function() {
                                this.panelPassLists.onManagePassedUrls();
                            }, this)
                        }
                    },
                    {
                        xtype:'fieldset',
                        items: {
                            xtype:'button',
                            name: 'Client IP addresses manage list',
                            text: this.i18n._("Edit Passed Client IPs"),
                            handler: Ext.bind(function() {
                                this.panelPassLists.onManagePassedClients();
                            }, this)
                        }
                    }],
                onManagePassedUrls: function() {
                    if (!this.winPassedUrls) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildPassedUrls();
                        this.winPassedUrls = Ext.create('Ung.ManageListWindow', {
                            breadcrumbs: [{
                                title: i18n._(rpc.currentPolicy.name),
                                action: Ext.bind(function() {
                                    Ung.Window.cancelAction(
                                       this.gridPassedUrls.isDirty() || this.isDirty(),
                                       Ext.bind(function() {
                                            this.panelPassLists.winPassedUrls.closeWindow();
                                            this.closeWindow();
                                       }, this)
                                    );
                                },settingsCmp)
                            }, {
                                title: settingsCmp.node.displayName,
                                action: Ext.bind(function() {
                                    this.panelPassLists.winPassedUrls.cancelAction();
                                },settingsCmp)
                            }, {
                                title: settingsCmp.i18n._("Sites")
                            }],
                            grid: settingsCmp.gridPassedUrls,
                            applyAction: function(callback) {
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridPassedUrls.getList(Ext.bind(function(saveList) {
                                    this.alterUrls(saveList);
                                    this.getRpcNode().setPassedUrls(Ext.bind(function(result, exception) {
                                        Ext.MessageBox.hide();
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getPassedUrls(Ext.bind(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.settings.passedUrls = result;
                                            this.gridPassedUrls.clearDirty();
                                            if(callback != null) {
                                                callback();
                                            }
                                        }, this));
                                    }, this), saveList);
                                },settingsCmp));
                            }                            
                        });
                    }
                    this.winPassedUrls.show();
                },
                onManagePassedClients: function() {
                    if (!this.winPassedClients) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildPassedClients();
                        this.winPassedClients = Ext.create('Ung.ManageListWindow',{
                            breadcrumbs: [{
                                title: i18n._(rpc.currentPolicy.name),
                                action: Ext.bind(function() {
                                    Ung.Window.cancelAction(
                                       this.gridPassedClients.isDirty() || this.isDirty(),
                                       Ext.bind(function() {
                                            this.panelPassLists.winPassedClients.closeWindow();
                                            this.closeWindow();
                                       }, this)
                                    );
                                },settingsCmp)
                            }, {
                                title: settingsCmp.node.displayName,
                                action: Ext.bind(function() {
                                    this.panelPassLists.winPassedClients.cancelAction();
                                },settingsCmp)
                            }, {
                                title: settingsCmp.i18n._("Client IP addresses")
                            }],
                            grid: settingsCmp.gridPassedClients,
                            applyAction: function(callback) {
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridPassedClients.getList(Ext.bind(function(saveList) {
                                    this.getRpcNode().setPassedClients(Ext.bind(function(result, exception) {
                                        Ext.MessageBox.hide();
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getPassedClients(Ext.bind(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.settings.passedClients = result;
                                            this.gridPassedClients.clearDirty();
                                            if(callback != null) {
                                                callback();
                                            }
                                        }, this));
                                    }, this), saveList);
                                },settingsCmp));
                            }                                                 
                        });
                    }
                    this.winPassedClients.show();
                },
                beforeDestroy: function() {
                    Ext.destroy(this.winPassedUrls, this.winPassedClients);
                    Ext.Panel.prototype.beforeDestroy.call(this);
                }
            });
        },
        // Passed Sites
        buildPassedUrls: function() {
            var urlValidator = Ext.bind(function(fieldValue) {
                if (fieldValue.indexOf("https://") == 0) {
                    return this.i18n._("\"URL\" specified cannot be passed because it uses secure http (https)");
                }
                // strip "http://" from beginning of rule
                // strip "www." from beginning of rule
                // strip "*." from beginning of rule
                // strip "/" from the end
                if (fieldValue.indexOf("http://") == 0) {
                    fieldValue = fieldValue.substr(7);
                }
                if (fieldValue.indexOf("www.") == 0) {
                    fieldValue = fieldValue.substr(4);
                }
                if (fieldValue.indexOf("*.") == 0) {
                    fieldValue = fieldValue.substr(2);
                }
                if (fieldValue.indexOf("/") == fieldValue.length - 1) {
                    fieldValue = fieldValue.substring(0, fieldValue.length - 1);
                }
                if (fieldValue.trim().length == 0) {
                    return this.i18n._("Invalid \"URL\" specified");
                }
                return true;
            }, this);


            this.gridPassedUrls = Ext.create('Ung.EditorGrid',{
                name: 'Sites',
                settingsCmp: this,
                emptyRow: {
                    "string": this.i18n._("[no site]"),
                    "enabled": true,
                    "description": this.i18n._("[no description]")
                },
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
                        validator: urlValidator,
                        blankText: this.i18n._("Invalid \"URL\" specified")
                    }
                }, 
                {
                    xtype:'checkcolumn',
                    width:55,
                    header: this.i18n._("Pass"),
                    dataIndex: 'enabled',
                    fixed: true
                },
                {
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
                    name: "Site",
                    dataIndex: "string",
                    fieldLabel: this.i18n._("Site"),
                    allowBlank: false,
                    width: 400,
                    validator: urlValidator,
                    blankText: this.i18n._("Invalid \"URL\" specified")
                },
                {
                    xtype:'checkbox',
                    name: "Pass",
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Pass")
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
        // Passed IP Addresses
        buildPassedClients: function() {

            this.gridPassedClients = Ext.create('Ung.EditorGrid',{
                name: 'Client IP addresses',
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
                columns: [{
                    header: this.i18n._("IP address/range"),
                    width: 200,
                    dataIndex: 'string',
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }, 
                {
                    xtype:'checkcolumn',
                    width:55,
                    header: this.i18n._("Pass"),
                    dataIndex: 'enabled',
                    fixed: true
                },
                {
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
                },
                {
                    xtype:'checkbox',
                    name: "Pass",
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Pass")
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
        // Event Log
        buildEventLog: function() {
            this.gridEventLog = new Ext.create('Ung.GridEventLog',{
                settingsCmp: this,
                fields: [{
                    name: 'time_stamp',
                    sortType: Ung.SortTypes.asTimestamp
                }, {
                    name: 'blocked',
                    mapping: 'wf_' + this.getRpcNode().getVendor() + '_blocked',
                    type: 'boolean'
                }, {
                    name: 'flagged',
                    mapping: 'wf_' + this.getRpcNode().getVendor() + '_flagged',
                    type: 'boolean'
                }, {
                    name: 'category',
                    mapping: 'wf_' + this.getRpcNode().getVendor() + '_category',
                    type: 'string'
                }, {
                    name: 'client',
                    mapping: 'c_client_addr'
                }, {
                    name: 'uid'
                }, {
                    name: 'server',
                    mapping: 'c_server_addr'
                }, {
                    name: 'server_port',
                    mapping: 's_server_port'
                }, {
                    name: 'host',
                    mapping: 'host'
                }, {
                    name: 'uri',
                    mapping: 'uri'
                }, {
                    name: 'reason',
                    mapping: 'wf_' + this.getRpcNode().getVendor() + '_reason',
                    type: 'string',
                    convert: Ext.bind(function(value) {
                        switch (value) {
                            case 'D':
                                return this.i18n._("in Categories Block list");
                            case 'U':
                                return this.i18n._("in URLs Block list");
                            case 'E':
                                return this.i18n._("in File Extensions Block list");
                            case 'M':
                                return this.i18n._("in MIME Types Block list");
                            case 'H':
                                return this.i18n._("Hostname is an IP address");
                            case 'I':
                                return this.i18n._("in URLs Pass list");
                            case 'C':
                                return this.i18n._("in Clients Pass list");
                            case 'B':
                                return this.i18n._("Client Bypass");
                            default:
                            case 'DEFAULT':
                                return this.i18n._("no rule applied");
                        }
                        return null;
                    }, this)

                }],
                columnsDefaultSortable: true,
                columns: [{
                    header: this.i18n._("Timestamp"),
                    width: Ung.Util.timestampFieldWidth,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: this.i18n._("Client"),
                    width: Ung.Util.ipFieldWidth,
                    dataIndex: 'client'
                }, {
                    header: this.i18n._("Username"),
                    width: Ung.Util.usernameFieldWidth,
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
                    header: this.i18n._("Blocked"),
                    width: Ung.Util.booleanFieldWidth,
                    dataIndex: 'blocked'
                }, {
                    header: this.i18n._("Flagged"),
                    width: Ung.Util.booleanFieldWidth,
                    dataIndex: 'flagged'
                }, {
                    header: this.i18n._("Reason For Action"),
                    width: 150,
                    dataIndex: 'reason'
                }, {
                    header: this.i18n._("Category"),
                    width: 120,
                    dataIndex: 'category'
                }, {
                    header: this.i18n._("Server"),
                    width: Ung.Util.ipFieldWidth,
                    dataIndex: 'server'
                }, {
                    header: this.i18n._("Server Port"),
                    width: Ung.Util.portFieldWidth,
                    dataIndex: 'server_port'
                }]

            });
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
//@ sourceURL=webfilter-settings.js
