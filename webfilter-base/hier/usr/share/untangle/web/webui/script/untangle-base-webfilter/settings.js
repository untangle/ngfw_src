if (!Ung.hasResource["Ung.BaseWebFilter"]) {
    Ung.hasResource["Ung.BaseWebFilter"] = true;
    Ung.NodeWin.registerClassName('untangle-base-webfilter', 'Ung.BaseWebFilter');

    Ung.BaseWebFilter = Ext.extend(Ung.NodeWin, {
        gridExceptions : null,
        gridEventLog : null,
        // called when the component is rendered
        initComponent : function() {
            this.genericRuleFields = Ung.Util.getGenericRuleFields(this);
            Ung.Util.generateListIds(this.getSettings().categories.list);
            Ung.Util.generateListIds(this.getSettings().blockedUrls.list);
            Ung.Util.generateListIds(this.getSettings().blockedExtensions.list);
            Ung.Util.generateListIds(this.getSettings().blockedMimeTypes.list);
            Ung.Util.generateListIds(this.getSettings().passedUrls.list);
            Ung.Util.generateListIds(this.getSettings().passedClients.list);
            this.buildBlockLists();
            this.buildPassLists();
            this.buildEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelBlockLists, this.panelPassLists, this.gridEventLog]);
            // keep initial base settings
            this.initialSettings = Ung.Util.clone(this.getSettings());
            
            Ung.BaseWebFilter.superclass.initComponent.call(this);
        },
        // Block Lists Panel
        buildBlockLists : function() {
            this.panelBlockLists = new Ext.Panel({
                name : 'Block Lists',
                helpSource : 'block_lists',
                // private fields
                winCategories : null,
                winBlockedUrls : null,
                winBlockedExtensions : null,
                winBlockedMimeTypes : null,
                parentId : this.getId(),

                title : this.i18n._('Block Lists'),
                layout : "form",
                cls: 'ung-panel',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    name : "fieldset_manage_categories",
                    items : [{
                        xtype : "button",
                        name : "manage_categories",
                        text : this.i18n._("Edit Categories"),
                        handler : function() {
                            this.panelBlockLists.onManageCategories();
                        }.createDelegate(this)
                    }]
                },{
                    items : [{
                        xtype : "button",
                        name : 'manage_sites',
                        text : this.i18n._("Edit Sites"),
                        handler : function() {
                            this.panelBlockLists.onManageBlockedUrls();
                        }.createDelegate(this)
                    }]
                },{
                    items : [{
                        xtype : "button",
                        name : "manage_file_types",
                        text : this.i18n._("Edit File Types"),
                        handler : function() {
                            this.panelBlockLists.onManageBlockedExtensions();
                        }.createDelegate(this)
                    }]
                },{
                    items : [{
                        xtype : "button",
                        name : "manage_mime_types",
                        text : this.i18n._("Edit MIME Types"),
                        handler : function() {
                            this.panelBlockLists.onManageBlockedMimeTypes();
                        }.createDelegate(this)
                    }]
                },{
                    name : "fieldset_miscellaneous",
                    items : [{
                        xtype : "checkbox",
                        boxLabel : this.i18n._("Block pages from IP only hosts"),
                        hideLabel : true,
                        name : 'Block IPHost',
                        checked : this.getSettings().blockAllIpHosts,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getSettings().blockAllIpHosts = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : "combo",
                        editable : false,
                        mode : "local",
                        fieldLabel : this.i18n._("Unblock"),
                        name : "user_bypass",
                        store : new Ext.data.SimpleStore({
                            fields : ['unblockModeValue', 'unblockModeName'],
                            data : [["None", this.i18n._("None")],
                                    ["Host", this.i18n._("Temporary")],
                                    ["Global", this.i18n._("Permanent and Global")]]
                        }),
                        displayField : "unblockModeName",
                        valueField : "unblockModeValue",
                        value : this.getSettings().unblockMode,
                        triggerAction : "all",
                        listClass : 'x-combo-list-small',
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getSettings().unblockMode = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }],

                onManageCategories : function() {
                    if (!this.winCategories) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildCategories();
                        this.winCategories = new Ung.ManageListWindow({
                            breadcrumbs : [{
                                title : i18n._(rpc.currentPolicy.name),
                                action : function() {
                                    Ung.Window.cancelAction(
                                       this.gridCategories.isDirty() || this.isDirty(),
                                       function() {
                                            this.panelBlockLists.winCategories.closeWindow();
                                            this.closeWindow();
                                       }.createDelegate(this)
                                    );
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.node.displayName,
                                action : function() {
                                    this.panelBlockLists.winCategories.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Categories")
                            }],
                            grid : settingsCmp.gridCategories,
                            applyAction : function(callback){
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridCategories.getGridSaveList(function(saveList) {
                                    this.getRpcNode().setCategories(function(result, exception) {
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getCategories(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.gridCategories.reloadGrid({data:result.list});
                                            this.getSettings().categories = result;
                                            this.initialSettings.categories = result;
                                            if(callback != null) {
                                                callback();
                                            }
                                        }.createDelegate(this));
                                    }.createDelegate(this), saveList);
                                }.createDelegate(settingsCmp));
                            }                                                        
                        });
                    }
                    this.winCategories.show();
                },
                onManageBlockedUrls : function() {
                    if (!this.winBlockedUrls) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildBlockedUrls();
                        this.winBlockedUrls = new Ung.ManageListWindow({
                            breadcrumbs : [{
                                title : i18n._(rpc.currentPolicy.name),
                                action : function() {
                                    Ung.Window.cancelAction(
                                       this.gridBlockedUrls.isDirty() || this.isDirty(),
                                       function() {
                                            this.panelBlockLists.winBlockedUrls.closeWindow();
                                            this.closeWindow();
                                       }.createDelegate(this)
                                    );
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.node.displayName,
                                action : function() {
                                    this.panelBlockLists.winBlockedUrls.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Sites")
                            }],
                            grid : settingsCmp.gridBlockedUrls,
                            applyAction : function(callback){
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridBlockedUrls.getGridSaveList(function(saveList) {
                                    this.alterUrls(saveList);
                                    this.getRpcNode().setBlockedUrls(function(result, exception) {
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getBlockedUrls(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.gridBlockedUrls.reloadGrid({data:result.list});
                                            this.getSettings().blockedUrls = result;
                                            this.initialSettings.blockedUrls = result;
                                            if(callback != null) {
                                                callback();
                                            }
                                        }.createDelegate(this));
                                    }.createDelegate(this), saveList);
                                }.createDelegate(settingsCmp));
                            }                                                        
                        });
                    }
                    this.winBlockedUrls.show();
                },
                onManageBlockedExtensions : function() {
                    if (!this.winBlockedExtensions) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildBlockedExtensions();
                        this.winBlockedExtensions = new Ung.ManageListWindow({
                            breadcrumbs : [{
                                title : i18n._(rpc.currentPolicy.name),
                                action : function() {
                                    Ung.Window.cancelAction(
                                       this.gridBlockedExtensions.isDirty() || this.isDirty(),
                                       function() {
                                            this.panelBlockLists.winBlockedExtensions.closeWindow();
                                            this.closeWindow();
                                       }.createDelegate(this)
                                    );
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.node.displayName,
                                action : function() {
                                    this.panelBlockLists.winBlockedExtensions.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("File Types")
                            }],
                            grid : settingsCmp.gridBlockedExtensions,
                            applyAction : function(callback){
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridBlockedExtensions.getGridSaveList(function(saveList) {
                                    this.getRpcNode().setBlockedExtensions(function(result, exception) {
                                        Ext.MessageBox.hide();
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getBlockedExtensions(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.gridBlockedExtensions.reloadGrid({data:result.list});
                                            this.getSettings().blockedExtensions = result;
                                            this.initialSettings.blockedExtensions = result;
                                            if(callback != null) {
                                                callback();
                                            }
                                        }.createDelegate(this));
                                    }.createDelegate(this), saveList);
                                }.createDelegate(settingsCmp));
                            }                                                        
                        });
                    }
                    this.winBlockedExtensions.show();
                },
                onManageBlockedMimeTypes : function() {
                    if (!this.winBlockedMimeTypes) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildBlockedMimeTypes();
                        this.winBlockedMimeTypes = new Ung.ManageListWindow({
                            breadcrumbs : [{
                                title : i18n._(rpc.currentPolicy.name),
                                action : function() {
                                    Ung.Window.cancelAction(
                                       this.gridBlockedMimeTypes.isDirty() || this.isDirty(),
                                       function() {
                                            this.panelBlockLists.winBlockedMimeTypes.closeWindow();
                                            this.closeWindow();
                                       }.createDelegate(this)
                                    );
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.node.displayName,
                                action : function() {
                                    this.panelBlockLists.winBlockedMimeTypes.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("MIME Types")
                            }],
                            grid : settingsCmp.gridBlockedMimeTypes,
                            applyAction : function(callback){
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridBlockedMimeTypes.getGridSaveList(function(saveList) {
                                    this.getRpcNode().setBlockedMimeTypes(function(result, exception) {
                                        Ext.MessageBox.hide();
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getBlockedMimeTypes(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.gridBlockedMimeTypes.reloadGrid({data:result.list});
                                            this.getSettings().blockedMimeTypes = result;
                                            this.initialSettings.blockedMimeTypes = result;
                                            if(callback != null) {
                                                callback();
                                            }
                                        }.createDelegate(this));
                                    }.createDelegate(this), saveList);
                                }.createDelegate(settingsCmp));
                            }
                        });
                    }
                    this.winBlockedMimeTypes.show();
                },
                beforeDestroy : function() {
                    Ext.destroy(this.winCategories, this.winBlockedUrls, this.winBlockedExtensions, this.winBlockedMimeTypes);
                    Ext.Panel.prototype.beforeDestroy.call(this);
                }
            });
        },
        // Block Categories
        buildCategories : function() {
            var blockColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("Block"),
                dataIndex : 'blocked',
                fixed : true,
                changeRecord : function(record) {
                    Ext.grid.CheckColumn.prototype.changeRecord.call(this, record);
                    var blocked = record.get(this.dataIndex);
                    if (blocked) {
                        record.set('flagged', true);
                    }
                }
            });
            var flagColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("Flag"),
                dataIndex : 'flagged',
                fixed : true,
                tooltip : this.i18n._("Flag as Violation")
            });
            this.gridCategories = new Ung.EditorGrid({
                name : 'Categories',
                settingsCmp : this,
                hasAdd : false,
                hasDelete : false,
                title : this.i18n._("Categories"),
                data: this.getSettings().categories.list,
                recordJavaClass : "com.untangle.uvm.node.GenericRule",
                fields : this.genericRuleFields,
                paginated : false,
                columns : [{
                    id : 'name',
                    header : this.i18n._("category"),
                    width : 200,
                    dataIndex : 'name'
                }, blockColumn, flagColumn, {
                    id : 'description',
                    header : this.i18n._("description"),
                    width : 200,
                    dataIndex : 'description',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }],
                sortField : 'name',
                columnsDefaultSortable : true,
                autoExpandColumn : 'description',
                plugins : [blockColumn, flagColumn],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "Category",
                    dataIndex : "name",
                    fieldLabel : this.i18n._("Category"),
                    allowBlank : false,
                    width : 200,
                    disabled : true,
                    ctCls: "fixed-pos"
                }), new Ext.form.Checkbox({
                    name : "Block",
                    dataIndex : "blocked",
                    fieldLabel : this.i18n._("Block"),
                    listeners : {
                        "check" : {
                            fn : function(elem, checked) {
                                var rowEditor = this.gridCategories.rowEditor;
                                if (checked) {
                                    rowEditor.inputLines[2].setValue(true);
                                }
                            }.createDelegate(this)
                        }
                    }
                }), new Ext.form.Checkbox({
                    name : "Flag",
                    dataIndex : "flagged",
                    fieldLabel : this.i18n._("Flag"),
                    tooltip : this.i18n._("Flag as Violation")
                }), new Ext.form.TextArea({
                    name : "Description",
                    dataIndex : "description",
                    fieldLabel : this.i18n._("Description"),
                    width : 200,
                    height : 60
                })]
            });
        },
        // Block Sites
        buildBlockedUrls : function() {
            var urlValidator = function(fieldValue) {
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
            }.createDelegate(this);
            var blockColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("Block"),
                dataIndex : 'blocked',
                fixed : true
            });
            var flagColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("Flag"),
                dataIndex : 'flagged',
                fixed : true,
                tooltip : this.i18n._("Flag as Violation")
            });

            this.gridBlockedUrls = new Ung.EditorGrid({
                name : 'Sites',
                settingsCmp : this,
                emptyRow : {
                    "string" : this.i18n._("[no site]"),
                    "blocked" : true,
                    "flagged" : true,
                    "description" : this.i18n._("[no description]")
                },
                title : this.i18n._("Sites"),
                data: this.getSettings().blockedUrls.list,
                recordJavaClass : "com.untangle.uvm.node.GenericRule",
                fields : this.genericRuleFields,
                columns : [{
                    id : 'string',
                    header : this.i18n._("site"),
                    width : 200,
                    dataIndex : 'string',
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        validator : urlValidator,
                        blankText : this.i18n._("Invalid \"URL\" specified")
                    })
                }, blockColumn, flagColumn, {
                    id : 'description',
                    header : this.i18n._("description"),
                    width : 200,
                    dataIndex : 'description',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }],
                sortField : 'string',
                columnsDefaultSortable : true,
                autoExpandColumn : 'description',
                plugins : [blockColumn, flagColumn],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "Site",
                    dataIndex : "string",
                    fieldLabel : this.i18n._("Site"),
                    allowBlank : false,
                    width : 200,
                    validator : urlValidator,
                    blankText : this.i18n._("Invalid \"URL\" specified")
                }), new Ext.form.Checkbox({
                    name : "Block",
                    dataIndex : "blocked",
                    fieldLabel : this.i18n._("Block")
                }), new Ext.form.Checkbox({
                    name : "Flag",
                    dataIndex : "flagged",
                    fieldLabel : this.i18n._("Flag"),
                    tooltip : this.i18n._("Flag as Violation")
                }), new Ext.form.TextArea({
                    name : "Description",
                    dataIndex : "description",
                    fieldLabel : this.i18n._("Description"),
                    width : 200,
                    height : 60
                })]
            });
        },
        // Block File Types
        buildBlockedExtensions : function() {
            var blockColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("Block"),
                dataIndex : 'blocked',
                fixed : true
            });
            var flagColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("Flag"),
                dataIndex : 'flagged',
                fixed : true,
                tooltip : this.i18n._("Flag as Violation")
            });

            this.gridBlockedExtensions = new Ung.EditorGrid({
                name : 'File Types',
                settingsCmp : this,
                emptyRow : {
                    "string" : this.i18n._("[no extension]"),
                    "blocked" : true,
                    "flagged" : true,
                    "category" : this.i18n._("[no category]"),
                    "description" : this.i18n._("[no description]")
                },
                title : this.i18n._("File Types"),
                data : this.getSettings().blockedExtensions.list,
                recordJavaClass : "com.untangle.uvm.node.GenericRule",
                fields : this.genericRuleFields,
                columns : [{
                    id : 'string',
                    header : this.i18n._("file type"),
                    width : 200,
                    dataIndex : 'string',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, blockColumn, flagColumn, {
                    id : 'category',
                    header : this.i18n._("category"),
                    width : 200,
                    dataIndex : 'category',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, {
                    id : 'description',
                    header : this.i18n._("description"),
                    width : 200,
                    dataIndex : 'description',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }],
                sortField : 'string',
                columnsDefaultSortable : true,
                autoExpandColumn : 'description',
                plugins : [blockColumn, flagColumn],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "File Type",
                    dataIndex : "string",
                    fieldLabel : this.i18n._("File Type"),
                    allowBlank : false,
                    width : 100
                }), new Ext.form.Checkbox({
                    name : "Block",
                    dataIndex : "blocked",
                    fieldLabel : this.i18n._("Block")
                }), new Ext.form.Checkbox({
                    name : "Flag",
                    dataIndex : "flagged",
                    fieldLabel : this.i18n._("Flag"),
                    tooltip : this.i18n._("Flag as Violation")
                }), new Ext.form.TextArea({
                    name : "Category",
                    dataIndex : "category",
                    fieldLabel : this.i18n._("Category"),
                    width : 100
                }), new Ext.form.TextArea({
                    name : "Description",
                    dataIndex : "description",
                    fieldLabel : this.i18n._("Description"),
                    width : 200,
                    height : 60
                })]
            });
        },
        // Block MIME Types
        buildBlockedMimeTypes : function() {
            var blockColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("Block"),
                dataIndex : 'blocked',
                fixed : true
            });
            var flagColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("Flag"),
                dataIndex : 'flagged',
                fixed : true,
                tooltip : this.i18n._("Flag as Violation")
            });

            this.gridBlockedMimeTypes = new Ung.EditorGrid({
                name : 'MIME Types',
                settingsCmp : this,
                emptyRow : {
                    "string" : this.i18n._("[no mime type]"),
                    "blocked" : true,
                    "flagged" : true,
                    "category" : this.i18n._("[no category]"),
                    "description" : this.i18n._("[no description]")
                },
                title : this.i18n._("MIME Types"),
                recordJavaClass : "com.untangle.uvm.node.GenericRule",
                fields : this.genericRuleFields,
                data : this.getSettings().blockedMimeTypes.list,
                columns : [{
                    id : 'string',
                    header : this.i18n._("MIME type"),
                    width : 200,
                    dataIndex : 'string',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, blockColumn, flagColumn, {
                    id : 'category',
                    header : this.i18n._("category"),
                    width : 100,
                    dataIndex : 'category',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, {
                    id : 'description',
                    header : this.i18n._("description"),
                    width : 200,
                    dataIndex : 'description',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }],
                sortField : 'string',
                columnsDefaultSortable : true,
                autoExpandColumn : 'description',
                plugins : [blockColumn, flagColumn],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "MIME Type",
                    dataIndex : "string",
                    fieldLabel : this.i18n._("MIME Type"),
                    allowBlank : false,
                    width : 200
                }), new Ext.form.Checkbox({
                    name : "Block",
                    dataIndex : "blocked",
                    fieldLabel : this.i18n._("Block")
                }), new Ext.form.Checkbox({
                    name : "Flag",
                    dataIndex : "flagged",
                    fieldLabel : this.i18n._("Flag"),
                    tooltip : this.i18n._("Flag as Violation")
                }), new Ext.form.TextArea({
                    name : "Category",
                    dataIndex : "category",
                    fieldLabel : this.i18n._("Category"),
                    width : 100
                }), new Ext.form.TextArea({
                    name : "Description",
                    dataIndex : "description",
                    fieldLabel : this.i18n._("Description"),
                    width : 200,
                    height : 60
                })]
            });
        },

        // Pass Lists Panel
        buildPassLists : function() {
            this.panelPassLists = new Ext.Panel({
                // private fields
                name : 'Pass Lists',
                helpSource : 'pass_lists',
                winPassedUrls : null,
                winPassedClients : null,
                parentId : this.getId(),
                autoScroll : true,
                title : this.i18n._('Pass Lists'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px; 5px;',
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    buttons : [{
                        name : 'Sites manage list',
                        text : this.i18n._("Edit Passed Sites"),
                        handler : function() {
                            this.panelPassLists.onManagePassedUrls();
                        }.createDelegate(this)
                    }]
                }, {
                    buttons : [{
                        name : 'Client IP addresses manage list',
                        text : this.i18n._("Edit Passed Client IPs"),
                        handler : function() {
                            this.panelPassLists.onManagePassedClients();
                        }.createDelegate(this)
                    }]
                }],

                onManagePassedUrls : function() {
                    if (!this.winPassedUrls) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildPassedUrls();
                        this.winPassedUrls = new Ung.ManageListWindow({
                            breadcrumbs : [{
                                title : i18n._(rpc.currentPolicy.name),
                                action : function() {
                                    Ung.Window.cancelAction(
                                       this.gridPassedUrls.isDirty() || this.isDirty(),
                                       function() {
                                            this.panelPassLists.winPassedUrls.closeWindow();
                                            this.closeWindow();
                                       }.createDelegate(this)
                                    );
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.node.displayName,
                                action : function() {
                                    this.panelPassLists.winPassedUrls.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Sites")
                            }],
                            grid : settingsCmp.gridPassedUrls,
                            applyAction : function(callback){
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridPassedUrls.getGridSaveList(function(saveList) {
                                    this.alterUrls(saveList);
                                    this.getRpcNode().setPassedUrls(function(result, exception) {
                                        Ext.MessageBox.hide();
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getPassedUrls(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.gridPassedUrls.reloadGrid({data:result.list});
                                            this.getSettings().passedUrls = result;
                                            this.initialSettings.passedUrls = result;
                                            if(callback != null) {
                                                callback();
                                            }
                                        }.createDelegate(this));
                                    }.createDelegate(this), saveList);
                                }.createDelegate(settingsCmp));
                            }                            
                        });
                    }
                    this.winPassedUrls.show();
                },
                onManagePassedClients : function() {
                    if (!this.winPassedClients) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildPassedClients();
                        this.winPassedClients = new Ung.ManageListWindow({
                            breadcrumbs : [{
                                title : i18n._(rpc.currentPolicy.name),
                                action : function() {
                                    Ung.Window.cancelAction(
                                       this.gridPassedClients.isDirty() || this.isDirty(),
                                       function() {
                                            this.panelPassLists.winPassedClients.closeWindow();
                                            this.closeWindow();
                                       }.createDelegate(this)
                                    );
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.node.displayName,
                                action : function() {
                                    this.panelPassLists.winPassedClients.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Client IP addresses")
                            }],
                            grid : settingsCmp.gridPassedClients,
                            applyAction : function(callback){
                                var validateSaveList = settingsCmp.gridPassedClients.getSaveList();
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridPassedClients.getGridSaveList(function(saveList) {
                                    this.getRpcNode().setPassedClients(function(result, exception) {
                                        Ext.MessageBox.hide();
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getPassedClients(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.gridPassedClients.reloadGrid({data:result.list});
                                            this.getSettings().passedClients = result;
                                            this.initialSettings.passedClients = result;
                                            if(callback != null) {
                                                callback();
                                            }
                                        }.createDelegate(this));
                                    }.createDelegate(this), saveList);
                                }.createDelegate(settingsCmp));
                            }                                                 
                        });
                    }
                    this.winPassedClients.show();
                },
                beforeDestroy : function() {
                    Ext.destroy(this.winPassedUrls, this.winPassedClients);
                    Ext.Panel.prototype.beforeDestroy.call(this);
                }
            });
        },
        // Passed Sites
        buildPassedUrls : function() {
            var urlValidator = function(fieldValue) {
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
            }.createDelegate(this);

            var enabledColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("pass"),
                dataIndex : 'enabled',
                fixed : true
            });

            this.gridPassedUrls = new Ung.EditorGrid({
                name : 'Sites',
                settingsCmp : this,
                emptyRow : {
                    "string" : this.i18n._("[no site]"),
                    "enabled" : true,
                    "description" : this.i18n._("[no description]")
                },
                title : this.i18n._("Sites"),
                data: this.getSettings().passedUrls.list,
                recordJavaClass : "com.untangle.uvm.node.GenericRule",
                fields : this.genericRuleFields,
                columns : [{
                    id : 'string',
                    header : this.i18n._("site"),
                    width : 200,
                    dataIndex : 'string',
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        validator : urlValidator,
                        blankText : this.i18n._("Invalid \"URL\" specified")
                    })
                }, enabledColumn, {
                    id : 'description',
                    header : this.i18n._("description"),
                    width : 200,
                    dataIndex : 'description',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }],
                sortField : 'string',
                columnsDefaultSortable : true,
                autoExpandColumn : 'description',
                plugins : [enabledColumn],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "Site",
                    dataIndex : "string",
                    fieldLabel : this.i18n._("Site"),
                    allowBlank : false,
                    width : 200,
                    validator : urlValidator,
                    blankText : this.i18n._("Invalid \"URL\" specified")
                }), new Ext.form.Checkbox({
                    name : "Pass",
                    dataIndex : "enabled",
                    fieldLabel : this.i18n._("Pass")
                }), new Ext.form.TextArea({
                    name : "Description",
                    dataIndex : "description",
                    fieldLabel : this.i18n._("Description"),
                    width : 200,

                    height : 60
                })]
            });
        },
        // Passed IP Addresses
        buildPassedClients : function() {
            var enabledColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("pass"),
                dataIndex : 'enabled',
                fixed : true
            });

            this.gridPassedClients = new Ung.EditorGrid({
                name : 'Client IP addresses',
                settingsCmp : this,
                emptyRow : {
                    "string" : "1.2.3.4",
                    "enabled" : true,
                    "description" : this.i18n._("[no description]")
                },
                title : this.i18n._("Client IP addresses"),
                data: this.getSettings().passedClients.list,
                recordJavaClass : "com.untangle.uvm.node.GenericRule",
                fields : this.genericRuleFields,
                columns : [{
                    id : 'string',
                    header : this.i18n._("IP address/range"),
                    width : 200,
                    dataIndex : 'string',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, enabledColumn, {
                    id : 'description',
                    header : this.i18n._("description"),
                    width : 200,
                    dataIndex : 'description',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }],
                sortField : 'string',
                columnsDefaultSortable : true,
                autoExpandColumn : 'description',
                plugins : [enabledColumn],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "IP address/range",
                    dataIndex : "string",
                    fieldLabel : this.i18n._("IP address/range"),
                    allowBlank : false,
                    width : 200
                }), new Ext.form.Checkbox({
                    name : "Pass",
                    dataIndex : "enabled",
                    fieldLabel : this.i18n._("Pass")
                }), new Ext.form.TextArea({
                    name : "Description",
                    dataIndex : "description",
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
                fields : [{
                    name : 'timeStamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'blocked',
                    mapping : 'wf' + main.capitalize(this.getRpcNode().getVendor()) + 'Blocked',
                    type : 'boolean'
                }, {
                    name : 'flagged',
                    mapping : 'wf' + main.capitalize(this.getRpcNode().getVendor()) + 'Flagged',
                    type : 'boolean'
                }, {
                    name : 'category',
                    mapping : 'wf' + main.capitalize(this.getRpcNode().getVendor()) + 'Category',
                    type : 'string'
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
                    name : 'reason',
                    mapping : 'wf' + main.capitalize(this.getRpcNode().getVendor()) + 'Reason',
                    type : 'string',
                    convert : function(value) {
                        switch (value) {
                            case 'D' :
                                return this.i18n._("in Categories Block list");
                            case 'U' :
                                return this.i18n._("in URLs Block list");
                            case 'E' :
                                return this.i18n._("in File Extensions Block list");
                            case 'M' :
                                return this.i18n._("in MIME Types Block list");
                            case 'H' :
                                return this.i18n._("Hostname is an IP address");
                            case 'I' :
                                return this.i18n._("in URLs Pass list");
                            case 'C' :
                                return this.i18n._("in Clients Pass list");
                            case 'B' :
                                return this.i18n._("Client Bypass");
                            default :
                            case 'DEFAULT' :
                                return this.i18n._("no rule applied");
                        }
                        return null;
                    }.createDelegate(this)

                }],
                autoExpandColumn: 'uri',
                columnsDefaultSortable : true,
                columns : [{
                    header : this.i18n._("timestamp"),
                    width : Ung.Util.timestampFieldWidth,
                    dataIndex : 'timeStamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : this.i18n._("client"),
                    width : Ung.Util.ipFieldWidth,
                    dataIndex : 'client'
                }, {
                    header : this.i18n._("username"),
                    width : Ung.Util.usernameFieldWidth,
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
                    header : this.i18n._("blocked"),
                    width : Ung.Util.booleanFieldWidth,
                    dataIndex : 'blocked'
                }, {
                    header : this.i18n._("flagged"),
                    width : Ung.Util.booleanFieldWidth,
                    dataIndex : 'flagged'
                }, {
                    header : this.i18n._("reason for action"),
                    width : 150,
                    dataIndex : 'reason'
                }, {
                    header : this.i18n._("category"),
                    width : 120,
                    dataIndex : 'category'
                }, {
                    header : this.i18n._("server"),
                    width : Ung.Util.ipFieldWidth,
                    dataIndex : 'server'
                }]

            });
        },
        // private method
        alterUrls : function(saveList) {
            if (saveList != null) {
                var list = saveList.list;
                for (var i = 0; i < list.length; i++) {
                    list[i]["string"] = this.alterUrl(list[i]["string"]);
                }
            }
        },
        // private method
        alterUrl : function(value) {
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
        },
        applyAction : function(){
            this.saveAction(true);
        },        
        // save function
        saveAction : function(keepWindowOpen) {
            // validate first
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.getRpcNode().setSettings(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    // exit settings screen
                    if(!keepWindowOpen){
                        Ext.MessageBox.hide();                    
                        this.closeWindow();
                    }else{
                        //refresh the settings
                        this.getSettings(true);
                        // keep initial  settings
                        this.initialSettings = Ung.Util.clone(this.getSettings());
                        Ext.MessageBox.hide();
                    }
                }.createDelegate(this), this.getSettings());
            }
        },        
        isDirty : function() {
            return !Ung.Util.equals(this.getSettings(), this.initialSettings);
        }
    });
}
//@ sourceURL=webfilter-settings.js
