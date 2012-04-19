if (!Ung.hasResource["Ung.Spyware"]) {
    Ung.hasResource["Ung.Spyware"] = true;
    Ung.NodeWin.registerClassName('untangle-node-spyware', "Ung.Spyware");

    Ung.Spyware = Ext.extend(Ung.NodeWin, {
        //gridActiveXList : null,
        gridCookiesList : null,
        gridSubnetList : null,
        panelBlockLists : null,
        gridPassList : null,
        gridCookieEventLog : null,
        gridSuspiciousEventLog : null,
        gridUrlEventLog : null,
        initComponent : function() {
            this.genericRuleFields = Ung.Util.getGenericRuleFields(this);
            Ung.Util.generateListIds(this.getSettings().cookies.list);
            Ung.Util.generateListIds(this.getSettings().subnets.list);
            Ung.Util.generateListIds(this.getSettings().passedUrls.list);
            this.buildBlockLists();
            this.buildPassList();
            this.buildUrlEventLog();
            this.buildCookieEventLog();
            this.buildSuspiciousEventLog();
            // builds a tab panel with the 3 panels
            this.buildTabPanel([this.panelBlockLists, this.gridPassList, 
                                this.gridUrlEventLog,
                                this.gridCookieEventLog,
                                this.gridSuspiciousEventLog]);

            // keep initial  settings
            this.initialSettings = Ung.Util.clone(this.getSettings());
            Ung.Spyware.superclass.initComponent.call(this);
        },
        // Block lists panel
        buildBlockLists : function() {
            this.panelBlockLists = new Ext.Panel({
                name : 'Block Lists',
                helpSource : 'block_lists',
                winCookiesList : null,
                //winActiveXList : null,
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
                        boxLabel : this.i18n._('Block Malware URLs (Community List)'),
                        hideLabel : true,
                        name : 'Block Malware URLs (Community List)',
                        checked : this.getSettings().scanUrls,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getSettings().scanUrls = checked;
                                }.createDelegate(this)
                            }
                        }
                    },  {
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Block Malware URLs (Google List)'),
                        hideLabel : true,
                        name : 'Block Malware URLs (Google List)',
                        checked : this.getSettings().scanGoogleSafeBrowsing,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getSettings().scanGoogleSafeBrowsing = checked;
                                }.createDelegate(this)
                            }
                        }
                    },  {
                        xtype : 'combo',
                        editable : false,
                        mode : 'local',
                        fieldLabel : this.i18n._('User Bypass'),
                        name : "User Bypass",
                        store : new Ext.data.SimpleStore({
                            fields : ['unblockModeValue', 'unblockModeName'],
                            data : [["None", this.i18n._("None")],
                                    ["Host", this.i18n._("Temporary")],
                                    ["Global", this.i18n._("Permanent and Global")]]
                        }),
                        displayField : 'unblockModeName',
                        valueField : 'unblockModeValue',
                        value : this.getSettings().unblockMode,
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getSettings().unblockMode = newValue;
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
                        checked : this.getSettings().scanCookies,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getSettings().scanCookies = checked;
                                }.createDelegate(this)
                            }
                        }
                    }],
                    buttons : [{
                        name : 'Edit Cookies List',
                        text : this.i18n._("Edit Cookie List"),
                        handler : function() {
                            this.panelBlockLists.onManageCookiesList();
                        }.createDelegate(this)
                    }]
                }, {
                    title : this.i18n._('Traffic'),
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Monitor Suspicious Traffic'),
                        hideLabel : true,
                        name : 'Monitor Suspicious Traffic',
                        checked : this.getSettings().scanSubnets,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getSettings().scanSubnets = checked;
                                }.createDelegate(this)
                            }
                        }
                    }],
                    buttons : [{
                        name : 'Edit Traffic List',
                        text : this.i18n._("Edit Traffic List"),
                        handler : function() {
                            this.panelBlockLists.onManageSubnetList();
                        }.createDelegate(this)
                    }]
                }, {
                    cls: 'description',
                    html : this.i18n._("Spyware Blocker signatures were last updated") + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                        + ((this.getRpcNode().getLastSignatureUpdate() != null) ? i18n.timestampFormat(this.getRpcNode().getLastSignatureUpdate()) :
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
                                title : settingsCmp.node.displayName,
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
                            applyAction : function(callback){
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridCookiesList.getGridSaveList(function(saveList) {
                                    this.alterUrls(saveList.list);
                                    this.getRpcNode().setCookies(function(result, exception) {
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getCookies(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.gridCookiesList.reloadGrid({data:result.list});
                                            this.getSettings().cookies = result;
                                            this.initialSettings.cookies = result;
                                            if(callback != null) {
                                                callback();
                                            }
                                        }.createDelegate(this));
                                    }.createDelegate(this), saveList);
                                }.createDelegate(settingsCmp));
                            }    
                        });
                    }
                    this.winCookiesList.show();
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
                                title : settingsCmp.node.displayName,
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
                            applyAction : function(callback){
                                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                                settingsCmp.gridSubnetList.getGridSaveList(function(saveList) {
                                    this.alterUrls(saveList);
                                    this.getRpcNode().setSubnets(function(result, exception) {
                                        if(Ung.Util.handleException(exception)) return;
                                        this.getRpcNode().getSubnets(function(result, exception) {
                                            Ext.MessageBox.hide();
                                            if(Ung.Util.handleException(exception)) return;
                                            this.gridSubnetList.reloadGrid({data:result.list});
                                            this.getSettings().subnets = result;
                                            this.initialSettings.subnets = result;
                                            if(callback != null) {
                                                callback();
                                            }
                                        }.createDelegate(this));
                                    }.createDelegate(this), saveList);
                                }.createDelegate(settingsCmp));
                            }
                        });
                    }
                    this.winSubnetList.show();
                },

                beforeDestroy : function() {
                    Ext.destroy(this.winCookiesList, this.winSubnetList);
                    Ext.each(this.subCmps, Ext.destroy);
                    Ext.Panel.prototype.beforeDestroy.call(this);
                }
            });

        },
        // Cookies List
        buildCookiesList : function() {
            var enabledColumn = new Ext.grid.CheckColumn({
                header : "<b>" + this.i18n._("block") + "</b>",
                dataIndex : 'enabled',
                fixed : true
            });

            this.gridCookiesList = new Ung.EditorGrid({
                name : 'Cookies List',
                settingsCmp : this,
                emptyRow : {
                    "string" : this.i18n._("[no identification]"),
                    "enabled" : true,
                    "description" : this.i18n._("[no description]")
                },
                title : this.i18n._("Cookies List"),
                data: this.getSettings().cookies.list,
                recordJavaClass : "com.untangle.uvm.node.GenericRule",
                fields : this.genericRuleFields,
                columns : [{
                    id : 'string',
                    header : this.i18n._("identification"),
                    width : 140,
                    dataIndex : 'string',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, enabledColumn],
                sortField : 'string',
                columnsDefaultSortable : true,
                autoExpandColumn : 'string',
                plugins : [enabledColumn],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "Identification",
                    dataIndex : "string",
                    fieldLabel : this.i18n._("Identification"),
                    allowBlank : false,
                    width : 200
                }), new Ext.form.Checkbox({
                    name : "Block",
                    dataIndex : "enabled",
                    fieldLabel : this.i18n._("Block")
                })]
            });
        },

        // Subnet List
        buildSubnetList : function() {
            var flagColumn = new Ext.grid.CheckColumn({
                header : "<b>" + this.i18n._("log") + "</b>",
                dataIndex : 'flagged',
                fixed : true
            });

            this.gridSubnetList = new Ung.EditorGrid({
                name : 'Subnet List',
                settingsCmp : this,
                emptyRow : {
                    "string" : "1.2.3.4/24",
                    "name" : this.i18n._("[no name]"),
                    "flagged" : true,
                    description : this.i18n._("[no description]")
                },
                title : this.i18n._("Subnet List"),
                data: this.getSettings().subnets.list,
                recordJavaClass : "com.untangle.uvm.node.GenericRule",
                fields : this.genericRuleFields,
                columns : [{
                    id : 'name',
                    header : this.i18n._("name"),
                    width : 150,
                    dataIndex : 'name',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, {
                    id : 'string',
                    header : this.i18n._("subnet"),
                    width : 200,
                    dataIndex : 'string',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, flagColumn],
                sortField : 'name',
                columnsDefaultSortable : true,
                autoExpandColumn : 'name',
                plugins : [flagColumn],
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "Name",
                    dataIndex : "name",
                    fieldLabel : this.i18n._("Name"),
                    allowBlank : false,
                    width : 200
                }), new Ext.form.TextField({
                    name : "Subnet",
                    dataIndex : "string",
                    fieldLabel : this.i18n._("Subnet"),
                    allowBlank : false,
                    width : 200
                }), new Ext.form.Checkbox({
                    name : "Log",
                    dataIndex : "flagged",
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
                dataIndex : 'enabled',
                width: 65,
                fixed : true
            });

            this.gridPassList = new Ung.EditorGrid({
                name : 'Pass List',
                helpSource : 'pass_list',
                settingsCmp : this,
                emptyRow : {
                    "string" : "",
                    "enabled" : true,
                    "description" : this.i18n._("[no description]")
                },
                title : this.i18n._("Pass List"),
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

        // Event Logs
        buildCookieEventLog : function() {
            this.gridCookieEventLog = new Ung.GridEventLog({
                settingsCmp : this,
                eventQueriesFn : this.getRpcNode().getCookieEventQueries,
                name : "Cookie Event Log",
                title : i18n._('Cookie Event Log'),
                fields : [{
                    name : 'timeStamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'uid'
                }, {
                    name : 'swBlacklisted'
                }, {
                    name : 'swCookie'
                }, {
                    name : 'swAccessIdent'
                }, {
                    name : 'uri'
                }, {
                    name : 'host'
                }, {
                    name : 'client',
                    mapping : 'CClientAddr'
                }, {
                    name : 'server',
                    mapping : 'CServerAddr'
                }],
                columns : [{
                    header : this.i18n._("timestamp"),
                    width : Ung.Util.timestampFieldWidth,
                    sortable : true,
                    dataIndex : 'timeStamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : this.i18n._("client"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'client'
                }, {
                    header : this.i18n._("username"),
                    width : Ung.Util.usernameFieldWidth,
                    sortable : true,
                    dataIndex : 'uid'
                }, {
                    header : this.i18n._("host"),
                    width : Ung.Util.hostnameFieldWidth,
                    sortable : true,
                    dataIndex : 'host'
                }, {
                    header : this.i18n._("Uri"),
                    width : Ung.Util.uriFieldWidth,
                    sortable : true,
                    dataIndex : 'uri'
                }, {
                    header : this.i18n._("cookie"),
                    width : 100,
                    sortable : true,
                    dataIndex : 'swCookie'
                }, {
                    header : this.i18n._("server"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'server'
                }]
            });
        },

        buildUrlEventLog : function() {
            this.gridUrlEventLog = new Ung.GridEventLog({
                settingsCmp : this,
                eventQueriesFn : this.getRpcNode().getUrlEventQueries,
                name : "Web Event Log",
                title : i18n._('Web Event Log'),
                fields : [{
                    name : 'timeStamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'uid'
                }, {
                    name : 'swBlacklisted'
                }, {
                    name : 'swCookie'
                }, {
                    name : 'swAccessIdent'
                }, {
                    name : 'uri'
                }, {
                    name : 'host'
                }, {
                    name : 'client',
                    mapping : 'CClientAddr'
                }, {
                    name : 'server',
                    mapping : 'CServerAddr'
                }],
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
                    header : this.i18n._("client"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'client'
                }, {
                    header : this.i18n._("username"),
                    width : Ung.Util.usernameFieldWidth,
                    sortable : true,
                    dataIndex : 'uid'
                }, {
                    header : this.i18n._("host"),
                    width : Ung.Util.hostnameFieldWidth,
                    sortable : true,
                    dataIndex : 'host'
                }, {
                    id: 'uri',
                    header : this.i18n._("Uri"),
                    width : Ung.Util.uriFieldWidth,
                    sortable : true,
                    dataIndex : 'uri'
                }, {
                    header : this.i18n._("server"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'server'
                }]
            });
        },

        buildSuspiciousEventLog : function() {
            this.gridSuspiciousEventLog = new Ung.GridEventLog({
                settingsCmp : this,
                eventQueriesFn : this.getRpcNode().getSuspiciousEventQueries,
                name : "Traffic Event Log",
                title : i18n._('Traffic Event Log'),
                fields : [{
                    name : 'timeStamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'uid'
                }, {
                    name : 'swBlacklisted'
                }, {
                    name : 'swCookie'
                }, {
                    name : 'swAccessIdent'
                }, {
                    name : 'client',
                    mapping : 'CClientAddr'
                }, {
                    name : 'server',
                    mapping : 'CServerAddr'
                }],
                columns : [{
                    header : this.i18n._("timestamp"),
                    width : Ung.Util.timestampFieldWidth,
                    sortable : true,
                    dataIndex : 'timeStamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : this.i18n._("client"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'client'
                }, {
                    header : this.i18n._("username"),
                    width : Ung.Util.usernameFieldWidth,
                    sortable : true,
                    dataIndex : 'uid'
                }, {
                    header : this.i18n._("subnet"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'swAccessIdent'
                }, {
                    header : this.i18n._("server"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'server'
                }]
            });
        },

        // private method
        alterUrls : function(list) {
            if (list != null) {
                for (var i = 0; i < list.length; i++) {
                    list[i]["string"] = this.alterUrl(list[i]["string"]);
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
            // ipMaskedAddress list must be validated server side
            var subnetSaveList = this.gridSubnetList ? this.gridSubnetList.getSaveList() : null;
            if (subnetSaveList != null) {
                var ipMaskedAddressList = [];
                // added
                for (var i = 0; i < subnetSaveList[0].list.length; i++) {
                    ipMaskedAddressList.push(subnetSaveList[0].list[i]["string"]);
                }
                // modified
                for (var i = 0; i < subnetSaveList[2].list.length; i++) {
                    ipMaskedAddressList.push(subnetSaveList[2].list[i]["string"]);
                }
                if (ipMaskedAddressList.length > 0) {
                    try {
                        var result=null;
                        try {
                            result = this.getValidator().validate({
                                list : ipMaskedAddressList,
                                "javaClass" : "java.util.LinkedList"
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
                            this.gridSubnetList.focusFirstChangedDataByFieldValue("string", result.cause);
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
                this.gridPassList.getGridSaveList(function(saveList) {
                    this.alterUrls(saveList.list);
                    this.getSettings().passedUrls = saveList;
                    this.getRpcNode().setSettings(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        // exit settings screen
                        if(keepWindowOpen!== true){
                            Ext.MessageBox.hide();                    
                            this.closeWindow();
                        }else{
                            //refresh the settings
                            this.getSettings(true);
                            Ung.Util.generateListIds(this.getSettings().cookies.list);
                            Ung.Util.generateListIds(this.getSettings().subnets.list);
                            Ung.Util.generateListIds(this.getSettings().passedUrls.list);
                            
                            this.gridPassList.reloadGrid({data:this.getSettings().passedUrls.list});
                            // keep initial  settings
                            this.initialSettings = Ung.Util.clone(this.getSettings());
                            Ext.MessageBox.hide();
                        }
                    }.createDelegate(this), this.getSettings());
                }.createDelegate(this));                  
            }
        },
        isDirty : function() {
            return !Ung.Util.equals(this.getSettings(), this.initialSettings) || this.gridPassList.isDirty();
            
        }
    });
}
//@ sourceURL=spyware-settings.js
