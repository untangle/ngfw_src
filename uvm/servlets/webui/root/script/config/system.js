Ext.define('Webui.config.system', {
    extend: 'Ung.ConfigWin',
    panelRegional: null,
    panelSupport: null,
    panelBackup: null,
    panelRestore: null,
    panelProtocols: null,
    panelShield: null,
    initComponent: function() {
        this.breadcrumbs = [{
            title: i18n._("Configuration"),
            action: Ext.bind(function() {
                this.cancelAction();
            }, this)
        }, {
            title: i18n._("System")
        }];
        try {
            this.oemName=Ung.Main.getOemManager().getOemName();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        if (this.oemName == "Untangle") {
            this.downloadLanguageHTML='<a href="http://pootle.untangle.com" target="_blank">' + i18n._("Download New Language Packs") + '</a>';
        } else {
            this.downloadLanguageHTML='';
        }
        this.buildRegional();
        this.buildSupport();
        this.buildBackup();
        this.buildRestore();
        this.buildProtocols();
        this.buildShield();
        // builds the tab panel with the tabs
        this.buildTabPanel([this.panelRegional, this.panelSupport, this.panelBackup, this.panelRestore, this.panelProtocols, this.panelShield]);
        if (!this.isHttpLoaded() && !this.isFtpLoaded() && !this.isMailLoaded() ) {
            this.panelProtocols.disable();
        }
        this.callParent(arguments);
    },
    getShieldMatchers: function () {
        return [
            {name:"DST_ADDR",displayName: this.i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"DST_PORT",displayName: this.i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
            {name:"DST_INTF",displayName: this.i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
            {name:"SRC_ADDR",displayName: this.i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: this.i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: false},
            {name:"SRC_INTF",displayName: this.i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
            {name:"PROTOCOL",displayName: this.i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["any",this.i18n._("any")]], visible: true},
            {name:"USERNAME",displayName: this.i18n._("Username"), type: "editor", editor: Ext.create('Ung.UserEditorWindow',{}), visible: true},
            {name:"CLIENT_HOSTNAME",displayName: this.i18n._("Client Hostname"), type: "text", visible: true},
            {name:"SERVER_HOSTNAME",displayName: this.i18n._("Server Hostname"), type: "text", visible: false},
            {name:"SRC_MAC", displayName: this.i18n._("Client MAC Address"), type: "text", visible: true },
            {name:"DST_MAC", displayName: this.i18n._("Server MAC Address"), type: "text", visible: true },
            {name:"CLIENT_MAC_VENDOR",displayName: this.i18n._("Client MAC Vendor"), type: "text", visible: true},
            {name:"SERVER_MAC_VENDOR",displayName: this.i18n._("Server MAC Vendor"), type: "text", visible: true},
            {name:"CLIENT_IN_PENALTY_BOX",displayName: this.i18n._("Client in Penalty Box"), type: "boolean", visible: true},
            {name:"SERVER_IN_PENALTY_BOX",displayName: this.i18n._("Server in Penalty Box"), type: "boolean", visible: true},
            {name:"CLIENT_HAS_NO_QUOTA",displayName: this.i18n._("Client has no Quota"), type: "boolean", visible: false},
            {name:"SERVER_HAS_NO_QUOTA",displayName: this.i18n._("Server has no Quota"), type: "boolean", visible: false},
            {name:"CLIENT_QUOTA_EXCEEDED",displayName: this.i18n._("Client has exceeded Quota"), type: "boolean", visible: true},
            {name:"SERVER_QUOTA_EXCEEDED",displayName: this.i18n._("Server has exceeded Quota"), type: "boolean", visible: true},
            {name:"DIRECTORY_CONNECTOR_GROUP",displayName: this.i18n._("Directory Connector: User in Group"), type: "editor", editor: Ext.create('Ung.GroupEditorWindow',{}), visible: true},
            {name:"HTTP_USER_AGENT",displayName: this.i18n._("HTTP: Client User Agent"), type: "text", visible: true},
            {name:"HTTP_USER_AGENT_OS",displayName: this.i18n._("HTTP: Client User OS"), type: "text", visible: true}
        ];
    },
    // get languange settings object
    getLanguageSettings: function(forceReload) {
        if (forceReload || this.rpc.languageSettings === undefined) {
            try {
                this.rpc.languageSettings = rpc.languageManager.getLanguageSettings();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.languageSettings;
    },
    getSystemSettings: function(forceReload) {
        if (forceReload || this.rpc.accessSettings === undefined) {
            try {
                this.rpc.accessSettings = rpc.systemManager.getSettings();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }

        }
        return this.rpc.accessSettings;
    },
    getHttpNode: function(forceReload) {
        if (forceReload || this.rpc.httpNode === undefined) {
            try {
                this.rpc.httpNode = rpc.nodeManager.node("untangle-casing-http");
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }

        }
        return this.rpc.httpNode;
    },
    isHttpLoaded: function(forceReload) {
        return this.getHttpNode(forceReload) !== null;
    },
    getHttpSettings: function(forceReload) {
        if (forceReload || this.rpc.httpSettings === undefined) {
            try {
                this.rpc.httpSettings = this.getHttpNode(forceReload).getHttpSettings();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }

        }
        return this.rpc.httpSettings;
    },
    getFtpNode: function(forceReload) {
        if (forceReload || this.rpc.ftpNode === undefined) {
            try {
                this.rpc.ftpNode = rpc.nodeManager.node("untangle-casing-ftp");
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }

        }
        return this.rpc.ftpNode;
    },
    isFtpLoaded: function(forceReload) {
        return this.getFtpNode(forceReload) !== null;
    },
    getFtpSettings: function(forceReload) {
        if (forceReload || this.rpc.ftpSettings === undefined) {
            try {
                this.rpc.ftpSettings = this.getFtpNode(forceReload).getFtpSettings();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.ftpSettings;
    },
    getSmtpNode: function(forceReload) {
        if (forceReload || this.rpc.smtpNode === undefined) {
            try {
                this.rpc.smtpNode = rpc.nodeManager.node("untangle-casing-smtp");
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.smtpNode;
    },
    isMailLoaded: function(forceReload) {
        return this.getSmtpNode(forceReload) !== null;
    },
    getSmtpNodeSettings: function(forceReload) {
        if (forceReload || this.rpc.mailSettings === undefined) {
            try {
                this.rpc.mailSettings = this.getSmtpNode(forceReload).getSmtpNodeSettings();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.mailSettings;
    },
    getShieldNode: function(forceReload) {
        if (forceReload || this.rpc.shieldNode === undefined) {
            try {
                this.rpc.shieldNode = rpc.nodeManager.node("untangle-node-shield");
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.shieldNode;
    },
    isShieldLoaded: function(forceReload) {
        return this.getShieldNode(forceReload) !== null;
    },
    getShieldSettings: function(forceReload) {
        if (forceReload || this.rpc.shieldSettings === undefined) {
            try {
                this.rpc.shieldSettings = this.getShieldNode().getSettings();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }

        }
        return this.rpc.shieldSettings;
    },
    getTimeZone: function(forceReload) {
        if (forceReload || this.rpc.timeZone === undefined) {
            try {
                /* Handle the serialization mess of java with ZoneInfo. */
                var tz = rpc.adminManager.getTimeZone();
                if ( tz && typeof ( tz ) != "string" ) {
                    tz = tz.ID;
                }

                this.rpc.timeZone = tz;
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.timeZone;
    },
    buildSupport: function() {
        this.panelSupport = Ext.create('Ext.panel.Panel',{
            name: "Support",
            helpSource: "system_support",
            title: this.i18n._("Support"),
            cls: "ung-panel",
            autoScroll: true,
            items: [{
                xtype: "fieldset",
                title: this.i18n._("Support"),
                items: [{
                    xtype: 'checkbox',
                    name: "Allow secure access to your server for support purposes",
                    boxLabel: Ext.String.format(this.i18n._("{0}Allow{1} secure access to your server for support purposes."), "<b>", "</b>"),
                    hideLabel: true,
                    checked: this.getSystemSettings().supportEnabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.getSystemSettings().supportEnabled = newValue;
                            }, this)
                        }
                    }
                },{
                    xtype: "component",
                    html: this.i18n._("Download system logs.")
                },{
                    xtype: "button",
                    name: 'logButton',
                    text: this.i18n._("Download"),
                    handler: Ext.bind(function() {
                        var downloadForm = document.getElementById('downloadForm');
                        downloadForm["type"].value="SystemSupportLogs";
                        downloadForm.submit();
                    }, this)
                }]
            },{
                xtype: "fieldset",
                title: this.i18n._("Manual Reboot"),
                items: [{
                    xtype: "component",
                    html: this.i18n._("Reboot the server.")
                },{
                    xtype: "button",
                    margin: '5 0 0 0',
                    text: this.i18n._("Reboot"),
                    name: "Manual Reboot",
                    iconCls: "reboot-icon",
                    handler: Ext.bind(function() {
                        Ext.MessageBox.confirm(this.i18n._("Manual Reboot Warning"),
                            Ext.String.format(this.i18n._("The server is about to manually reboot.  This will interrupt normal network operations until the {0} Server is finished automatically restarting. This may take up to several minutes to complete."), rpc.companyName ),
                            Ext.bind(function(btn) {
                            if (btn == "yes") {
                                rpc.jsonrpc.UvmContext.rebootBox(Ext.bind(function (result, exception) {
                                    if(exception) {
                                        Ext.MessageBox.alert(this.i18n._("Manual Reboot Failure Warning"),Ext.String.format(this.i18n._("Error: Unable to reboot {0} Server"), rpc.companyName));
                                    } else {
                                        Ext.MessageBox.wait(Ext.String.format(this.i18n._("The {0} Server is rebooting."), rpc.companyName), i18n._("Please wait"));
                                    }
                                }, this));
                            }
                         }, this));
                    }, this)
                }]
            },{
                xtype: "fieldset",
                title: this.i18n._("Manual Shutdown"),
                items: [{
                    xtype: "component",
                    html: this.i18n._("Power off the server.")
                },{
                    xtype: "button",
                    margin: '5 0 0 0',
                    text: this.i18n._("Shutdown"),
                    name: "Manual Shutdown",
                    iconCls: "reboot-icon",
                    handler: Ext.bind(function() {
                        Ext.MessageBox.confirm(this.i18n._("Manual Shutdown Warning"),
                            Ext.String.format(this.i18n._("The {0} Server is about to shutdown.  This will stop all network operations."), rpc.companyName ),
                            Ext.bind(function(btn) {
                            if (btn == "yes") {
                                rpc.jsonrpc.UvmContext.shutdownBox(Ext.bind(function (result, exception) {
                                    if(exception) {
                                        Ext.MessageBox.alert(this.i18n._("Manual Shutdown Failure Warning"),Ext.String.format(this.i18n._("Error: Unable to shutdown {0} Server"), rpc.companyName));
                                    } else {
                                        Ext.MessageBox.wait(Ext.String.format(this.i18n._("The {0} Server is shutting down."), rpc.companyName), i18n._("Please wait"));
                                    }
                                }, this));
                            }
                         }, this));
                    }, this)
                }]
            },{
                xtype: "fieldset",
                title: this.i18n._("Setup Wizard"),
                items: [{
                    xtype: "component",
                    html: this.i18n._("Launch the Setup Wizard.")
                },{
                    xtype: "button",
                    margin: '5 0 0 0',
                    text: this.i18n._("Setup Wizard"),
                    name: "Setup Wizard",
                    iconCls: "reboot-icon",
                    handler: Ext.bind(function() {
                        Ext.MessageBox.confirm(this.i18n._("Setup Wizard Warning"),
                            Ext.String.format(this.i18n._("The Setup Wizard is about to be re-run.  This may reconfigure the {0} Server and {1}overwrite your current settings.{2}"), rpc.companyName, "<b>", "</b>" ),
                            Ext.bind(function(btn) {
                                if (btn == "yes") {
                                    Ung.Main.openSetupWizardScreen();
                                }
                            }, this));
                    }, this)
                }]
            },{
                xtype: "fieldset",
                title: this.i18n._("Factory Defaults"),
                items: [{
                    xtype: "component",
                    html: this.i18n._("Reset all settings to factory defaults.")
                },{
                    xtype: "button",
                    margin: '5 0 0 0',
                    text: this.i18n._("Reset to Factory Defaults"),
                    name: "Factory Defaults",
                    iconCls: "reboot-icon",
                    handler: Ext.bind(function() {
                        Ext.MessageBox.confirm(this.i18n._("Reset to Factory Defaults Warning"),
                           this.i18n._("This will RESET ALL SETTINGS to factory defaults. ALL current settings WILL BE LOST."),
                           Ext.bind(function(btn) {
                               if (btn == "yes") {
    
                                   Ung.MetricManager.stop();
    
                                   var resettingWindow=Ext.create('Ext.window.MessageBox', {
                                       minProgressWidth: 360
                                   });
                                   resettingWindow.wait(i18n._("Resetting to factory defaults..."), i18n._("Please wait"));
    
                                   Ung.Main.getExecManager().exec(Ext.bind(function(result, exception) {
                                       Ext.MessageBox.hide();
                                       var resettingWindow=Ext.create('Ext.window.MessageBox', {
                                           minProgressWidth: 360
                                       });
                                       resettingWindow.wait(i18n._("Resetting to factory defaults..."), i18n._("Please wait"), {
                                           interval: 500,
                                           increment: 120,
                                           duration: 45000,
                                           scope: this,
                                           fn: function() {
                                               console.log("Reset to factory defaults. Press ok to go to the Start Page...");
                                               Ext.MessageBox.hide();
                                               Ext.MessageBox.alert(
                                                   i18n._("Factory Defaults"),
                                                   i18n._("All settings have been reset to factory defaults."), Ung.Util.goToStartPage);
                                           }
                                       });
                                   }, this), "nohup /usr/share/untangle/bin/factory-defaults");
                               }
                           }, this));
                    }, this)
                }]
            }]
        });

    },
    buildBackup: function() {
        this.panelBackup = Ext.create('Ext.panel.Panel',{
            name: "Backup",
            helpSource: "system_backup",
            title: this.i18n._("Backup"),
            cls: "ung-panel",
            autoScroll: true,
            onBackupToFile: Ext.bind(function() {
                Ext.MessageBox.wait(i18n._("Downloading backup..."), i18n._("Please wait"));
                var downloadForm = document.getElementById('downloadForm');
                downloadForm["type"].value="backup";
                downloadForm.submit();
                Ext.MessageBox.hide();
            }, this),
            items: [{
                xtype: "fieldset",
                title: this.i18n._("Backup to File"),
                items: [{
                    xtype: "component",
                    html: this.i18n._("Backup can save the current system configuration to a file on your local computer for later restoration. The file name will end with \".backup\"") +
                            "<br> <br> " +
                            this.i18n._("After backing up your current system configuration to a file, you can then restore that configuration through this dialog by going to \"Restore\" -> \"From File\".")
                },{
                    xtype:"button",
                    margin: '10 0 0 0',
                    text: this.i18n._("Backup to File"),
                    name: "Backup to File",
                    handler: Ext.bind(function() {
                        this.panelBackup.onBackupToFile();
                    }, this)
                }]
            }]
        });

    },
    buildRestore: function() {
        this.panelRestore = Ext.create('Ext.panel.Panel',{
            name: "Restore",
            helpSource: "system_restore",
            title: this.i18n._("Restore"),
            cls: "ung-panel",
            autoScroll: true,
            onRestoreFromFile: Ext.bind(function() {
                var fileForm = this.panelRestore.down("form[name=upload_restore_file_form]");
                var fileField = fileForm.down("filefield");
                if (fileField.getValue().length === 0) {
                    Ext.MessageBox.alert(this.i18n._("Failed"), this.i18n._("Please select a file to upload."));
                    return;
                }
                fileForm.getForm().submit({
                    waitMsg: this.i18n._("Inspecting File..."),
                    success: Ext.bind(function(form, action) {
                        Ung.MetricManager.stop();
                        Ext.MessageBox.alert(this.i18n._("Restore"), action.result.msg, Ung.Util.goToStartPage);
                    }, this),
                    failure: Ext.bind(function(form, action) {
                        var errorMsg = this.i18n._("The File restore procedure failed.");
                        if (action.result && action.result.msg) {
                            errorMsg = action.result.msg;
                        }
                        Ext.MessageBox.alert(this.i18n._("Failed"), errorMsg);
                    }, this)
                });
            }, this),
            items: [{
                title: this.i18n._("Restore from File"),
                xtype: "fieldset",
                items: [{
                    xtype: "component",
                    html: this.i18n._("Restore can restore a previous system configuration to the server from a backup file on your local computer.  The backup file name ends with \".backup\"")
                }, {
                    xtype: "form",
                    margin: '20 0 0 0',
                    name: "upload_restore_file_form",
                    url: "upload",
                    border: false,
                    items: [{
                        xtype: "combo",
                        name: "argument",
                        fieldLabel: i18n._( "Restore Options" ),
                        width: 400,
                        store: [["",this.i18n._("Restore all settings.")], [".*/network.*",this.i18n._("Restore all except keep current network settings.")]],
                        value: "",
                        queryMode: 'local',
                        allowBlank: false,
                        editable: false
                    }, {
                        xtype: 'filefield',
                        margin: '10 0 0 0',
                        fieldLabel: this.i18n._("File"),
                        name: "file",
                        id: "upload_restore_file_textfield",
                        width: 500,
                        allowBlank: false
                    }, {
                        xtype: "button",
                        margin: '10 0 0 0',
                        text: this.i18n._("Restore from File"),
                        name: "Restore from File",
                        handler: Ext.bind(function() {
                            this.panelRestore.onRestoreFromFile();
                        }, this)
                    }, {
                        xtype: "hidden",
                        name: "type",
                        value: "restore"
                    }]
                }]
            }]
        });
    },
    buildProtocols: function() {
        var protocolSettingsItems = [];

        protocolSettingsItems.push({
            html: "<b>" + "<font color=\"red\">" + this.i18n._("Warning:") + "</font>&nbsp;" + this.i18n._("These settings should not be changed unless instructed to do so by support.") + "</b>"
        });

        if (this.isHttpLoaded()) {
            protocolSettingsItems.push({
                title: this.i18n._("HTTP"),
                items: [{
                    xtype: "radio",
                    boxLabel: this.i18n._("Enable processing of HTTP traffic.  (This is the default setting)"),
                    hideLabel: true,
                    name: "Web Override",
                    checked: this.getHttpSettings().enabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                this.getHttpSettings().enabled = checked;
                            }, this)
                        }
                    }
                },{
                    xtype: "radio",
                    boxLabel: this.i18n._("Disable processing of HTTP traffic."),
                    hideLabel: true,
                    name: "Web Override",
                    checked: !this.getHttpSettings().enabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                this.getHttpSettings().enabled = !checked;
                            }, this)
                        }
                    }
                }]
            });
        }

        if (this.isFtpLoaded()) {
            protocolSettingsItems.push({
                title: this.i18n._("FTP"),
                items: [{
                    xtype: "radio",
                    boxLabel: this.i18n._("Enable processing of FTP traffic.  (This is the default setting)"),
                    hideLabel: true,
                    name: "FTP",
                    checked: this.getFtpSettings().enabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                this.getFtpSettings().enabled = checked;
                            }, this)
                        }
                    }
                },{
                    xtype: "radio",
                    boxLabel: this.i18n._("Disable processing of FTP traffic."),
                    hideLabel: true,
                    name: "FTP",
                    checked: !this.getFtpSettings().enabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                this.getFtpSettings().enabled = !checked;
                            }, this)
                        }
                    }
                }]
            });
        }

        if (this.isMailLoaded()) {
            protocolSettingsItems.push({
                title: this.i18n._("SMTP"),
                labelWidth: 200,
                items: [{
                    xtype: "radio",
                    boxLabel: this.i18n._("Enable processing of SMTP traffic.  (This is the default setting)"),
                    hideLabel: true,
                    name: "SMTP",
                    checked: this.getSmtpNodeSettings().smtpEnabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                this.getSmtpNodeSettings().smtpEnabled = checked;
                            }, this)
                        }
                    }
                },{
                    xtype: "radio",
                    boxLabel: this.i18n._("Disable processing of SMTP traffic."),
                    hideLabel: true,
                    name: "SMTP",
                    checked: !this.getSmtpNodeSettings().smtpEnabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                this.getSmtpNodeSettings().smtpEnabled = !checked;
                            }, this)
                        }
                    }
                },{
                    xtype : "numberfield",
                    hidden: Ung.Util.hideDangerous,
                    fieldLabel : this.i18n._("SMTP timeout (seconds)"),
                    name : "SMTP timeout",
                    id: "system_protocolSettings_smtpTimeout",
                    value : this.getSmtpNodeSettings().smtpTimeout/1000,
                    width: 250,
                    allowDecimals: false,
                    allowNegative: false,
                    minValue: 0,
                    maxValue: 86400,
                    listeners : {
                        "change" : {
                            fn : Ext.bind(function(elem, newValue) {
                                this.getSmtpNodeSettings().smtpTimeout = newValue*1000;
                            }, this)
                        }
                    }
                },{
                    xtype : "radio",
                    hidden: Ung.Util.hideDangerous,
                    boxLabel : this.i18n._("Allow TLS encryption over SMTP."),
                    hideLabel : true,
                    name : "AllowTLS",
                    checked : this.getSmtpNodeSettings().smtpAllowTLS,
                    listeners : {
                        "check" : {
                            fn : Ext.bind(function(elem, checked) {
                                this.getSmtpNodeSettings().smtpAllowTLS = checked;
                            }, this)
                        }
                    }
                },{
                    xtype : "radio",
                    hidden: Ung.Util.hideDangerous,
                    boxLabel : this.i18n._("Stop TLS encryption over SMTP."),
                    hideLabel : true,
                    name : "AllowTLS",
                    checked : !this.getSmtpNodeSettings().smtpAllowTLS,
                    listeners : {
                        "check" : {
                            fn : Ext.bind(function(elem, checked) {
                                this.getSmtpNodeSettings().smtpAllowTLS = !checked;
                            }, this)
                        }
                    }
                }]
            });
        }

        this.panelProtocols = Ext.create('Ext.panel.Panel',{
            name: "Protocols",
            helpSource: "system_protocols",
            title: this.i18n._("Protocols"),
            cls: "ung-panel",
            autoScroll: true,
            defaults: {
                xtype: "fieldset"
            },
            items: protocolSettingsItems.length !== 0 ? protocolSettingsItems: null
        });
    },
    buildRegional: function() {
        // keep initial language settings
        this.initialLanguage = this.getLanguageSettings().language;
        var languagesList;
        try {
            languagesList = rpc.languageManager.getLanguagesList();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        var languagesStore =Ext.create("Ext.data.Store", {
            fields: ["code", "name"],
            data: languagesList.list
        });

        var timeZones = [];
        var timeZonesResult;
        try {
            timeZonesResult = rpc.adminManager.getTimeZones();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        var timeZoneData = eval(timeZonesResult);
        for (var i = 0; i < timeZoneData.length; i++) {
            timeZones.push([timeZoneData[i][0], "(" + timeZoneData[i][1] + ") " + timeZoneData[i][0]]);
        }
        this.panelRegional = Ext.create('Ext.panel.Panel',{
            name: "Regional",
            helpSource: "system_regional",
            title: this.i18n._("Regional"),
            cls: "ung-panel",
            autoScroll: true,
            defaults: {
                xtype: "fieldset"
            },
            items: [{
                title: this.i18n._("Current Time"),
                items: [{
                    xtype: 'component',
                    html: this.i18n._("Time is automatically synced via NTP")
                }, {
                    xtype: 'component',
                    margin: '10 0 0 0',
                    name: 'currentTime',
                    html: ".",
                    listeners: {
                        "afterrender": {
                            fn: Ext.bind(function(elem) {
                                this.timeUpdate();
                            }, this)
                        }
                    }
                }]
            }, {
                title: this.i18n._("Timezone"),
                items: [{
                    xtype: "combo",
                    name: "Timezone",
                    id: "system_timezone",
                    editable: false,
                    store: timeZones,
                    width: 350,
                    hideLabel: true,
                    queryMode: 'local',
                    value: this.getTimeZone(),
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.rpc.timeZone = newValue;
                            }, this)
                        }
                    }
                }]
            }, {
                title: this.i18n._("Language"),
                items: [{
                    id: "system_language_combo",
                    xtype: "combo",
                    name: "Language",
                    store: languagesStore,
                    forceSelection: true,
                    displayField: "name",
                    valueField: "code",
                    editable: false,
                    queryMode: 'local',
                    hideLabel: true,
                    listeners: {
                        "select": {
                            fn: Ext.bind(function(elem, record) {
                                this.getLanguageSettings().language = record.get("code");
                            }, this)
                        },
                        "afterrender": {
                            fn: Ext.bind(function(elem) {
                                languagesStore.load({
                                    callback: Ext.bind(function(r, options, success) {
                                        if (success) {
                                            var languageComboCmp = Ext.getCmp("system_language_combo");
                                            if (languageComboCmp) {
                                                languageComboCmp.setValue(this.getLanguageSettings().language);
                                                languageComboCmp.clearDirty();
                                            }
                                        }
                                    }, this)
                                });
                            }, this)
                        }
                    }
                }]
            }, {
                title: this.i18n._("Upload New Language Pack"),
                items: {
                    xtype: "form",
                    name: "upload_language_form",
                    url: "upload",
                    border: false,
                    items: [{
                        xtype: 'filefield',
                        fieldLabel:this.i18n._("File"),
                        width: 500,
                        labelWidth: 50,
                        name: 'file',
                        allowBlank: false
                    }, {
                        xtype: "hidden",
                        name: "type",
                        value: "language"
                    }, {
                        xtype: "button",
                        text: this.i18n._("Upload"),
                        name: "Upload",
                        handler: Ext.bind(function() {
                            this.panelRegional.onUpload();
                        }, this)
                    }]
                }
            }, {
                html: this.downloadLanguageHTML
            }, {
                xtype: "fieldset",
                title: this.i18n._("Force Sync Time"),
                items: [{
                    xtype: "component",
                    html: this.i18n._("Click to force instant time synchronization.")
                },{
                    xtype: "button",
                    margin: '10 0 0 0',
                    text: this.i18n._("Synchronize Time"),
                    name: "Setup Wizard",
                    iconCls: "reboot-icon",
                    handler: Ext.bind(function() {
                        Ext.MessageBox.confirm(
                            this.i18n._("Force Time Synchronization"),
                            this.i18n._("Forced time synchronization can cause problems if the current date is far in the future.") + "<br/>" +
                            this.i18n._("A reboot is suggested after time sychronization.") + "<br/>" + "<br/>" +
                            this.i18n._("Continue?"),
                            Ext.bind(function(btn) {
                                if (btn == "yes") {
                                    Ext.MessageBox.wait(this.i18n._("Syncing time with the internet..."), i18n._("Please wait"));
                                    rpc.jsonrpc.UvmContext.forceTimeSync(Ext.bind(function (result, exception) {
                                        if(Ung.Util.handleException(exception)) return;
                                        if(result !== 0) {
                                            Ext.MessageBox.hide();
                                            Ext.MessageBox.alert(this.i18n._("Warning"), this.i18n._("Time synchronization failed. Return code: ") + result);
                                        } else {
                                            Ext.MessageBox.hide();
                                        }
                                    }, this));
                                }
                           }, this));
                    }, this)
                }]
            }],
            onUpload: Ext.bind(function() {
                var languageForm = this.panelRegional.down("form[name=upload_language_form]");
                languageForm.getForm().submit({
                    waitMsg: this.i18n._("Please wait while your language pack is uploaded..."),
                    success: Ext.bind(function(form, action) {
                        var languagesList;
                        try {
                            languagesList = rpc.languageManager.getLanguagesList();
                        } catch (e) {
                            Ung.Util.rpcExHandler(e);
                        }

                        languagesStore.loadData(languagesList.list);
                        if(action.result.success===true) {
                            Ext.MessageBox.alert(this.i18n._("Succeeded"), this.i18n._("Upload language pack succeeded"));
                        } else {
                            var msg = "An error occured while uploading the language pack";
                            if(action.result.msg) {
                                msg = action.result.msg;
                            }
                            Ext.MessageBox.alert(this.i18n._("Warning"), this.i18n._(msg));
                        }
                    }, this),
                    failure: Ext.bind(function(form, action) {
                        var errorMsg = this.i18n._("Upload language pack failed");
                        if (action.result && action.result.msg) {
                            msg = action.result.msg;
                            if(msg === "Invalid Language Pack") {
                                errorMsg = this.i18n._("Invalid language pack; not a zip file");
                            } else if((/.*MO file.*/).test(msg)) {
                                errorMsg = this.i18n._("Couldn't compile MO file for entry" + " " + msg.split(" ").pop());
                            } else if((/.*bundle file.*/).test(msg)) {
                                errorMsg = this.i18n._("Couldn't compile resource bundle for entry" + " " + msg.split(" ").pop());
                            }
                        }
                        Ext.MessageBox.alert(this.i18n._("Failed"), errorMsg);
                    }, this)
                });
            }, this)
        });
    },
    timeUpdateId:-1,
    timeUpdate: function() {
        if(this.isVisible()) {
            rpc.adminManager.getDate(Ext.bind(function(result, exception) {
                if( exception != null ) return; // ignore exception
                var currentTimeObj = this.panelRegional.down('component[name="currentTime"]');
                if (currentTimeObj) {
                    currentTimeObj.update(result);
                    if ( this.timeUpdateId != -1) {
                        // Clear timeout for any previously defined deferred call.
                        // This is used because this function is called for updating the time after applying changes and without this multiple deffered calls will accumulate
                        clearTimeout(this.timeUpdateId);
                    }
                    //Updates every 10 seconds to decrease data trafic...
                    this.timeUpdateId = Ext.defer(this.timeUpdate, 10000, this);
                }
            }, this));
        }
    },
    buildShield: function() {
        var multiplierData = [
            [1, 1 + ' ' + this.i18n._("1 user")],
            [5, 5 + ' ' + this.i18n._("users")],
            [25, 25 + ' ' + this.i18n._("users")],
            [50, 50 + ' ' + this.i18n._("users")],
            [100, 100 + ' ' + this.i18n._("users")],
            [-1, this.i18n._("unlimited")]
        ];
        this.gridShieldRules = Ext.create( 'Ung.grid.Panel', {
            name: 'Shield Rules',
            settingsCmp: this,
            hasReorder: true,
            addAtTop: false,
            title: this.i18n._("Shield Rules"),
            dataExpression:'getShieldSettings().rules.list',
            recordJavaClass: "com.untangle.node.shield.ShieldRule",
            emptyRow: {
                "ruleId": -1,
                "enabled": true,
                "description": "",
                "multiplier": -1
            },
            fields: [{
                name: 'ruleId'
            }, {
                name: 'enabled'
            }, {
                name: 'matchers'
            }, {
                name: 'description'
            }, {
                name: 'multiplier'
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
            }, {
                header: this.i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex: 1,
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[no description]")
                }
            }, {
                header: this.i18n._("User Count"),
                width: 120,
                dataIndex: 'multiplier',
                renderer: function(value) {
                    for (var i = 0; i < multiplierData.length; i++) {
                        if (multiplierData[i][0] === value) {
                            return multiplierData[i][1];
                        }
                    }
                    return value;
                }
            }],
            rowEditorInputLines:[{
                xtype:'checkbox',
                dataIndex: "enabled",
                fieldLabel: this.i18n._("Enable Shield Rule")
            }, {
                xtype:'textfield',
                dataIndex: "description",
                emptyText: this.i18n._("[no description]"),
                fieldLabel: this.i18n._("Description"),
                width: 500
            }, {
                xtype:'fieldset',
                title: this.i18n._("If all of the following conditions are met:"),
                items:[{
                    xtype:'rulebuilder',
                    settingsCmp: this,
                    javaClass: "com.untangle.node.shield.ShieldRuleMatcher",
                    dataIndex: "matchers",
                    matchers: this.getShieldMatchers()
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('Perform the following action(s):'),
                items: [{
                    xtype: 'combo',
                    store: multiplierData,
                    dataIndex: "multiplier",
                    fieldLabel: this.i18n._("User Count"),
                    queryMode: 'local',
                    typeAhead: true
                }]
            }]
        });

        this.gridShieldEventLog = Ext.create('Ung.grid.EventLog',{
            eventQueriesFn: this.getShieldNode().getEventQueries,
            settingsCmp: this,
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'username'
            }, {
                name: 'shield_blocked'
            }, {
                name: 'c_client_addr',
                sortType: 'asIp'
            }, {
                name: 'c_client_port',
                sortType: 'asInt'
            }, {
                name: 'c_server_addr',
                sortType: 'asIp'
            }, {
                name: 's_server_port',
                sortType: 'asInt'
            }],
            columns: [{
                header: this.i18n._("Timestamp"),
                width: Ung.Util.timestampFieldWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                renderer: function(value) {
                    return i18n.timestampFormat(value);
                }
            }, {
                header: this.i18n._("Client"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 'c_client_addr'
            }, {
                header: this.i18n._("Client port"),
                width: Ung.Util.portFieldWidth,
                sortable: true,
                dataIndex: 'c_client_port'
            }, {
                header: this.i18n._("Username"),
                width: Ung.Util.usernameFieldWidth,
                sortable: true,
                dataIndex: 'username'
            }, {
                header: this.i18n._("Server"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 'c_server_addr'
            }, {
                header: this.i18n._("Server Port"),
                width: Ung.Util.portFieldWidth,
                sortable: true,
                dataIndex: 's_server_port'
            }, {
                header: this.i18n._("Action"),
                width: 150,
                sortable: true,
                dataIndex: 'shield_blocked',
                renderer: function(value) {
                    if (value)
                        return i18n._("blocked");
                    else
                        return i18n._("passed");
                }
            }]
        });

        this.panelShield = Ext.create('Ext.panel.Panel',{
            name: 'Shield',
            title: this.i18n._('Shield'),
            helpSource: "system_shield",
            cls: 'ung-panel',
            layout: { type: 'vbox', pack: 'start', align: 'stretch' },
            items: [{
                xtype: 'fieldset',
                flex: 0,
                title: this.i18n._("Shield Settings"),
                items:[{
                    xtype: 'checkbox',
                    boxLabel: this.i18n._("Enable Shield"),
                    hideLabel: true,
                    checked: this.getShieldSettings().shieldEnabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.getShieldSettings().shieldEnabled = newValue;
                            }, this)
                        }
                    }
                }]
            }, {
                xtype: 'tabpanel',
                activeTab: 0,
                deferredRender: false,
                autoHeight: true,
                flex: 1,
                items: [this.gridShieldRules, this.gridShieldEventLog]
            }]
        });
    },
    // validation function
    validate: function() {
        return true;
    },
    save: function (isApply) {
        this.saveSemaphore = 7;
        // save language settings
        rpc.languageManager.setLanguageSettings(Ext.bind(function(result, exception) {
            this.afterSave(exception, isApply);
        }, this), this.getLanguageSettings());

        // save network settings
        rpc.systemManager.setSettings(Ext.bind(function(result, exception) {
            this.afterSave(exception, isApply);
        }, this), this.getSystemSettings());

        // save http settings
        if (this.isHttpLoaded()) {
            this.getHttpNode().setHttpSettings(Ext.bind(function(result, exception) {
                this.afterSave(exception, isApply);
            }, this), this.getHttpSettings());
        } else {
            this.saveSemaphore--;
        }

        // save ftp settings
        if (this.isFtpLoaded()) {
            this.getFtpNode().setFtpSettings(Ext.bind(function(result, exception) {
                this.afterSave(exception, isApply);
            }, this), this.getFtpSettings());
        } else {
            this.saveSemaphore--;
        }

        // save mail settings
        if (this.isMailLoaded()) {
            this.getSmtpNode().setSmtpNodeSettings(Ext.bind(function(result, exception) {
                this.afterSave(exception, isApply);
            }, this), this.getSmtpNodeSettings());
        } else {
            this.saveSemaphore--;
        }

        // save shield settings
        if (this.isShieldLoaded()) {
            this.getShieldSettings().rules.list = this.gridShieldRules.getList();
            this.getShieldNode().setSettings(Ext.bind(function(result, exception) {
                this.afterSave(exception, isApply);
            }, this), this.getShieldSettings());
        } else {
            this.saveSemaphore--;
        }

        //save timezone
        rpc.adminManager.setTimeZone(Ext.bind(function(result, exception) {
            this.afterSave(exception, isApply);
            this.timeUpdate();
        }, this), this.rpc.timeZone);
    },
    afterSave: function(exception, isApply) {
        if(Ung.Util.handleException(exception)) return;

        this.saveSemaphore--;
        if (this.saveSemaphore === 0) {
            var needRefresh = this.initialLanguage != this.getLanguageSettings().language;
            if (needRefresh) {
                Ung.Util.goToStartPage();
                return;
            }
            if(isApply) {
                this.initialLanguage = this.getLanguageSettings().language;
                if (this.isShieldLoaded()) {
                    this.getShieldSettings(true);
                }
                this.clearDirty();
                Ext.MessageBox.hide();
            } else {
                Ext.MessageBox.hide();
                this.closeWindow();
            }
        }
    }
});
//# sourceURL=system.js