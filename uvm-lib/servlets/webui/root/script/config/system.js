if (!Ung.hasResource["Ung.System"]) {
    Ung.hasResource["Ung.System"] = true;

    Ung.System = Ext.extend(Ung.ConfigWin, {
        panelUntangleSupport : null,
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
                title : i18n._('System')
            }];
            Ung.System.superclass.initComponent.call(this);
        },

        onRender : function(container, position) {
            // call superclass renderer first
            Ung.System.superclass.onRender.call(this, container, position);
            this.initSubCmps.defer(1, this);
            // builds the 5 tabs
        },
        initSubCmps : function() {
            this.buildUntangleSupport();
            this.buildBackup();
            this.buildRestore();
            this.buildProtocolSettings();
            this.buildRegionalSettings();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelUntangleSupport, this.panelBackup, this.panelRestore, this.panelProtocolSettings,
                    this.panelRegionalSettings]);
            this.tabs.activate(this.panelUntangleSupport);
            if (!this.isHttpLoaded() && !this.isFtpLoaded() && !this.isMailLoaded() ){
                this.panelProtocolSettings.disable();
            }

        },
        // get languange settings object
        getLanguageSettings : function(forceReload) {
            if (forceReload || this.rpc.languageSettings === undefined) {
                this.rpc.languageSettings = rpc.languageManager.getLanguageSettings();
            }
            return this.rpc.languageSettings;
        },
        getAccessSettings : function(forceReload) {
            if (forceReload || this.rpc.accessSettings === undefined) {
                this.rpc.accessSettings = rpc.networkManager.getAccessSettings();
            }
            return this.rpc.accessSettings;
        },
        getMiscSettings : function(forceReload) {
            if (forceReload || this.rpc.miscSettings === undefined) {
                this.rpc.miscSettings = rpc.networkManager.getMiscSettings();
            }
            return this.rpc.miscSettings;
        },
        getHttpNode : function(forceReload) {
            if (forceReload || this.rpc.httpNode === undefined) {
                this.rpc.httpNode = rpc.nodeManager.node("untangle-casing-http");
            }
            return this.rpc.httpNode;
        },
        isHttpLoaded : function(forceReload) {
        	return this.getHttpNode(forceReload) != null;
        },
        getHttpSettings : function(forceReload) {
            if (forceReload || this.rpc.httpSettings === undefined) {
                this.rpc.httpSettings = this.getHttpNode(forceReload).getHttpSettings();
            }
            return this.rpc.httpSettings;
        },
        getFtpNode : function(forceReload) {
            if (forceReload || this.rpc.ftpNode === undefined) {
                this.rpc.ftpNode = rpc.nodeManager.node("untangle-casing-ftp");
            }
            return this.rpc.ftpNode;
        },
        isFtpLoaded : function(forceReload) {
            return this.getFtpNode(forceReload) != null;
        },
        getFtpSettings : function(forceReload) {
            if (forceReload || this.rpc.ftpSettings === undefined) {
                this.rpc.ftpSettings = this.getFtpNode(forceReload).getFtpSettings();
            }
            return this.rpc.ftpSettings;
        },
        getMailNode : function(forceReload) {
            if (forceReload || this.rpc.mailNode === undefined) {
                this.rpc.mailNode = rpc.nodeManager.node("untangle-casing-mail");
            }
            return this.rpc.mailNode;
        },
        isMailLoaded : function(forceReload) {
            return this.getMailNode(forceReload) != null;
        },
        getMailSettings : function(forceReload) {
            if (forceReload || this.rpc.mailSettings === undefined) {
                this.rpc.mailSettings = this.getMailNode(forceReload).getMailNodeSettings();
            }
            return this.rpc.mailSettings;
        },
        getTimeZone : function(forceReload) {
            if (forceReload || this.rpc.timeZone === undefined) {
                this.rpc.timeZone = rpc.adminManager.getTimeZone();
            }
            return this.rpc.timeZone;
        },
        buildUntangleSupport : function() {
            this.panelUntangleSupport = new Ext.Panel({
                // private fields
                name : 'Untangle Support',
                parentId : this.getId(),
                title : this.i18n._('Untangle Support'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Support'),
                    defaults : {
                        border : false,
                        bodyStyle : 'padding:5px 5px 0px 5px;'
                    },
                    items : [{
                        xtype : 'checkbox',
                        name : 'Allow us to securely access your server for support purposes',
                        boxLabel : String.format(this.i18n._('{0}Allow{1} us to securely access your server for support purposes.'), '<b>', '</b>'),
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
                        xtype : 'checkbox',
                        name : 'Send us data about your server',
                        boxLabel : String.format(this.i18n
                                ._('{0}Send{1} us data about your server. This will send us status updates and an email if any unexpected problems occur, but will not allow us to login to your server. No personal information about your network traffic will be transmitted.'),
                                '<b>', '</b>'),
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
                }]
            });

        },
        buildBackup : function() {
            this.panelBackup = new Ext.Panel({
                // private fields
                name : 'Backup',
                parentId : this.getId(),
                title : this.i18n._('Backup'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                onBackupToFile: function() {
                	// A two step process: first asks the server for permission to download the file (the outer ajax request) 
                	// and then if successful opens the iframe which initiates the download.
                    Ext.Ajax.request({
                        url: 'backup',
                        params: {action:'requestBackup'},
                        success: function(response) {
                            try {
                                Ext.destroy(Ext.get('downloadIframe'));
                            }
                            catch(e) {}
                            Ext.DomHelper.append(document.body, {
                                tag: 'iframe',
                                id:'downloadIframe',
                                frameBorder: 0,
                                width: 0,
                                height: 0,
                                css: 'display:none;visibility:hidden;height:0px;',
                                src: 'backup?action=initiateDownload'
                            });
                        },
                        failure: function() {
                            Ext.MessageBox.alert(this.i18n._("Backup Failure Warning"),this.i18n._("Error:  The local file backup procedure failed.  Please try again.")); 
                        }
                    });                	
                }.createDelegate(this),
                onBackupToUSBKey: function() {
                	Ext.MessageBox.wait(i18n._("Backing Up..."), i18n._("Please wait"));
                    var cmp=Ext.getCmp(this.parentId);
                    rpc.jsonrpc.RemoteUvmContext.usbBackup(function (result, exception) {
                        if(exception) {
                            Ext.MessageBox.alert(this.i18n._("Backup Failure Warning"),this.i18n._("Error:  The USB Key backup procedure failed.  Contact support for further direction.")); 
                        } else {
                            Ext.MessageBox.alert(this.i18n._("Backup Success"),this.i18n._("Success:  The USB Key backup procedure completed."));
                        }
                    }.createDelegate(cmp));
                },
                onBackupToHardDisk: function() {
                    Ext.MessageBox.wait(i18n._("Backing Up..."), i18n._("Please wait"));
                    var cmp=Ext.getCmp(this.parentId);
                    rpc.jsonrpc.RemoteUvmContext.localBackup(function (result, exception) {
                        if(exception) {
                            Ext.MessageBox.alert(this.i18n._("Backup Failure Warning"),this.i18n._("Error:  The Hard Disk backup procedure failed.  Contact support for further direction.")); 
                        } else {
                            Ext.MessageBox.alert(this.i18n._("Backup Success"),this.i18n._("Success:  The Hard Disk backup procedure completed."));
                        }
                    }.createDelegate(cmp));
                },
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Backup to File'),
                    defaults : {
                        border : false,
                        bodyStyle : 'padding:5px 5px 0px 5px;'
                    },
                    items : [{
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
                },{
                    title : this.i18n._('Backup to USB Key'),
                    defaults : {
                        border : false,
                        bodyStyle : 'padding:5px 5px 0px 5px;'
                    },
                    items : [{
                        html: this.i18n._("You can backup your current system configuration to USB Key for later restoration, in the event that you would like to replace new settings with your current settings.") +
                        		"<br>\n<br>\n" +
                        		String.format(this.i18n._("After backing up your current system configuration to USB Key, you can then restore that configuration through the {0}Backup and Restore Utilities{1}.  To access the Backup and Restore Utilities, you must have a monitor and keyboard physically plugged into your server when it is turned on, and then select \"Backup and Restore Utilities\" from the boot prompt."),'<b>','</b>') +
                        		"<br>\n<br>\n<b>" +
                        		this.i18n._("Note: You must insert your USB Key into a valid USB port on the back of your server before pressing the button.  You must not remove the USB Key from the USB port until after the process is complete.  The progress bar will inform you when the process is complete.") +
                        		"</b>"
                    }],
                    buttons : [{
                    	name: "Backup to USB Key",
                        text : this.i18n._("Backup to USB Key"),
                        handler : function() {
                            this.panelBackup.onBackupToUSBKey();
                        }.createDelegate(this)
                    }]
                },{
                    title : this.i18n._('Backup to Hard Disk'),
                    defaults : {
                        border : false,
                        bodyStyle : 'padding:5px 5px 0px 5px;'
                    },
                    items : [{
                        html: this.i18n._("You can backup your current system configuration to Hard Disk for later restoration, in the event that you would like to replace new settings with your current settings.") +
                        		"<br>\n<br>\n" +
                        		String.format(this.i18n._("After backing up your current system configuration to Hard Disk, you can then restore that configuration through the {0}Backup and Restore Utilities{1}.  To access the Backup and Restore Utilities, you must have a monitor and keyboard physically plugged into your server when it is turned on, and then select \"Backup and Restore Utilities\" from the boot prompt.",'<b>','</b>'))
                    }],
                    buttons : [{
                        text : this.i18n._("Backup to Hard Disk"),
                        name: "Backup to Hard Disk",
                        handler : function() {
                            this.panelBackup.onBackupToHardDisk();
                        }.createDelegate(this)
                    }]
                }]
            });

        },
        buildRestore : function() {
            this.panelRestore = new Ext.Panel({
                // private fields
                name : 'Restore',
                parentId : this.getId(),
                title : this.i18n._('Restore'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                onRestoreFromFileFile: function() {
                	var prova = Ext.getCmp('upload_restore_file_form');
                    var cmp = Ext.getCmp(this.parentId);
                    var fileText = prova.items.get(0);
                    if (fileText.getValue().length == 0) {
                        Ext.MessageBox.alert(cmp.i18n._("Failed"), cmp.i18n._('Please select a file to upload.'));
                        return false;
                    }
                    var form = prova.getForm();
                    form.submit({
                        parentId : cmp.getId(),
                        waitMsg : cmp.i18n._('Please wait while Restoring...'),
                        success : function(form, action) {
                            var cmp = Ext.getCmp(action.options.parentId);
                            Ext.MessageBox.alert(cmp.i18n._("Restore Success"), cmp.i18n._("Success:  The Local File restore procedure completed."), function(btn, text){
                                Ext.MessageBox.alert(cmp.i18n._("Attention"),
                                    cmp.i18n._("You must now exit this program.")+"<br>"+
                                    cmp.i18n._("You can log in again after a brief period.")+"<br><b>"+
                                    cmp.i18n._("DO NOT MANUALLY SHUTDOWN OR RESTART THE UNTANGLE SERVER WHILE IT IS UPGRADING!")+"</b>");
                            });
                        },
                        failure : function(form, action) {
                            var cmp = Ext.getCmp(action.options.parentId);
                            var errorMsg = cmp.i18n._("The Local File restore procedure failed.");
                            if (action.result && action.result.msg) {
                                switch (action.result.msg) {
                                    case 'File does not seem to be valid Untangle backup' : 
                                        errorMsg = cmp.i18n._("File does not seem to be valid Untangle backup");
                                    break;
                                    case 'Error in processing restore itself (yet file seems valid)' : 
                                        errorMsg = cmp.i18n._("Error in processing restore itself (yet file seems valid)");
                                    break;
                                    case 'File is from an older version of Untangle and cannot be used' : 
                                        errorMsg = cmp.i18n._("File is from an older version of Untangle and cannot be used");
                                    break;
                                    case 'Unknown error in local processing' : 
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
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('From File'),
                    defaults : {
                        border : false,
                        bodyStyle : 'padding:5px 5px 0px 5px;'
                    },
                    items : [{
                        html: this.i18n._("You can restore a previous system configuration from a backup file on your local computer.  The backup file name ends with \".backup\"")
                    },{
                        fileUpload : true,
                        xtype : 'form',
                        id : 'upload_restore_file_form',
                        url : 'upload',
                        border : false,
                        items : [{
                            fieldLabel : 'File',
                            name : 'file',
                            inputType : 'file',
                            xtype : 'textfield',
                            allowBlank : false
                        }, {
                            xtype : 'hidden',
                            name : 'type',
                            value : 'restore'
                        }]
                    }],
                    buttons : [{
                        text : this.i18n._("Restore from File"),
                        name: "Restore from File",
                        handler : function() {
                            this.panelRestore.onRestoreFromFileFile();
                        }.createDelegate(this)
                    }]
                },{
                    title : this.i18n._('From Hard Disk and USB Key'),
                    defaults : {
                        border : false,
                        bodyStyle : 'padding:5px 5px 0px 5px;'
                    },
                    items : [{
                        html: String.format(this.i18n._("After backing up your system configuration, you can restore that configuration through the {0}Recovery Utilities{1} on your server once it is done booting."),'<b>','</b>') +
                        		"\n<br>\n<br>" +
                        		String.format(this.i18n._("To access the {0}Recovery Utilities{1}, you must have a monitor and keyboard physically plugged into your server, and then click on the Recovery Utilities toolbar button when it is done booting."),'<b>','</b>')
                    }]
                }]
            });
        },
        buildProtocolSettings : function() {
            var protocolSettingsItems = new Array();
            
            protocolSettingsItems.push(new Ext.form.FieldSet({
                autoHeight : true,
                border: false,
                html: this.i18n._('Warning: These settings should not be changed unless instructed to do so by support.')
            }));
            
            if (this.isHttpLoaded()) {
            	protocolSettingsItems.push(new Ext.form.FieldSet({
                    title: this.i18n._('HTTP'),
                    autoHeight : true,
                    defaults : {
                        xtype : 'fieldset',
                        autoHeight : true
                    },
                    items : [{
                      title: this.i18n._('Web Override'),
                        items : [{
                            xtype : 'radio',
                            boxLabel : String.format(this.i18n._('{0}Enable Processing{1} of web traffic.  (This is the default setting)'), '<b>', '</b>'), 
                            hideLabel : true,
                            name : 'Web Override',
                            checked : this.getHttpSettings().enabled,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getHttpSettings().enabled = checked;
                                    }.createDelegate(this)
                                }
                            }
                        },{
                            xtype : 'radio',
                            boxLabel : String.format(this.i18n._('{0}Disable Processing{1} of web traffic.'), '<b>', '</b>'), 
                            hideLabel : true,
                            name : 'Web Override',
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
                        title: this.i18n._('Long URIs'),
                        items : [{
                            xtype : 'radio',
                            boxLabel : String.format(this.i18n._('{0}Enable Processing{1} of long URIs.  The traffic is considered \"Non-Http\".  (This is the default setting)'), '<b>', '</b>'), 
                            hideLabel : true,
                            name : 'Long URIs',
                            checked : !this.getHttpSettings().blockLongUris,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getHttpSettings().blockLongUris = !checked;
                                    }.createDelegate(this)
                                }
                            }
                        },{
                            xtype : 'radio',
                            boxLabel : String.format(this.i18n._('{0}Disable Processing{1} of long URIs.'), '<b>', '</b>'), 
                            hideLabel : true,
                            name : 'Long URIs',
                            checked : this.getHttpSettings().blockLongUris,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getHttpSettings().blockLongUris = checked;
                                    }.createDelegate(this)
                                }
                            }
                        },{
                            xtype : 'numberfield',
                            fieldLabel : this.i18n._('Max URI Length (characters)'),
                            labelStyle: 'width:200px;',
                            name : 'Max URI Length',
                            id: 'system_protocolSettings_maxUriLength',
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
                        title: this.i18n._('Long Headers'),
                        items : [{
                            xtype : 'radio',
                            boxLabel : String.format(this.i18n._('{0}Enable Processing{1} of long headers.  The traffic is considered \"Non-Http\".  (This is the default setting)'), '<b>', '</b>'), 
                            hideLabel : true,
                            name : 'Long Headers',
                            checked : !this.getHttpSettings().blockLongHeaders,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getHttpSettings().blockLongHeaders = !checked;
                                    }.createDelegate(this)
                                }
                            }
                        },{
                            xtype : 'radio',
                            boxLabel : String.format(this.i18n._('{0}Disable Processing{1} of long headers.'), '<b>', '</b>'), 
                            hideLabel : true,
                            name : 'Long Headers',
                            checked : this.getHttpSettings().blockLongHeaders,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getHttpSettings().blockLongHeaders = checked;
                                    }.createDelegate(this)
                                }
                            }
                        },{
                            xtype : 'numberfield',
                            fieldLabel : this.i18n._('Max Header Length (characters)'),
                            labelStyle: 'width:200px;',
                            name : 'Max Header Length',
                            id: 'system_protocolSettings_maxHeaderLength',
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
                        title: this.i18n._('Non-Http Blocking'),
                        items : [{
                            xtype : 'radio',
                            boxLabel : String.format(this.i18n._('{0}Allow{1} non-Http traffic to travel over port 80.  (This is the default setting)'), '<b>', '</b>'), 
                            hideLabel : true,
                            name : 'Non-Http Blocking',
                            checked : !this.getHttpSettings().nonHttpBlocked,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getHttpSettings().nonHttpBlocked = !checked;
                                    }.createDelegate(this)
                                }
                            }
                        },{
                            xtype : 'radio',
                            boxLabel : String.format(this.i18n._('{0}Stop{1} non-Http traffic to travel over port 80.'), '<b>', '</b>'), 
                            hideLabel : true,
                            name : 'Non-Http Blocking',
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
            	}));
            }
        	
            if (this.isFtpLoaded()) {
                protocolSettingsItems.push( new Ext.form.FieldSet({
                    title: this.i18n._('FTP'),
                    autoHeight : true,
                    items : [{
                        xtype : 'radio',
                        boxLabel : String.format(this.i18n._('{0}Enable Processing{1} of File Transfer traffic.  (This is the default setting)'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'FTP',
                        checked : this.getFtpSettings().enabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getFtpSettings().enabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'radio',
                        boxLabel : String.format(this.i18n._('{0}Disable Processing{1} of File Transfer traffic.'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'FTP',
                        checked : !this.getFtpSettings().enabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getFtpSettings().enabled = !checked;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }));
            }
            
            if (this.isMailLoaded()) {
                protocolSettingsItems.push(new Ext.form.FieldSet({
                    title: this.i18n._('SMTP'),
                    autoHeight : true,
                    items : [{
                        xtype : 'radio',
                        boxLabel : String.format(this.i18n._('{0}Enable SMTP{1} email processing.  (This is the default setting)'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'SMTP',
                        checked : this.getMailSettings().smtpEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getMailSettings().smtpEnabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'radio',
                        boxLabel : String.format(this.i18n._('{0}Disable SMTP{1} email processing.'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'SMTP',
                        checked : !this.getMailSettings().smtpEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getMailSettings().smtpEnabled = !checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('SMTP timeout (seconds)'),
                        labelStyle: 'width:200px;',
                        name : 'SMTP timeout',
                        id: 'system_protocolSettings_smtpTimeout',
                        value : this.getMailSettings().smtpTimeout/1000,
                        width: 50,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 0,                        
                        maxValue: 86400,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getMailSettings().smtpTimeout = newValue*1000;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }));
                protocolSettingsItems.push(new Ext.form.FieldSet({
                    title: this.i18n._('POP3'),
                    autoHeight : true,
                    items : [{
                        xtype : 'radio',
                        boxLabel : String.format(this.i18n._('{0}Enable POP3{1} email processing.  (This is the default setting)'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'POP3',
                        checked : this.getMailSettings().popEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getMailSettings().popEnabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'radio',
                        boxLabel : String.format(this.i18n._('{0}Disable POP3{1} email processing.'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'POP3',
                        checked : !this.getMailSettings().popEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getMailSettings().popEnabled = !checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('POP3 timeout (seconds)'),
                        labelStyle: 'width:200px;',
                        name : 'POP3 timeout',
                        id: 'system_protocolSettings_popTimeout',
                        value : this.getMailSettings().popTimeout/1000,
                        width: 50,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 0,                        
                        maxValue: 86400,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getMailSettings().popTimeout = newValue*1000;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }));
                protocolSettingsItems.push( new Ext.form.FieldSet({
                    title: this.i18n._('IMAP'),
                    autoHeight : true,
                    items : [{
                        xtype : 'radio',
                        boxLabel : String.format(this.i18n._('{0}Enable IMAP{1} email processing.  (This is the default setting)'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'IMAP',
                        checked : this.getMailSettings().imapEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getMailSettings().imapEnabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'radio',
                        boxLabel : String.format(this.i18n._('{0}Disable IMAP{1} email processing.'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'IMAP',
                        checked : !this.getMailSettings().imapEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getMailSettings().imapEnabled = !checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('IMAP timeout (seconds)'),
                        labelStyle: 'width:200px;',
                        name : 'IMAP timeout',
                        id: 'system_protocolSettings_imapTimeout',
                        value : this.getMailSettings().imapTimeout/1000,
                        width: 50,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 0,                        
                        maxValue: 86400,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getMailSettings().imapTimeout = newValue*1000;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }));
            }            	
            
            this.panelProtocolSettings = new Ext.Panel({
                name : 'Protocol Settings',
                // private fields
                parentId : this.getId(),

                title : this.i18n._('Protocol Settings'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true
                },
                items: protocolSettingsItems.length != 0 ? protocolSettingsItems : null
            });
        },
        buildRegionalSettings : function() {
            var languagesStore = new Ext.data.Store({
                proxy : new Ung.RpcProxy(rpc.languageManager.getLanguagesList, null, false),
                reader : new Ext.data.JsonReader({
                    root : 'list',
                    fields : ['code', 'name']
                })
            });

            var timeZones = [];
            for (var i = 0; i < Ung.TimeZoneData.length; i++) {
                timeZones.push([Ung.TimeZoneData[i][2], "(" + Ung.TimeZoneData[i][0] + ") " + Ung.TimeZoneData[i][1]]);
            }
            this.panelRegionalSettings = new Ext.Panel({
                // private fields
                name : 'Regional Settings',
                parentId : this.getId(),
                title : this.i18n._('Regional Settings'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Current Time'),
                    defaults : {
                        border : false,
                        bodyStyle : 'padding:5px 5px 0px 5px'
                    },
                    items : [{
                        html : this.i18n._('time is automatically synced via NTP')
                    }, {
                        html : this.i18n.timestampFormat(rpc.adminManager.getDate())
                    }]
                }, {
                    title : this.i18n._('Timezone'),
                    items : [{
                        xtype : 'combo',
                        name : 'Timezone',
                        editable : false,
                        store : timeZones,
                        width : 350,
                        hideLabel : true,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
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
                    title : this.i18n._('Language'),
                    items : [{
                        id : 'system_language_combo',
                        xtype : 'combo',
                        name : "Language",
                        store : languagesStore,
                        forceSelection : true,
                        displayField : 'name',
                        valueField : 'code',
                        typeAhead : true,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        selectOnFocus : true,
                        hideLabel : true,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getLanguageSettings().language = newValue;
                                    Ext.MessageBox.alert(this.i18n._("Info"), this.i18n
                                            ._("Please note that you have to relogin after saving for the new language to take effect."));
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
                                    })
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    title : this.i18n._('Upload New Language Pack'),
                    items : {
                        fileUpload : true,
                        xtype : 'form',
                        id : 'upload_language_form',
                        url : 'upload',
                        border : false,
                        items : [{
                            fieldLabel : 'File',
                            name : 'file',
                            inputType : 'file',
                            xtype : 'textfield',
                            allowBlank : false
                        }, {
                            xtype : 'hidden',
                            name : 'type',
                            value : 'language'
                        }]
                    },
                    buttons : [{
                        text : this.i18n._("Upload"),
                        name : "Upload",
                        handler : function() {
                            this.panelRegionalSettings.onUpload();
                        }.createDelegate(this)
                    }]
                }],
                onUpload : function() {
                    var prova = Ext.getCmp('upload_language_form');
                    var cmp = Ext.getCmp(this.parentId);

                    var form = prova.getForm();
                    form.submit({
                        parentId : cmp.getId(),
                        waitMsg : cmp.i18n._('Please wait while your language pack is uploaded...'),
                        success : function(form, action) {
                            languagesStore.load();
                            var cmp = Ext.getCmp(action.options.parentId);
                            if (action.result && action.result.msg) {
                                Ext.MessageBox.alert(cmp.i18n._("Warning"), cmp.i18n._("Language Pack Uploaded With Errors"));
                            } else {
                                Ext.MessageBox.alert(cmp.i18n._("Succeeded"), cmp.i18n._("Upload Language Pack Succeeded"));
                            }
                        },
                        failure : function(form, action) {
                            var cmp = Ext.getCmp(action.options.parentId);
                            var errorMsg = cmp.i18n._("Upload Language Pack Failed");
                            if (action.result && action.result.msg) {
                                switch (action.result.msg) {
                                    case 'Invalid Language Pack' : 
                                        errorMsg = cmp.i18n._("Invalid Language Pack");
                                    break;
                                    default :
                                        errorMsg = cmp.i18n._("Upload Language Pack Failed");
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
            var maxUriLengthCmp = Ext.getCmp('system_protocolSettings_maxUriLength');
            if (maxUriLengthCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("Max URI Length should be between 1024 and 4096!"),
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
            var maxHeaderLengthCmp = Ext.getCmp('system_protocolSettings_maxHeaderLength');
            if (maxHeaderLengthCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("Max Header Length should be between 1024 and 8192!"),
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
            var smtpTimeoutCmp = Ext.getCmp('system_protocolSettings_smtpTimeout');
            if (smtpTimeoutCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("SMTP timeout should be between 0 and 86400!"),
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
            var popTimeoutCmp = Ext.getCmp('system_protocolSettings_popTimeout');
            if (popTimeoutCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("POP timeout should be between 0 and 86400!"),
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
            var imapTimeoutCmp = Ext.getCmp('system_protocolSettings_imapTimeout');
            if (imapTimeoutCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("IMAP timeout should be between 0 and 86400!"),
                    function () {
                        this.tabs.activate(this.panelProtocolSettings);
                        imapTimeoutCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.saveSemaphore = 6;
                // save language settings
                rpc.languageManager.setLanguageSettings(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getLanguageSettings());
                
                // save network settings
                rpc.networkManager.setSettings(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getAccessSettings(), this.getMiscSettings());
                
                // save http settings
                if (this.isHttpLoaded()) {
                    this.getHttpNode().setHttpSettings(function(result, exception) {
                        if (exception) {
                            Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                            return;
                        }
                        this.afterSave();
                    }.createDelegate(this), this.getHttpSettings());
                } else {
                	this.saveSemaphore--;
                };
                
                // save ftp settings
                if (this.isFtpLoaded()) {
                    this.getFtpNode().setFtpSettings(function(result, exception) {
                        if (exception) {
                            Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                            return;
                        }
                        this.afterSave();
                    }.createDelegate(this), this.getFtpSettings());
                } else {
                    this.saveSemaphore--;
                };
                
                // save mail settings
                if (this.isMailLoaded()) {
                    this.getMailNode().setMailNodeSettings(function(result, exception) {
                        if (exception) {
                            Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                            return;
                        }
                        this.afterSave();
                    }.createDelegate(this), this.getMailSettings());
                } else {
                    this.saveSemaphore--;
                };
                
                //save timezone
                rpc.adminManager.setTimeZone(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.rpc.timeZone);
            }
        },
        afterSave : function() {
            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
            	Ext.MessageBox.hide();
                this.cancelAction();
            }
        }

    });

}
