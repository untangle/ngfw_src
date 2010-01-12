if (!Ung.hasResource["Ung.CPD"]) {
    Ung.hasResource["Ung.CPD"] = true;
    Ung.NodeWin.registerClassName("untangle-node-cpd", "Ung.CPD");

    Ung.CPD = Ext.extend(Ung.NodeWin, {
        panelCaptiveHosts: null,
        gridCaptureRules : null,
        
        panelPassedHosts : null,
        panelUserAuthentication : null,
        panelCaptivePage : null,
        
        gridLoginEventLog : null,
        gridBlockEventLog : null,
        pageParameters : null,

        

        initComponent : function()
        {
            Ung.Util.clearInterfaceStore();

            // keep initial base settings
            this.initialBaseSettings = Ung.Util.clone(this.getBaseSettings());

            try {
                this.pageParameters = Ext.util.JSON.decode( this.getBaseSettings().pageParameters );
            } catch ( e ) {
                /* User should never see this. */
                /* XXX Currently this doesn't work because execution continues. */
                Ext.MessageBox.alert(this.i18n._("Warning"), this.i18n._("The current settings have an error, previous values may be lost."));
                this.pageParameters = {};
            }
            this.initialPageParameters = Ung.Util.clone(this.pageParameters);
            
            // builds the tabs
            this.buildCaptiveHosts();
            this.buildPassedHosts();
            this.buildUserAuthentication();
            this.buildCaptivePage();
            this.buildLoginEventLog();
            this.buildBlockEventLog();

            // builds the tab panel with the tabs
            this.buildTabPanel([ this.panelCaptiveHosts, this.panelPassedHosts, this.panelUserAuthentication,
                                 this.panelCaptivePage, this.gridLoginEventLog, this.gridBlockEventLog ]);

            Ung.CPD.superclass.initComponent.call(this);
        },
        // Rules Panel
        buildCaptiveHosts : function()
        {
            this.buildGridCaptureRules();

            this.panelCaptiveHosts = new Ext.Panel({
                name : "panelCaptiveHosts",
                helpSource : "",
                // private fields
                gridRulesList : null,
                parentId : this.getId(),
                title : this.i18n._("Captive Hosts"),
                autoScroll : true,
                border : false,
                cls: "ung-panel",
                items : [{
                    title : this.i18n._("Note"),
                    cls: "description",
                    bodyStyle : "padding: 5px 5px 5px; 5px;",
                    html : this.i18n._("The <b>Capture Rules</b> are a  set of rules to define which hosts and traffic are subject to the Captive Portal.  The rules are evailuated in order.")
                }, this.gridCaptureRules, {
                    xtype : "fieldset",
                    autoHeight : true,
                    items : [{
                        xtype : "checkbox",
                        boxLabel : this.i18n._("Capture Bypassed Traffic"),
                        hideLabel : true,
                        checked : this.getBaseSettings().captureBypassedTraffic,
                        listeners : {
                            "check" : function(elem, checked) {
                                this.getBaseSettings().captureBypassedTraffic = checked;
                            }.createDelegate(this)
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
                name : "gridCaptureRules",
                settingsCmp : this,
                height : 500,
                hasReorder : true,
                emptyRow : {
                    "live" : true,
                    "capture" : true,
                    "log" : false,
                    "clientInterface" : "internal",
                    "clientAddress" : "any",
                    "serverAddress" : "any",
                    "days" : "mon,tue,wed,thu,fri,sat,sun",
                    "startTime" : "00:00",
                    "endTime" : "23:59",
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
                },{
                    name : "description"
                },{
                    name : "startTime"
                },{
                    name : "endTime"
                },{
                    name : "days"
                },{
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

            var rowEditor = this.buildGridCaptureRulesRowEditor();
            this.gridCaptureRules.rowEditor = rowEditor;
        },
    
        buildGridCaptureRulesRowEditor : function()
        {
            return new Ung.RowEditorWindow({
                grid : this.gridCaptureRules,
                title : this.i18n._("Capture Rule"),
                inputLines : [{
                    xtype : "checkbox",
                    name : "live",
                    dataIndex : "live",
                    boxLabel : this.i18n._("Enabled"),
                    hideLabel : true
                },{
                    xtype : "checkbox",
                    name : "capture",
                    dataIndex : "capture",
                    boxLabel : this.i18n._("Capture"),
                    hideLabel : true
                },{
                    xtype : "textfield",
                    name : "description",
                    width : 220,
                    dataIndex : "description",
                    fieldLabel : this.i18n._("Description"),
                    allowBlank : false
                },{
                    xtype : "fieldset",
                    title : this.i18n._("Interface"),
                    autoHeight : true,
                    items : [{
                        cls: "description",
                        border : false,
                        html : this.i18n._("The ethernet interface (NIC).")
                    },new Ung.Util.InterfaceCombo({
                        name : "Client",
                        dataIndex : "clientInterface",
                        fieldLabel : this.i18n._("Client")
                    })]
                },{
                    xtype : "fieldset",
                    title : this.i18n._("Address"),
                    autoHeight : true,
                    items : [{
                        cls: "description",
                        border : false,
                        html : this.i18n._("The IP addresses.")
                    },{
                        xtype : "textfield",
                        name : "clientAddress",
                        dataIndex : "clientAddress",
                        fieldLabel : this.i18n._("Client"),
                        allowBlank : false
                    },{
                        xtype : "textfield",
                        name : "serverAddress",
                        dataIndex : "serverAddress",
                        fieldLabel : this.i18n._("Server"),
                        allowBlank : false
                    }]
                },{
                    xtype : "fieldset",
                    autoHeight : true,
                    title : this.i18n._("Time of Day"),
                    items : [{
                        cls: "description",
                        border : false,
                        html : this.i18n._("The time of day.")
                    },{
                        xtype : "utimefield",
                        name : "startTime",
                        dataIndex : "startTime",
                        fieldLabel : this.i18n._("Start Time"),
                        allowBlank : false
                    },{
                        xtype : "utimefield",
                        endTime : true,
                        name : "endTime",
                        dataIndex : "endTime",
                        fieldLabel : this.i18n._("End Time"),
                        allowBlank : false
                    }]
                },{
                    xtype : "fieldset",
                    autoHeight : true,
                    title : this.i18n._("Days of Week"),
                    items : [{
                        cls: "description",
                        border : false,
                        html : this.i18n._("The days of the week.")
                    },{
                        xtype : "checkbox",
                        name : "sunday",
                        dataIndex : "sun",
                        boxLabel : this.i18n._("Sunday"),
                        hideLabel : true
                    },{
                        xtype : "checkbox",
                        name : "monday",
                        dataIndex : "mon",
                        boxLabel : this.i18n._("Monday"),
                        hideLabel : true
                    },{
                        xtype : "checkbox",
                        name : "tuesday",
                        dataIndex : "tue",
                        boxLabel : this.i18n._("Tuesday"),
                        hideLabel : true
                    }, {
                        xtype : "checkbox",
                        name : "wednesday",
                        dataIndex : "wed",
                        boxLabel : this.i18n._("Wednesday"),
                        hideLabel : true
                    }, {
                        xtype : "checkbox",
                        name : "thursday",
                        dataIndex : "thu",
                        boxLabel : this.i18n._("Thursday"),
                        hideLabel : true
                    }, {
                        xtype : "checkbox",
                        name : "friday",
                        dataIndex : "fri",
                        boxLabel : this.i18n._("Friday"),
                        hideLabel : true
                    }, {
                        xtype : "checkbox",
                        name : "saturday",
                        dataIndex : "sat",
                        boxLabel : this.i18n._("Saturday"),
                        hideLabel : true
                    }]                    
                }],
                                
                populate : function(record, addMode) {
                    var days = record.get("days").split( "," );
                    for ( var c = 0 ; c < Ung.CPD.daysOfWeek.length ; c++ ) {
                        var day = Ung.CPD.daysOfWeek[c];
                        record.set( day, days.indexOf( day ) >= 0 );
                    }
                    
                    Ung.RowEditorWindow.prototype.populateTree.call(this, record, addMode);
                },
                isFormValid : function() {
                    return Ung.RowEditorWindow.prototype.isFormValid.call(this);
                },
                updateAction : function() {
                    Ung.RowEditorWindow.prototype.updateActionTree.call(this);
                    
                    if ( this.record !== null ) {
                        /* Create an array of the days, and then convert it to a string */
                        var days = [];
                        for ( var c = 0 ; c < Ung.CPD.daysOfWeek.length ; c++ ) {
                            var day = Ung.CPD.daysOfWeek[c];
                            if ( this.record.get( day ) === true ) {
                                days.push( day );
                            }
                            this.record.set( day, null );
                        }
                        
                        this.record.set( "days", days.join( "," ));
                    }
                },
                isDirty : function() {
                    return Ung.RowEditorWindow.prototype.isDirty.call(this);
                }
            });
        },

        buildPassedHosts : function()
        {
            this.gridPassedClients = 
                this.buildGridPassedList( "gridPassedClients", 
                                          this.i18n._( "Passed Listed Client Addresses"), 
                                          "com.untangle.node.cpd.PassedClient", 
                                          this.getRpcNode().getPassedClients);

            this.gridPassedServers = 
                this.buildGridPassedList( "gridPassedServers", 
                                          this.i18n._( "Passed Listed Server Addresses"), 
                                          "com.untangle.node.cpd.PassedServer", 
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
                cls: "ung-panel",
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
            var onUpdateRadioButton = function( elem, checked )
            {
                if ( checked ) {
                    this.getBaseSettings().authenticationType = elem.inputValue;
                }
            }.createDelegate(this);

            var onRenderRadioButton = function( elem )
            {
                elem.setValue(this.getBaseSettings().authenticationType);
            }.createDelegate(this);

            this.panelUserAuthentication = new Ext.Panel({
                name : "panelUserAuthentication",
                helpSource : "",
                // private fields
                gridRulesList : null,
                parentId : this.getId(),
                title : this.i18n._("User Authentication"),
                autoScroll : true,
                border : false,
                cls: "ung-panel",
                items : [{
                    xtype : "fieldset",
                    autoHeight : true,
                    title : this.i18n._( "User Authentication" ),
                    items : [{
                        xtype : "radio",
                        boxLabel : this.i18n._("None"),
                        hideLabel : true,
                        name : "authenticationType",
                        inputValue : "NONE",
                        listeners : {
                            "check" : onUpdateRadioButton,
                            "render" : onRenderRadioButton
                        }
                    },{
                        xtype : "radio",
                        boxLabel : this.i18n._("Local Directory"),
                        hideLabel : true,
                        name : "authenticationType",
                        inputValue : "LOCAL_DIRECTORY",
                        listeners : {
                            "check" : onUpdateRadioButton,
                            "render" : onRenderRadioButton
                        }
                    },{
                        xtype : "radio",
                        boxLabel : String.format( this.i18n._("RADIUS {0}(requires Directory Connector){1}"),
                                                  "<i>", "</i>" ),
                        hideLabel : true,
                        name : "authenticationType",
                        inputValue : "RADIUS",
                        listeners : {
                            "check" : onUpdateRadioButton,
                            "render" : onRenderRadioButton
                        }
                    },{
                        xtype : "radio",
                        boxLabel : String.format( this.i18n._("Active Directory {0}(requires Directory Connector){1}"),
                                                  "<i>", "</i>" ),
                        hideLabel : true,
                        name : "authenticationType",
                        inputValue : "ACTIVE_DIRECTORY",
                        listeners : {
                            "check" : onUpdateRadioButton,
                            "render" : onRenderRadioButton
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
                        value : this.getBaseSettings().idleTimeout / 60,
                        listeners : {
                            "change" : function( elem, newValue ){
                                this.getBaseSettings().idleTimeout = newValue * 60;
                            }.createDelegate(this)
                        }
                    },{
                        xtype : "unumberfield",
                        allowNegative : false,
                        allowBlank : false,
                        name : "timeout",
                        fieldLabel : this.i18n._( "Timeout" ),
                        boxLabel : this.i18n._( "minutes" ),
                        value : this.getBaseSettings().timeout / 60,
                        listeners : {
                            "change" : function( elem, newValue ){
                                this.getBaseSettings().timeout = newValue * 60;
                            }.createDelegate(this)
                        }
                    },{
                        xtype : "checkbox",
                        boxLabel : this.i18n._("Logout Button Popup"),
                        hideLabel : true,
                        checked : this.getBaseSettings().logoutButtonEnabled,
                        listeners : {
                            "check" : function(elem, checked) {
                                this.getBaseSettings().logoutButtonEnabled = checked;
                            }.createDelegate(this)
                        }
                    },{
                        xtype : "checkbox",
                        boxLabel : this.i18n._("Allow Concurrent Logins"),
                        hideLabel : true,
                        checked : this.getBaseSettings().concurrentLoginsEnabled,
                        listeners : {
                            "check" : function(elem, checked) {
                                this.getBaseSettings().concurrentLoginsEnabled = checked;
                            }.createDelegate(this)
                        }
                    }]
                }]
            });
        },
        captivePageHideComponents : function( currentValue )
        {
            var values = [ "BASIC_LOGIN", "BASIC_MESSAGE", "CUSTOM" ];
            for ( var c = 0 ; c < values.length ; c++ ) {
                var item = values[c];
                Ext.each( this.panelCaptivePage.find( "pageType", item ), function( component ) { 
                    if ( component.setContainerVisible ) {
                        component.setContainerVisible( currentValue == item );
                    } else {
                        component.setVisible( currentValue == item );
                    }
                }.createDelegate(this));
            }
        },
        setCaptivePageDefaults : function (){
            this.panelCaptivePage.find( "name", "pageType" )[0].setValue(this.getBaseSettings().pageType);  
            this.captivePageHideComponents(this.getBaseSettings().pageType );                          
        },

        buildCaptivePage : function()
        {
            var onUpdateRadioButton = function( elem, checked )
            {
                if ( checked ) {
                    this.getBaseSettings().pageType = elem.inputValue;
                    this.captivePageHideComponents( elem.inputValue );
                }
            }.createDelegate(this);
            
            var onRenderRadioButton = function( elem )
            {
                this.panelCaptivePage.find( "name", "pageType" )[0].setValue(this.getBaseSettings().pageType);
            }.createDelegate(this);
            
            this.panelCaptivePage = new Ext.Panel({
                name : "panelCaptivePage",
                helpSource : "",
                // private fields
                gridRulesList : null,
                parentId : this.getId(),
                title : this.i18n._("Captive Page"),
                autoScroll : true,
                border : false,
                cls: "ung-panel",
                listeners : {
                    "activate" : this.setCaptivePageDefaults.createDelegate(this)
                },
                items : [{
                    xtype : "fieldset",
                    autoHeight : true,
                    title : this.i18n._( "Captive Portal Page" ),
                    items : [{
                        xtype : "radio",
                        boxLabel : this.i18n._("Basic Login"),
                        hideLabel : true,
                        name : "pageType",
                        inputValue : "BASIC_LOGIN",
                        listeners : {
                            "check" : onUpdateRadioButton
                        }
                    },{
                        xtype : "radio",
                        boxLabel : this.i18n._("Basic Message"),
                        hideLabel : true,
                        name : "pageType",
                        inputValue : "BASIC_MESSAGE",
                        listeners : {
                            "check" : onUpdateRadioButton
                        }
                    },{
                        xtype : "radio",
                        boxLabel : this.i18n._("Custom"),
                        hideLabel : true,
                        name : "pageType",
                        inputValue : "CUSTOM",
                        listeners : {
                            "check" : onUpdateRadioButton
                        }
                    },{
                        xtype : "fieldset",
                        height : 400,
                        title : this.i18n._( "Captive Portal Page Configuration" ),
                        items : [{
                            xtype : "textfield",
                            allowBlank : false,
                            name : "basicLoginPageTitle",
                            fieldLabel : this.i18n._("Page Title"),
                            pageType : "BASIC_LOGIN",
                            value : this.pageParameters.basicLoginPageTitle,
                            listeners : {
                                "change" : function( elem, newValue ){
                                    this.pageParameters.basicLoginPageTitle = newValue;
                                }.createDelegate(this)
                            }
                        },{
                            xtype : "textfield",
                            allowBlank : false,
                            name : "basicLoginUsername",
                            fieldLabel : this.i18n._("Username Text"),
                            pageType : "BASIC_LOGIN",
                            value : this.pageParameters.basicLoginUsername,
                            listeners : {
                                "change" : function( elem, newValue ){
                                    this.pageParameters.basicLoginUsername = newValue;
                                }.createDelegate(this)
                            }
                        },{
                            xtype : "textfield",
                            allowBlank : false,
                            name : "basicLoginPassword",
                            fieldLabel : this.i18n._("Password Text"),
                            pageType : "BASIC_LOGIN",
                            value : this.pageParameters.basicLoginPassword,
                            listeners : {
                                "change" : function( elem, newValue ){
                                    this.pageParameters.basicLoginPassword = newValue;
                                }.createDelegate(this)
                            }
                        },{
                            xtype : "textfield",
                            allowBlank : false,
                            name : "basicLoginFooter",
                            fieldLabel : this.i18n._("Lower Text"),
                            pageType : "BASIC_LOGIN",
                            value : this.pageParameters.basicLoginFooter,
                            listeners : {
                                "change" : function( elem, newValue ){
                                    this.pageParameters.basicLoginFooter = newValue;
                                }.createDelegate(this)
                            }
                        },{
                            xtype : "textfield",
                            allowBlank : false,
                            name : "basicMessagePageTitle",
                            fieldLabel : this.i18n._("Page Title"),
                            pageType : "BASIC_MESSAGE",
                            value : this.pageParameters.basicMessagePageTitle,
                            listeners : {
                                "change" : function( elem, newValue ){
                                    this.pageParameters.basicMessagePageTitle = newValue;
                                }.createDelegate(this)
                            }
                        },{
                            xtype : "textarea",
                            allowBlank : false,
                            name : "basicMessageMessageText",
                            width : 400,
                            height : 250,
                            fieldLabel : this.i18n._("Message Text"),
                            pageType : "BASIC_MESSAGE",
                            value : this.pageParameters.basicMessageMessageText,
                            listeners : {
                                "change" : function( elem, newValue ){
                                    this.pageParameters.basicMessageMessageText = newValue;
                                }.createDelegate(this)
                            }
                        },{
                            xtype : "checkbox",
                            allowBlank : false,
                            name : "basicMessageAgree",
                            fieldLabel : this.i18n._("Agree Checkbox"),
                            pageType : "BASIC_MESSAGE",
                            checked : this.pageParameters.basicMessageAgree,
                            listeners : {
                                "check" : function(elem, checked) {
                                    this.pageParameters.basicMessageAgree = checked;
                                }.createDelegate(this)
                            }
                        },{
                            xtype : "textfield",
                            allowBlank : false,
                            name : "basicMessageAgreeText",
                            fieldLabel : this.i18n._("Agree Text"),
                            pageType : "BASIC_MESSAGE",
                            value : this.pageParameters.basicMessageAgreeText,
                            listeners : {
                                "change" : function( elem, newValue ){
                                    this.pageParameters.basicMessageAgreeText = newValue;
                                }.createDelegate(this)
                            }
                        },{
                            xtype : "textfield",
                            allowBlank : false,
                            name : "basicMessageFooter",
                            fieldLabel : this.i18n._("Page Title"),
                            pageType : "BASIC_MESSAGE",
                            value : this.pageParameters.basicMessageFooter,
                            listeners : {
                                "change" : function( elem, newValue ){
                                    this.pageParameters.basicMessageFooter = newValue;
                                }.createDelegate(this)
                            },
                        },{
                            fileUpload : true,
                            xtype : "form",
                            bodyStyle : "padding:0px 0px 0px 25px",
                            buttonAlign : "left",
                            id : "upload_custom_php",
                            url : "upload",
                            pageType : "CUSTOM",
                            border : false,
                            items : [{
                                fieldLabel : this.i18n._("File"),
                                name : "customUploadFile",
                                inputType : "file",
                                xtype : "textfield"
                            },{
                                xtype: "button",
                                name : "customSendFile",
                                text : i18n._("Upload File"),
                                handler : this.onUploadCustomFile.createDelegate(this)
                            },{
                                xtype : "hidden",
                                name : "type",
                                value : "cpd-custom-page"
                            }]
                        }]
                    },{
                        xtype: "button",
                        name : "viewPage",
                        text : i18n._("View Page")
                    }],
                },{
                    xtype : "fieldset",
                    autoHeight : true,
                    title : this.i18n._( "Session Redirect" ),
                    items : [{
                        xtype : "textfield",
                        name : "redirectUrl",
                        fieldLabel : this.i18n._("Redirect URL"),
                        value : this.getBaseSettings().redirectUrl,
                        listeners : {
                            "change" : function( elem, newValue ){
                                this.getBaseSettings().redirectUrl = newValue;
                            }.createDelegate(this)
                        }
                    },{
                        xtype : "checkbox",
                        boxLabel : this.i18n._("Redirect HTTP traffic to HTTPS captive page"),
                        hideLabel : true,
                        checked : this.getBaseSettings().useHttpsPage,
                        listeners : {
                            "check" : function(elem, checked) {
                                this.getBaseSettings().useHttpsPage = checked;
                            }.createDelegate(this)
                        }
                    },{
                        xtype : "checkbox",
                        boxLabel : this.i18n._("Redirect HTTPS traffic to HTTPS captive page"),
                        hideLabel : true,
                        checked : this.getBaseSettings().redirectHttpsEnabled,
                        listeners : {
                            "check" : function(elem, checked) {
                                this.getBaseSettings().redirectHttpsEnabled = checked;
                            }.createDelegate(this)
                        }
                    }]
                }]
            });
        },

        onUploadCustomFile : function()
        {
            var form = this.panelCaptivePage.find( "id", "upload_custom_php" )[0].getForm();
            form.submit({
                parentID : this.panelCaptivePage.getId(),
                waitMsg : this.i18n._("Please wait while uploading your custom captive portal page..."),
                success : this.uploadCustomFileSuccess.createDelegate( this ),
                failure : this.uploadCustomFileFailure.createDelegate( this )
            });
        },

        uploadCustomFileSuccess : function()
        {
            Ext.MessageBox.alert( this.i18n._("Succeeded"), this.i18n._("Uploading Custom Captive Portal Page succeeded"));
            var field = this.panelCaptivePage.find( "name", "customUploadFile" )[0];
            field.reset();
        },

        uploadCustomFileFailure : function()
        {
            Ext.MessageBox.alert(this.i18n._("Failed"), 
                                 this.i18n._("There was an error uploading the Custom Captive Portal Page." ));
        },

        buildLoginEventLog : function() {
            this.gridLoginEventLog = new Ung.GridEventLog({
                title : this.i18n._( "Login Event Log" ),
                eventManagerFn : this.getRpcNode().getLoginEventManager(),
                settingsCmp : this,
                autoExpandColumn: "username",
                fields : [{
                    name : "id"
                },{
                    name : "timeStamp",
                    sortType : Ung.SortTypes.asTimestamp
                },{
                    name : "clientAddr"
                },{
                    name : "loginName"
                }],
                
                columns : [{
                    header : this.i18n._("timestamp"),
                    width : 130,
                    sortable : true,
                    dataIndex : "timeStamp",
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                },{
                    header : this.i18n._("client"),
                    width : 100,
                    sortable : true,
                    dataIndex : "clientAddr"
                },{
                    header : this.i18n._("username"),
                    id : "username",
                    width : 165,
                    sortable : true,
                    dataIndex : "loginName",
                    renderer : Ung.SortTypes.asClient
                }]
            });
        },

        buildBlockEventLog : function() {
            this.gridBlockEventLog = new Ung.GridEventLog({
                title : this.i18n._( "Block Event Log" ),
                eventManagerFn : this.getRpcNode().getBlockEventManager(),
                settingsCmp : this,
                autoExpandColumn: "reason",
                fields : [{
                    name : "id"
                },{
                    name : "timeStamp",
                    sortType : Ung.SortTypes.asTimestamp
                },{
                    name : "client",
                    mapping : "pipelineEndpoints",
                    sortType : Ung.SortTypes.asClient
                }, {
                    name : "server",
                    mapping : "pipelineEndpoints",
                    sortType : Ung.SortTypes.asServer
                }],
                
                columns : [{
                    header : this.i18n._("timestamp"),
                    width : 130,
                    sortable : true,
                    dataIndex : "timeStamp",
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                },{
                    header : this.i18n._("action"),
                    width : 100,
                    sortable : false,
                    renderer : function(value) {
                        return this.i18n._( "block" );
                    }.createDelegate( this )
                },{
                    header : this.i18n._("client"),
                    width : 100,
                    sortable : true,
                    dataIndex : "clientAddr",
                    renderer : Ung.SortTypes.asServer
                },{
                    header : this.i18n._("reason"),
                    id : "reason",
                    width : 100,
                    sortable : false,
                    renderer : function(value) {
                        return this.i18n._( "unauthenticated" );
                    }.createDelegate( this )
                },{
                    header : this.i18n._("server"),
                    width : 100,
                    sortable : true,
                    dataIndex : "server",
                    renderer : Ung.SortTypes.asServer
                }]
            });
        },

        //apply function 
        applyAction : function()
        {
            this.commitSettings(this.reloadSettings.createDelegate(this));
        },
        saveAction : function()
        {
            this.commitSettings(this.completeSaveAction.createDelegate(this));
        },
        completeSaveAction : function()
        {
            Ext.MessageBox.hide();
            // exit settings screen
            this.closeWindow();
        },
        reloadSettings : function()
        {
            this.getRpcNode().getBaseSettings(this.completeReloadSettings.createDelegate( this ));
        },
        completeReloadSettings : function( result, exception )
        {
            if(Ung.Util.handleException(exception)) {
                return;
            }

            this.rpc.baseSettings = result;
            this.initialBaseSettings = Ung.Util.clone(this.rpc.baseSettings);

            try {
                this.pageParameters = Ext.util.JSON.decode( this.getBaseSettings().pageParameters );
            } catch ( e ) {
                /* User should never see this. */
                Ext.MessageBox.alert(i18n._("Warning"), i18n._("The current settings have an error, previous values may be lost."));
                this.pageParameters = {};
            }
            this.initialPageParameters = Ung.Util.clone(this.pageParameters);
            
            /* Only reload the data for the grids if they have already been rendered. */
            if ( this.gridCaptureRules.rendered ) {
                this.gridCaptureRules.reloadGrid();
            }
            if ( this.gridPassedClients.rendered ) {
                this.gridPassedClients.reloadGrid();
            }
            if ( this.gridPassedServers.rendered ) {
                this.gridPassedServers.reloadGrid();
            }

            Ext.MessageBox.hide();
        },
        // commit function
        commitSettings : function(callback)
        {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                var captureRules = null, passedClients = null, passedServers = null;
                this.getBaseSettings().pageParameters = Ext.util.JSON.encode( this.pageParameters );

                if ( this.gridCaptureRules.rendered ) {
                    captureRules = { 
                        javaClass : "java.util.ArrayList", 
                        list : this.gridCaptureRules.getFullSaveList()
                    };
                }
                if ( this.gridPassedClients.rendered ) {
                    passedClients = {
                        javaClass : "java.util.ArrayList", 
                        list : this.gridPassedClients.getFullSaveList()
                    };
                }
                if ( this.gridPassedServers.rendered ) {
                    passedServers = {
                        javaClass : "java.util.ArrayList", 
                        list : this.gridPassedServers.getFullSaveList()
                    };
                }
                
                var wrapper = function( result, exception )
                {
                    if(Ung.Util.handleException(exception)) {
                        return;
                    }
                    callback();
                }.createDelegate(this);
                this.getRpcNode().setAll(wrapper, this.getBaseSettings(), 
                                         captureRules, passedClients, passedServers );
            }
        },
        isDirty : function() {
            return !Ung.Util.equals(this.getBaseSettings(), this.initialBaseSettings)
                || !Ung.Util.equals(this.pageParameters, this.initialPageParameters )
                || this.gridCaptureRules.isDirty() 
                || this.gridPassedClients.isDirty()
                || this.gridPassedServers.isDirty();
        }
    });

    Ung.CPD.daysOfWeek = ["mon","tue","wed","thu","fri","sat","sun"];
}
