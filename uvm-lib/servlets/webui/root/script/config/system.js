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
                name : 'panelUntangleSupport',
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
                        name : 'isSupportEnabled',
                        boxLabel : this.i18n._('<b>Allow</b> us to securely access your server for support purposes.'),
                        hideLabel : true,
                        checked : this.getAccessSettings().isSupportEnabled,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getAccessSettings().isSupportEnabled = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'checkbox',
                        name : 'isSupportEnabled',
                        boxLabel : this.i18n
                                ._('<b>Send</b> us data about your server. This will send us status updates and an email if any unexpected problems occur, but will not allow us to login to your server. No personal information about your network traffic will be transmitted.'),
                        hideLabel : true,
                        checked : this.getMiscSettings().isExceptionReportingEnabled,
                        listeners : {
                            "change" : {
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
                name : 'panelBackup',
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
                	Ext.MessageBox.progress(i18n._("Please wait"),i18n._("Backing Up..."));
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
                    Ext.MessageBox.progress(i18n._("Please wait"),i18n._("Backing Up..."));
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
                    	html: this.i18n._("You can backup your current system configuration to a file on your local computer for later restoration, in the event that you would like to replace new settings with your current settings.  The file name will end with \".backup\"<br> <br> After backing up your current system configuration to a file, you can then restore that configuration through this dialog by going to \"Restore\" -> \"From Local File\".")
                    }],
                    buttons : [{
                        text : this.i18n._("Backup to File"),
                        name: "backupToFileButton",
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
                        html: this.i18n._("You can backup your current system configuration to USB Key for later restoration, in the event that you would like to replace new settings with your current settings.<br>\n<br>\nAfter backing up your current system configuration to USB Key, you can then restore that configuration through the <b>Backup and Restore Utilities</b>.  To access the Backup and Restore Utilities, you must have a monitor and keyboard physically plugged into your server when it is turned on, and then select \"Backup and Restore Utilities\" from the boot prompt.<br>\n<br>\n<b>Note: You must insert your USB Key into a valid USB port on the back of your server before pressing the button.  You must not remove the USB Key from the USB port until after the process is complete.  The progress bar will inform you when the process is complete.</b>")
                    }],
                    buttons : [{
                    	name: "backupToUSBKeyButton",
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
                        html: this.i18n._("You can backup your current system configuration to Hard Disk for later restoration, in the event that you would like to replace new settings with your current settings.<br>\n<br>\nAfter backing up your current system configuration to Hard Disk, you can then restore that configuration through the <b>Backup and Restore Utilities</b>.  To access the Backup and Restore Utilities, you must have a monitor and keyboard physically plugged into your server when it is turned on, and then select \"Backup and Restore Utilities\" from the boot prompt.")
                    }],
                    buttons : [{
                        text : this.i18n._("Backup to Hard Disk"),
                        name: "backupToHardDiskButton",
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
                name : 'panelBackup',
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
                            if (action.result && action.result.msg) {
                                Ext.MessageBox.alert(cmp.i18n._("Restore Backup File Warning"), cmp.i18n._("Error:  The Local File restore procedure failed.  The reason reported by the Untangle Server was:")+cmp.i18n._(action.result.msg));
                            } else {
                                Ext.MessageBox.alert(cmp.i18n._("Restore Backup File Warning"), cmp.i18n._("Error:  The Local File restore procedure failed."));
                            }
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
                        name: "restoreFromFileButton",
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
                        html: this.i18n._("After backing up your system configuration, you can restore that configuration through the <b>Recovery Utilities</b> on your server once it is done booting.\n<br>\n<br>To access the <b>Recovery Utilities</b>, you must have a monitor and keyboard physically plugged into your server, and then click on the Recovery Utilities toolbar button when it is done booting.")
                    }]
                }]
            });
        },
        buildProtocolSettings : function() {
            var casingItems = new Array();
            
            if (this.isHttpLoaded()) {
            	casingItems.push(new Ext.form.FieldSet({
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
                            boxLabel : i18n.sprintf(this.i18n._('%sEnable Processing%s of web traffic.  (This is the default setting)'), '<b>', '</b>'), 
                            hideLabel : true,
                            name : 'httpEnabled',
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
                            boxLabel : i18n.sprintf(this.i18n._('%sDisable Processing%s of web traffic.'), '<b>', '</b>'), 
                            hideLabel : true,
                            name : 'httpEnabled',
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
                            boxLabel : i18n.sprintf(this.i18n._('%sEnable Processing%s of long URIs.  The traffic is considered \"Non-Http\".  (This is the default setting)'), '<b>', '</b>'), 
                            hideLabel : true,
                            name : 'httpBlockLongUris',
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
                            boxLabel : i18n.sprintf(this.i18n._('%sDisable Processing%s of long URIs.'), '<b>', '</b>'), 
                            hideLabel : true,
                            name : 'httpBlockLongUris',
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
                            name : 'maxUriLength',
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
                            boxLabel : i18n.sprintf(this.i18n._('%sEnable Processing%s of long headers.  The traffic is considered \"Non-Http\".  (This is the default setting)'), '<b>', '</b>'), 
                            hideLabel : true,
                            name : 'httpBlockLongHeaders',
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
                            boxLabel : i18n.sprintf(this.i18n._('%sDisable Processing%s of long headers.'), '<b>', '</b>'), 
                            hideLabel : true,
                            name : 'httpBlockLongHeaders',
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
                            name : 'maxHeaderLength',
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
                            boxLabel : i18n.sprintf(this.i18n._('%sAllow%s non-Http traffic to travel over port 80.  (This is the default setting)'), '<b>', '</b>'), 
                            hideLabel : true,
                            name : 'nonHttpBlocked',
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
                            boxLabel : i18n.sprintf(this.i18n._('%sStop%s non-Http traffic to travel over port 80.'), '<b>', '</b>'), 
                            hideLabel : true,
                            name : 'nonHttpBlocked',
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
                casingItems.push( new Ext.form.FieldSet({
                    title: this.i18n._('FTP'),
                    autoHeight : true,
                    items : [{
                        style : 'padding-bottom:10px;',
                        border: false,
                        html: this.i18n._("Warning:  These settings should not be changed unless instructed to do so by support.")                        
                    },{
                        xtype : 'radio',
                        boxLabel : i18n.sprintf(this.i18n._('%sEnable Processing%s of File Transfer traffic.  (This is the default setting)'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'ftpEnabled',
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
                        boxLabel : i18n.sprintf(this.i18n._('%sDisable Processing%s of File Transfer traffic.'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'ftpEnabled',
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
                casingItems.push(new Ext.form.FieldSet({
                    title: this.i18n._('SMTP'),
                    autoHeight : true,
                    items : [{
                        xtype : 'radio',
                        boxLabel : i18n.sprintf(this.i18n._('%sEnable SMTP%s email processing.  (This is the default setting)'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'smtpEnabled',
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
                        boxLabel : i18n.sprintf(this.i18n._('%sDisable SMTP%s email processing.'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'smtpEnabled',
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
                        name : 'smtpTimeout',
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
                casingItems.push(new Ext.form.FieldSet({
                    title: this.i18n._('POP3'),
                    autoHeight : true,
                    items : [{
                        xtype : 'radio',
                        boxLabel : i18n.sprintf(this.i18n._('%sEnable POP3%s email processing.  (This is the default setting)'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'popEnabled',
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
                        boxLabel : i18n.sprintf(this.i18n._('%sDisable POP3%s email processing.'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'popEnabled',
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
                        name : 'popTimeout',
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
                casingItems.push( new Ext.form.FieldSet({
                    title: this.i18n._('IMAP'),
                    autoHeight : true,
                    items : [{
                        xtype : 'radio',
                        boxLabel : i18n.sprintf(this.i18n._('%sEnable IMAP%s email processing.  (This is the default setting)'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'imapEnabled',
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
                        boxLabel : i18n.sprintf(this.i18n._('%sDisable IMAP%s email processing.'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'imapEnabled',
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
                        name : 'imapTimeout',
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
                name : 'panelProtocolSettings',
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
                items: casingItems.length != 0 ? casingItems : null
            });
        },
        buildRegionalSettings : function() {
            var languagesStore = new Ext.data.Store({
                proxy : new Ung.RpcProxy(rpc.languageManager.getLanguagesList, false),
                reader : new Ext.data.JsonReader({
                    root : 'list',
                    fields : ['code', 'name']
                })
            });
            var timeZonesData = [
                    ["GMT-12:00", "International Dateline West", "Etc/GMT-12"],
                    ["GMT-11:00", "Midway Island, Samoa", "Pacific/Midway"],
                    ["GMT-10:00", "Hawaii", "US/Hawaii"],
                    ["GMT-09:00", "Alaska", "US/Alaska"],
                    ["GMT-08:00", "Pacific Time (US & Canada), Tijuana", "US/Pacific"],
                    ["GMT-07:00", "Chihuahua, La Paz, Mazatlan", "America/Chihuahua"],
                    ["GMT-07:00", "Mountain Time (US & Canada)", "US/Mountain"],
                    ["GMT-06:00", "Central Time (US & Canada & Central America)", "US/Central"],
                    ["GMT-06:00", "Guadalajara, Mexico City, Monterrey", "America/Mexico_City"],
                    ["GMT-06:00", "Saskatchewan", "Canada/Saskatchewan"],
                    ["GMT-05:00", "Bogota, Lima, Quito", "America/Bogota"],
                    ["GMT-05:00", "Eastern Time (US & Canada)", "US/Eastern"],
                    ["GMT-05:00", "Indiana (East)", "US/East-Indiana"],
                    ["GMT-04:00", "Atlantic Time (Canada)", "Canada/Atlantic"],
                    ["GMT-04:00", "Caracas, La Paz", "America/Caracas"],
                    ["GMT-04:00", "Santiago", "America/Santiago"],
                    ["GMT-03:30", "Newfoundland", "Canada/Newfoundland"],
                    ["GMT-03:00", "Buenos Aires, Georgetown, Brasilia, Greenland", "America/Buenos_Aires"],
                    ["GMT-02:00", "Mid-Atlantic", "Etc/GMT-2"],
                    ["GMT-01:00", "Azores", "Atlantic/Azores"],
                    ["GMT-01:00", "Cape Verde Is.", "Atlantic/Cape_Verde"],
                    ["GMT", "Casablanca, Monrovia", "Africa/Casablanca"],
                    ["GMT", "Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London", "Etc/Greenwich"],
                    ["GMT+01:00", "Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna", "Europe/Amsterdam"],
                    ["GMT+01:00", "Belgrade, Bratislava, Budapest, Ljubljana, Prague", "Europe/Belgrade"],
                    ["GMT+01:00", "Brussels, Copenhagen, Madrid, Paris", "Europe/Brussels"],
                    ["GMT+01:00", "Sarajevo, Skopje, Warsaw, Zagreb", "Europe/Sarajevo"],
                    ["GMT+01:00", "West Central Africa", "Etc/GMT+1"],
                    ["GMT+02:00", "Athens, Beirut, Istanbul, Minsk", "Europe/Athens"],
                    ["GMT+02:00", "Bucharest", "Europe/Bucharest"],
                    ["GMT+02:00", "Cairo, Harare, Pretoria", "Africa/Cairo"],
                    ["GMT+02:00", "Helsinki, Kyiv, Riga, Sofia, Talinn, Vilnius", "Europe/Helsinki"],
                    ["GMT+02:00", "Jerusalem", "Asia/Jerusalem"],
                    ["GMT+03:00", "Baghdad", "Asia/Baghdad"],
                    ["GMT+03:00", "Kuwait, Riyadh", "Asia/Kuwait"],
                    ["GMT+03:00", "Moscow, St. Petersburg, Volgograd", "Europe/Moscow"],
                    ["GMT+03:00", "Nairobi", "Africa/Nairobi"],
                    ["GMT+03:30", "Tehran", "Asia/Tehran"],
                    ["GMT+04:00", "Abu Dhabi, Muscat", "Asia/Muscat"],
                    ["GMT+04:00", "Baku, Tbilsi, Yerevan", "Asia/Baku"],
                    ["GMT+04:30", "Kabul", "Asia/Kabul"],
                    ["GMT+05:00", "Ekaterinburg, Islamabad, Karachi, Tashkent", "Asia/Karachi"],
                    ["GMT+05:30", "Chennai, Kolkata, Mumbai, New Delhi", "Asia/Calcutta"], // NOTICE
                                                                                            // TWO
                                                                                            // SPELLINGS
                                                                                            // OF
                                                                                            // KOLKATA/CALCUTTA
                    ["GMT+05:45", "Kathmandu", "Asia/Katmandu"], // NOTICE
                                                                    // TWO
                                                                    // SPELLINGS
                                                                    // OF
                                                                    // KATMANDU/KATHMANDU
                    ["GMT+06:00", "Almaty, Novosibirsk", "Asia/Almaty"], ["GMT+06:00", "Astana, Dhaka", "Asia/Dhaka"],
                    ["GMT+06:30", "Rangoon", "Asia/Rangoon"], ["GMT+07:00", "Bangkok, Hanoi, Jakarta", "Asia/Bangkok"],
                    ["GMT+07:00", "Krasnoyarsk", "Asia/Krasnoyarsk"],
                    ["GMT+08:00", "Beijing, Chongqing, Hong Kong, Urumqi", "Asia/Hong_Kong"],
                    ["GMT+08:00", "Irkutsk, Ulaan Bataar", "Asia/Irkutsk"], ["GMT+08:00", "Kuala Lumpur, Singapore", "Asia/Kuala_Lumpur"],
                    ["GMT+08:00", "Perth", "Australia/Perth"], ["GMT+08:00", "Taipei", "Asia/Taipei"],
                    ["GMT+09:00", "Osaka, Sapporo, Tokyo", "Asia/Tokyo"], ["GMT+09:00", "Seoul", "Asia/Seoul"],
                    ["GMT+09:00", "Yakutsk", "Asia/Yakutsk"], ["GMT+09:30", "Adelaide", "Australia/Adelaide"],
                    ["GMT+09:30", "Darwin", "Australia/Darwin"], ["GMT+10:00", "Brisbane", "Australia/Brisbane"],
                    ["GMT+10:00", "Canberra, Melbourne, Sydney", "Australia/Canberra"],
                    ["GMT+10:00", "Guam, Port Moresby", "Pacific/Guam"], ["GMT+10:00", "Hobart", "Australia/Hobart"],
                    ["GMT+10:00", "Vladivostok", "Asia/Vladivostok"], ["GMT+11:00", "Magadan, Solomon Is., New Caledonia", "Asia/Magadan"],
                    ["GMT+12:00", "Auckland, Wellington", "Pacific/Auckland"],
                    ["GMT+12:00", "Fiji, Kamchatka, Marshall Is.", "Pacific/Fiji"]];
            var timeZones = [];
            for (var i = 0; i < timeZonesData.length; i++) {
                timeZones.push([timeZonesData[i][2], "(" + timeZonesData[i][0] + ") " + timeZonesData[i][1]]);
            }
            this.panelRegionalSettings = new Ext.Panel({
                // private fields
                name : 'panelRegionalSettings',
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
                        name : 'timezone',
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
                        name : "language",
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
                            Ext.MessageBox.alert(cmp.i18n._("Succeeded"), cmp.i18n._("Upload Language Pack Succeeded"));
                        },
                        failure : function(form, action) {
                            var cmp = Ext.getCmp(action.options.parentId);
                            if (action.result && action.result.msg) {
                                Ext.MessageBox.alert(cmp.i18n._("Failed"), cmp.i18n._(action.result.msg));
                            } else {
                                Ext.MessageBox.alert(cmp.i18n._("Failed"), cmp.i18n._("Upload Language Pack Failed"));
                            }
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
                Ext.MessageBox.alert('Warning', this.i18n._("Max URI Length should be between 1024 and 4096!"));
                return false;
            }
        },
        //validate Max Header Length
        validateMaxHeaderLength : function() {
            var maxHeaderLengthCmp = Ext.getCmp('system_protocolSettings_maxHeaderLength');
            if (maxHeaderLengthCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert('Warning', this.i18n._("Max Header Length should be between 1024 and 8192!"));
                return false;
            }
        },
        //validate SMTP timeout
        validateSMTP : function() {
            var smtpTimeoutCmp = Ext.getCmp('system_protocolSettings_smtpTimeout');
            if (smtpTimeoutCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert('Warning', this.i18n._("SMTP timeout should be between 0 and 86400!"));
                return false;
            }
        },
        //validate POP timeout
        validatePOP : function() {
            var popTimeoutCmp = Ext.getCmp('system_protocolSettings_popTimeout');
            if (popTimeoutCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert('Warning', this.i18n._("POP timeout should be between 0 and 86400!"));
                return false;
            }
        },
        //validate IMAP timeout
        validateIMAP : function() {
            var imapTimeoutCmp = Ext.getCmp('system_protocolSettings_imapTimeout');
            if (imapTimeoutCmp.isValid()) {
                return true;
            } else {
                Ext.MessageBox.alert('Warning', this.i18n._("IMAP timeout should be between 0 and 86400!"));
                return false;
            }
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
            	Ext.MessageBox.progress(i18n._("Please wait"), i18n._("Saving..."));
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