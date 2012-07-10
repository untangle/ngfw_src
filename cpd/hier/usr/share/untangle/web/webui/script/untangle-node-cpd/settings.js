if (!Ung.hasResource["Ung.CPD"]) {
    Ung.hasResource["Ung.CPD"] = true;
    Ung.NodeWin.registerClassName("untangle-node-cpd", "Ung.CPD");

    Ext.define('Ung.CPD', {
        extend:'Ung.NodeWin',
        panelCaptiveStatus: null,
        gridCaptiveStatus: null,

        panelCaptureRules: null,
        gridCaptureRules: null,

        panelPassedHosts: null,
        panelUserAuthentication: null,
        panelCaptivePage: null,

        gridLoginEventLog: null,
        gridBlockEventLog: null,
        initComponent: function() {
            Ung.Util.clearInterfaceStore();

            // builds the tabs
            this.buildCaptiveStatus();
            this.buildCaptureRules();
            this.buildPassedHosts();
            this.buildCaptivePage();
            this.buildUserAuthentication();
            this.buildLoginEventLog();
            this.buildBlockEventLog();

            // builds the tab panel with the tabs
            this.buildTabPanel([ this.panelCaptiveStatus, this.panelCaptureRules, this.panelPassedHosts, this.panelCaptivePage,
                                 this.panelUserAuthentication, this.gridLoginEventLog, this.gridBlockEventLog ]);
            this.callParent(arguments);
        },

        buildCaptiveStatus: function() {
            this.buildGridCaptiveStatus();
            this.panelCaptiveStatus = Ext.create('Ext.panel.Panel', {
                name: 'Status',
                parentId: this.getId(),
                title: this.i18n._('Status'),
                layout: "anchor",
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Status'),
                    height: '90',
                    html: this.i18n._('Captive Portal allows administrators to require network users to complete a defined process, such as logging in or accepting a network usage policy, before accessing the internet.')
                 }]
            });

            this.panelCaptiveStatus.add( this.gridCaptiveStatus );
        },

        buildGridCaptiveStatus: function() {
            this.gridCaptiveStatus = Ext.create('Ung.EditorGrid',{
                anchor: '100% -90',
                name: "gridCaptiveStatus",
                settingsCmp: this,
                parentId: this.getId(),
                hasAdd: false,
                hasEdit: false,
                hasDelete: false,
                columnsDefaultSortable: true,
                title: this.i18n._("Active Sessions"),
                qtip: this.i18n._("The Active Sessions list shows authenticated users."),
                paginated: false,
                bbar: Ext.create('Ext.toolbar.Toolbar', {
                    items: [
                        '-',
                        {
                            xtype: 'button',
                            id: "refresh_"+this.getId(),
                            text: i18n._('Refresh'),
                            name: "Refresh",
                            tooltip: i18n._('Refresh'),
                            iconCls: 'icon-refresh',
                            handler: Ext.bind(function() {
                                this.gridCaptiveStatus.reload();
                            }, this)
                        }
                    ]
                }),
                recordJavaClass: "com.untangle.node.cpd.HostDatabaseEntry",
                dataFn: this.getRpcNode().getCaptiveStatus,
                fields: [{
                    name: "ipv4Address"
                },{
                    name: "username"
                },{
                    name: "lastSession"
                },{
                    name: "sessionStart"
                },{
                    name: "expirationDate"
                },{
                    name: "hardwareAddress"
                },{
                    name: "id"
                }],
                columns: [{
                    header: this.i18n._("IP Address"),
                    dataIndex:'ipv4Address',
                    width: 150
                },{
                    header: this.i18n._("User Name"),
                    dataIndex:'username',
                    width: 200
                },{
                    header: this.i18n._("Last Session"),
                    dataIndex:'lastSession',
                    width: 180,
                    renderer: function(value) { return i18n.timestampFormat(value); }
                },{
                    header: this.i18n._("Current Session"),
                    width: 180,
                    renderer: function(value) { return i18n.timestampFormat(value); }
                },{
                    header: this.i18n._("Expiration"),
                    dataIndex:'expirationDate',
                    width: 180,
                    renderer: function(value) { return i18n.timestampFormat(value); }
                },{
                    header: this.i18n._("Logout"),
                    xtype: 'actioncolumn',
                    width: 80,
                    items: [{
                        id: 'userLogout',
                        iconCls: 'icon-delete-row',
                        tooltip: 'Click to logout',
                        handler: Ext.bind(function(grid,row,col) {
                            var rec = grid.getStore().getAt(row);
                            this.getRpcNode().logout(rec.data.ipv4Address);
                            this.gridCaptiveStatus.reload();
                        }, this)
                    }]
                }]
            });
        },

        // Rules Panel
        buildCaptureRules: function() {
            this.buildGridCaptureRules();
            this.panelCaptureRules = Ext.create('Ext.panel.Panel',{
                name: "panelCaptureRules",
                helpSource: "captive_hosts",
                parentId: this.getId(),
                title: this.i18n._("Capture Rules"),
                layout: "anchor",
                cls: "ung-panel",
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._("Note"),
                    html: this.i18n._("The Capture Rules are a  set of rules to define which hosts and traffic are subject to the Captive Portal.  The rules are evaluated in order.")
                }, this.gridCaptureRules, {
                    xtype: "fieldset",
                    items: [{
                        xtype: "checkbox",
                        boxLabel: this.i18n._("Capture Bypassed Traffic"),
                        tooltip: this.i18n._("If enabled, traffic that is bypassed in Bypass Rules will also captured until the host is authenticated."),
                        hideLabel: true,
                        checked: this.settings.captureBypassedTraffic,
                        listeners: {
                            "change": Ext.bind(function(elem, checked) {
                                this.settings.captureBypassedTraffic = checked;
                            }, this)
                        }
                    }]
                }]
            });
        },

        buildGridCaptureRules: function() {
            this.gridCaptureRules = Ext.create('Ung.EditorGrid', {
                anchor: '100% -120',
                name: "gridCaptureRules",
                settingsCmp: this,
                hasReorder: true,
                emptyRow: {
                    "live": true,
                    "capture": true,
                    "log": false,
                    "clientInterface": "1",
                    "clientAddress": "any",
                    "serverAddress": "any",
                    "days": "mon,tue,wed,thu,fri,sat,sun",
                    "startTime": "00:00",
                    "endTime": "23:59",
                    "name": this.i18n._("[no name]"),
                    "category": this.i18n._("[no category]"),
                    "description": this.i18n._("[no description]"),
                    "javaClass": "com.untangle.node.cpd.CaptureRule"
                },
                title: this.i18n._("Rules"),
                qtip: this.i18n._("The Capture Rules are a set of rules to define which hosts and traffic are subject to the Captive Portal. All enabled rules are evaluated in order."),
                recordJavaClass: "com.untangle.node.cpd.CaptureRule",
                paginated: false,
                dataProperty: "captureRules",
                fields: [{
                    name: "id"
                },{
                    name: "live"
                },{
                    name: "capture"
                },{
                    name: "log"
                },{
                    name: "clientInterface"
                },{
                    name: "clientAddress"
                },{
                    name: "serverAddress"
                },{
                    name: "name"
                },{
                    name: "category"
                },{
                    name: "description"
                },{
                    name: "startTime"
                },{
                    name: "endTime"
                },{
                    name: "days"
                },{
                    name: "javaClass"
                }],
                columns: [
                    {
                        xtype:'checkcolumn',
                        header: this.i18n._("Enable"),
                        dataIndex: "live",
                        fixed: true,
                        width:55

                    },
                    {
                        xtype:'checkcolumn',
                        header: this.i18n._("Capture"),
                        dataIndex: "capture",
                        fixed: true,
                        width:55
                    },
                    {
                        header: this.i18n._("Description"),
                        width: 200,
                        dataIndex: "description",
                        flex:1,
                        editor: {
                            xtype:'textfield',
                            allowBlank: false
                        }
                    }
                ],
                columnsDefaultSortable: false
            });

            var rowEditor = this.buildGridCaptureRulesRowEditor();
            this.gridCaptureRules.rowEditor = rowEditor;
        },

        buildGridCaptureRulesRowEditor: function() {
            return Ext.create('Ung.RowEditorWindow',{
                grid: this.gridCaptureRules,
                title: this.i18n._("Capture Rule"),
                inputLines: [{
                    xtype: "checkbox",
                    name: "live",
                    dataIndex: "live",
                    boxLabel: this.i18n._("Enabled"),
                    hideLabel: true
                },{
                    xtype: "checkbox",
                    name: "capture",
                    dataIndex: "capture",
                    boxLabel: this.i18n._("Capture"),
                    hideLabel: true
                },{
                    xtype: 'textarea',
                    name: "description",
                    width: 400,
                    height: 60,
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    allowBlank: false
                },{
                    xtype: "fieldset",
                    title: this.i18n._("Interface"),
                    items: [{
                        cls: "description",
                        border: false,
                        html: this.i18n._("The ethernet interface (NIC).")
                    }, new Ung.Util.InterfaceCombo({
                        name: "Client",
                        dataIndex: "clientInterface",
                        fieldLabel: this.i18n._("Client"),
                        simpleMatchers: true
                    })]
                },{
                    xtype: "fieldset",
                    title: this.i18n._("Address"),
                    items: [{
                        cls: "description",
                        border: false,
                        html: this.i18n._("The IP addresses.")
                    },{
                        xtype: "textfield",
                        name: "clientAddress",
                        dataIndex: "clientAddress",
                        fieldLabel: this.i18n._("Client"),
                        allowBlank: false
                    },{
                        xtype: "textfield",
                        name: "serverAddress",
                        dataIndex: "serverAddress",
                        fieldLabel: this.i18n._("Server"),
                        allowBlank: false
                    }]
                },{
                    xtype: "fieldset",
                    title: this.i18n._("Time of Day"),
                    items: [{
                        cls: "description",
                        border: false,
                        html: this.i18n._("The time of day.")
                    },{
                        xtype: "utimefield",
                        name: "startTime",
                        dataIndex: "startTime",
                        fieldLabel: this.i18n._("Start Time"),
                        allowBlank: false
                    },{
                        xtype: "utimefield",
                        endTime: true,
                        name: "endTime",
                        dataIndex: "endTime",
                        fieldLabel: this.i18n._("End Time"),
                        allowBlank: false
                    }]
                },{
                    xtype: "fieldset",
                    title: this.i18n._("Days of Week"),
                    items: [{
                        cls: "description",
                        border: false,
                        html: this.i18n._("The days of the week.")
                    },{
                        xtype: "checkbox",
                        name: "sunday",
                        dataIndex: "sun",
                        boxLabel: this.i18n._("Sunday"),
                        hideLabel: true
                    },{
                        xtype: "checkbox",
                        name: "monday",
                        dataIndex: "mon",
                        boxLabel: this.i18n._("Monday"),
                        hideLabel: true
                    },{
                        xtype: "checkbox",
                        name: "tuesday",
                        dataIndex: "tue",
                        boxLabel: this.i18n._("Tuesday"),
                        hideLabel: true
                    }, {
                        xtype: "checkbox",
                        name: "wednesday",
                        dataIndex: "wed",
                        boxLabel: this.i18n._("Wednesday"),
                        hideLabel: true
                    }, {
                        xtype: "checkbox",
                        name: "thursday",
                        dataIndex: "thu",
                        boxLabel: this.i18n._("Thursday"),
                        hideLabel: true
                    }, {
                        xtype: "checkbox",
                        name: "friday",
                        dataIndex: "fri",
                        boxLabel: this.i18n._("Friday"),
                        hideLabel: true
                    }, {
                        xtype: "checkbox",
                        name: "saturday",
                        dataIndex: "sat",
                        boxLabel: this.i18n._("Saturday"),
                        hideLabel: true
                    }]
                }],

                populate: function(record, addMode) {
                    var days = record.get("days").split( "," );
                    for ( var c = 0 ; c < Ung.CPD.daysOfWeek.length ; c++ ) {
                        var day = Ung.CPD.daysOfWeek[c];
                        record.set( day, days.indexOf( day ) >= 0 );
                    }

                    Ung.RowEditorWindow.prototype.populateTree.call(this, record, addMode);
                },
                isFormValid: function() {
                    return Ung.RowEditorWindow.prototype.isFormValid.call(this);
                },
                updateAction: function() {
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
                }
            });
        },

        buildPassedHosts: function() {
            this.gridPassedClients =
                this.buildGridPassedList( "gridPassedClients",
                                          this.i18n._( "Pass Listed Client Addresses"),
                                          "com.untangle.node.cpd.PassedAddress",
                                          "passedClients",
                                          "Pass Listed Client Addresses is a list of Client IPs that are not subjected to the Captive Portal.");

                this.gridPassedServers =
                this.buildGridPassedList( "gridPassedServers",
                                          this.i18n._( "Pass Listed Server Addresses"),
                                          "com.untangle.node.cpd.PassedAddress",
                                          "passedServers",
                                          "Pass Listed Server Addresses is a list of Server IPs that unauthenticated clients can access without authentication.");

            this.panelPassedHosts = Ext.create('Ext.panel.Panel',{
                name: "panelPassedHosts",
                helpSource: "passed_hosts",
                // private fields
                parentId: this.getId(),
                title: this.i18n._("Passed Hosts"),
                layout: "anchor",
                autoScroll: true,
                border: false,
                cls: "ung-panel",
                items: [ this.gridPassedClients, this.gridPassedServers ]
            });
        },

        buildGridPassedList: function( name, title, javaClass, dataProperty , tooltip) {
            return Ext.create('Ung.EditorGrid', {
                name: name,
                tooltip: tooltip,
                settingsCmp: this,
                hasEdit: false,
                anchor: "100% 49%",
                emptyRow: {
                    "live": true,
                    "log": false,
                    "address": "any",
                    "name": this.i18n._("[no name]"),
                    "category": this.i18n._("[no category]"),
                    "description": this.i18n._("[no description]"),
                    "javaClass": javaClass
                },
                title: this.i18n._(title),
                recordJavaClass: javaClass,
                paginated: false,
                dataProperty: dataProperty,
                fields: [{
                    name: "id"
                },{
                    name: "live"
                },{
                    name: "log"
                },{
                    name: "address"
                },{
                    name: "name"
                },{
                    name: "category"
                }, {
                    name: "description"
                }, {
                    name: "javaClass"
                }],
                columns: [
                    {
                        xtype:'checkcolumn',
                        header: this.i18n._("Enable"),
                        dataIndex: "live",
                        fixed: true,
                        width:55
                    },
                    {
                        header: this.i18n._("Description"),
                        width: 200,
                        dataIndex: "description",
                        editor: {
                            xtype:'textfield',
                            allowBlank:false
                        }
                    },
                    {
                        header: this.i18n._("Address"),
                        width: 200,
                        dataIndex: "address",
                        flex:1,
                        editor:{
                            xtype:'textfield',
                            allowBlank: false
                        }
                    }
                ],
                columnsDefaultSortable: false
            });
        },

        buildUserAuthentication: function() {
            var onUpdateRadioButton = Ext.bind(function( elem, checked )
            {
                if ( checked ) {
                    this.settings.authenticationType = elem.inputValue;
                }
            }, this);

            var onRenderRadioButton = Ext.bind(function( elem )
            {
                elem.setValue(this.settings.authenticationType);
                elem.clearDirty();
            }, this);

            this.panelUserAuthentication = Ext.create('Ext.panel.Panel',{
                name: "panelUserAuthentication",
                helpSource: "user_authentication",
                // private fields
                parentId: this.getId(),
                title: this.i18n._("User Authentication"),
                autoScroll: true,
                border: false,
                cls: "ung-panel",
                items: [{
                    xtype: "fieldset",
                    title: this.i18n._( "User Authentication" ),
                    items: [{
                        xtype: "radio",
                        boxLabel: this.i18n._("None"),
                        hideLabel: true,
                        name: "authenticationType",
                        inputValue: "NONE",
                        listeners: {
                            "change": onUpdateRadioButton,
                            "render": onRenderRadioButton
                        }
                    },{
                        xtype: "radio",
                        boxLabel: this.i18n._("Local Directory"),
                        hideLabel: true,
                        name: "authenticationType",
                        inputValue: "LOCAL_DIRECTORY",
                        listeners: {
                            "change": onUpdateRadioButton,
                            "render": onRenderRadioButton
                        }
                    },{
                        xtype: "button",
                        name: "configureLocalDirectory",
                        text: i18n._("Configure Local Directory"),
                        handler: Ext.bind(this.configureLocalDirectory, this )
                    },{
                        xtype: "radio",
                        boxLabel: Ext.String.format( this.i18n._("RADIUS {0}(requires Directory Connector) {1}"),
                                                  "<i>", "</i>" ),
                        hideLabel: true,
                        disabled: !main.getLicenseManager().getLicense("untangle-node-adconnector"),
                        name: "authenticationType",
                        inputValue: "RADIUS",
                        listeners: {
                            "change": onUpdateRadioButton,
                            "render": onRenderRadioButton
                        }
                    },{
                        xtype: "button",
                        disabled: !main.getLicenseManager().getLicense("untangle-node-adconnector"),
                        name: "configureRadiusServer",
                        text: i18n._("Configure RADIUS"),
                        handler: Ext.bind(this.configureRadius, this )
                    },{
                        xtype: "radio",
                        boxLabel: Ext.String.format( this.i18n._("Active Directory {0}(requires Directory Connector) {1}"),
                                                  "<i>", "</i>" ),
                        hideLabel: true,
                        disabled: !main.getLicenseManager().getLicense("untangle-node-adconnector"),
                        name: "authenticationType",
                        inputValue: "ACTIVE_DIRECTORY",
                        listeners: {
                            "change": onUpdateRadioButton,
                            "render": onRenderRadioButton
                        }
                    },{
                        xtype: "button",
                        disabled: !main.getLicenseManager().getLicense("untangle-node-adconnector"),
                        name: "configureActiveDirectory",
                        text: i18n._("Configure Active Directory"),
                        handler: Ext.bind(this.configureActiveDirectory, this )
                    }]
                },{
                    xtype: "fieldset",
                    title: this.i18n._( "Session Settings" ),
                    items: [{
                        xtype: "numberfield",
                        allowNegative: false,
                        allowBlank: false,
                        name: "idleTimeout",
                        maxValue: 24 * 60,
                        minValue: 0,
                        hideTrigger:true,
                        invalidText: this.i18n._( "The Idle Timeout must be between 0 minutes and 24 hours." ),
                        fieldLabel: this.i18n._( "Idle Timeout" ),
                        boxLabel: this.i18n._( "minutes" ),
                        tooltip: this.i18n._( "Clients will be unauthenticated after this amount of idle time. They may re-authenticate immediately." ),
                        value: this.settings.idleTimeout / 60,
                        listeners: {
                            "change": Ext.bind(function( elem, newValue ) {
                                this.settings.idleTimeout = newValue * 60;
                            }, this)
                        }
                    },{
                        xtype: "numberfield",
                        allowNegative: false,
                        allowBlank: false,
                        name: "timeout",
                        maxValue: 24 * 60,
                        minValue: 5,
                        hideTrigger:true,
                        fieldLabel: this.i18n._( "Timeout" ),
                        boxLabel: this.i18n._( "minutes" ),
                        invalidText: this.i18n._( "The Timeout must be between 5 minutes and 24 hours." ),
                        tooltip: this.i18n._( "Clients will be unauthenticated after this amount of time regardless of activity. They may re-authenticate immediately." ),
                        value: this.settings.timeout / 60,
                        listeners: {
                            "change": Ext.bind(function( elem, newValue ) {
                                this.settings.timeout = newValue * 60;
                            }, this)
                        }
                    },{
                        xtype: "checkbox",
                        boxLabel: this.i18n._("Allow Concurrent Logins"),
                        tooltip: this.i18n._("This will allow multiple hosts to use the same username & password concurrently."),
                        hideLabel: true,
                        checked: this.settings.concurrentLoginsEnabled,
                        listeners: {
                            "change": Ext.bind(function(elem, checked) {
                                this.settings.concurrentLoginsEnabled = checked;
                            }, this)
                        }
                    }]
                }]
            });
        },
        captivePageHideComponents: function( currentValue ) {
            var values = [ "BASIC_LOGIN", "BASIC_MESSAGE", "CUSTOM" ];
            for ( var c = 0 ; c < values.length ; c++ ) {
                var item = values[c];
                Ext.each( this.panelCaptivePage.query('[pageType=' + item+']'), Ext.bind(function( component ) {
                    if ( component.isVisible ) {
                        component.setVisible( currentValue == item );
                    } else {
                        component.setVisible( currentValue == item );
                    }
                }, this));
            }
        },
        buildCaptivePage: function() {
            var onUpdateRadioButton = Ext.bind(function( elem, checked )
            {
                if ( checked ) {
                    this.settings.pageType = elem.inputValue;
                    this.captivePageHideComponents( elem.inputValue );
                }
            }, this);

            var onRenderRadioButton = Ext.bind(function( elem )
            {
                this.panelCaptivePage.query('radio[name="pageType"]')[0].setValue(this.settings.pageType);
            }, this);

            this.panelCaptivePage = Ext.create('Ext.panel.Panel',{
                name: "panelCaptivePage",
                helpSource: "captive_page",
                // private fields
                parentId: this.getId(),
                title: this.i18n._("Captive Page"),
                autoScroll: true,
                border: false,
                cls: "ung-panel",
                listeners: {
                    "render": Ext.bind(function () {
                        this.panelCaptivePage.query('radio[name="pageType"]')[0].setValue(this.settings.pageType);
                        this.captivePageHideComponents(this.settings.pageType );
                        Ung.Util.clearDirty(this.panelCaptivePage);
                    }, this)
                },
                items: [{
                    xtype: "fieldset",
                    title: this.i18n._( "Captive Portal Page" ),
                    items: [{
                        xtype: "radio",
                        boxLabel: this.i18n._("Basic Message"),
                        hideLabel: true,
                        name: "pageType",
                        inputValue: "BASIC_MESSAGE",
                        listeners: {
                            "change": onUpdateRadioButton
                        }
                    },{
                        xtype: "radio",
                        boxLabel: this.i18n._("Basic Login"),
                        hideLabel: true,
                        name: "pageType",
                        inputValue: "BASIC_LOGIN",
                        listeners: {
                            "change": onUpdateRadioButton
                        }
                    },{
                        xtype: "radio",
                        boxLabel: this.i18n._("Custom"),
                        hideLabel: true,
                        name: "pageType",
                        inputValue: "CUSTOM",
                        listeners: {
                            "change": onUpdateRadioButton
                        }
                    },{
                        xtype: "fieldset",
                        autoScroll: false,
                        title: this.i18n._( "Captive Portal Page Configuration" ),
                        items: [{
                            xtype: "textfield",
                            allowBlank: false,
                            name: "basicLoginPageTitle",
                            fieldLabel: this.i18n._("Page Title"),
                            pageType: "BASIC_LOGIN",
                            value: this.settings.basicLoginPageTitle,
                            listeners: {
                                "change": Ext.bind(function( elem, newValue ) {
                                    this.settings.basicLoginPageTitle = newValue;
                                }, this)
                            }
                        },{
                            xtype: "textfield",
                            allowBlank: false,
                            name: "basicLoginPageWelcome",
                            fieldLabel: this.i18n._("Welcome Text"),
                            width: 400,
                            pageType: "BASIC_LOGIN",
                            value: this.settings.basicLoginPageWelcome,
                            listeners: {
                                "change": Ext.bind(function( elem, newValue ) {
                                    this.settings.basicLoginPageWelcome = newValue;
                                }, this)
                            }
                        },{
                            xtype: "textfield",
                            allowBlank: false,
                            name: "basicLoginUsername",
                            fieldLabel: this.i18n._("Username Text"),
                            pageType: "BASIC_LOGIN",
                            value: this.settings.basicLoginUsername,
                            listeners: {
                                "change": Ext.bind(function( elem, newValue ) {
                                    this.settings.basicLoginUsername = newValue;
                                }, this)
                            }
                        },{
                            xtype: "textfield",
                            allowBlank: false,
                            name: "basicLoginPassword",
                            fieldLabel: this.i18n._("Password Text"),
                            pageType: "BASIC_LOGIN",
                            value: this.settings.basicLoginPassword,
                            listeners: {
                                "change": Ext.bind(function( elem, newValue ) {
                                    this.settings.basicLoginPassword = newValue;
                                }, this)
                            }
                        },{
                            xtype: "textarea",
                            allowBlank: true,
                            name: "basicLoginMessageText",
                            width: 400,
                            height: 250,
                            fieldLabel: this.i18n._("Message Text"),
                            pageType: "BASIC_LOGIN",
                            value: this.settings.basicLoginMessageText,
                            listeners: {
                                "change": Ext.bind(function( elem, newValue ) {
                                    this.settings.basicLoginMessageText = newValue;
                                }, this)
                            }
                        },{
                            xtype: "textfield",
                            allowBlank: false,
                            name: "basicLoginFooter",
                            fieldLabel: this.i18n._("Lower Text"),
                            width: 400,
                            pageType: "BASIC_LOGIN",
                            value: this.settings.basicLoginFooter,
                            listeners: {
                                "change": Ext.bind(function( elem, newValue ) {
                                    this.settings.basicLoginFooter = newValue;
                                }, this)
                            }
                        },{
                            xtype: "textfield",
                            allowBlank: false,
                            name: "basicMessagePageTitle",
                            fieldLabel: this.i18n._("Page Title"),
                            pageType: "BASIC_MESSAGE",
                            width: 400,
                            value: this.settings.basicMessagePageTitle,
                            listeners: {
                                "change": Ext.bind(function( elem, newValue ) {
                                    this.settings.basicMessagePageTitle = newValue;
                                }, this)
                            }
                        },{
                            xtype: "textfield",
                            allowBlank: false,
                            name: "basicMessagePageWelcome",
                            fieldLabel: this.i18n._("Welcome Text"),
                            width: 400,
                            pageType: "BASIC_MESSAGE",
                            value: this.settings.basicMessagePageWelcome,
                            listeners: {
                                "change": Ext.bind(function( elem, newValue ) {
                                    this.settings.basicMessagePageWelcome = newValue;
                                }, this)
                            }
                        },{
                            xtype: "textarea",
                            allowBlank: false,
                            name: "basicMessageMessageText",
                            width: 400,
                            height: 250,
                            fieldLabel: this.i18n._("Message Text"),
                            pageType: "BASIC_MESSAGE",
                            value: this.settings.basicMessageMessageText,
                            listeners: {
                                "change": Ext.bind(function( elem, newValue ) {
                                    this.settings.basicMessageMessageText = newValue;
                                }, this)
                            }
                        },{
                            xtype: "checkbox",
                            allowBlank: false,
                            name: "basicMessageAgreeBox",
                            fieldLabel: this.i18n._("Agree Checkbox"),
                            pageType: "BASIC_MESSAGE",
                            checked: this.settings.basicMessageAgreeBox,
                            listeners: {
                                "change": Ext.bind(function(elem, checked) {
                                    this.settings.basicMessageAgreeBox = checked;
                                }, this)
                            }
                        },{
                            xtype: "textfield",
                            allowBlank: false,
                            name: "basicMessageAgreeText",
                            fieldLabel: this.i18n._("Agree Text"),
                            width: 400,
                            pageType: "BASIC_MESSAGE",
                            value: this.settings.basicMessageAgreeText,
                            listeners: {
                                "change": Ext.bind(function( elem, newValue ) {
                                    this.settings.basicMessageAgreeText = newValue;
                                }, this)
                            }
                        },{
                            xtype: "textfield",
                            allowBlank: false,
                            name: "basicMessageFooter",
                            fieldLabel: this.i18n._("Lower Text"),
                            width: 400,
                            pageType: "BASIC_MESSAGE",
                            value: this.settings.basicMessageFooter,
                            listeners: {
                                "change": Ext.bind(function( elem, newValue ) {
                                    this.settings.basicMessageFooter = newValue;
                                }, this)
                            }
                        },{
                            fileUpload: true,
                            xtype: "form",
                            bodyStyle: "padding:0px 0px 0px 25px",
                            buttonAlign: "left",
                            id: "upload_custom_php",
                            url: "upload",
                            pageType: "CUSTOM",
                            border: false,
                            items: [{
                                fieldLabel: this.i18n._("File"),
                                name: "customUploadFile",
                                inputType: "file",
                                xtype: "textfield",
                                width: 500,
                                size: 50
                            },{
                                xtype: "button",
                                name: "customSendFile",
                                text: i18n._("Upload File"),
                                handler: Ext.bind(this.onUploadCustomFile, this)
                            },{
                                xtype: "hidden",
                                name: "type",
                                value: "cpd-custom-page"
                            }]
                        }]
                    },{
                        xtype: "button",
                        name: "viewPage",
                        text: i18n._("View Page"),
                        handler: Ext.bind(function()
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
                        }, this)
                    }]
                },{
                    xtype: "fieldset",
                    title: this.i18n._( "Session Redirect" ),
                    items: [{
                        xtype: "textfield",
                        name: "redirectUrl",
                        width: 200,
                        fieldLabel: this.i18n._("Redirect URL"),
                        tooltip: this.i18n._("Users will be redirected to this page immediately after authentication. Blank sends the user to their original destination."),
                        value: this.settings.redirectUrl,
                        listeners: {
                            "change": Ext.bind(function( elem, newValue ) {
                                this.settings.redirectUrl = newValue;
                            }, this)
                        }
                    },{
                        xtype: "checkbox",
                        boxLabel: this.i18n._("Redirect HTTP traffic to HTTPS captive page"),
                        tooltip: this.i18n._("If unchecked, HTTP traffic to unauthenticated hosts will be redirect to the HTTP Captive page. If checked, users will be redirected to an HTTPS captive page."),
                        hideLabel: true,
                        checked: this.settings.useHttpsPage,
                        listeners: {
                            "change": Ext.bind(function(elem, checked) {
                                this.settings.useHttpsPage = checked;
                            }, this)
                        }
                    },{
                        xtype: "checkbox",
                        boxLabel: this.i18n._("Redirect HTTPS traffic to HTTPS captive page"),
                        tooltip: this.i18n._("If unchecked, HTTPS traffic for unauthenticated users is blocked. If checked HTTPS traffic will be redirected to the HTTPS captive page. Warning: This will cause certificate warning errors in the browser."),
                        hideLabel: true,
                        checked: this.settings.redirectHttpsEnabled,
                        listeners: {
                            "change": Ext.bind(function(elem, checked) {
                                this.settings.redirectHttpsEnabled = checked;
                            }, this)
                        }
                    }]
                }]
            });
        },

        onUploadCustomFile: function() {
            var form = this.panelCaptivePage.query('xform[id="upload_custom_php"]')[0].getForm();
            form.submit({
                parentID: this.panelCaptivePage.getId(),
                waitMsg: this.i18n._("Please wait while uploading your custom captive portal page..."),
                success: Ext.bind(this.uploadCustomFileSuccess, this ),
                failure: Ext.bind(this.uploadCustomFileFailure, this )
            });
        },

        uploadCustomFileSuccess: function() {
            Ext.MessageBox.alert( this.i18n._("Succeeded"), this.i18n._("Uploading Custom Captive Portal Page succeeded"));
            var field = this.panelCaptivePage.query('textfield[name="customUploadFile"]')[0];
            field.reset();
        },

        uploadCustomFileFailure: function() {
            Ext.MessageBox.alert(this.i18n._("Failed"),
                                 this.i18n._("There was an error uploading the Custom Captive Portal Page." ));
        },

        buildLoginEventLog: function() {
            this.gridLoginEventLog = Ext.create('Ung.GridEventLog',{
                title: this.i18n._( "Login Event Log" ),
                helpSource: "login_event_log",
                eventQueriesFn: this.getRpcNode().getLoginEventQueries,
                settingsCmp: this,
                fields: [{
                    name: "time_stamp",
                    sortType: Ung.SortTypes.asTimestamp
                },{
                    name: "client_addr"
                },{
                    name: "login_name"
                },{
                    name: "auth_type"
                },{
                    name: "event"
                }],

                columns: [{
                    header: this.i18n._("Timestamp"),
                    width: Ung.Util.timestampFieldWidth,
                    sortable: true,
                    dataIndex: "time_stamp",
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                },{
                    header: this.i18n._("Client"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: "client_addr"
                },{
                    header: this.i18n._("Username"),
                    width: Ung.Util.usernameFieldWidth,
                    sortable: true,
                    dataIndex: "login_name",
                    flex:1
                },{
                    header: this.i18n._("Action"),
                    width: 165,
                    sortable: true,
                    dataIndex: "event",
                    renderer: Ext.bind(function( value ) {
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
                    }, this )
                },{
                    header: this.i18n._("Authentication"),
                    width: 165,
                    sortable: true,
                    dataIndex: "auth_type",
                    renderer: Ext.bind(function( value ) {
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
                    }, this )
                }]
            });
        },

        buildBlockEventLog: function() {
            this.gridBlockEventLog = Ext.create('Ung.GridEventLog',{
                title: this.i18n._( "Block Event Log" ),
                helpSource: "block_event_log",
                eventQueriesFn: this.getRpcNode().getBlockEventQueries,
                settingsCmp: this,
                fields: [{
                    name: "time_stamp",
                    sortType: Ung.SortTypes.asTimestamp
                },{
                    name: "client_address"
                },{
                    name: "client_port"
                },{
                    name: "server_address"
                },{
                    name: "server_port"
                },{
                    name: "client",
                    convert: function(value, record) {
                        return record.client_address + ":" + record.client_port;
                    }
                }, {
                    name: "server",
                    convert: function(value, record) {
                        return record.server_address + ":" + record.server_port;
                    }
                }],

                columns: [{
                    header: this.i18n._("Timestamp"),
                    width: Ung.Util.timestampFieldWidth,
                    sortable: true,
                    dataIndex: "time_stamp",
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                },{
                    header: this.i18n._("Action"),
                    width: 80,
                    sortable: false,
                    renderer: Ext.bind(function(value) {
                        return this.i18n._( "block" );
                    }, this )
                },{
                    header: this.i18n._("Client"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: "client_address"
                },{
                    header: this.i18n._("Reason"),
                    width: 100,
                    sortable: false,
                    flex:1,
                    renderer: Ext.bind(function(value) {
                        return this.i18n._( "unauthenticated" );
                    }, this )
                },{
                    header: this.i18n._("Server"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: "server_address"
                }]
            });
        },
        beforeSave: function(isApply, handler) {
            if ( this.gridCaptureRules.isDirty() ) {
                this.settings.captureRules.list = this.gridCaptureRules.getPageList();
            }
            if ( this.gridPassedClients.isDirty() ) {
                this.settings.passedClients.list = this.gridPassedClients.getPageList();
            }
            if ( this.gridPassedServers.isDirty() ) {
                this.settings.passedServers.list = this.gridPassedServers.getPageList();
            }
            handler.call(this, isApply);
        },
        validate: function() {
            // Iterate all of the fields checking if they are valid
            if ( !this.query('numberfield[name="idleTimeout"]')[0].isValid() ||
                 !this.query('numberfield[name="timeout"]')[0].isValid()) {
                Ext.MessageBox.alert(this.i18n._("Warning"),
                                     this.i18n._("Please correct any highlighted fields."),
                                     Ext.bind(function () {
                                         this.tabs.setActiveTab(this.panelUserAuthentication);
                                     }, this));
                return false;
            }

            if ( this.settings.pageType == "BASIC_MESSAGE" ) {
                if (this.settings.authenticationType != "NONE" ) {
                    Ext.MessageBox.alert(this.i18n._("Warning"),
                                         this.i18n._("When using 'Basic Message', 'Authentication' must be set to 'None'."),
                                         Ext.bind(function () {
                                             this.tabs.setActiveTab(this.panelUserAuthentication);
                                         }, this));
                    return false;
                }

                if ( !this.settings.concurrentLoginsEnabled ) {
                    Ext.MessageBox.alert(this.i18n._("Warning"),
                                         this.i18n._("When using 'Basic Message', 'Allow Concurrent Logins' must be enabled."),
                                         Ext.bind(function () {
                                             this.tabs.setActiveTab(this.panelUserAuthentication);
                                         }, this));
                    return false;
                }
            }

            if ( this.settings.pageType == "BASIC_LOGIN" ) {
                if (this.settings.authenticationType == "NONE" ) {
                    Ext.MessageBox.alert(this.i18n._("Warning"),
                                         this.i18n._("When using 'Basic Login', 'Authentication' cannot be set to 'None'."),
                                         Ext.bind(function () {
                                             this.tabs.setActiveTab(this.panelUserAuthentication);
                                         }, this));
                    return false;
                }
            }

            return true;
        },
        configureLocalDirectory: function() {
            Ext.MessageBox.wait(i18n._("Loading Config..."),
                                i18n._("Please wait"));

            Ext.defer(Ung.Util.loadResourceAndExecute,1, this,["Ung.LocalDirectory",Ung.Util.getScriptSrc("script/config/localDirectory.js"), Ext.bind(function() {

                main.localDirectoryWin=new Ung.LocalDirectory({
                    "name": "localDirectory"
                });

                main.localDirectoryWin.show();
                Ext.MessageBox.hide();
            }, this)]);
        },

        /* There is no way to select the radius tab because we don't
        get a callback once the settings are loaded. */
        configureRadius: function() {
            var node = main.getNode("untangle-node-adconnector");
            if (node != null) {
                var nodeCmp = Ung.Node.getCmp(node.nodeId);
                if (nodeCmp != null) {
                    nodeCmp.onSettingsAction();
                }
            }
        },
        configureActiveDirectory: function() {
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
//@ sourceURL=cpd-settings.js
