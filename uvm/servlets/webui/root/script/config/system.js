if (!Ung.hasResource["Ung.System"]) {
    Ung.hasResource["Ung.System"] = true;

    Ung.SystemUtil={
        getShieldMatchers: function (settingsCmp) {
            return [
{name:"DST_ADDR",displayName: settingsCmp.i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
                {name:"DST_PORT",displayName: settingsCmp.i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
                {name:"DST_INTF",displayName: settingsCmp.i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
                {name:"SRC_ADDR",displayName: settingsCmp.i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
                {name:"SRC_PORT",displayName: settingsCmp.i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: false},
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
    Ext.define("Ung.System", {
        extend: "Ung.ConfigWin",
        panelSupport: null,
        panelBackup: null,
        panelRestore: null,
        panelProtocolSettings: null,
        panelRegionalSettings: null,
        panelShieldSettings: null,
        initComponent: function() {
            this.breadcrumbs = [{
                title: i18n._("Configuration"),
                action: Ext.bind(function() {
                    this.cancelAction();
                }, this)
            }, {
                title: i18n._("System")
            }];
            this.companyName=main.getBrandingManager().getCompanyName();
            this.oemName=main.getOemManager().getOemName();
            if (this.oemName == "Untangle") {
                this.downloadLanguageHTML='<a href="http://pootle.untangle.com">' + i18n._("Download New Language Packs") + '</a>';
            } else {
                this.downloadLanguageHTML='';
            }
            this.buildSupport();
            this.buildBackup();
            this.buildRestore();
            this.buildProtocolSettings();
            this.buildRegionalSettings();
            this.buildShieldSettings();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelSupport, this.panelBackup, this.panelRestore, this.panelProtocolSettings, this.panelRegionalSettings, this.panelShieldSettings]);
            if (!this.isHttpLoaded() && !this.isFtpLoaded() && !this.isMailLoaded() ) {
                this.panelProtocolSettings.disable();
            }
            this.callParent(arguments);
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
            return this.getHttpNode(forceReload) != null;
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
            return this.getFtpNode(forceReload) != null;
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
            return this.getSmtpNode(forceReload) != null;
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
            return this.getShieldNode(forceReload) != null;
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
                    if ( tz != null && typeof ( tz ) != "string" ) {
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
                // private fields
                name: "Support",
                helpSource: "support",
                parentId: this.getId(),
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
                    }]
                },{
                    xtype: "fieldset",
                    title: this.i18n._("Manual Reboot"),
                    items: [{
                        border: false,
                        cls: "description",
                        html: Ext.String.format(this.i18n._("{0}Warning:{1} Clicking this button will reboot the {2} Server, temporarily interrupting network activity."),"<b>","</b>", this.companyName)
                    },{
                        xtype: "button",
                        text: this.i18n._("Reboot"),
                        name: "Manual Reboot",
                        iconCls: "reboot-icon",
                        handler: Ext.bind(function() {
                            Ext.MessageBox.confirm(this.i18n._("Manual Reboot Warning"),
                                Ext.String.format(this.i18n._("The server is about to manually reboot.  This will interrupt normal network operations until the {0} Server is finished automatically restarting. This may take up to several minutes to complete."), this.companyName ),
                                Ext.bind(function(btn) {
                                if (btn == "yes") {
                                    rpc.jsonrpc.UvmContext.rebootBox(Ext.bind(function (result, exception) {
                                        if(exception) {
                                            Ext.MessageBox.alert(this.i18n._("Manual Reboot Failure Warning"),Ext.String.format(this.i18n._("Error: Unable to reboot {0} Server"), this.companyName));
                                        } else {
                                            Ext.MessageBox.wait(Ext.String.format(this.i18n._("The {0} Server is rebooting."), this.companyName), i18n._("Please wait"));
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
                        border: false,
                        cls: "description",
                        html: Ext.String.format(this.i18n._("{0}Warning:{1} Clicking this button will shutdown the {2} Server, stopping all network activity."),"<b>","</b>", this.companyName)
                    },{
                        xtype: "button",
                        text: this.i18n._("Shutdown"),
                        name: "Manual Shutdown",
                        iconCls: "reboot-icon",
                        handler: Ext.bind(function() {
                            Ext.MessageBox.confirm(this.i18n._("Manual Shutdown Warning"),
                                Ext.String.format(this.i18n._("The server is about to shutdown the {0} Server.  This will stop all network operations."), this.companyName ),
                                Ext.bind(function(btn) {
                                if (btn == "yes") {
                                    rpc.jsonrpc.UvmContext.shutdownBox(Ext.bind(function (result, exception) {
                                        if(exception) {
                                            Ext.MessageBox.alert(this.i18n._("Manual Shutdown Failure Warning"),Ext.String.format(this.i18n._("Error: Unable to shutdown {0} Server"), this.companyName));
                                        } else {
                                            Ext.MessageBox.wait(Ext.String.format(this.i18n._("The {0} Server is shutting down."), this.companyName), i18n._("Please wait"));
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
                        border: false,
                        cls: "description",
                        html: this.i18n._("Clicking this button will launch the Setup Wizard.")
                    },{
                        xtype: "button",
                        text: this.i18n._("Setup Wizard"),
                        name: "Setup Wizard",
                        iconCls: "reboot-icon",
                        handler: Ext.bind(function() {
                            Ext.MessageBox.confirm(this.i18n._("Setup Wizard Warning"),
                               Ext.String.format(this.i18n._("The Setup Wizard is about to be re-run.  This may reconfigure the {0} Server and {1}overwrite your current settings.{2}"), this.companyName, "<b>", "</b>" ),
                               Ext.bind(function(btn) {
                                   if (btn == "yes") {
                                       main.openSetupWizardScreen();
                                   }
                               }, this));
                        }, this)
                    }]
                }]
            });

        },
        buildBackup: function() {
            this.panelBackup = Ext.create('Ext.panel.Panel',{
                // private fields
                name: "Backup",
                helpSource: "backup",
                parentId: this.getId(),
                title: this.i18n._("Backup"),
                cls: "ung-panel",
                autoScroll: true,
                onBackupToFile: Ext.bind(function() {
                    // A two step process: first asks the server for permission to download the file (the outer ajax request)
                    // and then if successful opens the iframe which initiates the download.
                    Ext.MessageBox.wait(this.i18n._("Generating Backup File..."), i18n._("Please wait"));
                    Ext.Ajax.request({
                        url: "backup",
                        params: {action:"requestBackup"},
                        success: function(response) {
                            try {
                                Ext.destroy(Ext.get("downloadIframe"));
                            }
                            catch(e) {}
                            Ext.DomHelper.append(document.body, {
                                tag: "iframe",
                                id:"downloadIframe",
                                frameBorder: 0,
                                width: 0,
                                height: 0,
                                css: "display:none;visibility:hidden;height:0px;",
                                src: "backup?action=initiateDownload"
                            });
                            Ext.MessageBox.hide();
                        },
                        failure: function() {
                            Ext.MessageBox.alert(this.i18n._("Backup Failure Warning"), this.i18n._("Error:  The local file backup procedure failed.  Please try again."));
                        }
                    });
                }, this),
                items: [{
                    xtype: "fieldset",
                    title: this.i18n._("Backup to File"),
                    items: [{
                        border: false,
                        cls: "description",
                        html: this.i18n._("Backup can save the current system configuration to a file on your local computer for later restoration. The file name will end with \".backup\"") +
                                "<br> <br> " +
                                this.i18n._("After backing up your current system configuration to a file, you can then restore that configuration through this dialog by going to \"Restore\" -> \"From File\".")
                    },{
                        xtype:"button",
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
                // private fields
                name: "Restore",
                helpSource: "restore",
                parentId: this.getId(),
                title: this.i18n._("Restore"),
                cls: "ung-panel",
                autoScroll: true,
                onRestoreFromFile: function() {
                    var prova = Ext.getCmp("upload_restore_file_form");
                    var cmp = Ext.getCmp(this.parentId);
                    var fileText = prova.items.get(0);
                    if (fileText.getValue().length == 0) {
                        Ext.MessageBox.alert(cmp.i18n._("Failed"), cmp.i18n._("Please select a file to upload."));
                        return false;
                    }
                    var form = prova.getForm();
                    form.submit({
                        parentId: cmp.getId(),
                        waitMsg: cmp.i18n._("Inspecting File..."),
                        success: function(form, action) {
                            var cmp = Ext.getCmp(action.parentId);
                            Ung.MessageManager.stop();
                            Ext.MessageBox.alert(cmp.i18n._("Restore"), action.result.msg, Ung.Util.goToStartPage);
                        },
                        failure: function(form, action) {
                            var cmp = Ext.getCmp(action.parentId);
                            var errorMsg = cmp.i18n._("The File restore procedure failed.");
                            if (action.result && action.result.msg) {
                                errorMsg = action.result.msg;
                            }

                            if ( errorMsg.indexOf("NEED_TO_INSTALL:") !== -1 ) {
                                var neededPkgs = errorMsg.replace("NEED_TO_INSTALL:","").split(",");
                                var neededPkgsStr = neededPkgs.join("<br/>");
                                
                                Ext.MessageBox.confirm(cmp.i18n._("Alert"),
                                                       cmp.i18n._("Missing packages are required to restore this backup file.") + "<br/>" +
                                                       cmp.i18n._("Download required packages now?") + "<br/><br/>" +
                                                       cmp.i18n._("Packages") + ":<br/>" + neededPkgsStr,
                                                       Ext.bind(function(btn) {
                                                           if (btn == "yes") {
                                                               Ext.MessageBox.hide();
                                                               this.neededPackages = neededPkgs.length;
                                                               Ext.MessageBox.wait(i18n._("Downloading packages..."), i18n._("Please wait"));

                                                               for (var i = 0; i < neededPkgs.length; i++) {
                                                                   var pkgName = neededPkgs[i];

                                                                   var restoreFn = Ext.bind( function() {
                                                                       this.neededPackages--;

                                                                       if ( this.neededPackages == 0 ) {
                                                                           Ext.MessageBox.alert(cmp.i18n._("Download Complete"), i18n._("To continue the restore relaunch the restore process."));
                                                                       }
                                                                   }, this);

                                                                   Ung.MessageManager.setModalDownloadMode( null, restoreFn );

                                                                   console.log("Installing: " + pkgName);
                                                                   rpc.aptManager.install(Ext.bind(function(result, exception) {
                                                                       if(Ung.Util.handleException(exception)) return;
                                                                   }, this), pkgName);
                                                               }
                                                           }
                                                       }, this));
                                return;
                            }
                                
                            Ext.MessageBox.alert(cmp.i18n._("Failed"), errorMsg);
                        }
                    });
                    return true;
                },
                items: [{
                    title: this.i18n._("From File"),
                    xtype: "fieldset",
                    items: [{
                        border: false,
                        cls: "description",
                        html: this.i18n._("Restore can restore a previous system configuration to the server from a backup file on your local computer.  The backup file name ends with \".backup\"")
                    },{
                        xtype: "form",
                        id: "upload_restore_file_form",
                        url: "upload",
                        border: false,
                        items: [{
                            xtype: 'filefield',
                            fieldLabel: this.i18n._("File"),
                            name: "file",
                            id: "upload_restore_file_textfield",
                            width: 500,
                            size: 50,
                            labelWidth: 50,
                            allowBlank: false
                        }, {
                            xtype: "button",
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
        buildProtocolSettings: function() {
            var protocolSettingsItems = [];

            protocolSettingsItems.push({
                border: false,
                cls: "description",
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
                    }]
                });
            }

            this.panelProtocolSettings = Ext.create('Ext.panel.Panel',{
                name: "Protocol Settings",
                helpSource: "protocol_settings",
                // private fields
                parentId: this.getId(),

                title: this.i18n._("Protocol Settings"),
                cls: "ung-panel",
                autoScroll: true,
                defaults: {
                    xtype: "fieldset"
                },
                items: protocolSettingsItems.length != 0 ? protocolSettingsItems: null
            });
        },
        buildRegionalSettings: function() {
            // keep initial language settings
            this.initialLanguage = this.getLanguageSettings().language;
            var languagesStore =Ext.create("Ext.data.Store", {
                fields: ["code", "name"],
                data: rpc.languageManager.getLanguagesList().list
            });

            var timeZones = [];
            for (var i = 0; i < Ung.TimeZoneData.length; i++) {
                timeZones.push([Ung.TimeZoneData[i][2], "(" + Ung.TimeZoneData[i][0] + ") " + Ung.TimeZoneData[i][1]]);
            }
            this.panelRegionalSettings = Ext.create('Ext.panel.Panel',{
                // private fields
                name: "Regional Settings",
                helpSource: "regional_settings",
                parentId: this.getId(),
                title: this.i18n._("Regional Settings"),
                cls: "ung-panel",
                autoScroll: true,
                defaults: {
                    xtype: "fieldset",
                    buttonAlign: "left"
                },
                items: [{
                    title: this.i18n._("Current Time"),
                    defaults: {
                        border: false,
                        cls: "description"
                    },
                    items: [{
                        html: this.i18n._("time is automatically synced via NTP")
                    }, {
                        id: "system_regionalSettings_currentTime",
                        html: ".",//rpc.adminManager.getDate(),
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
                        selectOnFocus: true,
                        hideLabel: true,
                        listeners: {
                            "select": {
                                fn: Ext.bind(function(elem, record) {
                                    this.getLanguageSettings().language = record[0].data.code;
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
                        id: "upload_language_form",
                        url: "upload",
                        border: false,
                        items: [{
                            xtype: 'filefield',
                            fieldLabel:this.i18n._("File"),
                            width: 500,
                            size: 50,
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
                                this.panelRegionalSettings.onUpload();
                            }, this)
                        }]
                    }
                }, {
                    html: this.downloadLanguageHTML
                }, {
                    xtype: "fieldset",
                    title: this.i18n._("Force Sync Time"),
                    items: [{
                        border: false,
                        cls: "description",
                        html: this.i18n._("Click to force instant time synchronization.")
                    },{
                        xtype: "button",
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
                            
                                            if(result != 0) {
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
                onUpload: function() {
                    var prova = Ext.getCmp("upload_language_form");
                    var cmp = Ext.getCmp(this.parentId);

                    var form = prova.getForm();
                    form.submit({
                        parentId: cmp.getId(),
                        waitMsg: cmp.i18n._("Please wait while your language pack is uploaded..."),
                        success: function(form, action) {
                            languagesStore.loadData(rpc.languageManager.getLanguagesList().list);
                            var cmp = Ext.getCmp(action.parentId);
                            if(action.result.success===true) {
                                Ext.MessageBox.alert(cmp.i18n._("Succeeded"), cmp.i18n._("Upload language pack succeeded"));
                            } else {
                                var msg = "An error occured while uploading the language pack";
                                if(action.result.msg) {
                                    msg = action.result.msg;
                                }
                                Ext.MessageBox.alert(cmp.i18n._("Warning"), cmp.i18n._(msg));
                            }
                        },
                        failure: function(form, action) {
                            var cmp = Ext.getCmp(action.parentId);
                            var errorMsg = cmp.i18n._("Upload language pack failed");
                            if (action.result && action.result.msg) {
                                msg = action.result.msg;
                                switch (true) {
                                    case (msg === "Invalid Language Pack"):
                                        errorMsg = cmp.i18n._("Invalid language pack; not a zip file");
                                    break;
                                    case ((/.*MO file.*/).test(msg)):
                                        errorMsg = cmp.i18n._("Couldn't compile MO file for entry" + " " + msg.split(" ").pop());
                                    break;
                                    case ((/.*bundle file.*/).test(msg)):
                                        errorMsg = cmp.i18n._("Couldn't compile resource bundle for entry" + " " + msg.split(" ").pop());
                                    break;
                                    default:
                                        errorMsg = cmp.i18n._("Upload language pack failed");
                                }
                            }
                            Ext.MessageBox.alert(cmp.i18n._("Failed"), errorMsg);
                        }
                    });
                }
            });
        },
        timeUpdate: function() {
            if(!this.isVisible())
                return;
            else {
                rpc.adminManager.getDate(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    var currentTimeObj = Ext.getCmp("system_regionalSettings_currentTime");
                    if (currentTimeObj != null && currentTimeObj.body != null) {
                        currentTimeObj.body.update(result);
                        //Updates every 10 seconds to decrease data trafic...
                        Ext.defer(this.timeUpdate, 10000, this);
                    }
                }, this));
            }
        },
        buildShieldSettings: function() {
            this.gridShieldRules = Ext.create( 'Ung.EditorGrid', {
                name: 'Shield Rules',
                settingsCmp: this,
                paginated: false,
                hasReorder: true,
                addAtTop: false,
                emptyRow: {
                    "ruleId": -1,
                    "enabled": true,
                    "description": this.i18n._("[no description]"),
                    "multiplier": 1,
                    "javaClass": "com.untangle.node.shield.ShieldRule"
                },
                title: this.i18n._("Forward Filter Rules"),
                recordJavaClass: "com.untangle.node.shield.ShieldRule",
                dataExpression:'getShieldSettings().rules.list',
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
                        allowBlank:false
                    }
                }, {
                    xtype:'checkcolumn',
                    header: this.i18n._("Block"),
                    dataIndex: 'blocked',
                    resizable: false,
                    width:55
                }],
                columnsDefaultSortable: false,
                rowEditorInputLines:[{
                    xtype:'checkbox',
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Enable Shield Rule")
                }, {
                    xtype:'textfield',
                    dataIndex: "description",
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
                        matchers: Ung.SystemUtil.getShieldMatchers(this)
                    }]
                }, {
                    xtype: 'fieldset',
                    cls:'description',
                    title: i18n._('Perform the following action(s):'),
                    border: false,
                    items: [{
                        xtype: "numberfield",
                        dataIndex: "multiplier",
                        fieldLabel: this.i18n._("Multiplier")
                    }]
                }]
            });
            
            this.gridShieldEventLog = Ext.create('Ung.GridEventLog',{
                eventQueriesFn: this.getShieldNode().getEventQueries,
                settingsCmp: this,
                fields: [{
                    name: 'time_stamp',
                    sortType: Ung.SortTypes.asTimestamp
                }, {
                    name: 'uid'
                }, {
                    name: 'client',
                    mapping: 'c_client_addr'
                }, {
                    name: 'clientPort',
                    mapping: 'c_client_port'
                }, {
                    name: 'server',
                    mapping: 'c_server_addr'
                }, {
                    name: 'serverPort',
                    mapping: 's_server_port'
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
                    dataIndex: 'client'
                }, {
                    header: this.i18n._("Client port"),
                    width: Ung.Util.portFieldWidth,
                    sortable: true,
                    dataIndex: 'clientPort'
                }, {
                    header: this.i18n._("Username"),
                    width: Ung.Util.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'uid'
                }, {
                    header: this.i18n._("Server"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'server'
                }, {
                    header: this.i18n._("Server Port"),
                    width: Ung.Util.portFieldWidth, 
                    sortable: true,
                    dataIndex: 'serverPort'
                }]
            });

            this.panelShieldSettings = Ext.create('Ext.panel.Panel',{
                parentId: this.getId(),
                title: this.i18n._('Shield Settings'),
                cls: 'ung-panel',
                layout: { type: 'vbox', pack: 'start', align: 'stretch' },
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    flex: 0,
                    title: this.i18n._("Shield settings"),
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
                this.getShieldSettings().rules.list = this.gridShieldRules.getPageList();
                this.getShieldNode().setSettings(Ext.bind(function(result, exception) {
                    this.afterSave(exception, isApply);
                }, this), this.getShieldSettings());
            } else {
                this.saveSemaphore--;
            }

            //save timezone
            rpc.adminManager.setTimeZone(Ext.bind(function(result, exception) {
                this.afterSave(exception, isApply);
            }, this), this.rpc.timeZone);
        },
        afterSave: function(exception, isApply) {
            if(Ung.Util.handleException(exception)) return;

            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
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
}
//@ sourceURL=system.js
