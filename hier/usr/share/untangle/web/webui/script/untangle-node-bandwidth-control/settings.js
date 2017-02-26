Ext.define('Webui.untangle-node-bandwidth-control.settings', {
    extend:'Ung.NodeWin',
    getAppSummary: function() {
        return i18n._("Bandwidth Control monitors, manages, and shapes bandwidth usage on the network");
    },
    initComponent: function() {
        this.quotaTimeStore = [
             [-3, i18n._("End of Week")], //END_OF_WEEK from QuotaBoxEntry
             [-2, i18n._("End of Day")], //END_OF_DAY from QuotaBoxEntry
             [-1, i18n._("End of Hour")]]; //END_OF_HOUR from QuotaBoxEntry

        this.priorityStore = [
            [1, i18n._("Very High")],
            [2, i18n._("High")],
            [3, i18n._("Medium")],
            [4, i18n._("Low")],
            [5, i18n._("Limited")],
            [6, i18n._("Limited More")],
            [7, i18n._("Limited Severely")]];

        this.quotaUnitsStore =[
            [1, i18n._("bytes")],
            [1000, i18n._("Kilobytes")],
            [1000000, i18n._("Megabytes")],
            [1000000000, i18n._("Gigabytes")],
            [1000000000000, i18n._("Terrabytes")]
        ];

        this.buildPanelRules();

        // builds the tab panel with the tabs
        this.buildTabPanel([this.panelRules ]);
        this.callParent(arguments);
    },
    getConditions: function () {
        return [
            {name:"DST_ADDR",displayName: i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"DST_PORT",displayName: i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
            {name:"DST_INTF",displayName: i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
            {name:"SRC_ADDR",displayName: i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: i18n._("Source Port"), type: "text", vtype:"portMatcher", visible: rpc.isExpertMode},
            {name:"SRC_INTF",displayName: i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
            {name:"PROTOCOL",displayName: i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["any",i18n._("any")]], visible: true},
            {name:"USERNAME",displayName: i18n._("Username"), type: "editor", editor: Ext.create('Ung.UserEditorWindow',{}), visible: true},
            {name:"HOST_HOSTNAME",displayName: i18n._("Host Hostname"), type: "text", visible: true},
            {name:"CLIENT_HOSTNAME",displayName: i18n._("Client Hostname"), type: "text", visible: rpc.isExpertMode},
            {name:"SERVER_HOSTNAME",displayName: i18n._("Server Hostname"), type: "text", visible: rpc.isExpertMode},
            {name:"HOST_MAC", displayName: i18n._("Host MAC Address"), type: "text", visible: true },
            {name:"SRC_MAC", displayName: i18n._("Client MAC Address"), type: "text", visible: true },
            {name:"DST_MAC", displayName: i18n._("Server MAC Address"), type: "text", visible: true },
            {name:"HOST_MAC_VENDOR",displayName: i18n._("Host MAC Vendor"), type: "text", visible: true},
            {name:"CLIENT_MAC_VENDOR",displayName: i18n._("Client MAC Vendor"), type: "text", visible: true},
            {name:"SERVER_MAC_VENDOR",displayName: i18n._("Server MAC Vendor"), type: "text", visible: true},
            {name:"HOST_IN_PENALTY_BOX",displayName: i18n._("Host in Penalty Box"), type: "boolean", visible: true},
            {name:"CLIENT_IN_PENALTY_BOX",displayName: i18n._("Client in Penalty Box"), type: "boolean", visible: rpc.isExpertMode},
            {name:"SERVER_IN_PENALTY_BOX",displayName: i18n._("Server in Penalty Box"), type: "boolean", visible: rpc.isExpertMode},
            {name:"HOST_HAS_NO_QUOTA",displayName: i18n._("Host has no Quota"), type: "boolean", visible: true},
            {name:"CLIENT_HAS_NO_QUOTA",displayName: i18n._("Client has no Quota"), type: "boolean", visible: rpc.isExpertMode},
            {name:"SERVER_HAS_NO_QUOTA",displayName: i18n._("Server has no Quota"), type: "boolean", visible: rpc.isExpertMode},
            {name:"USER_HAS_NO_QUOTA",displayName: i18n._("User has no Quota"), type: "boolean", visible: true},
            {name:"HOST_QUOTA_EXCEEDED",displayName: i18n._("Host has exceeded Quota"), type: "boolean", visible: true},
            {name:"CLIENT_QUOTA_EXCEEDED",displayName: i18n._("Client has exceeded Quota"), type: "boolean", visible: rpc.isExpertMode},
            {name:"SERVER_QUOTA_EXCEEDED",displayName: i18n._("Server has exceeded Quota"), type: "boolean", visible: rpc.isExpertMode},
            {name:"USER_QUOTA_EXCEEDED",displayName: i18n._("User has exceeded Quota"), type: "boolean", visible: true},
            {name:"HOST_QUOTA_ATTAINMENT",displayName: i18n._("Host Quota Attainment"), type: "text", visible: true},
            {name:"CLIENT_QUOTA_ATTAINMENT",displayName: i18n._("Client Quota Attainment"), type: "text", visible: rpc.isExpertMode},
            {name:"SERVER_QUOTA_ATTAINMENT",displayName: i18n._("Server Quota Attainment"), type: "text", visible: rpc.isExpertMode},
            {name:"USER_QUOTA_ATTAINMENT",displayName: i18n._("User Quota Attainment"), type: "text", visible: true},
            {name:"HTTP_HOST",displayName: i18n._("HTTP: Hostname"), type: "text", visible: true},
            {name:"HTTP_REFERER",displayName: i18n._("HTTP: Referer"), type: "text", visible: true},
            {name:"HTTP_URI",displayName: i18n._("HTTP: URI"), type: "text", visible: true},
            {name:"HTTP_URL",displayName: i18n._("HTTP: URL"), type: "text", visible: true},
            {name:"HTTP_CONTENT_TYPE",displayName: i18n._("HTTP: Content Type"), type: "text", visible: true},
            {name:"HTTP_CONTENT_LENGTH",displayName: i18n._("HTTP: Content Length"), type: "text", visible: true},
            {name:"HTTP_USER_AGENT",displayName: i18n._("HTTP: Client User Agent"), type: "text", visible: true},
            {name:"HTTP_USER_AGENT_OS",displayName: i18n._("HTTP: Client User OS"), type: "text", visible: false},
            {name:"APPLICATION_CONTROL_APPLICATION",displayName: i18n._("Application Control: Application"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_CATEGORY",displayName: i18n._("Application Control: Application Category"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_PROTOCHAIN",displayName: i18n._("Application Control: Protochain"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_DETAIL",displayName: i18n._("Application Control: Detail"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_CONFIDENCE",displayName: i18n._("Application Control: Confidence"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_PRODUCTIVITY",displayName: i18n._("Application Control: Productivity"), type: "text", visible: true},
            {name:"APPLICATION_CONTROL_RISK",displayName: i18n._("Application Control: Risk"), type: "text", visible: true},
            {name:"PROTOCOL_CONTROL_SIGNATURE",displayName: i18n._("Application Control Lite: Signature"), type: "text", visible: true},
            {name:"PROTOCOL_CONTROL_CATEGORY",displayName: i18n._("Application Control Lite: Category"), type: "text", visible: true},
            {name:"PROTOCOL_CONTROL_DESCRIPTION",displayName: i18n._("Application Control Lite: Description"), type: "text", visible: true},
            {name:"WEB_FILTER_CATEGORY",displayName: i18n._("Web Filter: Category"), type: "text", visible: true},
            {name:"WEB_FILTER_CATEGORY_DESCRIPTION",displayName: i18n._("Web Filter: Category Description"), type: "text", visible: true},
            {name:"WEB_FILTER_FLAGGED",displayName: i18n._("Web Filter: Website is Flagged"), type: "boolean", visible: true},
            {name:"DIRECTORY_CONNECTOR_GROUP",displayName: i18n._("Directory Connector: User in Group"), type: "editor", editor: Ext.create('Ung.GroupEditorWindow',{}), visible: true},
            {name:"REMOTE_HOST_COUNTRY",displayName: i18n._("Client Country"), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true},
            {name:"CLIENT_COUNTRY",displayName: i18n._("Client Country"), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: rpc.isExpertMode},
            {name:"SERVER_COUNTRY",displayName: i18n._("Server Country"), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: rpc.isExpertMode}
        ];
    },
    priorityRenderer: function(value) {
        if (Ext.isEmpty(value))
            return "";

        switch(value) {
          case 0: return "";
          case 1: return i18n._("Very High");
          case 2: return i18n._("High");
          case 3: return i18n._("Medium");
          case 4: return i18n._("Low");
          case 5: return i18n._("Limited");
          case 6: return i18n._("Limited More");
          case 7: return i18n._("Limited Severely");
          default: return Ext.String.format(i18n._("Unknown Priority: {0}"), value);
        }
    },
    setupWizard: function() {
        var welcomeCard = Ext.create('Webui.untangle-node-bandwidth-control.Wizard.Welcome', {
            i18n: i18n,
            gui: this
        });
        var wanBandwidthCard = Ext.create('Webui.untangle-node-bandwidth-control.Wizard.WAN', {
            i18n: i18n,
            gui: this
        });
        var defaultsCard = Ext.create('Webui.untangle-node-bandwidth-control.Wizard.Defaults', {
            i18n: i18n,
            gui: this
        });
        var quotaCard = Ext.create('Webui.untangle-node-bandwidth-control.Wizard.Quotas', {
            i18n: i18n,
            gui: this
        });
        var congratulationsCard = Ext.create('Webui.untangle-node-bandwidth-control.Wizard.Congratulations', {
            i18n: i18n,
            gui: this
        });
        var setupWizard = Ext.create('Ung.Wizard',{
            modalFinish: true,
            hasCancel: false, // cancel not working, still the window can be closed and acts like a cancel
            cardDefaults: {
                labelWidth: 200,
                cls: 'ung-panel'
            },
            cards: [welcomeCard, wanBandwidthCard, defaultsCard, quotaCard, congratulationsCard],
            cancelAction: function() {
                this.up("window").close();
            }
        });
        this.wizardWindow = Ext.create('Ung.Window',{
            title: i18n._("Bandwidth Control Setup Wizard"),
            items: setupWizard,
            maxWidth: 800,
            sizeToRack: false,
            border: false,
            closeWindow: Ext.bind(function() {
                this.wizardWindow.hide();
                Ext.destroy(this.wizardWindow);
                if(this.wizardCompleted) {
                    this.getSettings().configured = true;
                    this.markDirty();
                    this.afterSave = Ext.bind(function() {
                        this.afterSave = null;
                        var nodeCmp = Ung.Node.getCmp(this.nodeId);
                        if(nodeCmp) {
                            nodeCmp.start(Ext.bind(function() {
                                this.reload();
                            }, this));
                        }
                    }, this);
                    this.applyAction();
                } else {
                    this.reload();
                }
            }, this),
            listeners: {
                beforeclose: Ext.bind(function() {
                    var wizard = this.wizardWindow.down('panel[name="wizard"]');
                    if(!wizard.finished) {
                        wizard.finished=true;
                        Ext.MessageBox.alert(i18n._("Setup Wizard Warning"), i18n._("You have not finished configuring Bandwidth Control. Please run the Setup Wizard again."), Ext.bind(function () {
                            this.wizardWindow.close();
                        }, this));
                        return false;
                    }
                    return true;
                }, this)
            }
        });
        this.wizardWindow.show();
        //setupWizard.loadPage(0);
    },

    buildStatusMessage: function() {
        if (!this.getSettings().configured) {
            return {
                xtype: 'component',
                html: i18n._("Bandwidth Control is unconfigured. Use the Wizard to configure Bandwidth Control."),
                cls: 'warning'
            };
        }
        var node = Ung.Node.getCmp(this.nodeId);
        if (node.isRunning()) {
            var qosEnabled = Ung.Main.getNetworkSettings().qosSettings.qosEnabled;
            if(!qosEnabled) {
                return {
                    xtype: 'component',
                    html: i18n._("Bandwidth Control is enabled, but QoS is not enabled. Bandwidth Control requires QoS to be enabled."),
                    cls: 'warning'
                };
            }
        }

        return {
            xtype: 'component',
            html: i18n._("Bandwidth Control is configured")
        };
    },

    // Status Panel
    buildStatus: function() {
        // remove OpenVPN interface (does not work with bandwidth monitor)
        var intfList = Ung.Util.getInterfaceList(false, false);
        for(var i = intfList.length - 1; i >= 0; i--) {
            if( intfList[i][0] === 250 ) {
                intfList.splice(i, 1);
            }
        }

        this.intfCombo =  Ext.create('Ext.form.field.ComboBox',{
            margin: '10 0 0 0',
            store: intfList,
            fieldLabel: i18n._("Interface"),
            editable: false,
            queryMode: 'local',
            cls: 'x-combo-list-small'
        });
        // set "External" as the default value
        this.intfCombo.setValue(intfList[0][0]);

        this.panelStatus = Ext.create('Ung.panel.Status',{
            settingsCmp: this,
            helpSource: 'bandwidth_control_status',
            itemsAfterLicense: [{
                title: i18n._("Status"),
                items: [
                    this.buildStatusMessage(),
                    {
                        xtype: "button",
                        margin: '10 0 0 0',
                        name: 'setup_wizard_button',
                        text: i18n._("Run Bandwidth Control Setup Wizard"),
                        iconCls: "action-icon",
                        handler: Ext.bind(function() {
                            this.setupWizard();
                        }, this)
                    }]
            }, {
                title: i18n._("Bandwidth Monitor"),
                items: [{
                    xtype: 'component',
                    html: i18n._("The Bandwidth Monitor shows current sessions and each session's current bandwidth usage measured on the selected interface.")
                }, this.intfCombo, {
                    xtype: "button",
                    name: "bandwidth_monitor",
                    text: i18n._("Open Bandwidth Monitor"),
                    margin: '10 0 0 0',
                    handler: Ext.bind(function() {
                        Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
                        Ext.defer(function() {
                            if (this.bandwidthMonitorWin === undefined) {
                                this.bandwidthMonitorWin=Ext.create('Webui.untangle-node-bandwidth-control.Monitor',{
                                    parentCmp: this,
                                    intf: this.intfCombo.getValue()
                                });
                                this.subCmps.push(this.bandwidthMonitorWin);
                            }
                            var validLicense;
                            try {
                                validLicense = Ung.Main.getLicenseManager().getLicense('untangle-node-bandwidth-control').valid;
                            } catch (e) {
                                Ung.Util.rpcExHandler(e);
                            }
                            if (validLicense) {
                                this.bandwidthMonitorWin.setInterface(this.intfCombo.getValue());
                                this.bandwidthMonitorWin.show();
                                this.bandwidthMonitorWin.gridCurrentSessions.reload();
                            }
                            Ext.MessageBox.hide();
                        },10, this);
                    }, this)
                }]
            }, {
                title: i18n._("Penalty Box"),
                items: [{
                    xtype: 'component',
                    html: i18n._("View hosts currently in the Penalty Box.")
                }, {
                    xtype: "button",
                    name: "bandwidth_monitor",
                    text: i18n._('View Penalty Box'),
                    margin: '10 0 0 0',
                    handler: Ext.bind(function() {
                        Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
                        Ext.require(['Webui.config.hostMonitor'], function() {
                            Ext.Function.defer(function() {
                                if (this.hostMonitorWin === undefined) {
                                    this.hostMonitorWin=Ext.create('Webui.config.hostMonitor',{
                                        name: "hostMonitor",
                                        helpSource: "host_viewer",
                                        parentCmp: this
                                    });
                                    this.subCmps.push(this.hostMonitorWin);
                                }
                                this.hostMonitorWin.show();
                                this.hostMonitorWin.tabs.setActiveTab(1);
                                Ext.MessageBox.hide();
                            }, 10, this);
                        }, this);
                    }, this)
                }]
            }, {
                title: i18n._("Quotas"),
                items: [{
                    xtype: 'component',
                    html: i18n._("View current Quotas.")
                }, {
                    xtype: "button",
                    name: "bandwidth_monitor",
                    text: i18n._('View Quotas'),
                    margin: '10 0 0 0',
                    handler: Ext.bind(function() {
                        Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
                        Ext.require(['Webui.config.hostMonitor'], function() {
                            Ext.defer(function() {
                                if (this.hostMonitorWin === undefined) {
                                    this.hostMonitorWin=Ext.create('Webui.config.hostMonitor',{
                                        name: "hostMonitor",
                                        helpSource: "host_viewer",
                                        parentCmp: this
                                    });
                                    this.subCmps.push(this.hostMonitorWin);
                                }
                                this.hostMonitorWin.show();
                                this.hostMonitorWin.tabs.setActiveTab(2);
                                Ext.MessageBox.hide();
                            },10, this);
                        }, this);
                    }, this)
                    
                }]
            }] 
        });
    },

    buildPanelRules: function() {
        this.buildGridRules();

        this.panelRules = Ext.create('Ext.panel.Panel', {
            name: 'Rules',
            helpSource: 'bandwidth_control_rules',
            title: i18n._('Rules'),
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                flex: 0,
                title: i18n._('Note'),
                html: i18n._("Rules are evaluated in-order on network traffic.")
            }, this.gridRules]
        });
    },

    buildGridRules: function() {
        this.gridRules = Ext.create('Ung.grid.Panel',{
            flex: 1,
            name: "gridRules",
            settingsCmp: this,
            hasReorder: true,
            title: i18n._("Rules"),
            qtip: i18n._("Bandwidth Rules are used to control and enforce bandwidth usage."),
            dataFn: this.getRpcNode().getRules,
            recordJavaClass: "com.untangle.node.bandwidth_control.BandwidthControlRule",
            emptyRow: {
                "ruleId": 0,
                "enabled": true,
                "description": "",
                "action": ""
            },
            fields: [{
                name: 'enabled'
            },{
                name: 'ruleId'
            },{
                name: 'description'
            },{
                name: 'action'
            },{
                name: 'conditions'
            }],
            columns:[{
                xtype:'checkcolumn',
                header: i18n._("Enabled"),
                dataIndex: 'enabled',
                resizable: false,
                width:55
            }, {
                header: i18n._("Rule ID"),
                dataIndex: 'ruleId',
                width: 50,
                renderer: function(value) {
                    if (value < 0) {
                        return i18n._("new");
                    } else {
                        return value;
                    }
                }
            }, {
                header: i18n._("Description"),
                dataIndex:'description',
                flex:1,
                width: 200
            }, {
                header: i18n._("Action"),
                dataIndex:'action',
                width: 250,
                renderer: Ext.bind(function(value) {
                    if ( typeof(value) == 'undefined') {
                        return "Unknown action";
                    }
                    switch(value.actionType) {
                        case 'SET_PRIORITY': return i18n._("Set Priority") + " [" + this.priorityRenderer(value.priority) + "]";
                        case 'PENALTY_BOX_CLIENT_HOST': return i18n._("Send Client to Penalty Box");
                        case 'APPLY_PENALTY_PRIORITY': return i18n._("Apply Penalty Priority"); // DEPRECATED
                        case 'GIVE_CLIENT_HOST_QUOTA': return i18n._("Give Client a Quota");
                        case 'GIVE_HOST_QUOTA': return i18n._("Give Host a Quota");
                        case 'GIVE_USER_QUOTA': return i18n._("Give User a Quota");
                        default: return "Unknown Action: " + value;
                    }
                }, this)
            }]
        });
        this.gridRules.setRowEditor( Ext.create('Ung.RowEditorWindow',{
            sizeToParent: true,
            validate: function() {
                var components = this.query("component[dataIndex]");

                var actionType  = this.down('combo[name="actionType"]');
                components.push(actionType);
                switch(actionType.value) {
                  case "SET_PRIORITY":
                      var priority = this.down('combo[name="priority"]');
                      components.push(priority);
                      break;
                  case "PENALTY_BOX_CLIENT_HOST":
                      var penaltyTime = this.down('numberfield[name="penaltyTime"]');
                      components.push(penaltyTime);
                      break;
                  case "GIVE_USER_QUOTA":
                  case "GIVE_HOST_QUOTA":
                  case "GIVE_CLIENT_HOST_QUOTA":
                      var quotaTime   = this.down('combo[name="quotaTime"]');
                      var quotaSize  = this.down('numberfield[name="quotaSize"]');
                      var quotaUnit  = this.down('combo[name="quotaUnit"]');
                      components.push(quotaTime);
                      components.push(quotaSize);
                      components.push(quotaUnit);
                    break;
                }
                return this.validateComponents(components);
            },
            inputLines: [{
                xtype: "checkbox",
                name: "Enabled",
                dataIndex: "enabled",
                fieldLabel: i18n._( "Enabled" ),
                width: 360
            }, {
                xtype: "textfield",
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._( "Description" ),
                emptyText: i18n._("[no description]"),
                width: 360
            }, {
                xtype: "fieldset",
                autoScroll: true,
                title: "If all of the following conditions are met:",
                items:[{
                    xtype: 'rulebuilder',
                    settingsCmp: this,
                    javaClass: "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                    dataIndex: "conditions",
                    conditions: this.getConditions()
                }]
            }, {
                xtype: 'fieldset',
                dataIndex: 'action',
                defaults: {
                    width: 350,
                    labelWidth: 150
                },
                title: i18n._('Perform the following action:'),
                items: [{
                    xtype: "combo",
                    name: "actionType",
                    allowBlank: false,
                    fieldLabel: i18n._("Action Type"),
                    editable: false,
                    store: [['SET_PRIORITY', i18n._('Set Priority')],
                            ['PENALTY_BOX_CLIENT_HOST', i18n._('Send Client to Penalty Box')],
                            ['GIVE_HOST_QUOTA', i18n._('Give Host a Quota')],
                            ['GIVE_USER_QUOTA', i18n._('Give User a Quota')]],
                    queryMode: 'local',
                    listeners: {
                        'select': { 
                            fn: Ext.bind(function(combo, newVal, oldVal) {
                                this.gridRules.rowEditor.syncComponents();
                            }, this )
                        }
                    }
                }, {
                    xtype: "combo",
                    name: "priority",
                    allowBlank: false,
                    fieldLabel: i18n._("Priority"),
                    editable: false,
                    store: this.priorityStore,
                    queryMode: 'local'
                }, {
                    xtype: 'container',
                    layout: 'column',
                    name: 'penaltyTimeContainer',
                    width: 600,
                    margin: '0 0 5 0',
                    items: [{
                        xtype: "numberfield",
                        name: "penaltyTime",
                        allowBlank: false,
                        fieldLabel: i18n._("Penalty Time"),
                        width: 350,
                        labelWidth: 150
                    },{
                        xtype: 'label',
                        html: i18n._("(seconds)"),
                        cls: 'boxlabel'
                    }]
                }, {
                    xtype: "combo",
                    name: "quotaTime",
                    allowBlank: false,
                    fieldLabel: i18n._("Quota Expiration"),
                    editable: true,
                    forceSelection: false,
                    store: this.quotaTimeStore,
                    queryMode: 'local'
                }, {
                    xtype: 'container',
                    layout: 'column',
                    name: 'quotaBytesContainer',
                    width: 600,
                    margin: '0 0 5 0',
                    items: [{
                        name: "quotaSize",
                        allowBlank: false,
                        xtype: "numberfield",
                        fieldLabel: i18n._("Quota Size"),
                        width: 350,
                        labelWidth: 150
                    },{
                        xtype: "combo",
                        name: "quotaUnit",
                        margin: '0 0 0 5',
                        editable: false,
                        store: this.quotaUnitsStore,
                        width: 100,
                        queryMode: 'local'
                    }]
                }],
                isValid: function() {
                    return true;
                },
                setValue: function(value) {
                    var actionType  = this.down('combo[name="actionType"]');
                    var priority    = this.down('combo[name="priority"]');
                    var penaltyTime = this.down('numberfield[name="penaltyTime"]');
                    var quotaTime   = this.down('combo[name="quotaTime"]');
                    var quotaSize  = this.down('numberfield[name="quotaSize"]');
                    var quotaUnit  = this.down('combo[name="quotaUnit"]');

                    actionType.setValue(value.actionType);
                    priority.setValue(value.priority);
                    penaltyTime.setValue(value.penaltyTime);
                    quotaTime.setValue(value.quotaTime);

                    var qSize=value.quotaBytes;
                    var qUnit=1;
                    if(value.quotaBytes) {
                        for(var i=1; i <= 1000000000000; i=i*1000) {
                            if(value.quotaBytes >= i && ((value.quotaBytes*10) % i)==0 ) {
                                qUnit = i;
                                qSize = value.quotaBytes/i;
                            } else {
                                break;
                            }
                        }
                    }
                    quotaSize.setValue(qSize);
                    quotaUnit.setValue(qUnit);
                },
                getValue: function() {
                    var actionType  = this.down('combo[name="actionType"]').getValue();
                    var priorityCmp    = this.down('combo[name="priority"]');
                    var penaltyTimeCmp = this.down('numberfield[name="penaltyTime"]');
                    var quotaTimeCmp   = this.down('combo[name="quotaTime"]');
                    var quotaSizeCmp  = this.down('numberfield[name="quotaSize"]');
                    var quotaUnitCmp  = this.down('combo[name="quotaUnit"]');

                    var jsonobj = {
                        javaClass: "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                        actionType: actionType,
                        //must override toString in order for all objects not to appear the same
                        toString: function() {
                            return Ext.encode(this);
                        },
                        priority: (!priorityCmp.disabled) ? priorityCmp.getValue() : null,
                        penaltyTime: (!penaltyTimeCmp.disabled) ? penaltyTimeCmp.getValue() : null,
                        quotaTime: (!quotaTimeCmp.disabled)  ?quotaTimeCmp.getValue() : null,
                        quotaBytes: (!quotaSizeCmp.disabled) ? Math.round(quotaSizeCmp.getValue()*quotaUnitCmp.getValue()) : null
                    };
                    return jsonobj;
                }
            }],
            syncComponents: function () {
                var actionType  = this.down('combo[name="actionType"]');
                var priority    = this.down('combo[name="priority"]');
                var penaltyTime = this.down('numberfield[name="penaltyTime"]');
                var penaltyTimeContainer = this.down('container[name="penaltyTimeContainer"]');
                var quotaTime   = this.down('combo[name="quotaTime"]');
                var quotaSize  = this.down('numberfield[name="quotaSize"]');
                var quotaUnit  = this.down('combo[name="quotaUnit"]');
                var quotaBytesContainer = this.down('container[name="quotaBytesContainer"]');

                priority.disable();
                penaltyTime.disable();
                quotaTime.disable();
                quotaSize.disable();
                quotaUnit.disable();

                switch(actionType.value) {
                  case "SET_PRIORITY":
                    priority.enable();
                    break;
                  case "PENALTY_BOX_CLIENT_HOST":
                    penaltyTime.enable();
                    break;
                  case "APPLY_PENALTY_PRIORITY": // DEPRECATED
                    break;
                  case "GIVE_CLIENT_HOST_QUOTA":
                    quotaTime.enable();
                    quotaSize.enable();
                    quotaUnit.enable();
                    break;
                }
                priority.setVisible(!priority.disabled);
                penaltyTimeContainer.items.each(function(item, index, length) {
                    item.setVisible(!penaltyTime.disabled);
                });
                quotaTime.setVisible(!quotaTime.disabled);
                quotaBytesContainer.items.each(function(item, index, length) {
                    item.setVisible(!quotaUnit.disabled);
                });
            }
        }));
    },
    refreshSettings: function() {
        Ext.MessageBox.wait(i18n._("Reloading..."), i18n._("Please wait"));
        this.getSettings(function() {
            this.gridRules.reload();
            this.clearDirty();
            Ext.MessageBox.hide();
        });
    },
    beforeSave: function(isApply, handler) {
        this.getSettings().rules.list = this.gridRules.getList();
        handler.call(this, isApply);
    }
});


//Bandwidth Monitor
Ext.define('Webui.untangle-node-bandwidth-control.Monitor', {
    extend: 'Webui.config.sessionMonitor',
    parentCmp: null,
    name: 'bandwidthMonitor',
    helpSource: 'bandwidth_control_bandwidth_monitor',
    bandwidthColumns: true,
    sortField: 'totalKBps',
    sortOrder: 'DESC',
    initComponent: function() {
        this.breadcrumbs = [{
            title: i18n._('Bandwidth Monitor')
        }];
        this.callParent(arguments);
    },
    getSessions: function(handler, nodeId) {
        if (!this.isVisible()) {
            handler({javaClass:"java.util.LinkedList", list:[]});
            return;
        }
        rpc.sessionMonitor.getMergedBandwidthSessions(Ext.bind(function(result, exception) {
            if(exception) {
                handler(result, exception);
                return;
            }
            var sessions = (result!=null)?result.list:[];
            // iterate through each session and change its attachments map to properties
            for (var i = 0; i < sessions.length ; i++) {
                var session = sessions[i];
                if (session.attachments) {
                    for (var prop in session.attachments.map) {
                        session[prop] = session.attachments.map[prop];
                    }
                }
            }
            handler({javaClass:"java.util.LinkedList", list:sessions});
        }, this), this.intf, nodeId);
    },
    setInterface: function(intf) {
        this.intf = intf;
    }
});

// Bandwidth wizard configuration cards.
Ext.define('Webui.untangle-node-bandwidth-control.Wizard.Welcome',{
    constructor: function( config ) {
        Ext.apply(this, config);
        var items = [{
            xtype: 'component',
            html: '<h2 class="wizard-title">'+i18n._("Welcome to the Bandwidth Control Setup Wizard!")+'</h2>',
            margin: '10'
        },{
            xtype: 'component',
            html: i18n._('This wizard will help guide you through your initial setup and configuration of Bandwidth Control.'),
            margin: '0 0 10 10'
        },{
            xtype: 'component',
            html: i18n._('Bandwidth Control leverages information provided by other applications in the rack.') + "<br/>" + "<br/>" +
                i18n._('Web Filter (non-Lite) provides web site categorization.') + "<br/>" +
                i18n._('Application Control provides protocol profiling categorization.') + "<br/>" +
                i18n._('Application Control Lite provides protocol profiling categorization.') + "<br/>" +
                i18n._('Directory Connector provides username/group information.') + "<br/>" + "<br/>" +
                "<b>" + i18n._('For optimal Bandwidth Control performance install these applications.') + "</b>",
            margin: '0 0 10 10'
        }];
        if (this.gui.getSettings().configured) {
            items.push({
                xtype: 'component',
                html: i18n._('WARNING: Completing this setup wizard will overwrite the previous settings with new settings. All previous settings will be lost!'),
                cls: 'warning',
                margin: '0 0 10 10'
            });
        }
        this.title = i18n._("Welcome");
        this.panel = Ext.create('Ext.container.Container',{
            scrollable: true,
            items: items
        });
    }
});

Ext.define('Webui.untangle-node-bandwidth-control.Wizard.WAN',{
    constructor: function( config ) {
        Ext.apply(this, config);
        this.title = i18n._("WAN Bandwidth");
        this.networkSettings = Ung.Main.getNetworkSettings(true);

        var wanInterfaces = [];
        for(var i=0; i<this.networkSettings.interfaces.list.length; i++) {
            var intf = this.networkSettings.interfaces.list[i];
            if(intf.configType == "ADDRESSED" && intf.isWan) {
                wanInterfaces.push(intf);
            }
        }
        this.gridQosWanBandwidth = Ext.create( 'Ung.grid.Panel', {
            name: 'QoS Priorities',
            margin: '10 0 0 0',
            height: 160,
            settingsCmp: this,
            hasAdd: false,
            hasDelete: false,
            hasEdit: false,
            columnsDefaultSortable: false,
            storeData: wanInterfaces,
            recordJavaClass: "com.untangle.uvm.network.InterfaceSettings",
            fields: [{
                name: 'interfaceId'
            }, {
                name: 'name'
            }, {
                name: 'configType'
            }, {
                name: 'downloadBandwidthKbps'
            }, {
                name: 'uploadBandwidthKbps'
            },{
                name: 'isWan'
            }],
            columns: [{
                header: i18n._("Interface Id"),
                width: 80,
                dataIndex: 'interfaceId'
            }, {
                header: i18n._("WAN"),
                width: 150,
                dataIndex: 'name'
            }, {
                header: i18n._("Download Bandwidth"),
                dataIndex: 'downloadBandwidthKbps',
                width: 180,
                editor : {
                    xtype: 'numberfield',
                    allowBlank : false,
                    allowDecimals: false,
                    minValue: 0
                },
                renderer: Ext.bind(function( value, metadata, record ) {
                    if (Ext.isEmpty(value)) {
                        return i18n._("Not set");
                    } else {
                        return value + " " + i18n._( "kbps" );
                    }
                }, this )
            }, {
                header: i18n._("Upload Bandwidth"),
                dataIndex: 'uploadBandwidthKbps',
                width: 180,
                editor : {
                    xtype: 'numberfield',
                    allowBlank : false,
                    allowDecimals: false,
                    minValue: 0
                },
                renderer: Ext.bind(function( value, metadata, record ) {
                    if (Ext.isEmpty(value)) {
                        return i18n._("Not set");
                    } else {
                        return value + " " + i18n._( "kbps" );
                    }
                }, this )
            }],
            updateTotalBandwidth: Ext.bind(function() {
                var interfaceList=this.gridQosWanBandwidth.getList();
                var u = 0;
                var d = 0;

                for ( var i = 0 ; i < interfaceList.length ; i++ ) {
                    if(interfaceList[i].isWan) {
                        if(!Ext.isEmpty(interfaceList[i].uploadBandwidthKbps)) {
                            u += interfaceList[i].uploadBandwidthKbps;
                        }
                        if(!Ext.isEmpty(interfaceList[i].downloadBandwidthKbps) ) {
                            d += interfaceList[i].downloadBandwidthKbps;
                        }
                    }
                }

                var d_Mbit = d/1000;
                var u_Mbit = u/1000;

                var message = "<i>"+Ext.String.format( i18n._( "Total: {0} kbps ({1} Mbit) download, {2} kbps ({3} Mbit) upload" ), d, d_Mbit, u, u_Mbit )+"</i>";
                var bandwidthLabel = this.panel.down('component[name="bandwidthLabel"]');
                bandwidthLabel.update(Ext.String.format(i18n._("{0}Note:{1} When enabling QoS valid Download Bandwidth and Upload Bandwidth limits must be set for all WAN interfaces."),'<font color="red">','</font>')+"</br></br>"+message);
            }, this)
        });
        this.gridQosWanBandwidth.getStore().on("update", Ext.bind(function() {
            this.gridQosWanBandwidth.changed = true;
            this.gridQosWanBandwidth.updateTotalBandwidth();
        }, this));
        this.panel = Ext.create('Ext.container.Container',{
            items: [{
                xtype: 'component',
                html: '<h2 class="wizard-title">'+i18n._("Configure WANs download and upload bandwidths")+'</h2>',
                margin: '10'
            },{
                xtype: 'component',
                name: 'bandwidthLabel',
                html: ' ',
                margin: '10'
            }, this.gridQosWanBandwidth,{
                xtype: 'component',
                html: i18n._('It is suggested to set these around 95% to 100% of the actual measured bandwidth available for each WAN.') + "<br/>",
                margin: '10'
            },{
                xtype: 'component',
                html: i18n._('WARNING: These settings must be reasonably accurate for Bandwidth Control to operate properly!'),
                cls: 'warning',
                margin: '10'
            }]
        });
        this.gridQosWanBandwidth.updateTotalBandwidth();
        this.onValidate = function() {
            var qosBandwidthList = this.gridQosWanBandwidth.getList();
            for(var i=0; i<qosBandwidthList.length; i++) {
                var qosBandwidth = qosBandwidthList[i];
                if(Ext.isEmpty(qosBandwidth.downloadBandwidthKbps) || Ext.isEmpty(qosBandwidth.uploadBandwidthKbps) ) {
                    Ext.MessageBox.alert(i18n._("Failed"), Ext.String.format(i18n._("Please set valid Download Bandwidth and Upload Bandwidth limits for the interface {0}."), qosBandwidth.name));
                    return false;
                }
            }
            return true;
        };
        this.onNext = Ext.bind(function(handler) {
            if(this.networkSettings.qosSettings.qosEnabled && !this.gridQosWanBandwidth.changed) {
                //If Qos is already enabled and qos WANs speeds are unchanged it doesn't need to save network settings.
                handler();
                return;
            }
            Ext.MessageBox.wait(i18n._("Configuring WAN..."), i18n._("Please wait"));
            //Set downloadBandwidthKbps and uploadBandwidthKbps for all
            var qosBandwidthList = this.gridQosWanBandwidth.getList();
            var qosBandwidthMap = {};
            var i;
            for(i=0; i<qosBandwidthList.length; i++) {
                qosBandwidthMap[qosBandwidthList[i].interfaceId] = qosBandwidthList[i];
            }
            for(i=0; i<this.networkSettings.interfaces.list.length; i++) {
                var intf=this.networkSettings.interfaces.list[i];
                var intfBandwidth = qosBandwidthMap[intf.interfaceId];
                if(intfBandwidth) {
                    intf.downloadBandwidthKbps=intfBandwidth.downloadBandwidthKbps;
                    intf.uploadBandwidthKbps=intfBandwidth.uploadBandwidthKbps;
                }
            }
            //Enable Qos
            this.networkSettings.qosSettings.qosEnabled=true;

            // stop the metric manager to avoid any rpc calls when networking is being restarted.
            // this helps avoid any issues with timeouts/resets during saving.
            Ung.MetricManager.stop();
            Ung.Main.getNetworkManager().setNetworkSettings(Ext.bind(function(result, exception) {
                // restart the metric manager
                Ung.MetricManager.start();
                if(Ung.Util.handleException(exception)) return;
                this.gridQosWanBandwidth.changed = false;
                Ext.MessageBox.hide();
                handler();
            }, this), this.networkSettings);
        }, this);
    }
});

Ext.define('Webui.untangle-node-bandwidth-control.Wizard.Defaults', {
    constructor: function( config ) {
        Ext.apply(this, config);
        this.title = i18n._("Configuration");

        this.panel = Ext.create('Ext.container.Container',{
            scrollable: true,
            items: [{
                xtype: 'component',
                html: '<h2 class="wizard-title">'+i18n._("Choose a starting configuration")+'</h2>',
                margin: '10'
            }, {
                xtype: 'component',
                html: i18n._('Several initial default configurations are available for Bandwidth Control. Please select the environment most like yours below.'),
                margin: '0 10 10 10'
            },{
              xtype:'container',
              margin: '10 0 10 20',
              items:[{
                xtype: 'combo',
                editable: false,
                store: Ext.create('Ext.data.ArrayStore',{
                    fields: ['settingsName', 'displayName','description'],
                    data: [['business_business',
                       i18n._('Business'),
                        i18n._('This initial configuration provides common settings suitable for most small and medium-sized businesses.') + '<br/>' + '<br/>' +

                        i18n._('Benefits:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('optimizes internet responsiveness by prioritizing traffic of interactive services most commonly used in businesses') + '</li>' +
                        '<li>' + i18n._('de-prioritizes traffic of greedy non-work-related activities such as peer-to-peer file sharing') + '</li>' +
                        '<li>' + i18n._('de-prioritizes non-real-time background traffic, to avoid slowing your internet connection when you need it most') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('Prioritizes:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('interactive traffic and services (Remote Desktop, Email, DNS, SSH)') + '</li>' +
                        '<li>' + i18n._('interactive web traffic') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('De-prioritizes:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('non-real-time background services (e.g. Microsoft&reg; updates, backup services)') + '</li>' +
                        '<li>' + i18n._('any detected Peer-to-Peer traffic') + '</li>' +
                        '<li>' + i18n._('all web traffic in violation of company policy') + '</li>' +
                        '</ul>' + '<br/>'
                    ],
                    ['school_school',
                        i18n._('School'),
                        i18n._('This initial configuration provides common settings suitable for most School Districts, Elementary through High Schools, or Charter Schools.') + '<br/>' + '<br/>' +

                        i18n._('Benefits:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('optimizes internet responsiveness by prioritizing traffic of interactive services most commonly used in schools') + '</li>' +
                        '<li>' + i18n._('prioritizes school-related traffic, such as traffic to Education sites or Search Engines') + '</li>' +
                        '<li>' + i18n._('de-prioritizes traffic of greedy non-school-related activities such as peer-to-peer file sharing') + '</li>' +
                        '<li>' + i18n._('de-prioritizes non-real-time background traffic, to avoid slowing your internet connection when you need it most') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('Prioritizes:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('interactive traffic and services (Remote Desktop, Email, DNS, SSH)') + '</li>' +
                        '<li>' + i18n._('interactive web traffic') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('De-prioritizes:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('web traffic to download sites') + '</li>' +
                        '<li>' + i18n._('non-real-time background services (e.g. Microsoft&reg; updates, backup services)') + '</li>' +
                        '<li>' + i18n._('any detected Peer-to-Peer traffic') + '</li>' +
                        '<li>' + i18n._('all web traffic in violation of school policy') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('Limits:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('heavily limits BitTorrent usage') + '</li>' +
                        '</ul>' + '<br/>'
                    ],
                    ['school_college',
                        i18n._('College/University'),
                        i18n._('This initial configuration provides common settings suitable for most Community Colleges, Universities, and associated Campus Networks.') + '<br/>' + '<br/>' +

                        i18n._('Benefits:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('optimizes internet responsiveness by prioritizing traffic of interactive services most commonly used in schools') + '</li>' +
                        '<li>' + i18n._('prioritizes school-related traffic, such as traffic to Education sites or Search Engines') + '</li>' +
                        '<li>' + i18n._('de-prioritizes traffic of greedy non-school-related activities, peer-to-peer file sharing, or BitTorrent') + '</li>' +
                        '<li>' + i18n._('de-prioritizes non-real-time background traffic, to avoid slowing your internet connection when you need it most') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('Prioritizes:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('interactive traffic and services (Remote Desktop, Email, DNS, SSH)') + '</li>' +
                        '<li>' + i18n._('interactive web traffic') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('De-prioritizes:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('web traffic to download sites') + '</li>' +
                        '<li>' + i18n._('non-real-time background services (e.g. Microsoft&reg; updates, backup services)') + '</li>' +
                        '<li>' + i18n._('any detected Peer-to-Peer traffic') + '</li>' +
                        '<li>' + i18n._('all web traffic in violation of school policy') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('Limits:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('heavily limits BitTorrent usage') + '</li>' +
                        '</ul>' + '<br/>'
                    ],
                    ['business_government',
                        i18n._('Government'),
                        i18n._('This initial configuration provides common settings suitable for most governmental organizations.') + '<br/>' + '<br/>' +

                        i18n._('Benefits:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('optimizes internet responsiveness by prioritizing traffic of interactive services most commonly used in government') + '</li>' +
                        '<li>' + i18n._('de-prioritizes traffic of greedy non-work-related activities, such as peer-to-peer file sharing') + '</li>' +
                        '<li>' + i18n._('de-prioritizes non-real-time background traffic, to avoid slowing your internet connection when you need it most') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('Prioritizes:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('interactive traffic and services (Remote Desktop, Email, DNS, SSH)') + '</li>' +
                        '<li>' + i18n._('interactive web traffic') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('De-prioritizes:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('non-real-time background services (e.g. Microsoft&reg; updates, backup services)') + '</li>' +
                        '<li>' + i18n._('any detected Peer-to-Peer traffic') + '</li>' +
                        '<li>' + i18n._('all web traffic in violation of organization\'s policy') + '</li>' +
                        '</ul>' + '<br/>'
                    ],
                    ['business_nonprofit',
                        i18n._('Non-Profit'),
                        i18n._('This initial configuration provides common settings suitable for most charitable and not-for-profit organizations.') + '<br/>' + '<br/>' +

                        i18n._('Benefits:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('optimizes internet responsiveness by prioritizing traffic of interactive services most commonly used in businesses') + '</li>' +
                        '<li>' + i18n._('de-prioritizes traffic of greedy non-work-related activities such as peer-to-peer file sharing') + '</li>' +
                        '<li>' + i18n._('de-prioritizes non-real-time background traffic, to avoid slowing your internet connection when you need it most') + '</li>' +
                        '<li>' + i18n._('saves money by controlling bandwidth') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('Prioritizes:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('interactive traffic and services (Remote Desktop, Email, DNS, SSH)') + '</li>' +
                        '<li>' + i18n._('interactive web traffic') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('De-prioritizes:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('non-real-time background services (e.g. Microsoft&reg; updates, backup services)') + '</li>' +
                        '<li>' + i18n._('any detected Peer-to-Peer traffic') + '</li>' +
                        '<li>' + i18n._('all web traffic in violation of the organization\'s policy') + '</li>' +
                        '</ul>' + '<br/>'
                    ],
                    ['school_hotel',
                        i18n._('Hotel'),
                        i18n._('This initial configuration provides common settings suitable for most Hotel and Motels.') + '<br/>' + '<br/>' +

                        i18n._('Benefits:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('optimizes internet responsiveness by prioritizing traffic of interactive services most commonly used by guests') + '</li>' +
                        '<li>' + i18n._('web traffic to business-related sites (Search Engines, Finance, Business/Services, etc)') + '</li>' +
                        '<li>' + i18n._('de-prioritizes traffic of peer-to-peer file sharing') + '</li>' +
                        '<li>' + i18n._('de-prioritizes non-real-time background traffic, to avoid slowing your internet connection when it is needed most') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('Prioritizes:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('interactive traffic and services (Remote Desktop, Email, DNS, SSH)') + '</li>' +
                        '<li>' + i18n._('interactive web traffic') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('De-prioritizes:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('web traffic to download sites') + '</li>' +
                        '<li>' + i18n._('non-real-time background services (e.g. Microsoft&reg; updates, backup services)') + '</li>' +
                        '<li>' + i18n._('any detected Peer-to-Peer traffic') + '</li>' +
                        '<li>' + i18n._('all web traffic in violation of school policy') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('Limits:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('heavily limits BitTorrent usage') + '</li>' +
                        '</ul>' + '<br/>'
                    ],
                    ['home',
                        i18n._('Home'),
                        i18n._('This initial configuration provides common settings suitable for most home users and households.') + '<br/>' + '<br/>' +

                        i18n._('Benefits:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('optimizes internet responsiveness by prioritizing traffic of interactive services most commonly used in households') + '</li>' +
                        '<li>' + i18n._('prioritizes internet radio and video so that these services run flawlessly while downloads are running') + '</li>' +
                        '<li>' + i18n._('de-prioritizes non-real-time background traffic, to avoid slowing your internet connection when you need it most') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('Prioritizes:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('interactive traffic and services (Remote Desktop, Email, DNS, SSH)') + '</li>' +
                        '<li>' + i18n._('internet radio (e.g. Pandora&reg;,  Last.fm<small><sup>TM</sup></small>)') + '</li>' +
                        '<li>' + i18n._('internet video (e.g. YouTube&reg;, Hulu<small><sup>TM</sup></small>, NetFlix&reg;)') + '</li>' +
                        '<li>' + i18n._('interactive web traffic') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('De-prioritizes:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('non-real-time background services (e.g. Microsoft&reg; updates, backup services)') + '</li>' +
                        '<li>' + i18n._('all web traffic in violation of the household policy') + '</li>' +
                        '</ul>' + '<br/>'
                    ],
                    ['metered',
                        i18n._('Metered Internet'),
                        i18n._('This initial configuration provides common settings suitable for organizations that pay variable rates for bandwidth.') + '<br/>' + '<br/>' +
                        i18n._('For organizations that have to pay bandwidth rates proportional to bandwidth usage or have significant overage fees, this configuration maximizes bandwidth available to important interactive services, while minimizing bandwidth use for other less important or background tasks.') + '<br/>' + '<br/>' +

                        i18n._('Benefits:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('optimizes internet responsiveness by prioritizing traffic of interactive services most commonly used in organizations') + '</li>' +
                        '<li>' + i18n._('de-prioritizes traffic of non-work-related activities, such as gaming, peer-to-peer file sharing, or online videos') + '</li>' +
                        '<li>' + i18n._('de-prioritizes non-real-time background traffic, to avoid slowing your internet connection when you need it most') + '</li>' +
                        '<li>' + i18n._('saves money') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('Prioritizes:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('interactive traffic and services (Remote Desktop, Email, DNS, SSH)') + '</li>' +
                        '<li>' + i18n._('interactive web traffic') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('De-prioritizes:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('any detected Peer-to-Peer traffic') + '</li>' +
                        '<li>' + i18n._('all web traffic in violation of the organization\'s policy') + '</li>' +
                        '</ul>' + '<br/>' +

                        i18n._('Limits:') + '<br/>' +
                        '<ul style="list-style: circle inside;">' +
                        '<li>' + i18n._('non-real-time background services (e.g. Microsoft&reg; updates, backup services)') + '</li>' +
                        '<li>' + i18n._('all web traffic to Download Sites') + '</li>' +
                        '<li>' + i18n._('heavily limits BitTorrent usage') + '</li>' +
                        '</ul>' + '<br/>'
                    ],
                    ['custom',
                        i18n._('Custom'),
                        i18n._('This is a basic configuration with no rules set by default.') + '<br/>' +
                        i18n._('This is a good option for users who wish to build their own rules configuration from scratch.') + '<br/>' +
                        '<br/>'
                    ]]
                }),
                valueField: 'settingsName',
                displayField: 'displayName',
                name: 'starting_configuration',
                fieldLabel: i18n._('Configuration'),
                width: 325,
                labelWidth: 150,
                queryMode: 'local',
                listeners: {
                    "select": {
                        fn: Ext.bind(function(elem, record) {
                            this.panel.down('component[name="configurationDescription"]').update(record.data.description);
                        }, this)
                    }
                }
            }, {
                xtype: 'component',
                name: 'configurationDescription',
                html: ' ',
                autoScroll: true,
                margin: '10 0 0 10'
            }]}]
        });

        this.onValidate = function() {
            var startingConfiguration = this.panel.down('combo[name="starting_configuration"]').getValue();
            if (Ext.isEmpty(startingConfiguration)) {
                Ext.MessageBox.alert(i18n._("Error"), i18n._("You must select a starting configuration."));
                return false;
            }
            return true;
        };
        this.onNext = Ext.bind(function(handler) {
            Ext.MessageBox.wait(i18n._("Configuring Settings..."), i18n._("Please wait"));
            var startingConfiguration = this.panel.down('combo[name="starting_configuration"]').getValue();
            this.gui.getRpcNode().wizardLoadDefaults(startingConfiguration.replace(/_.*/,""));
            this.gui.refreshSettings();
            Ext.MessageBox.hide();
            handler();
        }, this);
    }
});

Ext.define('Webui.untangle-node-bandwidth-control.Wizard.Quotas', {
    constructor: function( config ) {
        Ext.apply(this, config);
        this.title = i18n._("Quotas");
        this.enableQuotas = false;
        this.panel = Ext.create('Ext.container.Container',{
            scrollable: true,
            items: [{
                xtype: 'component',
                html: '<h2 class="wizard-title">'+i18n._("Configure Quotas")+'</h2>',
                margin: '10'
            },{
                xtype: 'component',
                html: i18n._('Quotas for bandwidth can be set for certain hosts. This allows some hosts to be allocated high bandwidth, as long as it is remains within a certain usage quota; however, their bandwidth will be slowed if their usage is excessive.') + "<br/>",
                margin: '0 10 10 10'
            },{
                xtype: 'radio',
                boxLabel: i18n._('Disable Quotas (default)'),
                hideLabel: true,
                name: 'enableQuotas',
                checked: true,
                handler: Ext.bind(function(elem, checked) {
                    if(checked) {
                        this.enableQuotas = false;
                        this.setVisi(false);
                    }
                }, this),
                margin: '0 10 10 10'
            }, {
                xtype: 'radio',
                boxLabel: i18n._('Enable Quotas'),
                hideLabel: true,
                name: 'enableQuotas',
                id: 'enableQuotas',
                checked: false,
                handler: Ext.bind(function(elem, checked) {
                    if(checked) {
                        this.enableQuotas = true;
                        this.setVisi(true);
                    }
                }, this),
                margin: '0 10 10 10'
            }, {
                name:'quotaSettings',
                xtype:'fieldset',
                hidden: true,
                items: [{
                    xtype: "checkbox",
                    name: "quotaHostEnabled",
                    fieldLabel: i18n._( "Enable Quotas for Hosts (IP addresses)" ),
                    width: 500,
                    checked: true
                }, {
                    xtype: "checkbox",
                    name: "quotaUserEnabled",
                    fieldLabel: i18n._( "Enable Quotas for Users (usernames)" ),
                    width: 500,
                    checked: true
                }, {
                    xtype: 'component',
                    html: "<i>" + i18n._("Quota Expiration") + "</i> " + i18n._('controls how long a quota lasts (hourly, daily, weekly). The default is Daily.'),
                    margin: '15 0 5 0'
                }, {
                    xtype: "combo",
                    name: "quotaTime",
                    width: 300,
                    labelWidth: 150,
                    allowBlank: false,
                    fieldLabel: i18n._("Quota Expiration"),
                    editable: true,
                    forceSelection: false,
                    store: this.gui.quotaTimeStore,
                    queryMode: 'local',
                    value: -2
                }, {
                    xtype: 'component',
                    html: "<i>" + i18n._("Quota Size") + "</i> " + i18n._('configures the size of the quota given to each host. The default is 1 Gb.'),
                    margin: '15 0 5 0'
                }, {
                    xtype: 'container',
                    layout: 'column',
                    items: [{
                        xtype: "numberfield",
                        name: "quotaSize",
                        allowBlank: false,
                        width: 300,
                        labelWidth: 150,
                        fieldLabel: i18n._("Quota Size"),
                        value: 1
                    },{
                        xtype: "combo",
                        name: "quotaUnit",
                        margin: '0 0 0 5',
                        editable: false,
                        store: this.gui.quotaUnitsStore,
                        width: 100,
                        queryMode: 'local',
                        value: 1000000000
                    }]
                }, {
                    xtype: 'component',
                    html: "<i>" + i18n._("Quota Exceeded Priority") + "</i> " + i18n._('configures the priority given to hosts that have exceeded their quota.'),
                    margin: '15 0 5 0'
                }, {
                    xtype: "combo",
                    name: "quotaExceededPriority",
                    allowBlank: false,
                    fieldLabel: i18n._("Quota Exceeded Priority"),
                    width: 325,
                    labelWidth: 150,
                    editable: false,
                    store: this.gui.priorityStore,
                    queryMode: 'local',
                    value: 6
                }]
            }]
        });

        this.setVisi = Ext.bind(function(enabled) {
            this.panel.down('fieldset[name="quotaSettings"]').setVisible(enabled);
        }, this);

        this.onValidate = function() {
            var enabled = Ext.getCmp("enableQuotas").getValue();
            if (enabled) {
                // nothing
            }
            return true;
        };
        this.onNext = Ext.bind(function(handler) {
            Ext.MessageBox.wait(i18n._("Configuring Quotas..."), i18n._("Please wait"));
            if (this.enableQuotas) {
                var quotaTime   = this.panel.down('combo[name="quotaTime"]').getValue();
                var quotaSize  = this.panel.down('numberfield[name="quotaSize"]').getValue();
                var quotaUnit  = this.panel.down('combo[name="quotaUnit"]').getValue();
                var quotaBytes = Math.round(quotaSize * quotaUnit);
                var quotaPrio   = this.panel.down('combo[name="quotaExceededPriority"]').getValue();

                var quotaHosts   = this.panel.down('checkbox[name="quotaHostEnabled"]').getValue();
                var quotaUsers   = this.panel.down('checkbox[name="quotaUserEnabled"]').getValue();
                
                if (quotaHosts)
                    this.gui.getRpcNode().wizardAddHostQuotaRules(quotaTime, quotaBytes, quotaPrio);
                if (quotaUsers)
                    this.gui.getRpcNode().wizardAddUserQuotaRules(quotaTime, quotaBytes, quotaPrio);
                
                this.gui.refreshSettings();
            }
            Ext.MessageBox.hide();
            handler();
        }, this);
    }
});

Ext.define('Webui.untangle-node-bandwidth-control.Wizard.Congratulations',{
    constructor: function( config ) {
        Ext.apply(this, config);
        this.title = i18n._( "Finish" );
        this.panel = Ext.create('Ext.container.Container', {
            items: [{
                xtype: 'component',
                html: '<h2 class="wizard-title">'+i18n._("Congratulations!")+'</h2>',
                margin: '10'
            }, {
                xtype: 'component',
                html: i18n._('Bandwidth Control is now configured and enabled.'),
                margin: '0 10 10 10'
            }]
        });
        
        this.onLoad = Ext.bind(function(handler) {
            this.gui.wizardCompleted = true;
            handler();
        }, this);
        this.onNext = Ext.bind(function(handler) {
            this.gui.wizardWindow.close();
            handler();
        }, this);
    }
});

//# sourceURL=bandwidth-control-settings.js
