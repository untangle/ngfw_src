if (!Ung.hasResource["Ung.CPD"]) {
    Ung.hasResource["Ung.CPD"] = true;
    Ung.NodeWin.registerClassName('untangle-node-cpd', 'Ung.CPD');

    Ung.CPD = Ext.extend(Ung.NodeWin, {
        panelCaptiveHosts: null,
        gridCaptureRules : null,
        
        panelPassedHosts : null,
        panelUserAuthentication : null,
        panelCaptivePage : null,
        
        gridLoginEventLog : null,
        gridBlockEventLog : null,
        initComponent : function()
        {
            Ung.Util.clearInterfaceStore();

            // keep initial base settings
            this.initialBaseSettings = Ung.Util.clone(this.getBaseSettings());
            
            // builds the tabs
            this.buildCaptiveHosts();
            this.buildPassedHosts();
            this.buildUserAuthentication();
//             this.buildCaptivePage();
//             this.buildLoginEventLog();
//             this.buildBlockEventLog();

            // builds the tab panel with the tabs
            // this.buildTabPanel([this.panelCaptiveHosts, this.panelPassedHosts, this.panelUserAuthentication,
            // this.panelCaptivePage, this.gridLoginEventLog, this.gridBlockEventLog]);
            
            this.buildTabPanel([ this.panelCaptiveHosts, this.panelPassedHosts, this.panelUserAuthentication ]);

            Ung.CPD.superclass.initComponent.call(this);
        },
        // Rules Panel
        buildCaptiveHosts : function()
        {
            this.buildGridCaptureRules();

            this.panelCaptiveHosts = new Ext.Panel({
                name : "panelCaptiveHosts",
                helpSource : '',
                // private fields
                gridRulesList : null,
                parentId : this.getId(),
                title : this.i18n._("Captive Hosts"),
                autoScroll : true,
                border : false,
                cls: 'ung-panel',
                items : [{
                    title : this.i18n._('Note'),
                    cls: "description",
                    bodyStyle : "padding: 5px 5px 5px; 5px;",
                    html : this.i18n._("The <b>Capture Rules</b> area  set of rules to define which hosts and traffic are subject to the Captive Portal.  The rules are evailuated in order.")
                }, this.gridCaptureRules, {
                    xtype : "fieldset",
                    autoHeight : true,
                    items : [{
                        xtype : "checkbox",
                        boxLabel : this.i18n._("Capture Bypassed Traffic"),
                        hideLabel : true,
                        checked : this.getBaseSettings().captureBypassedTraffic,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().captureBypassedTraffic = checked;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }]
                         
            });
        },
        
        buildGridCaptureRules : function()
        {
            // enable is a check column
            var liveColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("Enable"),
                dataIndex : "live",
                fixed : true
            });
            
            // Capture  a check column
            var captureColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("Capture"),
                dataIndex : "capture",
                fixed : true
            });
            
            this.gridCaptureRules = new Ung.EditorGrid({
                name : 'gridCaptureRules',
                settingsCmp : this,
                height : 500,
                hasReorder : true,
                emptyRow : {
                    "live" : true,
                    "capture" : true,
                    "log" : false,
                    "clientInterface" : "",
                    "clientAddress" : "any",
                    "serverAddress" : "any",
                    "name" : this.i18n._("[no name]"),
                    "category" : this.i18n._("[no category]"),
                    "description" : this.i18n._("[no description]"),
                    "javaClass" : "com.untangle.node.cpd.CaptureRule"
                },
                title : this.i18n._("Capture Rules"),
                recordJavaClass : "com.untangle.node.cpd.CaptureRule",
                paginated : false,
                proxyRpcFn : this.getRpcNode().getCaptureRules,
                fields : [{
                    name : "id"
                },{
                    name : "live"
                },{
                    name : "capture"
                },{
                    name : "log"
                },{
                    name : "clientInterface"
                },{
                    name : "clientAddress"
                },{
                    name : "serverAddress"
                },{
                    name : "name"
                },{
                    name : "category"
                }, {
                    name : "description"
                }, {
                    name : "javaClass"
                }],
                columns : [liveColumn, captureColumn, {
                    id : "description",
                    header : this.i18n._("Description"),
                    width : 200,
                    dataIndex : "description",
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }],
                columnsDefaultSortable : false,
                autoExpandColumn : "description",
                plugins : [liveColumn, captureColumn]
            });

            //var rowEditor = this.buildGridCaptureRulesRowEditor();
            //this.gridCaptureRules.rowEditor = rowEditor;
        },
    
        buildGridCaptureRulesRowEditor : function()
        {
            return new Ung.RowEditorWindow({
                grid : this,
                sizeToGrid : true,
                inputLines : this.customInputLines,
                rowEditorLabelWidth:120,
                populate : function(record, addMode) {
                    this.addMode=addMode;
                    this.record = record;
                    this.initialRecordData = Ext.encode(record.data);
                    for (var i = 0; i < this.inputLines.length; i++) {
                        var inputLine = this.inputLines[i];
                        if (inputLine instanceof Ext.form.Field) {
                            this.populateField(inputLine, record);
                        } else if (inputLine instanceof Ext.Panel) {
                            for (var j = 0; j < inputLine.items.length; j++) {
                                var field = inputLine.items.get(j);
                                if ( field instanceof Ext.form.Field) {
                                    this.populateField(field, record);
                                }
                            }
                        }
                    }
                },
                populateField : function(field, record) {
                    if(field.dataIndex!=null) {
                        field.suspendEvents();
                        field.setValue(record.get(field.dataIndex));
                        field.resumeEvents();
                    }
                },
                isFormValid : function() {
                    for (var i = 0; i < this.inputLines.length; i++) {
                        var inputLine = this.inputLines[i];
                        if (inputLine instanceof Ext.form.Field && !inputLine.isValid()) {
                            return false;
                        } else if (inputLine instanceof Ext.Panel) {
                            for (var j = 0; j < inputLine.items.length; j++) {
                                var field = inputLine.items.get(j);
                                if (field instanceof Ext.form.Field && !field.isValid()) {
                                    return false;
                                }
                            }
                        }
                    }
                    return true;
                },
                updateAction : function() {
                    if (this.isFormValid()) {
                        if (this.record !== null) {
                            for (var i = 0; i < this.inputLines.length; i++) {
                                var inputLine = this.inputLines[i];
                                if (inputLine instanceof Ext.form.Field) {
                                    this.record.set(inputLine.dataIndex, inputLine.getValue());
                                } else if (inputLine instanceof Ext.Panel) {
                                    for (var j = 0; j < inputLine.items.length; j++) {
                                        var field = inputLine.items.get(j);
                                        if (field instanceof Ext.form.Field) {
                                            this.record.set(field.dataIndex, field.getValue());
                                        }
                                    }
                                }
                            }
                            
                            if (this.addMode) {
                                this.grid.getStore().insert(0, [this.record]);
                                this.grid.updateChangedData(this.record, "added");
                            }
                        }
                        this.hide();
                    } else {
                        Ext.MessageBox.alert(i18n._('Warning'), i18n._("The form is not valid!"));
                    }
                },
                isDirty : function() {
                    if (this.record !== null) {
                        if (this.inputLines) {
                            for (var i = 0; i < this.inputLines.length; i++) {
                                var inputLine = this.inputLines[i];
                                if(inputLine instanceof Ext.form.Field) {
                                    if (this.record.get(inputLine.dataIndex) != inputLine.getValue()) {
                                        return true;
                                    }
                                } else if (inputLine instanceof Ext.Panel) {
                                    for (var j = 0; j < inputLine.items.length; j++) {
                                        var field = inputLine.items.get(j);
                                        if (field instanceof Ext.form.Field) {
                                            if (this.record.get(field.dataIndex) != field.getValue()) {
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return false;
                }
            });
        },        

        // Event Log
        buildEventLog : function() {
            this.gridEventLog = new Ung.GridEventLog({
                settingsCmp : this,
                fields : [{
                    name : 'id'
                }, {
                    name : 'timeStamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'action',
                    mapping : 'wasBlocked',
                    type : 'string',
                    convert : function(value) {
                        return value ? this.i18n._("blocked") : this.i18n._("passed");
                    }.createDelegate(this)
                }, {
                    name : 'ruleIndex'
                }, {
                    name : 'client',
                    mapping : 'pipelineEndpoints',
                    sortType : Ung.SortTypes.asClient
                }, {
                    name : 'server',
                    mapping : 'pipelineEndpoints',
                    sortType : Ung.SortTypes.asServer
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
                    width : 100,
                    sortable : true,
                    dataIndex : 'action'
                }, {
                    header : this.i18n._("client"),
                    width : 165,
                    sortable : true,
                    dataIndex : 'client',
                    renderer : Ung.SortTypes.asClient
                }, {
                    id: 'ruleIndex',
                    header : this.i18n._('reason for action'),
                    width : 150,
                    sortable : true,
                    dataIndex : 'ruleIndex',
                    renderer : function(value, metadata, record) {
                           return String.format(this.i18n._("rule #{0}"), value);
                    }.createDelegate(this)
                }, {
                    header : this.i18n._("server"),
                    width : 165,
                    sortable : true,
                    dataIndex : 'server',
                    renderer : Ung.SortTypes.asServer
                }],
                autoExpandColumn: 'ruleIndex'

            });
        },

        buildPassedHosts : function()
        {
            this.gridPassedClients = 
                this.buildGridPassedList( "gridPassedClients", 
                                          this.i18n._( "Passed Listed Client Addresses"), 
                                          "com.untangle.node.cpd.PassedClients", 
                                          this.getRpcNode().getPassedClients);

            this.gridPassedServers = 
                this.buildGridPassedList( "gridPassedServers", 
                                          this.i18n._( "Passed Listed Server Addresses"), 
                                          "com.untangle.node.cpd.PassedServers", 
                                          this.getRpcNode().getPassedServers);
            
            this.panelPassedHosts = new Ext.Panel({
                name : "panelPassedHosts",
                helpSource : "",
                // private fields
                gridRulesList : null,
                parentId : this.getId(),
                title : this.i18n._("Passed Hosts"),
                layout : "form",
                autoScroll : true,
                border : false,
                cls: 'ung-panel',
                items : [ this.gridPassedClients, this.gridPassedServers ]
            });
        },

        buildGridPassedList : function( name, title, javaClass, rpcFn )
        {
            var liveColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("Enable"),
                dataIndex : "live",
                fixed : true
            });
            
            return new Ung.EditorGrid({
                name : name,
                settingsCmp : this,
                hasEdit : false,
                anchor : "100% 49%",
                hasReorder : false,
                emptyRow : {
                    "live" : true,
                    "log" : false,
                    "address" : "any",
                    "name" : this.i18n._("[no name]"),
                    "category" : this.i18n._("[no category]"),
                    "description" : this.i18n._("[no description]"),
                    "javaClass" : javaClass
                },
                title : this.i18n._(title),
                recordJavaClass : javaClass,
                paginated : false,
                proxyRpcFn : rpcFn,
                fields : [{
                    name : "id"
                },{
                    name : "live"
                },{
                    name : "log"
                },{
                    name : "address"
                },{
                    name : "name"
                },{
                    name : "category"
                }, {
                    name : "description"
                }, {
                    name : "javaClass"
                }],
                columns : [liveColumn, {
                    header : this.i18n._("Description"),
                    width : 200,
                    dataIndex : "description",
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                },{
                    id : name + "-address",
                    header : this.i18n._("Address"),
                    width : 200,
                    dataIndex : "address",
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }],
                plugins : [ liveColumn ],
                columnsDefaultSortable : false,
                autoExpandColumn : name + "-address"
            });
        },

        buildUserAuthentication : function()
        {
            this.panelUserAuthentication = new Ext.Panel({
                name : "panelUserAuthentication",
                helpSource : '',
                // private fields
                gridRulesList : null,
                parentId : this.getId(),
                title : this.i18n._("Captive Hosts"),
                autoScroll : true,
                border : false,
                cls: 'ung-panel',
                items : [{
                    xtype : "fieldset",
                    autoHeight : true,
                    title : this.i18n._( "User Authentication" ),
                    items : [{
                        xtype : "radio",
                        boxLabel : this.i18n._("None"),
                        hideLabel : true,
                        name : "authenticationType",
                        checked : !this.getBaseSettings().defaultAccept,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().authenticationType = "NONE";
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : "radio",
                        boxLabel : String.format( this.i18n._("Radius {0}(requires Directory Connector){1}"),
                                                  "<i>", "</i>" ),
                        hideLabel : true,
                        name : "authenticationType",
                        checked : this.getBaseSettings().defaultAccept,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().authenticationType = "RADIUS";
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : "radio",
                        boxLabel : String.format( this.i18n._("Active Directory {0}(requires Directory Connector){1}"),
                                                  "<i>", "</i>" ),
                        hideLabel : true,
                        name : "authenticationType",
                        checked : this.getBaseSettings().defaultAccept,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().authenticationType = "ACTIVE_DIRECTORY";
                                }.createDelegate(this)
                            }
                        }
                    }]
                },{
                    xtype : "fieldset",
                    autoHeight : true,
                    title : this.i18n._( "Session Settings" ),
                    items : [{
                        xtype : "unumberfield",
                        allowNegative : false,
                        allowBlank : false,
                        name : "idleTimeout",
                        fieldLabel : this.i18n._( "Idle Timeout" ),
                        boxLabel : this.i18n._( "minutes" ),
                        value : this.getBaseSettings().idleTimeout,
                        listeners : {
                            "change" : {
                                fn : function( elem, newValue ){
                                    this.getBaseSettings().idleTimeout = newValue;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : "unumberfield",
                        allowNegative : false,
                        allowBlank : false,
                        name : "timeout",
                        fieldLabel : this.i18n._( "Timeout" ),
                        boxLabel : this.i18n._( "minutes" ),
                        value : this.getBaseSettings().timeout,
                        listeners : {
                            "change" : {
                                fn : function( elem, newValue ){
                                    this.getBaseSettings().timeout = newValue;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : "checkbox",
                        boxLabel : this.i18n._("Logout Button Popup"),
                        hideLabel : true,
                        checked : this.getBaseSettings().logoutButtonEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().logoutButtonEnabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : "checkbox",
                        boxLabel : this.i18n._("Allow Concurrent Logins"),
                        hideLabel : true,
                        checked : this.getBaseSettings().concurrentLoginsEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().concurrentLoginsEnabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }]
            });
        },

        validateServer : function() {
            // ipMaddr list must be validated server side
            var passedAddresses = this.gridRules ? this.gridRules.getFullSaveList() : null;
            if (passedAddresses != null) {
                var srcAddrList = [];
                var dstAddrList = [];
                var srcPortList = [];
                var dstPortList = [];
                for (var i = 0; i < passedAddresses.length; i++) {
                    srcAddrList.push(passedAddresses[i]["srcAddress"]);
                    dstAddrList.push(passedAddresses[i]["dstAddress"]);
                    srcPortList.push(passedAddresses[i]["srcPort"]);
                    dstPortList.push(passedAddresses[i]["dstPort"]);
                }
                var validateData = {
                    map : {},
                    javaClass : "java.util.HashMap"
                };
                if (srcAddrList.length > 0) {
                    validateData.map["SRC_ADDR"] = {"javaClass" : "java.util.ArrayList", list : srcAddrList};
                }
                if (dstAddrList.length > 0) {
                    validateData.map["DST_ADDR"] = {"javaClass" : "java.util.ArrayList", list : dstAddrList};
                }
                if (srcPortList.length > 0) {
                    validateData.map["SRC_PORT"] = {"javaClass" : "java.util.ArrayList", list : srcPortList};
                }
                if (dstPortList.length > 0) {
                    validateData.map["DST_PORT"] = {"javaClass" : "java.util.ArrayList", list : dstPortList};
                }
                if (Ung.Util.hasData(validateData.map)) {
                    try {
                        var result=null;
                        try {
                            result = this.getValidator().validate(validateData);
                        } catch (e) {
                            Ung.Util.rpcExHandler(e);
                        }
                        if (!result.valid) {
                            var errorMsg = "";
                            switch (result.errorCode) {
                                case 'INVALID_SRC_ADDR' :
                                    errorMsg = this.i18n._("Invalid address specified for Source Address") + ": " + result.cause;
                                break;
                                case 'INVALID_DST_ADDR' :
                                    errorMsg = this.i18n._("Invalid address specified for Destination Address") + ": " + result.cause;
                                break;
                                case 'INVALID_SRC_PORT' :
                                    errorMsg = this.i18n._("Invalid port specified for Source Port") + ": " + result.cause;
                                break;
                                case 'INVALID_DST_PORT' :
                                    errorMsg = this.i18n._("Invalid port specified for Destination Port") + ": " + result.cause;
                                break;
                                default :
                                    errorMsg = this.i18n._(result.errorCode) + ": " + result.cause;
                            }
                            Ext.MessageBox.alert(this.i18n._("Validation failed"), errorMsg);
                            return false;
                        }
                    } catch (e) {
                        var message = exception.message;
                        if (message == null || message == "Unknown") {
                            message = i18n._("Please Try Again");
                        }
                        
                        Ext.MessageBox.alert(i18n._("Failed"), message);
                        return false;
                    }
                }
            }
            return true;
        },
        //apply function 
        applyAction : function()
        {
            this.saveAction(true);
        },         
        // save function
        saveAction : function(keepWindowOpen)
        {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.getRpcNode().updateAll(function(result, exception) {
                    Ext.MessageBox.hide();
                    if(Ung.Util.handleException(exception)) return;
                    // exit settings screen
                    if(!keepWindowOpen){
                        Ext.MessageBox.hide();                    
                        this.closeWindow();
                    }else{
                        //refresh the settings
                        Ext.MessageBox.hide();
                        //refresh the settings
                        this.getRpcNode().getBaseSettings(function(result2,exception2){
                            Ext.MessageBox.hide();                            
                            this.initialBaseSettings = Ung.Util.clone(this.getBaseSettings());
                            this.gridRules.setTotalRecords(result2.firewallRulesLength);
                            this.gridRules.reloadGrid();
                            this.initialRules =this.gridRules.getFullSaveList();                                 
                        }.createDelegate(this));                        
                        //this.gridEventLog.reloadGrid();                            
                    }
                }.createDelegate(this), this.getBaseSettings(), this.gridRules ? {javaClass:"java.util.ArrayList",list:this.gridRules.getFullSaveList()} : null);
            }
        },
        isDirty : function() {
            return !Ung.Util.equals(this.getBaseSettings(), this.initialBaseSettings)
                || this.gridCaptureRules.isDirty() 
                || this.gridPassedClients.isDirty()
                || this.gridPassedServers.isDirty();
        }
    });
}
