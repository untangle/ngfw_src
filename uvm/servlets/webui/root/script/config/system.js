if (!Ung.hasResource["Ung.System"]) {
    Ung.hasResource["Ung.System"] = true;

    Ung.System = Ext.extend(Ung.ConfigWin, {
        panelSupport : null,
        panelBackup : null,
        panelRestore : null,
        panelProtocolSettings : null,
        panelRegionalSettings : null,
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }, {
                title : i18n._("System")
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
            this.tabs.activate(this.panelSupport);
            if (!this.isHttpLoaded() && !this.isFtpLoaded() && !this.isMailLoaded() ){
                this.panelProtocolSettings.disable();
            }
            Ung.System.superclass.initComponent.call(this);
        },

        // get languange settings object
        getLanguageSettings : function(forceReload) {
            if (forceReload || this.rpc.languageSettings === undefined) {
                try {
                    this.rpc.languageSettings = rpc.languageManager.getLanguageSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                
            }
            return this.rpc.languageSettings;
        },
        getAccessSettings : function(forceReload) {
            if (forceReload || this.rpc.accessSettings === undefined) {
                try {
                    this.rpc.accessSettings = rpc.networkManager.getAccessSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                
            }
            return this.rpc.accessSettings;
        },
        getMiscSettings : function(forceReload) {
            if (forceReload || this.rpc.miscSettings === undefined) {
                try {
                    this.rpc.miscSettings = rpc.networkManager.getMiscSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                
            }
            return this.rpc.miscSettings;
        },
        getHttpNode : function(forceReload) {
            if (forceReload || this.rpc.httpNode === undefined) {
                try {
                    this.rpc.httpNode = rpc.nodeManager.node("untangle-casing-http");
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                
            }
            return this.rpc.httpNode;
        },
        isHttpLoaded : function(forceReload) {
            return this.getHttpNode(forceReload) != null;
        },
        getHttpSettings : function(forceReload) {
            if (forceReload || this.rpc.httpSettings === undefined) {
                try {
                    this.rpc.httpSettings = this.getHttpNode(forceReload).getHttpSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                
            }
            return this.rpc.httpSettings;
        },
        getFtpNode : function(forceReload) {
            if (forceReload || this.rpc.ftpNode === undefined) {
                try {
                    this.rpc.ftpNode = rpc.nodeManager.node("untangle-casing-ftp");
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                    
            }
            return this.rpc.ftpNode;
        },
        isFtpLoaded : function(forceReload) {
            return this.getFtpNode(forceReload) != null;
        },
        getFtpSettings : function(forceReload) {
            if (forceReload || this.rpc.ftpSettings === undefined) {
                try {
                    this.rpc.ftpSettings = this.getFtpNode(forceReload).getFtpSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                
            }
            return this.rpc.ftpSettings;
        },
        getMailNode : function(forceReload) {
            if (forceReload || this.rpc.mailNode === undefined) {
                try {
                    this.rpc.mailNode = rpc.nodeManager.node("untangle-casing-mail");
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                    
            }
            return this.rpc.mailNode;
        },
        isMailLoaded : function(forceReload) {
            return this.getMailNode(forceReload) != null;
        },
        getMailNodeSettings : function(forceReload) {
            if (forceReload || this.rpc.mailSettings === undefined) {
                try {
                    this.rpc.mailSettings = this.getMailNode(forceReload).getMailNodeSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                    
            }
            return this.rpc.mailSettings;
        },
        getTimeZone : function(forceReload) {
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
        buildSupport : function() {
            // keep initial settings
            this.initialAccessSettings = Ung.Util.clone(this.getAccessSettings());
            this.initialMiscSettings = Ung.Util.clone(this.getMiscSettings());
            
            this.panelSupport = new Ext.Panel({
                // private fields
                name : "Support",
                helpSource : "support",
                parentId : this.getId(),
                title : this.i18n._("Support"),
                layout : "form",
                cls: "ung-panel",
                autoScroll : true,
                items : [{
                    xtype : "fieldset",
                    autoHeight : true,
                    title : this.i18n._("Support"),
                    items : [{
                        xtype : "checkbox",
                        name : "Allow secure access to your server for support purposes",
                        boxLabel : String.format(this.i18n._("{0}Allow{1} secure access to your server for support purposes."), "<b>", "</b>"),
                        hideLabel : true,
                        checked : this.getAccessSettings().isSupportEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getAccessSettings().isSupportEnabled = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : "checkbox",
                        name : "Send data about your server for support purposes",
                        boxLabel : String.format(this.i18n
                                ._("{0}Send{1} data about your server. This will send status updates and alerts if any unexpected problems occur, but will not allow support access to your server."),
                                "<b>", "</b>"),
                        hideLabel : true,
                        checked : this.getMiscSettings().isExceptionReportingEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getMiscSettings().isExceptionReportingEnabled = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                },{
                    xtype : "fieldset",
                    autoHeight : true,
                    title : this.i18n._("Manual Reboot"),
                    buttonAlign: "left",
                    items : [{
                        border: false,
                        cls: "description",
                        html: String.format(this.i18n._("{0}Warning:{1} Clicking this button will reboot the {2} Server, temporarily interrupting network activity."),"<b>","</b>",this.companyName)                      
                    }],
                    buttons: [{
                        xtype : "button",
                        text : this.i18n._("Reboot"),
                        name : "Manual Reboot",
                        iconCls : "reboot-icon",
                        handler : function() {
                            Ext.MessageBox.confirm(this.i18n._("Manual Reboot Warning"),
                                String.format(this.i18n._("You are about to manually reboot.  This will interrupt normal network operations until the {0} Server is finished automatically restarting. This may take up to several minutes to complete."), this.companyName ), 
                                function(btn) {
                                if (btn == "yes") {
                                    rpc.jsonrpc.RemoteUvmContext.rebootBox(function (result, exception) {
                                        if(exception) {
                                            Ext.MessageBox.alert(this.i18n._("Manual Reboot Failure Warning"),String.format(this.i18n._("Error: Unable to reboot {0} Server"),this.companyName)); 
                                        } else {
                                            Ext.MessageBox.wait(String.format(this.i18n._("The {0} Server is rebooting."),this.companyName), i18n._("Please wait"));
                                        }
                                    }.createDelegate(this));
                                }
                             }.createDelegate(this));
                        }.createDelegate(this)
                    }]
                },{
                    xtype : "fieldset",
                    autoHeight : true,
                    title : this.i18n._("Manual Shutdown"),
                    buttonAlign: "left",
                    items : [{
                        border: false,
                        cls: "description",
                        html: String.format(this.i18n._("{0}Warning:{1} Clicking this button will shutdown the {2} Server, stopping all network activity."),"<b>","</b>",this.companyName)                      
                    }],
                    buttons: [{
                        xtype : "button",
                        text : this.i18n._("Shutdown"),
                        name : "Manual Shutdown",
                        iconCls : "reboot-icon",
                        handler : function() {
                            Ext.MessageBox.confirm(this.i18n._("Manual Shutdown Warning"),
                                String.format(this.i18n._("You are about to shutdown the {0} Server.  This will stop all network operations."), this.companyName ), 
                                function(btn) {
                                if (btn == "yes") {
                                    rpc.jsonrpc.RemoteUvmContext.shutdownBox(function (result, exception) {
                                        if(exception) {
                                            Ext.MessageBox.alert(this.i18n._("Manual Shutdown Failure Warning"),String.format(this.i18n._("Error: Unable to shutdown {0} Server"),this.companyName)); 
                                        } else {
                                            Ext.MessageBox.wait(String.format(this.i18n._("The {0} Server is shutting down."),this.companyName), i18n._("Please wait"));
                                        }
                                    }.createDelegate(this));
                                }
                             }.createDelegate(this));
                        }.createDelegate(this)
                    }]
                },{
                    xtype : "fieldset",
                    autoHeight : true,
                    title : this.i18n._("Setup Wizard"),
                    buttonAlign: "left",
                    items : [{
                        border: false,
                        cls: "description",
                        html: this.i18n._("Clicking this button will launch the Setup Wizard.")
                    }],
                    buttons: [{
                        xtype : "button",
                        text : this.i18n._("Setup Wizard"),
                        name : "Setup Wizard",
                        iconCls : "reboot-icon",
                        handler : function() {
                            Ext.MessageBox.confirm(this.i18n._("Setup Wizard Warning"),
                                String.format(this.i18n._("You are about to re-run the Setup Wizard.  This may reconfigure the {0} Server and {1}overwrite your current settings.{2}"), this.companyName, "<b>", "</b>" ), 
                                function(btn) {
                                if (btn == "yes") {
                                    main.showSetupWizardScreen();
                                }
                             }.createDelegate(this));
                        }.createDelegate(this)
                    }]
                }]
            });

        },
        buildBackup : function() {
            this.panelBackup = new Ext.Panel({
                // private fields
                name : "Backup",
                helpSource : "backup",
                parentId : this.getId(),
                title : this.i18n._("Backup"),
                layout : "form",
                cls: "ung-panel",
                autoScroll : true,
                onBackupToFile: function() {
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
                            Ext.MessageBox.alert(this.i18n._("Backup Failure Warning"),this.i18n._("Error:  The local file backup procedure failed.  Please try again.")); 
                        }
                    });                    
                }.createDelegate(this),
                defaults : {
                    xtype : "fieldset",
                    autoHeight : true,
                    buttonAlign : "left"
                },
                items : [{
                    title : this.i18n._("Backup to File"),
                    items : [{
                        border : false,
                        cls: "description",
                        html: this.i18n._("You can backup your current system configuration to a file on your local computer for later restoration, in the event that you would like to replace new settings with your current settings.  The file name will end with \".backup\"") +
                                "<br> <br> " +
                                this.i18n._("After backing up your current system configuration to a file, you can then restore that configuration through this dialog by going to \"Restore\" -> \"From Local File\".")
                    }],
                    buttons : [{
                        text : this.i18n._("Backup to File"),
                        name: "Backup to File",
                        handler : function() {
                            this.panelBackup.onBackupToFile();
                        }.createDelegate(this)
                    }]
                }]
            });

        },
        buildRestore : function() {
            this.panelRestore = new Ext.Panel({
                // private fields
                name : "Restore",
                helpSource : "restore",
                parentId : this.getId(),
                title : this.i18n._("Restore"),
                layout : "form",
                cls: "ung-panel",
                autoScroll : true,
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
                        parentId : cmp.getId(),
                        waitMsg : cmp.i18n._("Please wait while Restoring..."),
                        success : function(form, action) {
                            var cmp = Ext.getCmp(action.options.parentId);
                            Ung.MessageManager.stop();
                            Ext.MessageBox.alert(cmp.i18n._("Restore In Progress"),
                         cmp.i18n._("The restore procedure is running. The server may be unavailable during this time. Once the process is complete you will be able to log in again."), 
                         Ung.Util.goToStartPage);
                            },
                        failure : function(form, action) {
                            var cmp = Ext.getCmp(action.options.parentId);
                            var errorMsg = cmp.i18n._("The Local File restore procedure failed.");
                            if (action.result && action.result.msg) {
                                switch (action.result.msg) {
                                    case "File does not seem to be valid backup" : 
                                        errorMsg = String.format(cmp.i18n._("File does not seem to be valid {0} backup"), main.getBrandingManager().getCompanyName());
                                    break;
                                    case "Error in processing restore itself (yet file seems valid)" : 
                                        errorMsg = cmp.i18n._("Error in processing restore itself (yet file seems valid)");
                                    break;
                                    case "File is from an older version and cannot be used" : 
                                        errorMsg = String.format(cmp.i18n._("File is from an older version of {0} and cannot be used"), main.getBrandingManager().getCompanyName());
                                    break;
                                    case "Unknown error in local processing" : 
                                        errorMsg = cmp.i18n._("Unknown error in local processing");
                                    break;
                                    default :
                                        errorMsg = cmp.i18n._("The Local File restore procedure failed.");
                                }
                            }
                            Ext.MessageBox.alert(cmp.i18n._("Failed"), errorMsg);
                        }
                    });
                },
                defaults : {
                    xtype : "fieldset",
                    autoHeight : true,
                    buttonAlign : "left"
                },
                items : [{
                    title : this.i18n._("From File"),
                    items : [{
                        border : false,
                        cls: "description",
                        html: this.i18n._("You can restore a previous system configuration from a backup file on your local computer.  The backup file name ends with \".backup\"")
                    },{
                        fileUpload : true,
                        xtype : "form",
                        id : "upload_restore_file_form",
                        url : "upload",
                        border : false,
                        items : [{
                            fieldLabel : this.i18n._("File"),
                            name : "file",
                            id : "upload_restore_file_textfield",
                            inputType : "file",
                            xtype : "textfield",
                            allowBlank : false
                        }, {
                            xtype : "button",
                            text : this.i18n._("Restore from File"),
                            name: "Restore from File",
                            handler : function() {
                                this.panelRestore.onRestoreFromFileFile();
                            }.createDelegate(this)
                        }, {
                            xtype : "hidden",
                            name : "type",
                            value : "restore"
                        }]
                    }]
                }]
            });
        },
        buildProtocolSettings : function()
        {
            var protocolSettingsItems = [];
            
            protocolSettingsItems.push({
                autoHeight : true,
                border: false,
                cls: "description",
                html: this.i18n._("Warning: These settings should not be changed unless instructed to do so by support.")
            });
            
            if (this.isHttpLoaded()) {
                // keep initial http settings
                this.initialHttpSettings = Ung.Util.clone(this.getHttpSettings());
                
                protocolSettingsItems.push({
                    xtype : "fieldset",
                    collapsible: true,
                    collapsed: true,
                    title: this.i18n._("HTTP"),
                    autoHeight : true,
                    defaults : {
                        xtype : "fieldset",
                        autoHeight : true
                    },
                    items : [{
                        title: this.i18n._("Web Override"),
                        items : [{
                            xtype : "radio",
                            boxLabel : String.format(this.i18n._("{0}Enable Processing{1} of web traffic.  (This is the default setting)"), "<b>", "</b>"), 
                            hideLabel : true,
                            name : "Web Override",
                            checked : this.getHttpSettings().enabled,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getHttpSettings().enabled = checked;
                                    }.createDelegate(this)
                                }
                            }
                        },{
                            xtype : "radio",
                            boxLabel : String.format(this.i18n._("{0}Disable Processing{1} of web traffic."), "<b>", "</b>"), 
                            hideLabel : true,
                            name : "Web Override",
                            checked : !this.getHttpSettings().enabled,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getHttpSettings().enabled = !checked;
                                    }.createDelegate(this)
                                }
                            }
                        }]
                    },{
                        title: this.i18n._("Long URIs"),
                        labelWidth: 250,                      
                        items : [{
                            xtype : "radio",
                            boxLabel : String.format(this.i18n._("{0}Enable Processing{1} of long URIs.  The traffic is considered \"Non-Http\".  (This is the default setting)"), "<b>", "</b>"), 
                            hideLabel : true,
                            name : "Long URIs",
                            checked : !this.getHttpSettings().blockLongUris,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getHttpSettings().blockLongUris = !checked;
                                    }.createDelegate(this)
                                }
                            }
                        },{
                            xtype : "radio",
                            boxLabel : String.format(this.i18n._("{0}Disable Processing{1} of long URIs."), "<b>", "</b>"), 
                            hideLabel : true,
                            name : "Long URIs",
                            checked : this.getHttpSettings().blockLongUris,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getHttpSettings().blockLongUris = checked;
                                    }.createDelegate(this)
                                }
                            }
                        },{
                            xtype : "numberfield",
                            fieldLabel : this.i18n._("Max URI Length (characters)"),
                            name : "Max URI Length",
                            id: "system_protocolSettings_maxUriLength",
                            value : this.getHttpSettings().maxUriLength,
                            width: 50,
                            allowDecimals: false,
                            allowNegative: false,
                            minValue: 1024,                        
                            maxValue: 4096,
                            listeners : {
                                "change" : {
                                    fn : function(elem, newValue) {
                                        this.getHttpSettings().maxUriLength = newValue;
                                    }.createDelegate(this)
                                }
                            }
                        }]
                    },{
                        title: this.i18n._("Long Headers"),
                        labelWidth: 250,                      
                        items : [{
                            xtype : "radio",
                            boxLabel : String.format(this.i18n._("{0}Enable Processing{1} of long headers.  The traffic is considered \"Non-Http\".  (This is the default setting)"), "<b>", "</b>"), 
                            hideLabel : true,
                            name : "Long Headers",
                            checked : !this.getHttpSettings().blockLongHeaders,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getHttpSettings().blockLongHeaders = !checked;
                                    }.createDelegate(this)
                                }
                            }
                        },{
                            xtype : "radio",
                            boxLabel : String.format(this.i18n._("{0}Disable Processing{1} of long headers."), "<b>", "</b>"), 
                            hideLabel : true,
                            name : "Long Headers",
                            checked : this.getHttpSettings().blockLongHeaders,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getHttpSettings().blockLongHeaders = checked;
                                    }.createDelegate(this)
                                }
                            }
                        },{
                            xtype : "numberfield",
                            fieldLabel : this.i18n._("Max Header Length (characters)"),
                            name : "Max Header Length",
                            id: "system_protocolSettings_maxHeaderLength",
                            value : this.getHttpSettings().maxHeaderLength,
                            width: 50,
                            allowDecimals: false,
                            allowNegative: false,
                            minValue: 1024,                        
                            maxValue: 8192,
                            listeners : {
                                "change" : {
                                    fn : function(elem, newValue) {
                                        this.getHttpSettings().maxHeaderLength = newValue;
                                    }.createDelegate(this)
                                }
                            }
                        }]
                    },{
                        title: this.i18n._("Non-Http Blocking"),
                        items : [{
                            xtype : "radio",
                            boxLabel : String.format(this.i18n._("{0}Allow{1} non-Http traffic to travel over port 80.  (This is the default setting)"), "<b>", "</b>"), 
                            hideLabel : true,
                            name : "Non-Http Blocking",
                            checked : !this.getHttpSettings().nonHttpBlocked,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getHttpSettings().nonHttpBlocked = !checked;
                                    }.createDelegate(this)
                                }
                            }
                        },{
                            xtype : "radio",
                            boxLabel : String.format(this.i18n._("{0}Stop{1} non-Http traffic to travel over port 80."), "<b>", "</b>"), 
                            hideLabel : true,
                            name : "Non-Http Blocking",
                            checked : this.getHttpSettings().nonHttpBlocked,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getHttpSettings().nonHttpBlocked = checked;
                                    }.createDelegate(this)
                                }
                            }
                        }]
                    }]
                });
            }
            
            if (this.isFtpLoaded()) {
                // keep initial ftp settings
                this.initialFtpSettings = Ung.Util.clone(this.getFtpSettings());
                
                protocolSettingsItems.push({
                    collapsible: true,
                    collapsed: true,
                    title: this.i18n._("FTP"),
                    autoHeight : true,
                    items : [{
                        xtype : "radio",
                        boxLabel : String.format(this.i18n._("{0}Enable Processing{1} of File Transfer traffic.  (This is the default setting)"), "<b>", "</b>"), 
                        hideLabel : true,
                        name : "FTP",
                        checked : this.getFtpSettings().enabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getFtpSettings().enabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : "radio",
                        boxLabel : String.format(this.i18n._("{0}Disable Processing{1} of File Transfer traffic."), "<b>", "</b>"), 
                        hideLabel : true,
                        name : "FTP",
                        checked : !this.getFtpSettings().enabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getFtpSettings().enabled = !checked;
                                }.createDelegate(this)
                            }
                        }
                    }]
                });
            }
            
            if (this.isMailLoaded()) {
                // keep initial mail settings
                this.initialMailSettings = Ung.Util.clone(this.getMailNodeSettings());
                
                protocolSettingsItems.push({
                    collapsible: true,
                    collapsed: true,
                    title: this.i18n._("SMTP"),
                    autoHeight : true,
                    labelWidth: 200,                      
                    items : [{
                        xtype : "radio",
                        boxLabel : String.format(this.i18n._("{0}Enable SMTP{1} email processing.  (This is the default setting)"), "<b>", "</b>"), 
                        hideLabel : true,
                        name : "SMTP",
                        checked : this.getMailNodeSettings().smtpEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getMailNodeSettings().smtpEnabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : "radio",
                        boxLabel : String.format(this.i18n._("{0}Disable SMTP{1} email processing."), "<b>", "</b>"), 
                        hideLabel : true,
                        name : "SMTP",
                        checked : !this.getMailNodeSettings().smtpEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getMailNodeSettings().smtpEnabled = !checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : "numberfield",
                        fieldLabel : this.i18n._("SMTP timeout (seconds)"),
                        name : "SMTP timeout",
                        id: "system_protocolSettings_smtpTimeout",
                        value : this.getMailNodeSettings().smtpTimeout/1000,
                        width: 50,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 0,                        
                        maxValue: 86400,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getMailNodeSettings().smtpTimeout = newValue*1000;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : "radio",
                        boxLabel : String.format(this.i18n._("{0}Allow TLS{1} encryption over SMTP."), "<b>", "</b>"), 
                        hideLabel : true,
                        name : "AllowTLS",
                        checked : this.getMailNodeSettings().smtpAllowTLS,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getMailNodeSettings().smtpAllowTLS = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : "radio",
                        boxLabel : String.format(this.i18n._("{0}Stop TLS{1} encryption over SMTP.  (This is the default setting)"), "<b>", "</b>"), 
                        hideLabel : true,
                        name : "AllowTLS",
                        checked : !this.getMailNodeSettings().smtpAllowTLS,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getMailNodeSettings().smtpAllowTLS = !checked;
                                }.createDelegate(this)
                            }
                        }
                    }]
                });
                protocolSettingsItems.push({
                    collapsible: true,
                    collapsed: true,
                    xtype : "fieldset",
                    title: this.i18n._("POP3"),
                    autoHeight : true,
                    labelWidth: 200,                      
                    items : [{
                        xtype : "radio",
                        boxLabel : String.format(this.i18n._("{0}Enable POP3{1} email processing.  (This is the default setting)"), "<b>", "</b>"), 
                        hideLabel : true,
                        name : "POP3",
                        checked : this.getMailNodeSettings().popEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getMailNodeSettings().popEnabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : "radio",
                        boxLabel : String.format(this.i18n._("{0}Disable POP3{1} email processing."), "<b>", "</b>"), 
                        hideLabel : true,
                        name : "POP3",
                        checked : !this.getMailNodeSettings().popEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getMailNodeSettings().popEnabled = !checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : "numberfield",
                        fieldLabel : this.i18n._("POP3 timeout (seconds)"),
                        name : "POP3 timeout",
                        id: "system_protocolSettings_popTimeout",
                        value : this.getMailNodeSettings().popTimeout/1000,
                        width: 50,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 0,                        
                        maxValue: 86400,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getMailNodeSettings().popTimeout = newValue*1000;
                                }.createDelegate(this)
                            }
                        }
                    }]
                });
                protocolSettingsItems.push({
                    collapsible: true,
                    collapsed: true,
                    title: this.i18n._("IMAP"),
                    autoHeight : true,
                    labelWidth: 200,                      
                    items : [{
                        xtype : "radio",
                        boxLabel : String.format(this.i18n._("{0}Enable IMAP{1} email processing.  (This is the default setting)"), "<b>", "</b>"), 
                        hideLabel : true,
                        name : "IMAP",
                        checked : this.getMailNodeSettings().imapEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getMailNodeSettings().imapEnabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : "radio",
                        boxLabel : String.format(this.i18n._("{0}Disable IMAP{1} email processing."), "<b>", "</b>"), 
                        hideLabel : true,
                        name : "IMAP",
                        checked : !this.getMailNodeSettings().imapEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getMailNodeSettings().imapEnabled = !checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : "numberfield",
                        fieldLabel : this.i18n._("IMAP timeout (seconds)"),
                        name : "IMAP timeout",
                        id: "system_protocolSettings_imapTimeout",
                        value : this.getMailNodeSettings().imapTimeout/1000,
                        width: 50,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 0,                        
                        maxValue: 86400,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getMailNodeSettings().imapTimeout = newValue*1000;
                                }.createDelegate(this)
                            }
                        }
                    }]
                });
            }                
            
            this.panelProtocolSettings = new Ext.Panel({
                name : "Protocol Settings",
                helpSource : "protocol_settings",
                // private fields
                parentId : this.getId(),

                title : this.i18n._("Protocol Settings"),
                layout : "form",
                cls: "ung-panel",
                autoScroll : true,
                defaults : {
                    xtype : "fieldset",
                    autoHeight : true
                },
                items: protocolSettingsItems.length != 0 ? protocolSettingsItems : null
            });
        },
        buildRegionalSettings : function() {
            // keep initial settings
            this.initialLanguageSettings = Ung.Util.clone(this.getLanguageSettings());
            this.initialTimeZone = Ung.Util.clone(this.getTimeZone());
            
            var languagesStore = new Ext.data.Store({
                proxy : new Ung.RpcProxy(rpc.languageManager.getLanguagesList, null, false),
                reader : new Ext.data.JsonReader({
                    root : "list",
                    fields : ["code", "name"]
                })
            });

            var timeZones = [];
            for (var i = 0; i < Ung.TimeZoneData.length; i++) {
                timeZones.push([Ung.TimeZoneData[i][2], "(" + Ung.TimeZoneData[i][0] + ") " + Ung.TimeZoneData[i][1]]);
            }
            this.panelRegionalSettings = new Ext.Panel({
                // private fields
                name : "Regional Settings",
                helpSource : "regional_settings",
                parentId : this.getId(),
                title : this.i18n._("Regional Settings"),
                layout : "form",
                cls: "ung-panel",
                autoScroll : true,
                defaults : {
                    xtype : "fieldset",
                    autoHeight : true,
                    buttonAlign : "left"
                },
                items : [{
                    title : this.i18n._("Current Time"),
                    defaults : {
                        border : false,
                        cls: "description"
                    },
                    items : [{
                        html : this.i18n._("time is automatically synced via NTP")
                    }, {
                        html : rpc.adminManager.getDate()
                    }]
                }, {
                    title : this.i18n._("Timezone"),
                    items : [{
                        xtype : "combo",
                        name : "Timezone",
                        id : "system_timezone",
                        editable : false,
                        store : timeZones,
                        width : 350,
                        hideLabel : true,
                        mode : "local",
                        triggerAction : "all",
                        listClass : "x-combo-list-small",
                        value : this.getTimeZone(),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.rpc.timeZone = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    title : this.i18n._("Language"),
                    items : [{
                        id : "system_language_combo",
                        xtype : "combo",
                        name : "Language",
                        store : languagesStore,
                        forceSelection : true,
                        displayField : "name",
                        valueField : "code",
                        typeAhead : true,
                        mode : "local",
                        triggerAction : "all",
                        listClass : "x-combo-list-small",
                        selectOnFocus : true,
                        hideLabel : true,
                        listeners : {
                            "select" : {
                                fn : function(elem, record) {
                                    this.getLanguageSettings().language = record.data.code;
                                }.createDelegate(this)
                            },
                            "render" : {
                                fn : function(elem) {
                                    languagesStore.load({
                                        callback : function(r, options, success) {
                                            if (success) {
                                                var languageComboCmp = Ext.getCmp("system_language_combo");
                                                if (languageComboCmp) {
                                                    languageComboCmp.setValue(this.getLanguageSettings().language);
                                                }
                                            }
                                        }.createDelegate(this)
                                    });
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    title : this.i18n._("Upload New Language Pack"),
                    items : {
                        fileUpload : true,
                        xtype : "form",
                        id : "upload_language_form",
                        url : "upload",
                        border : false,
                        items : [{
                            fieldLabel : this.i18n._("File"),
                            name : "file",
                            id : "upload_language_file_textfield",
                            inputType : "file",
                            xtype : "textfield",
                            allowBlank : false
                        }, {
                            xtype : "button",
                            text : this.i18n._("Upload"),
                            name : "Upload",
                            handler : function() {
                                this.panelRegionalSettings.onUpload();
                            }.createDelegate(this)
                        }, {
                            xtype : "hidden",
                            name : "type",
                            value : "language"
                        }]
                    }
                }, {
            html : this.downloadLanguageHTML
        }],
                onUpload : function() {
                    var prova = Ext.getCmp("upload_language_form");
                    var cmp = Ext.getCmp(this.parentId);

                    var form = prova.getForm();
                    form.submit({
                        parentId : cmp.getId(),
                        waitMsg : cmp.i18n._("Please wait while your language pack is uploaded..."),
                        success : function(form, action) {
                            languagesStore.load();
                            var cmp = Ext.getCmp(action.options.parentId);
                            if(action.result.success===true){
                                Ext.MessageBox.alert(cmp.i18n._("Succeeded"), cmp.i18n._("Upload language pack succeeded"), 
                                    function() {
                                        Ext.getCmp("upload_language_file_textfield").reset();
                                    } 
                                );
                            }else{
                                var msg = "An error occured while uploading the language pack";
                                if(action.result.msg){
                                    msg = action.result.msg;
                                }
                                Ext.MessageBox.alert(cmp.i18n._("Warning"), cmp.i18n._(msg));                                
                            }
                        },
                        failure : function(form, action) {
                            var cmp = Ext.getCmp(action.options.parentId);
                            var errorMsg = cmp.i18n._("Upload language pack failed");
                            if (action.result && action.result.msg) {
                                msg = action.result.msg;
                                switch (true) {
                                    case (msg === "Invalid Language Pack") :
                                        errorMsg = cmp.i18n._("Invalid language pack; not a zip file");
                                    break;
                                    case ((/.*MO file.*/).test(msg)) :
                                        errorMsg = cmp.i18n._("Couldn't compile MO file for entry" + " " + msg.split(" ").pop());
                                    break;
                                    case ((/.*bundle file.*/).test(msg)) :
                                        errorMsg = cmp.i18n._("Couldn't compile resource bundle for entry" + " " + msg.split(" ").pop());
                                    break;
                                    default :
                                        errorMsg = cmp.i18n._("Upload language pack failed");
                                }
                            }
                            Ext.MessageBox.alert(cmp.i18n._("Failed"), errorMsg);
                        }
                    });
                }
            });

        },
        // validation function
        validateClient : function() {
            //validate timeout
            return  (!this.isHttpLoaded() || this.validateMaxHeaderLength() && this.validateMaxUriLength()) && 
               (!this.isMailLoaded() || this.validateSMTP() && this.validatePOP() && this.validateIMAP()); 
        },
        
        //validate Max URI Length
        validateMaxUriLength : function() {
            var maxUriLengthCmp = Ext.getCmp("system_protocolSettings_maxUriLength");
            if (maxUriLengthCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert(this.i18n._("Warning"), this.i18n._("Max URI Length should be between 1024 and 4096!"),
                    function () {
                        this.tabs.activate(this.panelProtocolSettings);
                        maxUriLengthCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }
        },
        //validate Max Header Length
        validateMaxHeaderLength : function() {
            var maxHeaderLengthCmp = Ext.getCmp("system_protocolSettings_maxHeaderLength");
            if (maxHeaderLengthCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert(this.i18n._("Warning"), this.i18n._("Max Header Length should be between 1024 and 8192!"),
                    function () {
                        this.tabs.activate(this.panelProtocolSettings);
                        maxHeaderLengthCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }
        },
        //validate SMTP timeout
        validateSMTP : function() {
            var smtpTimeoutCmp = Ext.getCmp("system_protocolSettings_smtpTimeout");
            if (smtpTimeoutCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert(this.i18n._("Warning"), this.i18n._("SMTP timeout should be between 0 and 86400!"),
                    function () {
                        this.tabs.activate(this.panelProtocolSettings);
                        smtpTimeoutCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }
        },
        //validate POP timeout
        validatePOP : function() {
            var popTimeoutCmp = Ext.getCmp("system_protocolSettings_popTimeout");
            if (popTimeoutCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert(this.i18n._("Warning"), this.i18n._("POP timeout should be between 0 and 86400!"),
                    function () {
                        this.tabs.activate(this.panelProtocolSettings);
                        popTimeoutCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }
        },
        //validate IMAP timeout
        validateIMAP : function() {
            var imapTimeoutCmp = Ext.getCmp("system_protocolSettings_imapTimeout");
            if (imapTimeoutCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert(this.i18n._("Warning"), this.i18n._("IMAP timeout should be between 0 and 86400!"),
                    function () {
                        this.tabs.activate(this.panelProtocolSettings);
                        imapTimeoutCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }
        },
        applyAction : function()
        {
            this.commitSettings(this.reloadSettings.createDelegate(this));
        },
        reloadSettings : function()
        {
            this.initialAccessSettings = Ung.Util.clone(this.getAccessSettings(true));
            this.initialMiscSettings = Ung.Util.clone(this.getMiscSettings(true));
            this.initialLanguageSettings = Ung.Util.clone(this.getLanguageSettings());
            this.initialTimeZone = Ung.Util.clone(this.getTimeZone());

            if (this.isHttpLoaded()) {
                this.initialHttpSettings = Ung.Util.clone(this.getHttpSettings(true));
            }

            if (this.isMailLoaded()) {
                this.initialMailSettings = Ung.Util.clone(this.getMailNodeSettings());
            }

            if (this.isFtpLoaded()) {
                // keep initial ftp settings
                this.initialFtpSettings = Ung.Util.clone(this.getFtpSettings(true));
            }

            Ext.MessageBox.hide();
        },
        saveAction : function()
        {
            this.commitSettings(this.completeSaveAction.createDelegate(this));
        },
        completeSaveAction : function()
        {
            Ext.MessageBox.hide();
            this.closeWindow();
        },
        // save function
        commitSettings : function(callback)
        {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.saveSemaphore = 6;
                // save language settings
                rpc.languageManager.setLanguageSettings(function(result, exception) {
                    this.afterSave(exception,callback);
                }.createDelegate(this), this.getLanguageSettings());
                
                // save network settings
                rpc.networkManager.setSettings(function(result, exception) {
                    this.afterSave(exception,callback);
                }.createDelegate(this), this.getAccessSettings(), this.getMiscSettings());
                
                // save http settings
                if (this.isHttpLoaded()) {
                    this.getHttpNode().setHttpSettings(function(result, exception) {
                        this.afterSave(exception,callback);
                    }.createDelegate(this), this.getHttpSettings());
                } else {
                    this.saveSemaphore--;
                }
                
                // save ftp settings
                if (this.isFtpLoaded()) {
                    this.getFtpNode().setFtpSettings(function(result, exception) {
                        this.afterSave(exception,callback);
                    }.createDelegate(this), this.getFtpSettings());
                } else {
                    this.saveSemaphore--;
                }
                
                // save mail settings
                if (this.isMailLoaded()) {
                    var quarantineSettings = this.getMailNodeSettings().quarantineSettings;
                    delete quarantineSettings.secretKey;

                    this.getMailNode().setMailNodeSettings(function(result, exception) {
                        this.afterSave(exception,callback);
                    }.createDelegate(this), this.getMailNodeSettings());
                } else {
                    this.saveSemaphore--;
                }
                
                //save timezone
                rpc.adminManager.setTimeZone(function(result, exception) {
                    this.afterSave(exception,callback);
                }.createDelegate(this), this.rpc.timeZone);
            }
        },
        afterSave : function(exception,callback)
        {
            if(Ung.Util.handleException(exception)) return;

            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
                var needRefresh = this.initialLanguageSettings.language != this.getLanguageSettings().language;

                if (needRefresh) {                    
                    Ung.Util.goToStartPage();
                    return;
                }

                callback();
            }
        },
        isDirty : function() {
            return !Ung.Util.equals(this.getLanguageSettings(), this.initialLanguageSettings)
                || !Ung.Util.equals(this.getTimeZone(), this.initialTimeZone)
                || !Ung.Util.equals(this.getAccessSettings(), this.initialAccessSettings)
                || !Ung.Util.equals(this.getMiscSettings(), this.initialMiscSettings)
                || this.isHttpLoaded() && !Ung.Util.equals(this.getHttpSettings(), this.initialHttpSettings)
                || this.isFtpLoaded() && !Ung.Util.equals(this.getFtpSettings(), this.initialFtpSettings)
                || this.isMailLoaded() && !Ung.Util.equals(this.getMailNodeSettings(), this.initialMailSettings);
        }
    });

}
