if (!Ung.hasResource["Ung.CPD"]) {
    Ung.hasResource["Ung.CPD"] = true;
    Ung.NodeWin.registerClassName("untangle-node-cpd", "Ung.CPD");

    Ung.CPD = Ext.extend(Ung.NodeWin, {
        panelCaptiveStatus : null,
        gridCaptiveStatus : null,

        panelCaptiveHosts: null,
        gridCaptureRules : null,

        panelPassedHosts : null,
        panelUserAuthentication : null,
        panelCaptivePage : null,

        gridLoginEventLog : null,
        gridBlockEventLog : null,
        pageParameters : null,

        workingNodeSettings : null,
        initialNodeSettings : null,

        initComponent : function()
        {
            Ung.Util.clearInterfaceStore();

            // keep initial base settings
            this.initialNodeSettings = Ung.Util.clone(this.getRpcNode().getCPDSettings());
            this.workingNodeSettings = Ung.Util.clone(this.getRpcNode().getCPDSettings());

            try {
                this.pageParameters = Ext.util.JSON.decode( this.workingNodeSettings.pageParameters );
            } catch ( e ) {
                /* User should never see this. */
                /* XXX Currently this doesn't work because execution continues. */
                Ext.MessageBox.alert(
                    this.i18n._("Warning"),
                    this.i18n._("The current settings have an error, previous values may be lost."));

                this.pageParameters = {};
            }
            this.initialPageParameters = Ung.Util.clone(this.pageParameters);

            // builds the tabs
            this.buildCaptiveStatus();
            this.buildCaptiveHosts();
            this.buildPassedHosts();
            this.buildCaptivePage();
            this.buildUserAuthentication();
            this.buildLoginEventLog();
            this.buildBlockEventLog();

            // builds the tab panel with the tabs
            this.buildTabPanel([ this.panelCaptiveStatus, this.panelCaptiveHosts, this.panelPassedHosts, this.panelCaptivePage,
                                 this.panelUserAuthentication, this.gridLoginEventLog, this.gridBlockEventLog ]);

            Ung.CPD.superclass.initComponent.call(this);
        },

        buildCaptiveStatus : function() {

            this.buildGridCaptiveStatus();

            this.panelCaptiveStatus = new Ext.Panel({
                name : 'Status',
                parentId : this.getId(),
                title : this.i18n._('Status'),
                layout : "anchor",
                defaults: {
                        anchor: "98%"
                },
                cls: 'ung-panel',
                autoScroll : true,
                items : [{
                    title : this.i18n._('Status'),
                    name : 'Status',
                    xtype : 'fieldset',
                    autoHeight : true,
                    items: [{
                        html : String.format(this.i18n._('Captive Portal allows administrators to require network users to complete a defined process, such as logging in or accepting a network usage policy, before accessing the internet.'),'<b>','</b>'),
                        cls: 'description',
                        border : false}
                    ]
                 }]
            });

            this.panelCaptiveStatus.add( this.gridCaptiveStatus );
        },

        buildGridCaptiveStatus : function()
        {
            // this function will logout an active user
            userLogout = function(record) {
                this.getRpcNode().logout( record.data.ipv4Address );
                this.gridCaptiveStatus.store.reload();
            }.createDelegate( this );

            // logout is a button column
            var logoutColumn = new Ext.grid.ButtonColumn({
                width : 80,
                sortable : false,
                header : this.i18n._("Control"),
                dataIndex : null,
                handle : userLogout,
                renderer : function(value, metadata, record) {
                    var out= '';
                    if(record.data.id>=0) {
                        out = '<div class="ung-button button-column" style="text-align:center;" >' + this.i18n._("Logout") + '</div>';
                    }
                    return out;
                }.createDelegate( this )
            });

            this.gridCaptiveStatus = new Ung.EditorGrid({
                name : "gridCaptiveStatus",
                settingsCmp : this,
                height : 500,
                parentId : this.getId(),
                hasAdd : false,
                configAdd : null,
                hasEdit : false,
                configEdit : null,
                hasDelete : false,
                configDelete : null,
                columnsDefaultSortable : true,
                title : this.i18n._("Active Sessions"),
                qtip : this.i18n._("The Active Sessions list shows authenticated users."),
                paginated : false,
                bbar : new Ext.Toolbar({
                    items : [
                        '-',
                        {
                            xtype : 'tbbutton',
                            id: "refresh_"+this.getId(),
                            text : i18n._('Refresh'),
                            name : "Refresh",
                            tooltip : i18n._('Refresh'),
                            iconCls : 'icon-refresh',
                            handler : function() {
                                this.gridCaptiveStatus.store.reload();
                            }.createDelegate(this)
                        }
                    ]
                }),
                recordJavaClass : "com.untangle.node.cpd.HostDatabaseEntry",
                proxyRpcFn : this.getRpcNode().getCaptiveStatus,
                plugins : [logoutColumn],
                fields : [{
                    name : "ipv4Address"
                },{
                    name : "username"
                },{
                    name : "lastSession"
                },{
                    name : "sessionStart"
                },{
                    name : "expirationDate"
                },{
                    name : "hardwareAddress"
                },{
                    name : "id"
                }],
                columns : [{
                    id : "ipv4Address",
                    header : this.i18n._("IP Address"),
                    width : 150
                },{
                    id : "username",
                    header : this.i18n._("User Name"),
                    width : 200
                },{
                    id : "lastSession",
                    header : this.i18n._("Last Session"),
                    width : 180,
                    renderer : function(value) { return i18n.timestampFormat(value); }
                },{
                    id : "sessionStart",
                    header : this.i18n._("Current Session"),
                    width : 180,
                    renderer : function(value) { return i18n.timestampFormat(value); }
                },{
                    id : "expirationDate",
                    header : this.i18n._("Expiration"),
                    width : 180,
                    renderer : function(value) { return i18n.timestampFormat(value); }
                },logoutColumn]
            });
        },

        // Rules Panel
        buildCaptiveHosts : function()
        {
            this.buildGridCaptureRules();

            this.panelCaptiveHosts = new Ext.Panel({
                name : "panelCaptiveHosts",
                helpSource : "captive_hosts",
                // private fields
                parentId : this.getId(),
                title : this.i18n._("Captive Hosts"),
                autoScroll : true,
                border : false,
                cls: "ung-panel",
                items : [{
                    title : this.i18n._("Note"),
                    cls: "description",
                    bodyStyle : "padding: 5px 5px 5px; 5px;",
                    html : this.i18n._("The <b>Capture Rules</b> are a  set of rules to define which hosts and traffic are subject to the Captive Portal.  The rules are evaluated in order.")
                }, this.gridCaptureRules, {
                    xtype : "fieldset",
                    autoHeight : true,
                    items : [{
                        xtype : "checkbox",
                        boxLabel : this.i18n._("Capture Bypassed Traffic"),
                        tooltip : this.i18n._("If enabled, traffic that is bypassed in Bypass Rules will also captured until the host is authenticated."),
                        hideLabel : true,
                        checked : this.workingNodeSettings.captureBypassedTraffic,
                        listeners : {
                            "check" : function(elem, checked) {
                                this.workingNodeSettings.captureBypassedTraffic = checked;
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
                    "clientInterface" : "1",
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
                qtip : this.i18n._("The Capture Rules are a set of rules to define which hosts and traffic are subject to the Captive Portal. All enabled rules are evaluated in order."),
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
                        fieldLabel : this.i18n._("Client"),
                        /* Exclude the UVM specific matchers like More Trusted and Less Trusted. */
                        simpleMatchers : true
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
                                          this.i18n._( "Pass Listed Client Addresses"),
                                          "com.untangle.node.cpd.PassedClient",
                                          this.getRpcNode().getPassedClients,
                                          "Pass Listed Client Addresses is a list of Client IPs that are not subjected to the Captive Portal.");

            this.gridPassedServers =
                this.buildGridPassedList( "gridPassedServers",
                                          this.i18n._( "Pass Listed Server Addresses"),
                                          "com.untangle.node.cpd.PassedServer",
                                          this.getRpcNode().getPassedServers,
                                          "Pass Listed Server Addresses is a list of Server IPs that unauthenticated clients can access without authentication.");

            this.panelPassedHosts = new Ext.Panel({
                name : "panelPassedHosts",
                helpSource : "passed_hosts",
                // private fields
                parentId : this.getId(),
                title : this.i18n._("Passed Hosts"),
                layout : "form",
                autoScroll : true,
                border : false,
                cls: "ung-panel",
                items : [ this.gridPassedClients, this.gridPassedServers ]
            });
        },

        buildGridPassedList : function( name, title, javaClass, rpcFn , tooltip)
        {
            var liveColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("Enable"),
                dataIndex : "live",
                fixed : true
            });

            return new Ung.EditorGrid({
                name : name,
                tooltip  : tooltip,
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
                    this.workingNodeSettings.authenticationType = elem.inputValue;
                }
            }.createDelegate(this);

            var onRenderRadioButton = function( elem )
            {
                elem.setValue(this.workingNodeSettings.authenticationType);
            }.createDelegate(this);

            this.panelUserAuthentication = new Ext.Panel({
                name : "panelUserAuthentication",
                helpSource : "user_authentication",
                // private fields
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
                        xtype: "button",
                        name : "configureLocalDirectory",
                        text : i18n._("Configure Local Directory"),
                        handler : this.configureLocalDirectory.createDelegate( this )
                    },{
                        xtype : "radio",
                        boxLabel : String.format( this.i18n._("RADIUS {0}(requires Directory Connector){1}"),
                                                  "<i>", "</i>" ),
                        hideLabel : true,
                        disabled : !main.getLicenseManager().getLicense("untangle-node-adconnector"),
                        name : "authenticationType",
                        inputValue : "RADIUS",
                        listeners : {
                            "check" : onUpdateRadioButton,
                            "render" : onRenderRadioButton
                        }
                    },{
                        xtype: "button",
                        disabled : !main.getLicenseManager().getLicense("untangle-node-adconnector"),
                        name : "configureRadiusServer",
                        text : i18n._("Configure RADIUS"),
                        handler : this.configureRadius.createDelegate( this )
                    },{
                        xtype : "radio",
                        boxLabel : String.format( this.i18n._("Active Directory {0}(requires Directory Connector){1}"),
                                                  "<i>", "</i>" ),
                        hideLabel : true,
                        disabled : !main.getLicenseManager().getLicense("untangle-node-adconnector"),
                        name : "authenticationType",
                        inputValue : "ACTIVE_DIRECTORY",
                        listeners : {
                            "check" : onUpdateRadioButton,
                            "render" : onRenderRadioButton
                        }
                    },{
                        xtype: "button",
                        disabled : !main.getLicenseManager().getLicense("untangle-node-adconnector"),
                        name : "configureActiveDirectory",
                        text : i18n._("Configure Active Directory"),
                        handler : this.configureActiveDirectory.createDelegate( this )
                    }]
                },{
                    xtype : "fieldset",
                    autoHeight : true,
                    title : this.i18n._( "Session Settings" ),
                    items : [{
                        xtype : "numberfield",
                        allowNegative : false,
                        allowBlank : false,
                        name : "idleTimeout",
                        maxValue : 24 * 60,
                        minValue : 0,
                        invalidText : this.i18n._( "The Idle Timeout must be between 0 minutes and 24 hours." ),
                        fieldLabel : this.i18n._( "Idle Timeout" ),
                        boxLabel : this.i18n._( "minutes" ),
                        tooltip : this.i18n._( "Clients will be unauthenticated after this amount of idle time. They may re-authenticate immediately." ),
                        value : this.workingNodeSettings.idleTimeout / 60,
                        listeners : {
                            "change" : function( elem, newValue ){
                                this.workingNodeSettings.idleTimeout = newValue * 60;
                            }.createDelegate(this)
                        }
                    },{
                        xtype : "numberfield",
                        allowNegative : false,
                        allowBlank : false,
                        name : "timeout",
                        maxValue : 24 * 60,
                        minValue : 5,
                        fieldLabel : this.i18n._( "Timeout" ),
                        boxLabel : this.i18n._( "minutes" ),
                        invalidText : this.i18n._( "The Timeout must be between 5 minutes and 24 hours." ),
                        tooltip : this.i18n._( "Clients will be unauthenticated after this amount of time regardless of activity. They may re-authenticate immediately." ),
                        value : this.workingNodeSettings.timeout / 60,
                        listeners : {
                            "change" : function( elem, newValue ){
                                this.workingNodeSettings.timeout = newValue * 60;
                            }.createDelegate(this)
                        }
                    },{
                        xtype : "checkbox",
                        boxLabel : this.i18n._("Allow Concurrent Logins"),
                        tooltip : this.i18n._("This will allow multiple hosts to use the same username & password concurrently."),
                        hideLabel : true,
                        checked : this.workingNodeSettings.concurrentLoginsEnabled,
                        listeners : {
                            "check" : function(elem, checked) {
                                this.workingNodeSettings.concurrentLoginsEnabled = checked;
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
            this.panelCaptivePage.find( "name", "pageType" )[0].setValue(this.workingNodeSettings.pageType);
            this.captivePageHideComponents(this.workingNodeSettings.pageType );
        },

        buildCaptivePage : function()
        {
            var onUpdateRadioButton = function( elem, checked )
            {
                if ( checked ) {
                    this.workingNodeSettings.pageType = elem.inputValue;
                    this.captivePageHideComponents( elem.inputValue );
                }
            }.createDelegate(this);

            var onRenderRadioButton = function( elem )
            {
                this.panelCaptivePage.find( "name", "pageType" )[0].setValue(this.workingNodeSettings.pageType);
            }.createDelegate(this);

            this.panelCaptivePage = new Ext.Panel({
                name : "panelCaptivePage",
                helpSource : "captive_page",
                // private fields
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
                        boxLabel : this.i18n._("Basic Message"),
                        hideLabel : true,
                        name : "pageType",
                        inputValue : "BASIC_MESSAGE",
                        listeners : {
                            "check" : onUpdateRadioButton
                        }
                    },{
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
                        boxLabel : this.i18n._("Custom"),
                        hideLabel : true,
                        name : "pageType",
                        inputValue : "CUSTOM",
                        listeners : {
                            "check" : onUpdateRadioButton
                        }
                    },{
                        xtype : "fieldset",
                        autoHeight : true,
                        autoScroll : false,
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
                            name : "basicLoginPageWelcome",
                            fieldLabel : this.i18n._("Welcome Text"),
                            width : 400,
                            pageType : "BASIC_LOGIN",
                            value : this.pageParameters.basicLoginPageWelcome,
                            listeners : {
                                "change" : function( elem, newValue ){
                                    this.pageParameters.basicLoginPageWelcome = newValue;
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
                            xtype : "textarea",
                            allowBlank : true,
                            name : "basicLoginMessageText",
                            width : 400,
                            height : 250,
                            fieldLabel : this.i18n._("Message Text"),
                            pageType : "BASIC_LOGIN",
                            value : this.pageParameters.basicLoginMessageText,
                            listeners : {
                                "change" : function( elem, newValue ){
                                    this.pageParameters.basicLoginMessageText = newValue;
                                }.createDelegate(this)
                            }
                        },{
                            xtype : "textfield",
                            allowBlank : false,
                            name : "basicLoginFooter",
                            fieldLabel : this.i18n._("Lower Text"),
                            width : 400,
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
                            width : 400,
                            value : this.pageParameters.basicMessagePageTitle,
                            listeners : {
                                "change" : function( elem, newValue ){
                                    this.pageParameters.basicMessagePageTitle = newValue;
                                }.createDelegate(this)
                            }
                        },{
                            xtype : "textfield",
                            allowBlank : false,
                            name : "basicMessagePageWelcome",
                            fieldLabel : this.i18n._("Welcome Text"),
                            width : 400,
                            pageType : "BASIC_MESSAGE",
                            value : this.pageParameters.basicMessagePageWelcome,
                            listeners : {
                                "change" : function( elem, newValue ){
                                    this.pageParameters.basicMessagePageWelcome = newValue;
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
                            width : 400,
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
                            fieldLabel : this.i18n._("Lower Text"),
                            width : 400,
                            pageType : "BASIC_MESSAGE",
                            value : this.pageParameters.basicMessageFooter,
                            listeners : {
                                "change" : function( elem, newValue ){
                                    this.pageParameters.basicMessageFooter = newValue;
                                }.createDelegate(this)
                            }
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
                        text : i18n._("View Page"),
                        handler : function()
                        {
                            if ( this.node.state != "on" ) {
                                Ext.MessageBox.alert(this.i18n._("Captive Portal is Disabled"),
                                                     this.i18n._("You must turn on the Captive Portal to preview the Captive Page." ));
                                return;
                            }

                            if ( this.isDirty()) {
                                Ext.MessageBox.alert(this.i18n._("Unsaved Changes"),
                                                     this.i18n._("You must save your settings before previewing the page." ));
                                return;
                            }

                            window.open("/cpd", "_blank");
                        }.createDelegate(this)
                    }]
                },{
                    xtype : "fieldset",
                    autoHeight : true,
                    title : this.i18n._( "Session Redirect" ),
                    items : [{
                        xtype : "textfield",
                        name : "redirectUrl",
                        width : 200,
                        fieldLabel : this.i18n._("Redirect URL"),
                        tooltip : this.i18n._("Users will be redirected to this page immediately after authentication. Blank sends the user to their original destination."),
                        value : this.workingNodeSettings.redirectUrl,
                        listeners : {
                            "change" : function( elem, newValue ){
                                this.workingNodeSettings.redirectUrl = newValue;
                            }.createDelegate(this)
                        }
                    },{
                        xtype : "checkbox",
                        boxLabel : this.i18n._("Redirect HTTP traffic to HTTPS captive page"),
                        tooltip : this.i18n._("If unchecked, HTTP traffic to unauthenticated hosts will be redirect to the HTTP Captive page. If checked, users will be redirected to an HTTPS captive page."),
                        hideLabel : true,
                        checked : this.workingNodeSettings.useHttpsPage,
                        listeners : {
                            "check" : function(elem, checked) {
                                this.workingNodeSettings.useHttpsPage = checked;
                            }.createDelegate(this)
                        }
                    },{
                        xtype : "checkbox",
                        boxLabel : this.i18n._("Redirect HTTPS traffic to HTTPS captive page"),
                        tooltip : this.i18n._("If unchecked, HTTPS traffic for unauthenticated users is blocked. If checked HTTPS traffic will be redirected to the HTTPS captive page. Warning: This will cause certificate warning errors in the browser."),
                        hideLabel : true,
                        checked : this.workingNodeSettings.redirectHttpsEnabled,
                        listeners : {
                            "check" : function(elem, checked) {
                                this.workingNodeSettings.redirectHttpsEnabled = checked;
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
                helpSource : "login_event_log",
                eventQueriesFn : this.getRpcNode().getLoginEventQueries,
                settingsCmp : this,
                autoExpandColumn: "username",
                fields : [{
                    name : "timeStamp",
                    sortType : Ung.SortTypes.asTimestamp
                },{
                    name : "clientAddr"
                },{
                    name : "loginName"
                },{
                    name : "authType"
                },{
                    name : "event"
                }],

                columns : [{
                    header : this.i18n._("timestamp"),
                    width : Ung.Util.timestampFieldWidth,
                    sortable : true,
                    dataIndex : "timeStamp",
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                },{
                    header : this.i18n._("client"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : "clientAddr"
                },{
                    header : this.i18n._("username"),
                    id : "username",
                    width : Ung.Util.usernameFieldWidth,
                    sortable : true,
                    dataIndex : "loginName"
                },{
                    header : this.i18n._("action"),
                    width : 165,
                    sortable : true,
                    dataIndex : "event",
                    render : function( value ) {
                        switch ( value ) {
                        case "LOGIN":
                            return this.i18n._( "authenticated" );
                        case "FAILED":
                            return this.i18n._( "access denied" );
                        case "UPDATE":
                            return this.i18n._( "re-authenticated" );

                        case "LOGOUT":
                            return this.i18n._( "logout" );
                        }
                        return "";
                    }.createDelegate( this )
                },{
                    header : this.i18n._("Authentication"),
                    width : 165,
                    sortable : true,
                    dataIndex : "authType",
                    render : function( value ) {
                        switch ( value ) {
                        case "NONE":
                            return this.i18n._( "None" );
                        case "LOCAL_DIRECTORY":
                            return this.i18n._( "Local Directory" );
                        case "ACTIVE_DIRECTORY":
                            return this.i18n._( "Active Directory" );
                        case "RADIUS":
                            return this.i18n._( "RADIUS" );
                        }

                        return "";
                    }.createDelegate( this )
                }]
            });
        },

        buildBlockEventLog : function() {
            this.gridBlockEventLog = new Ung.GridEventLog({
                title : this.i18n._( "Block Event Log" ),
                helpSource : "block_event_log",
                eventQueriesFn : this.getRpcNode().getBlockEventQueries,
                settingsCmp : this,
                autoExpandColumn: "reason",
                fields : [{
                    name : "timeStamp",
                    sortType : Ung.SortTypes.asTimestamp
                },{
                    name : "clientAddress"
                },{
                    name : "clientPort"
                },{
                    name : "serverAddress"
                },{
                    name : "serverPort"
                },{
                    name : "client",
                    convert : function(value, record) {
                        return record.clientAddress + ":" + record.clientPort;
                    }
                }, {
                    name : "server",
                    convert : function(value, record) {
                        return record.serverAddress + ":" + record.serverPort;
                    }
                }],

                columns : [{
                    header : this.i18n._("timestamp"),
                    width : Ung.Util.timestampFieldWidth,
                    sortable : true,
                    dataIndex : "timeStamp",
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                },{
                    header : this.i18n._("action"),
                    width : 80,
                    sortable : false,
                    renderer : function(value) {
                        return this.i18n._( "block" );
                    }.createDelegate( this )
                },{
                    header : this.i18n._("client"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : "client"
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
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : "server"
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
            this.getRpcNode().getCPDSettings(this.completeReloadSettings.createDelegate( this ));
        },
        completeReloadSettings : function( result, exception )
        {
            if(Ung.Util.handleException(exception)) {
                return;
            }

            this.rpc.baseSettings = result;
            this.initialNodeSettings = Ung.Util.clone(this.rpc.baseSettings);

            try {
                this.pageParameters = Ext.util.JSON.decode( this.workingNodeSettings.pageParameters );
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
        validateClient : function()
        {
            /* Iterate all of the fields checking if they are valid */
            if ( !this.find( "name", "idleTimeout" )[0].isValid() ||
                 !this.find( "name", "timeout" )[0].isValid()) {
                Ext.MessageBox.alert(this.i18n._("Warning"),
                                     this.i18n._("Please correct any highlighted fields."),
                                     function () {
                                         this.tabs.activate(this.panelUserAuthentication);
                                     }.createDelegate(this));
                return false;
            }

            if ( this.workingNodeSettings.pageType == "BASIC_MESSAGE" ) {
                if (this.workingNodeSettings.authenticationType != "NONE" ) {
                    Ext.MessageBox.alert(this.i18n._("Warning"),
                                         this.i18n._("When using 'Basic Message', 'Authentication' must be set to 'None'."),
                                         function () {
                                             this.tabs.activate(this.panelUserAuthentication);
                                         }.createDelegate(this));
                    return false;
                }

                if ( !this.workingNodeSettings.concurrentLoginsEnabled ) {
                    Ext.MessageBox.alert(this.i18n._("Warning"),
                                         this.i18n._("When using 'Basic Message', 'Allow Concurrent Logins' must be enabled."),
                                         function () {
                                             this.tabs.activate(this.panelUserAuthentication);
                                         }.createDelegate(this));
                    return false;
                }
            }

            if ( this.workingNodeSettings.pageType == "BASIC_LOGIN" ) {
                if (this.workingNodeSettings.authenticationType == "NONE" ) {
                    Ext.MessageBox.alert(this.i18n._("Warning"),
                                         this.i18n._("When using 'Basic Login', 'Authentication' cannot be set to 'None'."),
                                         function () {
                                             this.tabs.activate(this.panelUserAuthentication);
                                         }.createDelegate(this));
                    return false;
                }
            }

            return true;
        },
        // commit function
        commitSettings : function(callback)
        {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                var captureRules = null, passedClients = null, passedServers = null;
                this.workingNodeSettings.pageParameters = Ext.util.JSON.encode( this.pageParameters );

                if ( this.gridCaptureRules.rendered ) {
                    this.workingNodeSettings.captureRules = {
                        javaClass : "java.util.ArrayList",
                        list : this.gridCaptureRules.getFullSaveList()
                    };
                }
                if ( this.gridPassedClients.rendered ) {
                    this.workingNodeSettings.passedClients = {
                        javaClass : "java.util.ArrayList",
                        list : this.gridPassedClients.getFullSaveList()
                    };
                }
                if ( this.gridPassedServers.rendered ) {
                    this.workingNodeSettings.passedServers = {
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
                this.getRpcNode().setCPDSettings(wrapper, this.workingNodeSettings );
            }
        },
        isDirty : function() {
            return !Ung.Util.equals(this.workingNodeSettings, this.initialNodeSettings)
                || !Ung.Util.equals(this.pageParameters, this.initialPageParameters )
                || this.gridCaptureRules.isDirty()
                || this.gridPassedClients.isDirty()
                || this.gridPassedServers.isDirty();
        },

        configureLocalDirectory : function()
        {
            Ext.MessageBox.wait(i18n._("Loading Config..."),
                                i18n._("Please wait"));

            Ung.Util.loadResourceAndExecute.defer(1,this,["Ung.LocalDirectory",Ung.Util.getScriptSrc("script/config/localDirectory.js"), function() {

                main.localDirectoryWin=new Ung.LocalDirectory({
                    "name" : "localDirectory"
                });

                main.localDirectoryWin.show();
                Ext.MessageBox.hide();
            }.createDelegate(this)]);
        },

        /* There is no way to select the radius tab because we don't
        get a callback once the settings are loaded. */
        configureRadius : function()
        {
            var node = main.getNode("untangle-node-adconnector");
            if (node != null) {
                var nodeCmp = Ung.Node.getCmp(node.nodeId);
                if (nodeCmp != null) {
                    nodeCmp.onSettingsAction();
                }
            }
        },

        configureActiveDirectory : function()
        {
            var node = main.getNode("untangle-node-adconnector");
            if (node != null) {
                var nodeCmp = Ung.Node.getCmp(node.nodeId);
                if (nodeCmp != null) {
                    nodeCmp.onSettingsAction();
                }
            }
        }
    });

    Ung.CPD.daysOfWeek = ["mon","tue","wed","thu","fri","sat","sun"];
}
