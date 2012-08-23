if (!Ung.hasResource["Ung.System"]) {
    Ung.hasResource["Ung.System"] = true;

    Ext.define("Ung.System", {
        extend: "Ung.ConfigWin",
        panelSupport: null,
        panelBackup: null,
        panelRestore: null,
        panelProtocolSettings: null,
        panelRegionalSettings: null,
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
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelSupport, this.panelBackup, this.panelRestore, this.panelProtocolSettings, this.panelRegionalSettings]);
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
        getAccessSettings: function(forceReload) {
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
        getMailNode: function(forceReload) {
            if (forceReload || this.rpc.mailNode === undefined) {
                try {
                    this.rpc.mailNode = rpc.nodeManager.node("untangle-casing-mail");
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
            }
            return this.rpc.mailNode;
        },
        isMailLoaded: function(forceReload) {
            return this.getMailNode(forceReload) != null;
        },
        getMailNodeSettings: function(forceReload) {
            if (forceReload || this.rpc.mailSettings === undefined) {
                try {
                    this.rpc.mailSettings = this.getMailNode(forceReload).getMailNodeSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
            }
            return this.rpc.mailSettings;
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
                        checked: this.getAccessSettings().supportEnabled,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getAccessSettings().supportEnabled = newValue;
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
                                Ext.String.format(this.i18n._("You are about to manually reboot.  This will interrupt normal network operations until the {0} Server is finished automatically restarting. This may take up to several minutes to complete."), this.companyName ),
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
                                Ext.String.format(this.i18n._("You are about to shutdown the {0} Server.  This will stop all network operations."), this.companyName ),
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
                               Ext.String.format(this.i18n._("You are about to re-run the Setup Wizard.  This may reconfigure the {0} Server and {1}overwrite your current settings.{2}"), this.companyName, "<b>", "</b>" ),
                               Ext.bind(function(btn) {
                                   if (btn == "yes") {
                                       main.showSetupWizardScreen();
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
                        html: this.i18n._("You can backup your current system configuration to a file on your local computer for later restoration, in the event that you would like to replace new settings with your current settings.  The file name will end with \".backup\"") +
                                "<br> <br> " +
                                this.i18n._("After backing up your current system configuration to a file, you can then restore that configuration through this dialog by going to \"Restore\" -> \"From Local File\".")
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
                onRestoreFromFileFile: function() {
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
                        waitMsg: cmp.i18n._("Please wait while Restoring..."),
                        success: function(form, action) {
                            var cmp = Ext.getCmp(action.parentId);
                            Ung.MessageManager.stop();
                            Ext.MessageBox.alert(cmp.i18n._("Restore In Progress"),
                                                 cmp.i18n._("The restore procedure is running. This may take several minutes. The server may be unavailable during this time. Once the process is complete you will be able to log in again."),
                                                 Ung.Util.goToStartPage);
                            },
                        failure: function(form, action) {
                            var cmp = Ext.getCmp(action.parentId);
                            var errorMsg = cmp.i18n._("The Local File restore procedure failed.");
                            if (action.result && action.result.msg) {
                                switch (action.result.msg) {
                                    case "File does not seem to be valid backup":
                                        errorMsg = Ext.String.format(cmp.i18n._("File does not seem to be valid {0} backup"), main.getBrandingManager().getCompanyName());
                                    break;
                                    case "Error in processing restore itself (yet file seems valid)":
                                        errorMsg = cmp.i18n._("Error in processing restore itself (yet file seems valid)");
                                    break;
                                    case "File is from an older version and cannot be used":
                                        errorMsg = Ext.String.format(cmp.i18n._("File is from an older version of {0} and cannot be used"), main.getBrandingManager().getCompanyName());
                                    break;
                                    case "Unknown error in local processing":
                                        errorMsg = cmp.i18n._("Unknown error in local processing");
                                    break;
                                    default:
                                        errorMsg = cmp.i18n._("The Local File restore procedure failed.");
                                }
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
                        html: this.i18n._("You can restore a previous system configuration from a backup file on your local computer.  The backup file name ends with \".backup\"")
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
                                this.panelRestore.onRestoreFromFileFile();
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
                html: this.i18n._("Warning: These settings should not be changed unless instructed to do so by support.")
            });

            if (this.isHttpLoaded()) {
                protocolSettingsItems.push({
                    xtype: "fieldset",
                    collapsible: true,
                    collapsed: true,
                    title: this.i18n._("HTTP"),
                    defaults: {
                        xtype: "fieldset"
                    },
                    items: [{
                        title: this.i18n._("Web Override"),
                        items: [{
                            xtype: "radio",
                            boxLabel: Ext.String.format(this.i18n._("{0}Enable Processing{1} of web traffic.  (This is the default setting)"), "<b>", "</b>"),
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
                            boxLabel: Ext.String.format(this.i18n._("{0}Disable Processing{1} of web traffic."), "<b>", "</b>"),
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
                    },{
                        title: this.i18n._("Long URIs"),
                        labelWidth: 250,
                        items: [{
                            xtype: "radio",
                            boxLabel: Ext.String.format(this.i18n._("{0}Enable Processing{1} of long URIs.  The traffic is considered \"Non-Http\".  (This is the default setting)"), "<b>", "</b>"),
                            hideLabel: true,
                            name: "Long URIs",
                            checked: !this.getHttpSettings().blockLongUris,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, checked) {
                                        this.getHttpSettings().blockLongUris = !checked;
                                    }, this)
                                }
                            }
                        },{
                            xtype: "radio",
                            boxLabel: Ext.String.format(this.i18n._("{0}Disable Processing{1} of long URIs."), "<b>", "</b>"),
                            hideLabel: true,
                            name: "Long URIs",
                            checked: this.getHttpSettings().blockLongUris,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, checked) {
                                        this.getHttpSettings().blockLongUris = checked;
                                    }, this)
                                }
                            }
                        },{
                            xtype: "numberfield",
                            fieldLabel: this.i18n._("Max URI Length (characters)"),
                            name: "Max URI Length",
                            id: "system_protocolSettings_maxUriLength",
                            value: this.getHttpSettings().maxUriLength,
                            labelWidth: 200,
                            allowDecimals: false,
                            allowNegative: false,
                            minValue: 1024,
                            maxValue: 4096,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, newValue) {
                                        this.getHttpSettings().maxUriLength = newValue;
                                    }, this)
                                }
                            }
                        }]
                    },{
                        title: this.i18n._("Long Headers"),
                        labelWidth: 250,
                        items: [{
                            xtype: "radio",
                            boxLabel: Ext.String.format(this.i18n._("{0}Enable Processing{1} of long headers.  The traffic is considered \"Non-Http\".  (This is the default setting)"), "<b>", "</b>"),
                            hideLabel: true,
                            name: "Long Headers",
                            checked: !this.getHttpSettings().blockLongHeaders,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, checked) {
                                        this.getHttpSettings().blockLongHeaders = !checked;
                                    }, this)
                                }
                            }
                        },{
                            xtype: "radio",
                            boxLabel: Ext.String.format(this.i18n._("{0}Disable Processing{1} of long headers."), "<b>", "</b>"),
                            hideLabel: true,
                            name: "Long Headers",
                            checked: this.getHttpSettings().blockLongHeaders,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, checked) {
                                        this.getHttpSettings().blockLongHeaders = checked;
                                    }, this)
                                }
                            }
                        },{
                            xtype: "numberfield",
                            fieldLabel: this.i18n._("Max Header Length (characters)"),
                            name: "Max Header Length",
                            id: "system_protocolSettings_maxHeaderLength",
                            value: this.getHttpSettings().maxHeaderLength,
                            labelWidth: 200,
                            allowDecimals: false,
                            allowNegative: false,
                            minValue: 1024,
                            maxValue: 8192,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, newValue) {
                                        this.getHttpSettings().maxHeaderLength = newValue;
                                    }, this)
                                }
                            }
                        }]
                    },{
                        title: this.i18n._("Non-Http Blocking"),
                        items: [{
                            xtype: "radio",
                            boxLabel: Ext.String.format(this.i18n._("{0}Allow{1} non-Http traffic to travel over port 80.  (This is the default setting)"), "<b>", "</b>"),
                            hideLabel: true,
                            name: "Non-Http Blocking",
                            checked: !this.getHttpSettings().nonHttpBlocked,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, checked) {
                                        this.getHttpSettings().nonHttpBlocked = !checked;
                                    }, this)
                                }
                            }
                        },{
                            xtype: "radio",
                            boxLabel: Ext.String.format(this.i18n._("{0}Stop{1} non-Http traffic to travel over port 80."), "<b>", "</b>"),
                            hideLabel: true,
                            name: "Non-Http Blocking",
                            checked: this.getHttpSettings().nonHttpBlocked,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, checked) {
                                        this.getHttpSettings().nonHttpBlocked = checked;
                                    }, this)
                                }
                            }
                        }]
                    }]
                });
            }

            if (this.isFtpLoaded()) {
                protocolSettingsItems.push({
                    collapsible: true,
                    collapsed: true,
                    title: this.i18n._("FTP"),
                    items: [{
                        xtype: "radio",
                        boxLabel: Ext.String.format(this.i18n._("{0}Enable Processing{1} of File Transfer traffic.  (This is the default setting)"), "<b>", "</b>"),
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
                        boxLabel: Ext.String.format(this.i18n._("{0}Disable Processing{1} of File Transfer traffic."), "<b>", "</b>"),
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
                    collapsible: true,
                    collapsed: true,
                    title: this.i18n._("SMTP"),
                    labelWidth: 200,
                    items: [{
                        xtype: "radio",
                        boxLabel: Ext.String.format(this.i18n._("{0}Enable SMTP{1} email processing.  (This is the default setting)"), "<b>", "</b>"),
                        hideLabel: true,
                        name: "SMTP",
                        checked: this.getMailNodeSettings().smtpEnabled,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    this.getMailNodeSettings().smtpEnabled = checked;
                                }, this)
                            }
                        }
                    },{
                        xtype: "radio",
                        boxLabel: Ext.String.format(this.i18n._("{0}Disable SMTP{1} email processing."), "<b>", "</b>"),
                        hideLabel: true,
                        name: "SMTP",
                        checked: !this.getMailNodeSettings().smtpEnabled,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    this.getMailNodeSettings().smtpEnabled = !checked;
                                }, this)
                            }
                        }
                    },{
                        xtype: "numberfield",
                        fieldLabel: this.i18n._("SMTP timeout (seconds)"),
                        name: "SMTP timeout",
                        id: "system_protocolSettings_smtpTimeout",
                        value: this.getMailNodeSettings().smtpTimeout/1000,
                        labelWidth: 200,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 0,
                        maxValue: 86400,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getMailNodeSettings().smtpTimeout = newValue*1000;
                                }, this)
                            }
                        }
                    },{
                        xtype: "radio",
                        boxLabel: Ext.String.format(this.i18n._("{0}Allow TLS{1} encryption over SMTP."), "<b>", "</b>"),
                        hideLabel: true,
                        name: "AllowTLS",
                        checked: this.getMailNodeSettings().smtpAllowTLS,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    this.getMailNodeSettings().smtpAllowTLS = checked;
                                }, this)
                            }
                        }
                    },{
                        xtype: "radio",
                        boxLabel: Ext.String.format(this.i18n._("{0}Stop TLS{1} encryption over SMTP.  (This is the default setting)"), "<b>", "</b>"),
                        hideLabel: true,
                        name: "AllowTLS",
                        checked: !this.getMailNodeSettings().smtpAllowTLS,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    this.getMailNodeSettings().smtpAllowTLS = !checked;
                                }, this)
                            }
                        }
                    }]
                });
                protocolSettingsItems.push({
                    collapsible: true,
                    collapsed: true,
                    xtype: "fieldset",
                    title: this.i18n._("POP3"),
                    labelWidth: 200,
                    items: [{
                        xtype: "radio",
                        boxLabel: Ext.String.format(this.i18n._("{0}Enable POP3{1} email processing.  (This is the default setting)"), "<b>", "</b>"),
                        hideLabel: true,
                        name: "POP3",
                        checked: this.getMailNodeSettings().popEnabled,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    this.getMailNodeSettings().popEnabled = checked;
                                }, this)
                            }
                        }
                    },{
                        xtype: "radio",
                        boxLabel: Ext.String.format(this.i18n._("{0}Disable POP3{1} email processing."), "<b>", "</b>"),
                        hideLabel: true,
                        name: "POP3",
                        checked: !this.getMailNodeSettings().popEnabled,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    this.getMailNodeSettings().popEnabled = !checked;
                                }, this)
                            }
                        }
                    },{
                        xtype: "numberfield",
                        fieldLabel: this.i18n._("POP3 timeout (seconds)"),
                        name: "POP3 timeout",
                        id: "system_protocolSettings_popTimeout",
                        value: this.getMailNodeSettings().popTimeout/1000,
                        labelWidth: 200,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 0,
                        maxValue: 86400,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getMailNodeSettings().popTimeout = newValue*1000;
                                }, this)
                            }
                        }
                    }]
                });
                protocolSettingsItems.push({
                    collapsible: true,
                    collapsed: true,
                    title: this.i18n._("IMAP"),
                    labelWidth: 200,
                    items: [{
                        xtype: "radio",
                        boxLabel: Ext.String.format(this.i18n._("{0}Enable IMAP{1} email processing.  (This is the default setting)"), "<b>", "</b>"),
                        hideLabel: true,
                        name: "IMAP",
                        checked: this.getMailNodeSettings().imapEnabled,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    this.getMailNodeSettings().imapEnabled = checked;
                                }, this)
                            }
                        }
                    },{
                        xtype: "radio",
                        boxLabel: Ext.String.format(this.i18n._("{0}Disable IMAP{1} email processing."), "<b>", "</b>"),
                        hideLabel: true,
                        name: "IMAP",
                        checked: !this.getMailNodeSettings().imapEnabled,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    this.getMailNodeSettings().imapEnabled = !checked;
                                }, this)
                            }
                        }
                    },{
                        xtype: "numberfield",
                        fieldLabel: this.i18n._("IMAP timeout (seconds)"),
                        name: "IMAP timeout",
                        id: "system_protocolSettings_imapTimeout",
                        value: this.getMailNodeSettings().imapTimeout/1000,
                        labelWidth: 200,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 0,
                        maxValue: 86400,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getMailNodeSettings().imapTimeout = newValue*1000;
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
                        triggerAction: "all",
                        listClass: "x-combo-list-small",
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
                        triggerAction: 'all',
                        listClass: "x-combo-list-small",
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
                        items: [
                        {
                          xtype: 'filefield',
                          fieldLabel:this.i18n._("File"),
                          width: 500,
                          size: 50,
                          labelWidth: 50,
                          name: 'file',
                          allowBlank: false,
                        },
                        {
                            xtype: "hidden",
                            name: "type",
                            value: "language"
                        },
                        {
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
                        Ext.defer(this.timeUpdate, 10000, this)
                    }
                }, this));
            }
        },
        // validation function
        validate: function() {
            //validate timeout
            return  (!this.isHttpLoaded() || this.validateMaxHeaderLength() && this.validateMaxUriLength()) &&
               (!this.isMailLoaded() || this.validateSMTP() && this.validatePOP() && this.validateIMAP());
        },
        //validate Max URI Length
        validateMaxUriLength: function() {
            var maxUriLengthCmp = Ext.getCmp("system_protocolSettings_maxUriLength");
            if (maxUriLengthCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert(this.i18n._("Warning"), this.i18n._("Max URI Length should be between 1024 and 4096!"),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelProtocolSettings);
                        maxUriLengthCmp.focus(true);
                    }, this)
                );
                return false;
            }
        },
        //validate Max Header Length
        validateMaxHeaderLength: function() {
            var maxHeaderLengthCmp = Ext.getCmp("system_protocolSettings_maxHeaderLength");
            if (maxHeaderLengthCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert(this.i18n._("Warning"), this.i18n._("Max Header Length should be between 1024 and 8192!"),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelProtocolSettings);
                        maxHeaderLengthCmp.focus(true);
                    }, this)
                );
                return false;
            }
        },
        //validate SMTP timeout
        validateSMTP: function() {
            var smtpTimeoutCmp = Ext.getCmp("system_protocolSettings_smtpTimeout");
            if (smtpTimeoutCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert(this.i18n._("Warning"), this.i18n._("SMTP timeout should be between 0 and 86400!"),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelProtocolSettings);
                        smtpTimeoutCmp.focus(true);
                    }, this)
                );
                return false;
            }
        },
        //validate POP timeout
        validatePOP: function() {
            var popTimeoutCmp = Ext.getCmp("system_protocolSettings_popTimeout");
            if (popTimeoutCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert(this.i18n._("Warning"), this.i18n._("POP timeout should be between 0 and 86400!"),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelProtocolSettings);
                        popTimeoutCmp.focus(true);
                    }, this)
                );
                return false;
            }
        },
        //validate IMAP timeout
        validateIMAP: function() {
            var imapTimeoutCmp = Ext.getCmp("system_protocolSettings_imapTimeout");
            if (imapTimeoutCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert(this.i18n._("Warning"), this.i18n._("IMAP timeout should be between 0 and 86400!"),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelProtocolSettings);
                        imapTimeoutCmp.focus(true);
                    }, this)
                );
                return false;
            }
        },
        save: function (isApply) {
            this.saveSemaphore = 6;
            // save language settings
            rpc.languageManager.setLanguageSettings(Ext.bind(function(result, exception) {
                this.afterSave(exception, isApply);
            }, this), this.getLanguageSettings());

            // save network settings
            rpc.systemManager.setSettings(Ext.bind(function(result, exception) {
                this.afterSave(exception, isApply);
            }, this), this.getAccessSettings());

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
                this.getMailNode().setMailNodeSettings(Ext.bind(function(result, exception) {
                    this.afterSave(exception, isApply);
                }, this), this.getMailNodeSettings());
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
