if (!Ung.hasResource["Ung.Spyware"]) {
    Ung.hasResource["Ung.Spyware"] = true;
    Ung.NodeWin.registerClassName('untangle-node-spyware', "Ung.Spyware");

    Ung.Spyware = Ext.extend(Ung.NodeWin, {
        gridActiveXList : null,
        gridCookiesList : null,
        gridSubnetList : null,
        panelBlockLists : null,
        gridPassList : null,
        gridEventLog : null,
        initComponent : function() {
            // keep initial base settings
            this.initialBaseSettings = Ung.Util.clone(this.getBaseSettings());

            this.buildBlockLists();
            this.buildPassList();
            this.buildEventLog();
            // builds a tab panel with the 3 panels
            this.buildTabPanel([this.panelBlockLists, this.gridPassList, this.gridEventLog]);
            Ung.Spyware.superclass.initComponent.call(this);
        },
        // Block lists panel
        buildBlockLists : function() {
            this.panelBlockLists = new Ext.Panel({
                name : 'Block Lists',
                helpSource : 'block_lists',
                winCookiesList : null,
                winActiveXList : null,
                winSubnetList : null,
                subCmps : [],
                title : this.i18n._("Block Lists"),
                parentId : this.getId(),

                layout : "form",
                cls: 'ung-panel',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Web'),
                    labelWidth: 150,
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Block Spyware & Ad URLs'),
                        hideLabel : true,
                        name : 'Block Spyware & Ad URLs',
                        checked : this.getBaseSettings().urlBlacklistEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().urlBlacklistEnabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'combo',
                        editable : false,
                        mode : 'local',
                        fieldLabel : this.i18n._('User Bypass'),
                        name : "User Bypass",
                        store : new Ext.data.SimpleStore({
                            fields : ['userWhitelistValue', 'userWhitelistName'],
                            data : [["NONE", this.i18n._("None")], ["USER_ONLY", this.i18n._("Temporary")],
                                    ["USER_AND_GLOBAL", this.i18n._("Permanent and Global")]]
                        }),
                        displayField : 'userWhitelistName',
                        valueField : 'userWhitelistValue',
                        value : this.getBaseSettings().userWhitelistMode,
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().userWhitelistMode = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    title : this.i18n._('Cookies'),
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Block Tracking & Ad Cookies'),
                        hideLabel : true,
                        name : 'Block Tracking & Ad Cookies',
                        checked : this.getBaseSettings().cookieBlockerEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().cookieBlockerEnabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    }],
                    buttons : [{
                        name : 'Cookies manage list',
                        text : this.i18n._("manage list"),
                        handler : function() {
                            this.panelBlockLists.onManageCookiesList();
                        }.createDelegate(this)
                    }]
                }, {
                    title : this.i18n._('ActiveX'),
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Block Malware ActiveX Installs'),
                        hideLabel : true,
                        name : 'Block Malware ActiveX Installs',
                        checked : this.getBaseSettings().activeXEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().activeXEnabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Block All ActiveX'),
                        hideLabel : true,
                        name : 'Block All ActiveX',
                        checked : this.getBaseSettings().blockAllActiveX,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().blockAllActiveX = checked;
                                }.createDelegate(this)
                            }
                        }
                    }],
                    buttons : [{
                        name : 'ActiveX manage list',
                        text : this.i18n._("manage list"),
                        handler : function() {
                            this.panelBlockLists.onManageActiveXList();
                        }.createDelegate(this)
                    }]
                }, {
                    title : this.i18n._('Traffic'),
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Monitor Suspicious Traffic'),
                        hideLabel : true,
                        name : 'Monitor Suspicious Traffic',
                        checked : this.getBaseSettings().spywareEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().spywareEnabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    }],
                    buttons : [{
                        name : 'Traffic manage list',
                        text : this.i18n._("manage list"),
                        handler : function() {
                            this.panelBlockLists.onManageSubnetList();
                        }.createDelegate(this)
                    }]
                }, {
                    cls: 'description',
                    html : this.i18n._("Spyware Blocker signatures were last updated") + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                            + ((this.getLastSignatureUpdate() != null) ? i18n.timestampFormat(this.getLastSignatureUpdate()) :
                            this.i18n._("Unknown"))
                }],

                onManageCookiesList : function() {
                    if (!this.winCookiesList) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildCookiesList();
                        this.winCookiesList = new Ung.ManageListWindow({
                            breadcrumbs : [{
                                title : i18n._(rpc.currentPolicy.name),
                                action : function() {
                                    Ung.Window.cancelAction(
                                       this.gridCookiesList.isDirty() || this.isDirty(),
                                       function() {
                                            this.panelBlockLists.winCookiesList.closeWindow();
                                            this.closeWindow();
                                       }.createDelegate(this)
                                    );
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.node.md.displayName,
                                action : function() {
                                    this.panelBlockLists.winCookiesList.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Block Lists"),
                                action : function() {
                                    this.panelBlockLists.winCookiesList.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Cookies List")
                            }],
                            grid : settingsCmp.gridCookiesList,
                            applyAction : function(forceLoad){
                                            Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                            var saveList = settingsCmp.gridCookiesList.getSaveList();
                                            settingsCmp.getRpcNode().updateCookieRules(function(result, exception) {
                                                if(Ung.Util.handleException(exception)){
                                                    Ext.MessageBox.hide();
                                                    return;
                                                }
                                                this.getRpcNode().getBaseSettings(function(result2,exception2){
                                                    Ext.MessageBox.hide();                                                
                                                    if(Ung.Util.handleException(exception2)){
                                                        return;
                                                    }
                                                    this.gridCookiesList.setTotalRecords(result2.cookieRulesLength);
                                                    if(forceLoad===true){                                                
                                                        this.gridCookiesList.reloadGrid();
                                                    }                                                    
                                                }.createDelegate(this));
                                            }.createDelegate(settingsCmp), saveList[0],saveList[1],saveList[2]);
                            }    
                        });
                    }
                    this.winCookiesList.show();
                },
                onManageActiveXList : function() {
                    if (!this.winActiveXList) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildActiveXList();
                        this.winActiveXList = new Ung.ManageListWindow({
                            breadcrumbs : [{
                                title : i18n._(rpc.currentPolicy.name),
                                action : function() {
                                    Ung.Window.cancelAction(
                                       this.gridActiveXList.isDirty() || this.isDirty(),
                                       function() {
                                            this.panelBlockLists.winActiveXList.closeWindow();
                                            this.closeWindow();
                                       }.createDelegate(this)
                                    );
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.node.md.displayName,
                                action : function() {
                                    this.panelBlockLists.winActiveXList.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Block Lists"),
                                action : function() {
                                    this.panelBlockLists.winActiveXList.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("ActiveX List")
                            }],
                            grid : settingsCmp.gridActiveXList,
                            applyAction : function(forceLoad){
                                            Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                            var saveList = settingsCmp.gridActiveXList.getSaveList();
                                            settingsCmp.getRpcNode().updateActiveXRules(function(result, exception) {
                                                if(Ung.Util.handleException(exception)){
                                                    Ext.MessageBox.hide();
                                                    return;
                                                }
                                                this.getRpcNode().getBaseSettings(function(result2,exception2){
                                                    Ext.MessageBox.hide();                                                
                                                    if(Ung.Util.handleException(exception2)){
                                                        return;
                                                    }
                                                    this.gridActiveXList.setTotalRecords(result2.activeXRulesLength);
                                                    if(forceLoad===true){                                                
                                                        this.gridActiveXList.reloadGrid();
                                                    }                                                    
                                                }.createDelegate(this));
                                            }.createDelegate(settingsCmp), saveList[0],saveList[1],saveList[2]);
                            }
                        });
                    }
                    this.winActiveXList.show();
                },
                onManageSubnetList : function() {
                    if (!this.winSubnetList) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildSubnetList();
                        this.winSubnetList = new Ung.ManageListWindow({
                            breadcrumbs : [{
                                title : i18n._(rpc.currentPolicy.name),
                                action : function() {
                                    Ung.Window.cancelAction(
                                       this.gridSubnetList.isDirty() || this.isDirty(),
                                       function() {
                                            this.panelBlockLists.winSubnetList.closeWindow();
                                            this.closeWindow();
                                       }.createDelegate(this)
                                    );
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.node.md.displayName,
                                action : function() {
                                    this.panelBlockLists.winSubnetList.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Block Lists"),
                                action : function() {
                                    this.panelBlockLists.winSubnetList.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Subnet List")
                            }],
                            grid : settingsCmp.gridSubnetList,
                            applyAction : function(forceLoad){
                                            Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                            var saveList = settingsCmp.gridSubnetList.getSaveList();
                                            settingsCmp.getRpcNode().updateSubnetRules(function(result, exception) {
                                                if(Ung.Util.handleException(exception)){
                                                    Ext.MessageBox.hide();
                                                    return;
                                                }
                                                this.getRpcNode().getBaseSettings(function(result2,exception2){
                                                    Ext.MessageBox.hide();                                                
                                                    if(Ung.Util.handleException(exception2)){
                                                        return;
                                                    }
                                                    this.gridSubnetList.setTotalRecords(result2.subnetRulesLength);
                                                    if(forceLoad===true){                                                
                                                        this.gridSubnetList.reloadGrid();
                                                    }                                                    
                                                }.createDelegate(this));
                                            }.createDelegate(settingsCmp), saveList[0],saveList[1],saveList[2]);
                            }
                        });
                    }
                    this.winSubnetList.show();
                },

                beforeDestroy : function() {
                    Ext.destroy(this.winCookiesList, this.winActiveXList, this.winSubnetList);
                    Ext.each(this.subCmps, Ext.destroy);
                    Ext.Panel.prototype.beforeDestroy.call(this);
                }
            });

        },
        // Cookies List
        buildCookiesList : function() {
            var liveColumn = new Ext.grid.CheckColumn({
                header : "<b>" + this.i18n._("block") + "</b>",
                dataIndex : 'live',
                fixed : true
            });

            this.gridCookiesList = new Ung.EditorGrid({
                name : 'Cookies List',
                settingsCmp : this,
                totalRecords : this.getBaseSettings().cookieRulesLength,
                emptyRow : {
                    "string" : this.i18n._("[no identification]"),
                    "live" : true,
                    "description" : this.i18n._("[no description]")
                },
                title : this.i18n._("Cookies List"),
                recordJavaClass : "com.untangle.uvm.node.StringRule",
                proxyRpcFn : this.getRpcNode().getCookieRules,
                fields : [{
                    name : 'id'
                }, {
                    name : 'string',
                    type : 'string'
                }, {
                    name : 'live'
                }, {
                    name : 'description',
                    type : 'string'
                }],
                columns : [{
                    id : 'string',
                    header : this.i18n._("identification"),
                    width : 140,
                    dataIndex : 'string',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, liveColumn],
                sortField : 'string',
                columnsDefaultSortable : true,
                autoExpandColumn : 'string',
                plugins : [liveColumn],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "Identification",
                    dataIndex : "string",
                    fieldLabel : this.i18n._("Identification"),
                    allowBlank : false,
                    width : 200
                }), new Ext.form.Checkbox({
                    name : "Block",
                    dataIndex : "live",
                    fieldLabel : this.i18n._("Block")
                })]
            });
        },
        // ActiveX List
        buildActiveXList : function() {
            var liveColumn = new Ext.grid.CheckColumn({
                header : "<b>" + this.i18n._("block") + "</b>",
                dataIndex : 'live',
                fixed : true
            });

            this.gridActiveXList = new Ung.EditorGrid({
                name : 'ActiveX List',
                settingsCmp : this,
                totalRecords : this.getBaseSettings().activeXRulesLength,
                emptyRow : {
                    "string" : this.i18n._("[no identification]"),
                    "live" : true,
                    "description" : this.i18n._("[no description]")
                },
                title : this.i18n._("ActiveX List"),
                recordJavaClass : "com.untangle.uvm.node.StringRule",
                proxyRpcFn : this.getRpcNode().getActiveXRules,
                fields : [{
                    name : 'id'
                }, {
                    name : 'string',
                    type : 'string'
                }, {
                    name : 'live'
                }, {
                    name : 'description',
                    type : 'string'
                }],
                columns : [{
                    id : 'string',
                    header : this.i18n._("identification"),
                    width : 300,
                    dataIndex : 'string',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, liveColumn],
                sortField : 'string',
                columnsDefaultSortable : true,
                autoExpandColumn : 'string',
                plugins : [liveColumn],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "Identification",
                    dataIndex : "string",
                    fieldLabel : this.i18n._("Identification"),
                    allowBlank : false,
                    width : 300
                }), new Ext.form.Checkbox({
                    name : "Block",
                    dataIndex : "live",
                    fieldLabel : this.i18n._("Block")
                })]
            });
        },
        // Subnet List
        buildSubnetList : function() {
            var logColumn = new Ext.grid.CheckColumn({
                header : "<b>" + this.i18n._("log") + "</b>",
                dataIndex : 'log',
                fixed : true
            });

            this.gridSubnetList = new Ung.EditorGrid({
                name : 'Subnet List',
                settingsCmp : this,
                totalRecords : this.getBaseSettings().subnetRulesLength,
                emptyRow : {
                    "ipMaddr" : "1.2.3.4/5",
                    "name" : this.i18n._("[no name]"),
                    "log" : true,
                    description : this.i18n._("[no description]")
                },
                title : this.i18n._("Subnet List"),
                recordJavaClass : "com.untangle.uvm.node.IPMaskedAddressRule",
                proxyRpcFn : this.getRpcNode().getSubnetRules,
                fields : [{
                    name : 'id'
                }, {
                    name : 'name',
                    type : 'string'
                }, {
                    name : 'ipMaddr'
                }, {
                    name : 'log'
                }, {
                    name : 'description',
                    type : 'string'
                }],
                columns : [{
                    id : 'name',
                    header : this.i18n._("name"),
                    width : 150,
                    dataIndex : 'name',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, {
                    id : 'ipMaddr',
                    header : this.i18n._("subnet"),
                    width : 200,
                    dataIndex : 'ipMaddr',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, logColumn],
                sortField : 'name',
                columnsDefaultSortable : true,
                autoExpandColumn : 'name',
                plugins : [logColumn],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "Name",
                    dataIndex : "name",
                    fieldLabel : this.i18n._("Name"),
                    allowBlank : false,
                    width : 200
                }), new Ext.form.TextField({
                    name : "Subnet",
                    dataIndex : "ipMaddr",
                    fieldLabel : this.i18n._("Subnet"),
                    allowBlank : false,
                    width : 200
                }), new Ext.form.Checkbox({
                    name : "Log",
                    dataIndex : "log",
                    fieldLabel : this.i18n._("Log")
                })]
            });
        },
        // Pass List
        buildPassList : function() {
            var urlValidator = function(fieldValue) {
                if (fieldValue.indexOf("https://") == 0) {
                    return this.i18n._("\"URL\" specified cannot be blocked because it uses secure http (https)");
                }
                if (fieldValue.indexOf("http://") == 0) {
                    fieldValue = fieldValue.substr(7);
                }
                if (fieldValue.indexOf("www.") == 0) {
                    fieldValue = fieldValue.substr(4);
                }
                if (fieldValue.indexOf("/") == fieldValue.length - 1) {
                    fieldValue = fieldValue.substring(0, fieldValue.length - 1);
                }
                if (fieldValue.trim().length == 0) {
                    return this.i18n._("Invalid \"URL\" specified");
                }
                return true;
            }.createDelegate(this);

            var passColumn = new Ext.grid.CheckColumn({
                header : "<b>" + this.i18n._("pass") + "</b>",
                dataIndex : 'live',
                width: 65,
                fixed : true
            });

            this.gridPassList = new Ung.EditorGrid({
                name : 'Pass List',
                helpSource : 'pass_list',
                settingsCmp : this,
                totalRecords : this.getBaseSettings().domainWhitelistLength,
                emptyRow : {
                    "string" : "",
                    "live" : true,
                    "category" : this.i18n._("[no category]"),
                    "description" : this.i18n._("[no description]")
                },
                title : this.i18n._("Pass List"),
                proxyRpcFn : this.getRpcNode().getDomainWhitelist,
                recordJavaClass : "com.untangle.uvm.node.StringRule",
                fields : [{
                    name : 'id'
                }, {
                    name : 'string',
                    type : 'string'
                }, {
                    name : 'live'
                }, {
                    name : 'category',
                    type : 'string'
                }, {
                    name : 'description',
                    type : 'string'
                }],
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
                }, passColumn, {
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
                plugins : [passColumn],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "Site",
                    dataIndex : "string",
                    fieldLabel : this.i18n._("Site"),
                    width : 200,
                    validator : urlValidator,
                    allowBlank : false,
                    blankText : this.i18n._("Invalid \"URL\" specified")
                }), new Ext.form.Checkbox({
                    name : "Pass",
                    dataIndex : "live",
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
                    name : 'action',
                    mapping : 'blocked',
                    type : 'string',
                    convert :  function(value) {
                        return value ? this.i18n._("block") : this.i18n._("pass");
                    }.createDelegate(this)
                }, {
                    name : 'client',
                    mapping : 'pipelineEndpoints',
                    sortType : Ung.SortTypes.asClient
                }, {
                    name : 'server',
                    mapping : 'pipelineEndpoints',
                    sortType : Ung.SortTypes.asServer
                }, {
                    name : 'request',
                    mapping : 'location',
                    type : 'string',
                    convert : function(value, rec ) {
                        return rec.location + " : " + rec.identification
                    }
                }, {
                    name : 'reason',
                    mapping : 'reason',
                    type : 'string',
                    convert : function(value, rec ) {
                        var displayValue = value;
                        switch (rec.type) {
                            case 'Access' :
                                displayValue = this.i18n._("in Subnet List");
                                break;
                            case 'ActiveX' :
                                displayValue = this.i18n._("in ActiveX List");
                                break;
                            case 'Blacklist' :
                                displayValue = this.i18n._("in URL List");
                                break;
                            case 'Cookie' :
                                displayValue = this.i18n._("in Cookie List");
                                break;
                        }
                        return displayValue;
                    }.createDelegate(this)
                }],
                columns : [{
                    header : this.i18n._("timestamp"),
                    width : 130,
                    sortable : true,
                    dataIndex : 'timeStamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : this.i18n._("action"),
                    width : 90,
                    sortable : true,
                    dataIndex : 'action'
                }, {
                    header : this.i18n._("client"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'client',
                    renderer : Ung.SortTypes.asClient
                }, {
                    id: 'request',
                    header : this.i18n._("request"),
                    width : 200,
                    sortable : true,
                    dataIndex : 'request'
                }, {
                    header : this.i18n._("reason for action"),
                    width : 140,
                    sortable : true,
                    dataIndex : 'reason'
                }, {
                    header : this.i18n._("server"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'server',
                    renderer : Ung.SortTypes.asServer
                }],
                autoExpandColumn: 'request'
            });
        },

        // validation functions
        validateClient : function() {
            // no need for validation here...just alter the URLs
            if (this.gridPassList) {
                this.alterUrls(this.gridPassList.getSaveList());
            }
            return true;
        },
        // private method
        alterUrls : function(list) {
            if (list != null) {
                // added
                for (var i = 0; i < list[0].list.length; i++) {
                    list[0].list[i]["string"] = this.alterUrl(list[0].list[i]["string"]);
                }
                // modified
                for (var i = 0; i < list[2].list.length; i++) {
                    list[2].list[i]["string"] = this.alterUrl(list[2].list[i]["string"]);
                }
            }
        },
        // private method
        alterUrl : function(value) {
            if (value.indexOf("http://") == 0) {
                value = value.substr(7);
            }
            if (value.indexOf("www.") == 0) {
                value = value.substr(4);
            }
            if (value.indexOf("/") == value.length - 1) {
                value = value.substring(0, value.length - 1);
            }
            return value.trim();
        },
        // validation
        validateServer : function() {
            // ipMaddr list must be validated server side
            var subnetSaveList = this.gridSubnetList ? this.gridSubnetList.getSaveList() : null;
            if (subnetSaveList != null) {
                var ipMaddrList = [];
                // added
                for (var i = 0; i < subnetSaveList[0].list.length; i++) {
                    ipMaddrList.push(subnetSaveList[0].list[i]["ipMaddr"]);
                }
                // modified
                for (var i = 0; i < subnetSaveList[2].list.length; i++) {
                    ipMaddrList.push(subnetSaveList[2].list[i]["ipMaddr"]);
                }
                if (ipMaddrList.length > 0) {
                    try {
                        var result=null;
                        try {
                            result = this.getValidator().validate({
                                list : ipMaddrList,
                                "javaClass" : "java.util.ArrayList"
                            });
                        } catch (e) {
                            Ung.Util.rpcExHandler(e);
                        }
                        if (!result.valid) {
                            var errorMsg = "";
                            switch (result.errorCode) {
                                case 'INVALID_IPMADDR' :
                                    errorMsg = this.i18n._("Invalid subnet specified") + ": " + result.cause;
                                break;
                                default :
                                    errorMsg = this.i18n._(result.errorCode) + ": " + result.cause;
                            }

                            this.panelBlockLists.onManageSubnetList();
                            this.gridSubnetList.focusFirstChangedDataByFieldValue("ipMaddr", result.cause);
                            Ext.MessageBox.alert(this.i18n._("Validation failed"), errorMsg);
                            return false;
                        }
                    } catch (e) {
                        var message = e.message;
                        if (message == null || message == "Unknown") {
                            message = i18n._("Please Try Again");
                        }
                        Ext.MessageBox.alert("Failed", message);
                        return false;
                    }
                }
            }
            return true;
        },
        validate : function() {
            // reverse the order because valdate client alters the data
            return this.validateServer() && this.validateClient();
        },
        //apply function 
        applyAction : function(){
            this.saveAction(true);
        },        
        // save function
        saveAction : function(keepWindowOpen) {
            // validate first
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.getRpcNode().updateAll(function(result, exception) {
                    Ext.MessageBox.hide();
                    if(Ung.Util.handleException(exception)) return;
                    // exit settings screen
                    if(keepWindowOpen!== true){
                        Ext.MessageBox.hide();                    
                        this.closeWindow();
                    }else{
                        //refresh the settings
                        this.getRpcNode().getBaseSettings(function(result2,exception2){
                            Ext.MessageBox.hide();                            
                            this.initialBaseSettings = Ung.Util.clone(this.getBaseSettings());                                      
                            this.gridPassList.setTotalRecords(result2.domainWhitelistLength);
                            this.gridPassList.reloadGrid();
                        }.createDelegate(this));
                        //this.gridEventLog.reloadGrid();                                     
                    }
                }.createDelegate(this), this.getBaseSettings(), this.gridActiveXList ? this.gridActiveXList.getSaveList() : null,
                        this.gridCookiesList ? this.gridCookiesList.getSaveList() : null,
                        this.gridSubnetList ? this.gridSubnetList.getSaveList() : null, this.gridPassList.getSaveList());
            }
        },
        isDirty : function() {
            return !Ung.Util.equals(this.getBaseSettings(), this.initialBaseSettings)
                || (this.gridActiveXList ? this.gridActiveXList.isDirty() : false)
                || (this.gridCookiesList ? this.gridCookiesList.isDirty() : false)
                || (this.gridSubnetList ? this.gridSubnetList.isDirty() : false)
                || this.gridPassList.isDirty();
        }
    });
}
