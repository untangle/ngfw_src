if (!Ung.hasResource["Ung.Capture"]) {
    Ung.hasResource["Ung.Capture"] = true;
    Ung.NodeWin.registerClassName("untangle-node-capture", "Ung.Capture");

    Ung.CaptureUtil={
        getMatchers: function (settingsCmp) {
            return [
                {name:"DST_ADDR",displayName: settingsCmp.i18n._("Destination Address"), type: "text", visible: true, vtype:"ipAddress"},
                {name:"DST_PORT",displayName: settingsCmp.i18n._("Destination Port"), type: "text",vtype:"port", visible: true},
                {name:"DST_INTF",displayName: settingsCmp.i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
                {name:"SRC_ADDR",displayName: settingsCmp.i18n._("Source Address"), type: "text", visible: true, vtype:"ipAddress"},
                {name:"SRC_PORT",displayName: settingsCmp.i18n._("Source Port"), type: "text",vtype:"port", visible: false},
                {name:"SRC_INTF",displayName: settingsCmp.i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
                {name:"PROTOCOL",displayName: settingsCmp.i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["any","any"]], visible: true},
                {name:"USERNAME",displayName: settingsCmp.i18n._("Username"), type: "editor", editor: Ext.create('Ung.UserEditorWindow',{}), visible: true},
                {name:"CLIENT_HOSTNAME",displayName: settingsCmp.i18n._("Client Hostname"), type: "text", visible: true},
                {name:"SERVER_HOSTNAME",displayName: settingsCmp.i18n._("Server Hostname"), type: "text", visible: false},
                {name:"CLIENT_IN_PENALTY_BOX",displayName: settingsCmp.i18n._("Client in Penalty Box"), type: "boolean", visible: true},
                {name:"SERVER_IN_PENALTY_BOX",displayName: settingsCmp.i18n._("Server in Penalty Box"), type: "boolean", visible: true},
                {name:"CLIENT_HAS_NO_QUOTA",displayName: settingsCmp.i18n._("Client has no Quota"), type: "boolean", visible: false},
                {name:"SERVER_HAS_NO_QUOTA",displayName: settingsCmp.i18n._("Server has no Quota"), type: "boolean", visible: false},
                {name:"CLIENT_QUOTA_EXCEEDED",displayName: settingsCmp.i18n._("Client has exceeded Quota"), type: "boolean", visible: true},
                {name:"SERVER_QUOTA_EXCEEDED",displayName: settingsCmp.i18n._("Server has exceeded Quota"), type: "boolean", visible: true},
                {name:"DIRECTORY_CONNECTOR_GROUP",displayName: settingsCmp.i18n._("Directory Connector: User in Group"), type: "editor", editor: Ext.create('Ung.GroupEditorWindow',{}), visible: true},
                {name:"HTTP_USER_AGENT",displayName: settingsCmp.i18n._("HTTP: Client User Agent"), type: "text", visible: true},
                {name:"HTTP_USER_AGENT_OS",displayName: settingsCmp.i18n._("HTTP: Client User OS"), type: "text", visible: true}
            ];
        }
    };

    Ext.define('Ung.Capture', {
        extend:'Ung.NodeWin',
        panelCaptiveStatus: null,
        gridCaptiveStatus: null,

        panelCaptureRules: null,
        gridCaptureRules: null,

        panelPassedHosts: null,
        panelUserAuthentication: null,
        panelCaptivePage: null,

        gridUserEventLog: null,
        gridRuleEventLog: null,
        initComponent: function() {
            Ung.Util.clearInterfaceStore();

            // builds the tabs
            this.buildCaptiveStatus();
            this.buildCaptureRules();
            this.buildPassedHosts();
            this.buildCaptivePage();
            this.buildUserAuthentication();
            this.buildUserEventLog();
            this.buildRuleEventLog();

            // builds the tab panel with the tabs
            this.buildTabPanel([ this.panelCaptiveStatus, this.panelCaptureRules, this.panelPassedHosts, this.panelCaptivePage,
                                 this.panelUserAuthentication, this.gridUserEventLog, this.gridRuleEventLog ]);
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
                    height: 50,
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
                recordJavaClass: "com.untangle.node.capture.HostDatabaseEntry",
                dataFn: this.getRpcNode().getActiveUsers,
                fields: [{
                    name: "userAddress"
                },{
                    name: "userName"
                },{
                    name: "sessionCreation"
                },{
                    name: "sessionActivity"
                },{
                    name: "sessionCounter"
                },{
                    name: "id"
                }],
                columns: [{
                    header: this.i18n._("IP Address"),
                    dataIndex:'userAddress',
                    width: 150
                },{
                    header: this.i18n._("User Name"),
                    dataIndex:'userName',
                    width: 200
                },{
                    header: this.i18n._("Login Time"),
                    dataIndex:'sessionCreation',
                    width: 180,
                    renderer: function(value) { return i18n.timestampFormat(value); }
                },{
                    header: this.i18n._("Last Activity"),
                    dataIndex:'sessionActivity',
                    width: 180,
                    renderer: function(value) { return i18n.timestampFormat(value); }
                },{
                    header: this.i18n._("Session Count"),
                    dataIndex:'sessionCounter',
                    width: 120
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
                            this.getRpcNode().userAdminLogout(rec.data.userAddress);
                            this.gridCaptiveStatus.reload();
                        }, this)
                    }]
                }]
            });
        },

        buildCaptureRules: function() {
            this.panelCaptureRules = Ext.create('Ext.panel.Panel',{
                name: 'panelCaptureRules',
                helpSource: 'capture_rules',
                parentId: this.getId(),
                title: this.i18n._('Capture Rules'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Capture Rules'),
                    height: 50,
                    html: this.i18n._("Network access is controlled based on the set of rules defined below. To learn more click on the <b>Help</b> button below.")
                },  this.gridCaptureRules= Ext.create('Ung.EditorGrid',{
                    anchor: '100% -80',
                    name: 'Rules',
                    settingsCmp: this,
                    paginated: false,
                    hasReorder: true,
                    addAtTop: false,
                    emptyRow: {
                        "ruleId": 0,
                        "enabled": true,
                        "capture": false,
                        "description": this.i18n._("[no description]"),
                        "javaClass": "com.untangle.node.capture.CaptureRule"
                    },
                    title: this.i18n._("Rules"),
                    recordJavaClass: "com.untangle.node.capture.CaptureRule",
                    dataProperty:'captureRules',
                    fields: [{
                        name: 'ruleId'
                    }, {
                        name: 'enabled'
                    }, {
                        name: 'capture'
                    }, {
                        name: 'matchers'
                    },{
                        name: 'description'
                    }, {
                        name: 'javaClass'
                    }],
                    columns: [{
                                header: this.i18n._("Rule Id"),
                                width: 50,
                                dataIndex: 'ruleId',
                                renderer: function(value) {
                                    if (value < 0) {
                                        return i18n._("new");
                                    } else {
                                        return value;
                                    }
                                }
                            },{
                                xtype:'checkcolumn',
                                header: this.i18n._("Enable"),
                                dataIndex: 'enabled',
                                fixed: true,
                                width:55
                            },{
                                header: this.i18n._("Description"),
                                width: 200,
                                dataIndex: 'description',
                                flex:1
                            },{
                                xtype:'checkcolumn',
                                header: this.i18n._("Capture"),
                                dataIndex: 'capture',
                                fixed: true,
                                width:55
                            }],
                    columnsDefaultSortable: false,

                    initComponent: function() {
                        this.rowEditor = Ext.create('Ung.RowEditorWindow',{
                            grid: this,
                            sizeToComponent: this.settingsCmp,
                            inputLines: this.rowEditorInputLines,
                            rowEditorLabelWidth: 100,
                            populate: function(record, addMode) {
                                return this.populateTree(record, addMode);
                            },
                            // updateAction is called to update the record after the edit
                            updateAction: function() {
                                return this.updateActionTree();
                            },
                            isDirty: function() {
                                if (this.record !== null) {
                                    if (this.inputLines) {
                                        for (var i = 0; i < this.inputLines.length; i++) {
                                            var inputLine = this.inputLines[i];
                                            if(inputLine.dataIndex!=null) {
                                                if (this.record.get(inputLine.dataIndex) != inputLine.getValue()) {
                                                    return true;
                                                }
                                            }
                                            /* for fieldsets */
                                            if(inputLine.items !=null && inputLine.items.dataIndex != null) {
                                                if (this.record.get(inputLine.items.dataIndex) != inputLine.items.getValue()) {
                                                    return true;
                                                }
                                            }
                                        }
                                    }
                                }
                                return Ext.getCmp('builder').isDirty();
                            },
                            isFormValid: function() {
                                for (var i = 0; i < this.inputLines.length; i++) {
                                    var item = null;
                                    if ( this.inputLines.get != null ) {
                                        item = this.inputLines.get(i);
                                    } else {
                                        item = this.inputLines[i];
                                    }
                                    if ( item == null ) {
                                        continue;
                                    }

                                    if ( item.isValid != null) {
                                        if(!item.isValid()) {
                                            return false;
                                        }
                                    } else if(item.items !=null && item.items.getCount()>0) {
                                        /* for fieldsets */
                                        for (var j = 0; j < item.items.getCount(); j++) {
                                            var subitem=item.items.get(j);
                                            if ( subitem == null ) {
                                                continue;
                                            }

                                            if ( subitem.isValid != null && !subitem.isValid()) {
                                                return false;
                                            }
                                        }
                                    }

                                }
                                return true;
                            }
                        });
                        Ung.EditorGrid.prototype.initComponent.call(this);
                    },

                    rowEditorInputLines: [
                        {
                            xtype:'checkbox',
                            name: "Enable Rule",
                            dataIndex: "enabled",
                            fieldLabel: this.i18n._("Enable Rule")
                        },{
                            xtype:'textfield',
                            name: "Description",
                            dataIndex: "description",
                            fieldLabel: this.i18n._("Description"),
                            width: 500
                        },{
                            xtype:'fieldset',
                            title: this.i18n._("Rule") ,
                            title: "If all of the following conditions are met:",
                            items:[{
                                xtype:'rulebuilder',
                                settingsCmp: this,
                                javaClass: "com.untangle.node.capture.CaptureRuleMatcher",
                                anchor:"98%",
                                width: 900,
                                dataIndex: "matchers",
                                matchers: Ung.CaptureUtil.getMatchers(this),
                                id:'builder'
                            }]
                        },{
                            xtype: 'fieldset',
                            cls:'description',
                            title: i18n._('Perform the following action(s):'),
                            border: false
                        },{
                            xtype: "combo",
                            name: "actionType",
                            allowBlank: false,
                            dataIndex: "capture",
                            fieldLabel: this.i18n._("Action Type"),
                            editable: false,
                            store: [[true,i18n._('Capture')], [false,i18n._('Pass')]],
                            valueField: "value",
                            displayField: "displayName",
                            queryMode: 'local',
                            triggerAction: 'all',
                            listClass: 'x-combo-list-small'
                        }]
                })]
            });
        },

        buildPassedHosts: function() {
            this.gridPassedClients =
                this.buildGridPassedList( "gridPassedClients",
                                          this.i18n._( "Pass Listed Client Addresses"),
                                          "passedClients",
                                          "Pass Listed Client Addresses is a list of Client IPs that are not subjected to the Captive Portal.");

                this.gridPassedServers =
                this.buildGridPassedList( "gridPassedServers",
                                          this.i18n._( "Pass Listed Server Addresses"),
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
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Passed Hosts'),
                    height: 50,
                    html: this.i18n._("The pass lists provide a quick alternative way to allow access from specific clients, or to specific servers.")
                }, this.gridPassedClients, this.gridPassedServers ]
            });
        },

        buildGridPassedList: function( name, title, dataProperty , tooltip) {
            return Ext.create('Ung.EditorGrid', {
                name: name,
                tooltip: tooltip,
                settingsCmp: this,
                hasEdit: false,
                anchor: "100% 45%",
                emptyRow: {
                    "live": true,
                    "log": false,
                    "address": "0.0.0.0",
                    "description": this.i18n._("[no description]"),
                    "javaClass": "com.untangle.node.capture.PassedAddress"
                },
                title: this.i18n._(title),
                recordJavaClass: "com.untangle.node.capture.PassedAddress",
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
                }, {
                    name: "description"
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
                        header: this.i18n._("Log"),
                        dataIndex: "log",
                        fixed: true,
                        width:55
                    },
                    {
                        header: this.i18n._("Address"),
                        width: 200,
                        dataIndex: "address",
                        editor:{
                            xtype:'textfield',
                            allowBlank: false
                        }
                    },
                    {
                        header: this.i18n._("Description"),
                        width: 400,
                        flex:1,
                        dataIndex: "description",
                        editor: {
                            xtype:'textfield',
                            allowBlank:false
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
                            "afterrender": onRenderRadioButton
                        }
                    },{
                        xtype: "radio",
                        boxLabel: this.i18n._("Local Directory"),
                        hideLabel: true,
                        name: "authenticationType",
                        inputValue: "LOCAL_DIRECTORY",
                        listeners: {
                            "change": onUpdateRadioButton,
                            "afterrender": onRenderRadioButton
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
                            "afterrender": onRenderRadioButton
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
                            "afterrender": onRenderRadioButton
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
                        fieldLabel: this.i18n._( "Idle Timeout (minutes)" ),
// TODO                        toolTip: this.i18n._( "minutes" ),
                        boxLabel: this.i18n._( "Clients will be unauthenticated after this amount of idle time. They may re-authenticate immediately.  Use zero to disable." ),
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
                        name: "userTimeout",
                        maxValue: 24 * 60,
                        minValue: 5,
                        hideTrigger:true,
                        fieldLabel: this.i18n._( "Timeout (minutes)" ),
// TODO                       toolTip: this.i18n._( "minutes" ),
                        invalidText: this.i18n._( "The Timeout must be between 5 minutes and 24 hours." ),
                        boxLabel: this.i18n._( "Clients will be unauthenticated after this amount of time regardless of activity. They may re-authenticate immediately." ),
                        value: this.settings.userTimeout / 60,
                        listeners: {
                            "change": Ext.bind(function( elem, newValue ) {
                                this.settings.userTimeout = newValue * 60;
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
                    "afterrender": Ext.bind(function () {
                        this.panelCaptivePage.query('radio[name="pageType"]')[0].setValue(this.settings.pageType);
                        this.captivePageHideComponents(this.settings.pageType );
                        Ung.Util.clearDirty(this.panelCaptivePage);
                    }, this)
                },
                items: [{
                    xtype: "fieldset",
                    title: this.i18n._( "Captive Page" ),
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
                            xtype: "form",
                            bodyStyle: "padding:0px 0px 10px 25px",
                            buttonAlign: "left",
                            pageType: "CUSTOM",
                            id: "upload_form",
                            url: "/capture/handler.py/custom_upload",
                            border: false,
                            items: [{
                                xtype: 'fileuploadfield',
                                name: "upload_file",
                                id: "upload_file",
                                allowBlank:false,
                                width: 500,
                                size: 50
                            },{
                                xtype: 'hidden',
                                name: 'appid',
                                value: this.node.nodeId
                            },{
                                xtype: "button",
                                name: "upload",
                                text: i18n._("Upload Custom File"),
                                handler: Ext.bind(this.onUploadCustomFile, this)
                            }]
                        },{
                            xtype: "form",
                            bodyPadding: "10px 0px 0px 25px",
                            buttonAlign: "left",
                            pageType: "CUSTOM",
                            id: "remove_form",
                            url: "/capture/handler.py/custom_remove",
                            border: false,
                            items: [{
                                xtype: "label",
                                forId: "custom_file",
                                text: "Active Custom File"
                            },{

                                xtype: 'textfield',
                                readOnly: true,
                                name: "custom_file",
                                id: "custom_file",
                                value: this.settings.customFilename,
                                width: 500,
                                size: 50
                            },{
                                xtype: 'hidden',
                                name: 'appid',
                                value: this.node.nodeId
                            },{
                                xtype: "button",
                                name: "remove",
                                text: i18n._("Remove Custom File"),
                                handler: Ext.bind(this.onRemoveCustomFile, this)
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
                            window.open("/capture/handler.py/index?appid=" + this.node.nodeId , "_blank");
                        }, this)
                    }]
                },{
                    xtype: "fieldset",
                    title: this.i18n._( "Session Redirect" ),
                    items: [{
                        xtype: "textfield",
                        name: "redirectUrl",
                        width: 400,
                        fieldLabel: this.i18n._("Redirect URL"),
                        tooltip: this.i18n._("Page to display after successful user authentication."),
                        value: this.settings.redirectUrl,
                        listeners: {
                            "change": Ext.bind(function( elem, newValue ) {
                                this.settings.redirectUrl = newValue;
                            }, this)
                        }
                    }]
                },{
                    xtype: 'fieldset',
                    cls: 'description',
                    height: 100,
                    width: 500,
                    html: this.i18n._('NOTE: The Redirect URL field allows you to specify a page to display immediately after user authentication.  If you leave this field blank, users will instead be forwarded to their original destination.')
                }]
            });
        },

        onUploadCustomFile: function() {
            var form = Ext.getCmp('upload_form').getForm();
            if (form.isValid() == false) {
                Ext.MessageBox.show({
                    title: this.i18n._("Missing Filename"),
                    msg: this.i18n._("Click the Browse button to select a custom file to upload"),
                    buttons: Ext.MessageBox.OK,
                    icon: Ext.MessageBox.ERROR
                    });
                return;
            }
            form.submit({
                parentID: this.panelCaptivePage.getId(),
                waitMsg: this.i18n._("Please wait while uploading your custom captive portal page..."),
                success: Ext.bind(this.uploadCustomFileSuccess, this ),
                failure: Ext.bind(this.uploadCustomFileFailure, this )
            });
        },

        uploadCustomFileSuccess: function(origin,reply) {
            this.settings.customFilename = reply.result.filename;
            var worker = Ext.getCmp('custom_file')
            worker.setValue(reply.result.filename);
            Ext.Msg.show({
                title: this.i18n._("Custom Page Upload Success"),
                msg: this.i18n._(reply.result.msg),
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.INFO
            });
        },

        uploadCustomFileFailure: function(origin,reply) {
            Ext.MessageBox.show({
                title: this.i18n._("Custom Page Upload Failure"),
                msg: this.i18n._(reply.result.msg),
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.ERROR
            })
        },

        onRemoveCustomFile: function() {
            var form = Ext.getCmp('remove_form').getForm();
            form.submit({
                parentID: this.panelCaptivePage.getId(),
                waitMsg: this.i18n._("Please wait while the previous custom file is removed..."),
                success: Ext.bind(this.removeCustomFileSuccess, this ),
                failure: Ext.bind(this.removeCustomFileFailure, this )
            });
        },

        removeCustomFileSuccess: function(origin,reply) {
            this.settings.customFilename = "";
            var worker = Ext.getCmp('custom_file')
            worker.setValue("");
            Ext.MessageBox.show({
                title: this.i18n._("Custom Page Remove Success"),
                msg: this.i18n._(reply.result.msg),
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.INFO
            })
        },

        removeCustomFileFailure: function(origin,reply) {
            Ext.MessageBox.show({
                title: this.i18n._("Custom Page Remove Failure"),
                msg: this.i18n._(reply.result.msg),
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.ERROR
            })
        },

        buildUserEventLog: function() {
            this.gridUserEventLog = Ext.create('Ung.GridEventLog',{
                title: this.i18n._( "User Event Log" ),
                helpSource: "login_event_log",
                eventQueriesFn: this.getRpcNode().getUserEventQueries,
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
                    name: "event_info"
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
                    dataIndex: "event_info",
                    renderer: Ext.bind(function( value ) {
                        switch ( value ) {
                        case "LOGIN":
                            return this.i18n._( "Login Success" );
                        case "FAILED":
                            return this.i18n._( "Login Failure" );
                        case "TIMEOUT":
                            return this.i18n._( "Session Timeout" );
                        case "INACTIVE":
                            return this.i18n._( "Idle Timeout" );
                        case "USER_LOGOUT":
                            return this.i18n._( "User Logout" );
                        case "ADMIN_LOGOUT":
                            return this.i18n._( "Admin Logout" );
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

        buildRuleEventLog: function() {
            this.gridRuleEventLog = Ext.create('Ung.GridEventLog',{
                title: this.i18n._( "Rule Event Log" ),
                helpSource: "rule_event_log",
                eventQueriesFn: this.getRpcNode().getRuleEventQueries,
                settingsCmp: this,
                fields: [{
                    name: "capture_rule_index"
                },{
                    name: "time_stamp",
                    sortType: Ung.SortTypes.asTimestamp
                },{
                    name: "c_client_addr"
                },{
                    name: "c_client_port"
                },{
                    name: "s_server_addr"
                },{
                    name: "s_server_port"
                },{
                    name: "capture_blocked"
                }],

                columns: [{
                    header: this.i18n._("Rule ID"),
                    width: 80,
                    dataIndex: 'capture_rule_index'
                },{
                    header: this.i18n._("Timestamp"),
                    width: Ung.Util.timestampFieldWidth + 20,
                    sortable: true,
                    dataIndex: "time_stamp",
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                },{
                    header: this.i18n._("Client Address"),
                    width: Ung.Util.ipFieldWidth + 20,
                    sortable: true,
                    dataIndex: "c_client_addr"
                },{
                    header: this.i18n._("Client Port"),
                    width: 100,
                    sortable: true,
                    dataIndex: "c_client_port"
                },{
                    header: this.i18n._("Server Address"),
                    width: Ung.Util.ipFieldWidth + 20,
                    sortable: true,
                    dataIndex: "s_server_addr"
                },{
                    header: this.i18n._("Server Port"),
                    width: 100,
                    sortable: true,
                    dataIndex: "s_server_port"
                },{
                    header: this.i18n._("Captured"),
                    width: 100,
                    sortable: true,
                    dataIndex: "capture_blocked"
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
                 !this.query('numberfield[name="userTimeout"]')[0].isValid()) {
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

    Ung.Capture.daysOfWeek = ["mon","tue","wed","thu","fri","sat","sun"];
}
//@ sourceURL=capture-settings.js
