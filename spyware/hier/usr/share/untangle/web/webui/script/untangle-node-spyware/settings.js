if (!Ung.hasResource["Ung.Spyware"]) {
    Ung.hasResource["Ung.Spyware"] = true;
    Ung.Settings.registerClassName('untangle-node-spyware', "Ung.Spyware");

    Ung.Spyware = Ext.extend(Ung.Settings, {
        gridActiveXList : null,
        gridCookiesList : null,
        gridSubnetList : null,
        panelBlockLists : null,
        gridPassList : null,
        gridEventLog : null,
        // called when the component is rendered
        onRender : function(container, position) {
            // call superclass renderer first
            Ung.Spyware.superclass.onRender.call(this, container, position);

            // build the 3 tabs
            this.buildBlockLists();
            this.buildPassList();
            this.buildEventLog();
            // builds a tab panel with the 3 panels
            this.buildTabPanel([this.panelBlockLists, this.gridPassList, this.gridEventLog]);
        },
        // Block lists panel
        buildBlockLists : function() {
            this.panelBlockLists = new Ext.Panel({
                name : 'Block Lists',
                winCookiesList : null,
                winActiveXList : null,
                winSubnetList : null,
                subCmps : [],
                title : this.i18n._("Block Lists"),
                parentId : this.getId(),

                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Web'),
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : 'Block Spyware & Ad URLs',
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
                        boxLabel : 'Block Tracking & Ad Cookies',
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
                        boxLabel : 'Block Malware ActiveX Installs',
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
                        boxLabel : 'Block All ActiveX',
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
                        boxLabel : 'Monitor Suspicious Traffic',
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
                    html : this.i18n._("Spyware Blocker signatures were last updated") + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                            + ((this.getBaseSettings().lastUpdate != null) ? i18n.timestampFormat(this.getBaseSettings().lastUpdate) : 
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
                                    this.panelBlockLists.winCookiesList.cancelAction();
                                    this.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.node.md.displayName,
                                action : function() {
                                    this.panelBlockLists.winCookiesList.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Cookies List")
                            }],
                            grid : settingsCmp.gridCookiesList
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
                                    this.panelBlockLists.winActiveXList.cancelAction();
                                    this.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.node.md.displayName,
                                action : function() {
                                    this.panelBlockLists.winActiveXList.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("ActiveX List")
                            }],
                            grid : settingsCmp.gridActiveXList
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
                                    this.panelBlockLists.winSubnetList.cancelAction();
                                    this.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.node.md.displayName,
                                action : function() {
                                    this.panelBlockLists.winSubnetList.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Subnet List")
                            }],
                            grid : settingsCmp.gridSubnetList
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
                    name : 'string'
                }, {
                    name : 'live'
                }, {
                    name : 'description',
                    convert : function(v) {
                        return this.i18n._(v)
                    }.createDelegate(this)
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
                    name : 'string'
                }, {
                    name : 'live'
                }, {
                    name : 'description',
                    convert : function(v) {
                        return this.i18n._(v)
                    }.createDelegate(this)
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
                recordJavaClass : "com.untangle.uvm.node.IPMaddrRule",
                proxyRpcFn : this.getRpcNode().getSubnetRules,
                fields : [{
                    name : 'id'
                }, {
                    name : 'name'
                }, {
                    name : 'ipMaddr'
                }, {
                    name : 'log'
                }, {
                    name : 'description',
                    convert : function(v) {
                        return this.i18n._(v)
                    }.createDelegate(this)
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
                fixed : true
            });

            this.gridPassList = new Ung.EditorGrid({
                name : 'Pass List',
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
                    name : 'string'
                }, {
                    name : 'live'
                }, {
                    name : 'category'
                }, {
                    name : 'description',
                    convert : function(v) {
                        return this.i18n._(v)
                    }.createDelegate(this)
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
                    name : 'timeStamp'
                }, {
                    name : 'blocked'
                }, {
                    name : 'pipelineEndpoints'
                }, {
                    name : 'location'
                }, {
                    name : 'identification'
                }, {
                    name : 'reason'
                }, {
                    name : 'type'
                }],
                columns : [{
                    header : this.i18n._("timestamp"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'timeStamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : this.i18n._("action"),
                    width : 70,
                    sortable : true,
                    dataIndex : 'blocked',
                    renderer : function(value) {
                        return value ? this.i18n._("block") : this.i18n._("pass");
                    }.createDelegate(this)
                }, {
                    header : this.i18n._("client"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'pipelineEndpoints',
                    renderer : function(value) {
                        return value === null ? "" : value.CClientAddr + ":" + value.CClientPort;
                    }
                }, {
                    header : this.i18n._("request"),
                    width : 200,
                    sortable : true,
                    dataIndex : 'location',
                    renderer : function(value, metadata, record ) {
                    	return record.data.location + " : " + record.data.identification
                    }
                }, {
                    header : this.i18n._("reason for action"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'reason',
                    renderer : function(value, metadata, record ) {
                    	var displayValue = value;
                        switch (record.data.type) {
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
                }, {
                    header : this.i18n._("server"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'pipelineEndpoints',
                    renderer : function(value) {
                        return value === null ? "" : value.SServerAddr + ":" + value.SServerPort;
                    }
                }]
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
                        var result = this.getValidator().validate({
                            list : ipMaddrList,
                            "javaClass" : "java.util.ArrayList"
                        });
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
                        Ext.MessageBox.alert(i18n._("Failed"), e.message);
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
        // save function
        saveAction : function() {
            // validate first
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.getRpcNode().updateAll(function(result, exception) {
                    Ext.MessageBox.hide();
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    // exit settings screen
                    this.cancelAction();
                }.createDelegate(this), this.getBaseSettings(), this.gridActiveXList ? this.gridActiveXList.getSaveList() : null,
                        this.gridCookiesList ? this.gridCookiesList.getSaveList() : null,
                        this.gridSubnetList ? this.gridSubnetList.getSaveList() : null, this.gridPassList.getSaveList());
            }
        }
    });
}
