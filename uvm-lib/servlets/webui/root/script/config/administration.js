if (!Ung.hasResource["Ung.Administration"]) {
    Ung.hasResource["Ung.Administration"] = true;

    Ung.Administration = Ext.extend(Ung.ConfigWin, {
        panelAdministration : null,
        panelPublicAddress : null,
        panelCertificates : null,
        panelMonitoring : null,
        panelSkins : null,
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }, {
                title : i18n._('Administration')
            }];
            Ung.Administration.superclass.initComponent.call(this);
        },

        onRender : function(container, position) {
            // call superclass renderer first
            Ung.Administration.superclass.onRender.call(this, container, position);
            this.initSubCmps.defer(1, this);
            // builds the 2 tabs
        },
        initSubCmps : function() {
            this.buildAdministration();
            this.buildPublicAddress();
            this.buildCertificates();
            this.buildMonitoring();
            this.buildSkins();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelAdministration, this.panelPublicAddress, this.panelCertificates, this.panelMonitoring,
                    this.panelSkins]);
            this.tabs.activate(this.panelMonitoring);
            this.panelPublicAddress.disable();
            this.panelCertificates.disable();

        },
        // get base settings object
        getSkinSettings : function(forceReload) {
            if (forceReload || this.rpc.skinSettings === undefined) {
                this.rpc.skinSettings = rpc.skinManager.getSkinSettings();
            }
            return this.rpc.skinSettings;
        },
        // get admin settings
        getAdminSettings : function(forceReload) {
            if (forceReload || this.rpc.adminSettings === undefined) {
                this.rpc.adminSettings = rpc.adminManager.getAdminSettings();
            }
            return this.rpc.adminSettings;
        },
        // get admin settings
        getSnmpSettings : function(forceReload) {
            if (forceReload || this.rpc.snmpSettings === undefined) {
                this.rpc.snmpSettings = rpc.adminManager.getSnmpManager().getSnmpSettings();
            }
            return this.rpc.snmpSettings;
        },
        // get logging settings
        getLoggingSettings : function(forceReload) {
            if (forceReload || this.rpc.loggingSettings === undefined) {
                this.rpc.loggingSettings = main.getLoggingManager().getLoggingSettings();
            }
            return this.rpc.loggingSettings;
        },
        
        getTODOPanel : function(title) {
            return new Ext.Panel({
                title : this.i18n._(title),
                layout : "form",
                autoScroll : true,
                bodyStyle : 'padding:5px 5px 0px 5px;',
                items : [{
                    xtype : 'fieldset',
                    title : this.i18n._(title),
                    autoHeight : true,
                    items : [{
                        xtype : 'textfield',
                        fieldLabel : 'TODO',
                        name : 'todo',
                        allowBlank : false,
                        value : 'todo',
                        disabled : true
                    }]
                }]
            });
        },
        buildAdministration : function() {
            // read-only is a check column
            var readOnlyColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("read-only"),
                dataIndex : 'readOnly',
                fixed : true
            });
            var storeData=[];
            var storeDataSet=this.getAdminSettings().users.set;
            for(var id in storeDataSet) {
            	storeData.push(storeDataSet[id]);
            }
            this.panelAdministration = new Ext.Panel({
                name : 'panelAdministration',
                // private fields
                parentId : this.getId(),

                title : this.i18n._('Administration'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    autoHeight : true
                },
                items : [this.gridAdministration=new Ung.EditorGrid({
                    settingsCmp : this,
                    name : 'gridAdminAccounts',
//                    // the total records is set from the base settings
//                    // patternsLength field
//                    totalRecords : this.getBaseSettings().patternsLength,
                    emptyRow : {
                        "login" : this.i18n._("[no login]"),
                        "name" : this.i18n._("[no description]"),
                        "readOnly" : false,
                        "email" : this.i18n._("[no email]")
//                        ,
//                        "clearPassword" : ""
                    },
                    title : this.i18n._("Admin Accounts"),
                    // the column is autoexpanded if the grid width permits
                    autoExpandColumn : 'name',
                    recordJavaClass : "com.untangle.uvm.security.User",
                    
                    data : storeData,
                    dataRoot: null,
                    // the list of fields
                    fields : [{
                        name : 'id'
                    }, {
                        name : 'login'
                    },
                    // this field is internationalized so a converter was added
                    {
                        name : 'name',
                        convert : function(v) {
                            return this.i18n._(v)
                        }.createDelegate(this)
                    }, {
                        name : 'readOnly'
                    }, {
                        name : 'email'
//                    }, {
//                        name : 'clearPassword'
                    }],
                    // the list of columns for the column model
                    columns : [{
                        id : 'id',
                        dataIndex : 'id',
                        hidden : true
                    }, {
                        id : 'login',
                        header : this.i18n._("login"),
                        width : 200,
                        dataIndex : 'login',
                        editor : new Ext.form.TextField({
                            allowBlank : false
                        })
                    }, {
                        id : 'name',
                        header : this.i18n._("description"),
                        width : 200,
                        dataIndex : 'name',
                        editor : new Ext.form.TextField({
                            allowBlank : false
                        })
                    }, readOnlyColumn, {
                        id : 'email',
                        header : this.i18n._("email"),
                        width : 200,
                        dataIndex : 'email',
                        editor : new Ext.form.TextField({
                            allowBlank : false
                        })
                    }
//                    , {
//                        id : 'password',
//                        header : this.i18n._("description"),
//                        width : 200,
//                        dataIndex : 'description',
//                        editor : new Ext.form.TextField({
//                            allowBlank : false
//                        })
//                    }
                    ],
                    sortField : 'login',
                    columnsDefaultSortable : true,
                    plugins : [readOnlyColumn],
                    // the row input lines used by the row editor window
                    rowEditorInputLines : [new Ext.form.TextField({
                        name : "login",
                        fieldLabel : this.i18n._("Login"),
                        allowBlank : false,
                        width : 200
                    }), new Ext.form.TextField({
                        name : "name",
                        fieldLabel : this.i18n._("Description"),
                        allowBlank : false,
                        width : 200
                    }), new Ext.form.Checkbox({
                        name : "readOnly",
                        fieldLabel : this.i18n._("Read-only")
                    }), new Ext.form.TextField({
                        name : "email",
                        fieldLabel : this.i18n._("Email"),
                        width : 200
//                    }), new Ext.form.TextField({
//                        name : "password",
//                        fieldLabel : this.i18n._("Password"),
//                        width : 200
                    })]
                })]
            })
        },
        buildPublicAddress : function() {
            this.panelPublicAddress = this.getTODOPanel("Public Address");
        },
        buildCertificates : function() {
            this.panelCertificates = this.getTODOPanel("Certificates");
        },
        buildMonitoring : function() {
            this.panelMonitoring = new Ext.Panel({
                name : 'panelMonitoring',
                // private fields
                parentId : this.getId(),

                title : this.i18n._('Monitoring'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true
                },
                items: [{
                    title: this.i18n._('SNMP'),
                    items : [{
                        xtype : 'radio',
                        boxLabel : i18n.sprintf(this.i18n._('%sDisable%s SNMP Monitoring. (This is the default setting.)'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'snmpEnabled',
                        checked : !this.getSnmpSettings().enabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getSnmpSettings().enabled = !checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'radio',
                        boxLabel : i18n.sprintf(this.i18n._('%sEnable%s SNMP Monitoring.'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'snmpEnabled',
                        checked : this.getSnmpSettings().enabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getSnmpSettings().enabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Community'),
//                        labelStyle: 'width:200px;',
                        name : 'communityString',
                        value : this.getSnmpSettings().communityString,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getSnmpSettings().communityString = newValue;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('System Contact'),
//                        labelStyle: 'width:200px;',
                        name : 'sysContact',
                        value : this.getSnmpSettings().sysContact,
                        vtype : 'email',
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getSnmpSettings().sysContact = newValue;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('System Location'),
//                        labelStyle: 'width:200px;',
                        name : 'sysLocation',
                        value : this.getSnmpSettings().sysLocation,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getSnmpSettings().sysLocation = newValue;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        border: false,
                        html : '<hr>'
                    },{
                        xtype : 'radio',
                        boxLabel : i18n.sprintf(this.i18n._('%sDisable Traps%s so no trap events are generated.  (This is the default setting.)'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'sendTraps',
                        checked : !this.getSnmpSettings().sendTraps,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getSnmpSettings().sendTraps = !checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'radio',
                        boxLabel : i18n.sprintf(this.i18n._('%sEnable Traps%s so trap events are sent when they are generated.'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'sendTraps',
                        checked : this.getSnmpSettings().sendTraps,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getSnmpSettings().sendTraps = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Community'),
//                        labelStyle: 'width:200px;',
                        name : 'trapCommunity',
                        value : this.getSnmpSettings().trapCommunity,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getSnmpSettings().trapCommunity = newValue;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Host'),
//                        labelStyle: 'width:200px;',
                        name : 'trapHost',
                        value : this.getSnmpSettings().trapHost,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getSnmpSettings().trapHost = newValue;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Port'),
//                        labelStyle: 'width:200px;',
                        name : 'trapPort',
                        value : this.getSnmpSettings().trapPort,
                        width: 50,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 0,                        
                        maxValue: 86400,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getSnmpSettings().trapPort = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    title: this.i18n._('Syslog'),
                    items: [{
                        xtype : 'radio',
                        boxLabel : i18n.sprintf(this.i18n._('%sDisable%s Syslog Monitoring. (This is the default setting.)'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'syslogEnabled',
                        checked : !this.getLoggingSettings().syslogEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getLoggingSettings().syslogEnabled = !checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'radio',
                        boxLabel : i18n.sprintf(this.i18n._('%sEnable%s Syslog Monitoring.'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'syslogEnabled',
                        checked : this.getLoggingSettings().syslogEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getLoggingSettings().syslogEnabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Host'),
//                        labelStyle: 'width:200px;',
                        name : 'syslogHost',
                        value : this.getLoggingSettings().syslogHost,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getLoggingSettings().syslogHost = newValue;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Port'),
//                        labelStyle: 'width:200px;',
                        name : 'syslogPort',
                        value : this.getLoggingSettings().syslogPort,
                        width: 50,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 0,                        
                        maxValue: 86400,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getLoggingSettings().trapPort = syslogPort;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'combo',
                        name : 'syslogFacility',
                        editable : false,
                        fieldLabel : this.i18n._('Facility'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        store : new Ext.data.SimpleStore({
                            fields : ['key', 'name'],
                            data :[
                                ["KERNEL", this.i18n._("kernel")],
                                ["USER", this.i18n._("user")],
                                ["MAIL", this.i18n._("mail")],
                                ["DAEMON", this.i18n._("daemon")],
                                ["SECURITY", this.i18n._("security 0")],
                                ["SYSLOG", this.i18n._("syslog")],
                                ["PRINTER", this.i18n._("printer")],
                                ["NEWS", this.i18n._("news")],
                                ["UUCP", this.i18n._("uucp")],
                                ["CLOCK_0", this.i18n._("clock 0")],
                                ["SECURITY_1", this.i18n._("security 1")],
                                ["FTP", this.i18n._("ftp")],
                                ["NTP", this.i18n._("ntp")],
                                ["LOG_AUDIT", this.i18n._("log audit")],
                                ["LOG_ALERT", this.i18n._("log alert")],
                                ["CLOCK_1", this.i18n._("clock 1")],
                                ["LOCAL_0", this.i18n._("local 0")],
                                ["LOCAL_1", this.i18n._("local 1")],
                                ["LOCAL_2", this.i18n._("local 2")],
                                ["LOCAL_3", this.i18n._("local 3")],
                                ["LOCAL_4", this.i18n._("local 4")],
                                ["LOCAL_5", this.i18n._("local 5")],
                                ["LOCAL_6", this.i18n._("local 6")],
                                ["LOCAL_7", this.i18n._("local 7")]
                            ]        
                        }),
                        displayField : 'name',
                        valueField : 'key',
                        value : this.getLoggingSettings().syslogFacility,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getLoggingSettings().syslogFacility = newValue;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'combo',
                        name : 'syslogThreshold',
                        editable : false,
                        fieldLabel : this.i18n._('Threshold'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        store : new Ext.data.SimpleStore({
                            fields : ['key', 'name'],
                            data :[
                                ["EMERGENCY", this.i18n._("emergency")],
                                ["ALERT", this.i18n._("alert")],
                                ["CRITICAL", this.i18n._("critical")],
                                ["ERROR", this.i18n._("error")],
                                ["WARNING", this.i18n._("warning")],
                                ["NOTICE", this.i18n._("notice")],
                                ["INFORMATIONAL", this.i18n._("informational")],
                                ["DEBUG", this.i18n._("debug")]
                            ]        
                        }),
                        
                        displayField : 'name',
                        valueField : 'key',
                        value : this.getLoggingSettings().syslogThreshold,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getLoggingSettings().syslogThreshold = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }]
            });
        },
        buildSkins : function() {
            var skinsStore = new Ext.data.Store({
                proxy : new Ung.RpcProxy(rpc.skinManager.getSkinsList, false),
                reader : new Ung.JsonListReader({
                    root : 'list',
                    fields : ['skinName']
                })
            });

            this.panelSkins = new Ext.Panel({
                name : "panelSkins",
                // private fields
                parentId : this.getId(),
                title : this.i18n._('Skins'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Administration Skin'),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._("This skin will used in the administration client")
                    }, {
                        xtype : 'combo',
                        name : "administrationClientSkin",
                        store : skinsStore,
                        displayField : 'skinName',
                        valueField : 'skinName',
                        forceSelection : true,
                        value : this.getSkinSettings().administrationClientSkin,
                        typeAhead : true,
                        mode : 'remote',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        selectOnFocus : true,
                        hideLabel : true,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getSkinSettings().administrationClientSkin = newValue;
                                    Ext.MessageBox.alert(this.i18n._("Info"), this.i18n
                                            ._("Please note that you have to relogin after saving for the new skin to take effect."));
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    title : this.i18n._('Block Page Skin'),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._("This skin will used in the user pages like quarantine and block pages")
                    }, {
                        xtype : 'combo',
                        name : "userPagesSkin",
                        store : skinsStore,
                        displayField : 'skinName',
                        valueField : 'skinName',
                        forceSelection : true,
                        value : this.getSkinSettings().userPagesSkin,
                        typeAhead : true,
                        mode : 'remote',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        selectOnFocus : true,
                        hideLabel : true,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getSkinSettings().userPagesSkin = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    title : this.i18n._('Upload New Skin'),
                    items : {
                        fileUpload : true,
                        xtype : 'form',
                        id : 'upload_skin_form',
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
                            value : 'skin'
                        }]
                    },
                    buttons : [{
                        text : this.i18n._("Upload"),
                        handler : function() {
                            this.panelSkins.onUpload();
                        }.createDelegate(this)
                    }]
                }],
                onUpload : function() {
                    var prova = Ext.getCmp('upload_skin_form');
                    var cmp = Ext.getCmp(this.parentId);

                    var form = prova.getForm();
                    form.submit({
                        parentId : cmp.getId(),
                        waitMsg : cmp.i18n._('Please wait while your skin is uploaded...'),
                        success : function(form, action) {
                            skinsStore.load();
                            var cmp = Ext.getCmp(action.options.parentId);
                            Ext.MessageBox.alert(cmp.i18n._("Succeeded"), cmp.i18n._("Upload Skin Succeeded"));
                        },
                        failure : function(form, action) {
                            var cmp = Ext.getCmp(action.options.parentId);
                            if (action.result && action.result.msg) {
                                Ext.MessageBox.alert(cmp.i18n._("Failed"), cmp.i18n._(action.result.msg));
                            } else {
                                Ext.MessageBox.alert(cmp.i18n._("Failed"), cmp.i18n._("Upload Skin Failed"));
                            }
                        }
                    });
                }
            });
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
            	this.saveSemaphore = 3;
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                
//                var listAdministration=this.gridAdministration.getFullSaveList();
//                var setAdministration={};
//                for(var i=0; i<listAdministration.length;i++) {
//                    setAdministration[i]=listAdministration[i];
//                }
//                this.getAdminSettings().users.set=setAdministration;
//                rpc.adminManager.setAdminSettings(function(result, exception) {
//                    if (exception) {
//                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
//                        return;
//                    }
//                    this.afterSave();
//                }.createDelegate(this), this.getAdminSettings());

               rpc.adminManager.getSnmpManager().setSnmpSettings(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getSnmpSettings());
                
                main.getLoggingManager.setLoggingSettings(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getLoggingSettings());
                
                rpc.skinManager.setSkinSettings(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getSkinSettings());
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