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
            // builds the tabs
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
            this.tabs.activate(this.panelAdministration);
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
                items : [this.gridAdminAccounts=new Ung.EditorGrid({
                    settingsCmp : this,
                    title : this.i18n._("Admin Accounts"),
                    height : 350,
                    autoScroll : true,
                    hasEdit : false,
                    name : 'gridAdminAccounts',
                    recordJavaClass : "com.untangle.uvm.security.User",
                    emptyRow : {
                        "login" : this.i18n._("[no login]"),
                        "name" : this.i18n._("[no description]"),
                        "readOnly" : false,
                        "email" : this.i18n._("[no email]"),
                        "clearPassword" : "",
                        "javaClass" : "com.untangle.uvm.security.User"
                    },
                    // the column is autoexpanded if the grid width permits
                    autoExpandColumn : 'name',
                    
                    data : storeData,
                    dataRoot: null,
                    // the list of fields; we need all as we get/set all records once 
                    fields : [{
                        name : 'id'
                    }, {
                        name : 'login'
                    }, {
                        name : 'name'
                    }, {
                        name : 'email'
                    }, {
                        name : 'notes'
                    }, {
                        name : 'sendAlerts'
                    }, {
                        name : 'readOnly'
                    }, {
                        name : 'javaClass' //needed as users is a set
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
                            allowBlank : false,
                            blankText : this.i18n._("The login name cannot be blank.")
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
                    }, {
                        id : 'password',
                        header : this.i18n._("password"),
                        width : 150,
                        dataIndex : null,
                        renderer: function(value, meta, record, row, col, store) {
                          var id = Ext.id();
                          var btn = new Ext.Button({
                            text: this.i18n._("change password"),
                            handler : function(record) {
                                // populate row editor
                                this.gridAdminAccounts.rowEditorChangePass.populate(record);
                                this.gridAdminAccounts.rowEditorChangePass.show();
                            }.createDelegate(this,[record])
                          });
                          btn.render.defer(1, btn, [id]);
                          return '<div  id=' + id + '></div>';
                        }.createDelegate(this)                    
                    }],
                    sortField : 'login',
                    columnsDefaultSortable : true,
                    plugins : [readOnlyColumn],
                    // the row input lines used by the row editor window
                    rowEditorInputLines : [new Ext.form.TextField({
                        name : "login",
                        fieldLabel : this.i18n._("Login"),
                        allowBlank : false,
                        blankText : this.i18n._("The login name cannot be blank."),
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
                    }), new Ext.form.TextField({
                    	inputType: 'password',
                        name : "clearPassword",
                        id : 'administration_rowEditor_password',
                        fieldLabel : this.i18n._("Password"),
                        width : 200,
                        minLength : 3,
                        minLengthText : i18n.sprintf(this.i18n._("The password is shorter than the minimum %d characters."), 3)
                    }), new Ext.form.TextField({
                        inputType: 'password',
//                        name : "confirm_password",
                        name : "clearPassword",
                        vtype: 'password',
                        initialPassField: 'administration_rowEditor_password', // id of the initial password field
                        fieldLabel : this.i18n._("Confirm Password"),
                        width : 200
                    })],
                    // the row input lines used by the change password window
                    rowEditorChangePassInputLines : [new Ext.form.TextField({
                        inputType: 'password',
                        name : "clearPassword",
                        id : 'administration_rowEditor1_password',
                        fieldLabel : this.i18n._("Password"),
                        width : 200,
                        minLength : 3,
                        minLengthText : i18n.sprintf(this.i18n._("The password is shorter than the minimum %d characters."), 3)                        
                    }), new Ext.form.TextField({
                        inputType: 'password',
//                        name : "confirm_password",
                        name : "clearPassword",
                        vtype: 'password',
                        initialPassField: 'administration_rowEditor1_password', // id of the initial password field
                        fieldLabel : this.i18n._("Confirm Password"),
                        width : 200
                    })]
                }), {
                	xtype : 'fieldset',
                	title : this.i18n._('External Administration'),
                    autoHeight : true,
                	items : [{
                		html : 'test'
                	}] 
                },{
                    xtype : 'fieldset',
                    title : this.i18n._('Internal Administration'),
                    autoHeight : true,
                    items : [{
                        html : 'Note: HTTPS administration is always enabled internally'
                    }] 
                }]
            });

            if ( this.gridAdminAccounts.rowEditorChangePassInputLines != null) {
                 this.gridAdminAccounts.rowEditorChangePass = new Ung.RowEditorWindow({
                    grid : this.gridAdminAccounts,
                    inputLines : this.gridAdminAccounts.rowEditorChangePassInputLines
                });
                 this.gridAdminAccounts.rowEditorChangePass.render('container');
            }
                                
            
            
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
                                    if (checked) {
                                        Ext.getCmp('administration_snmp_communityString').disable();
                                        Ext.getCmp('administration_snmp_sysContact').disable();
                                        Ext.getCmp('administration_snmp_sysLocation').disable();
                                        Ext.getCmp('administration_snmp_sendTraps_disable').disable();
                                        Ext.getCmp('administration_snmp_sendTraps_enable').disable();
                                        Ext.getCmp('administration_snmp_trapCommunity').disable();
                                        Ext.getCmp('administration_snmp_trapHost').disable();
                                        Ext.getCmp('administration_snmp_trapPort').disable();
                                    }
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
                                    if (checked) {
                                        Ext.getCmp('administration_snmp_communityString').enable();
                                        Ext.getCmp('administration_snmp_sysContact').enable();
                                        Ext.getCmp('administration_snmp_sysLocation').enable();
                                        Ext.getCmp('administration_snmp_sendTraps_disable').enable();
                                        var sendTrapsEnableCmp = null;
                                        (sendTrapsEnableCmp = Ext.getCmp('administration_snmp_sendTraps_enable')).enable();
                                        if (sendTrapsEnableCmp.getValue()){
                                            Ext.getCmp('administration_snmp_trapCommunity').enable();
                                            Ext.getCmp('administration_snmp_trapHost').enable();
                                            Ext.getCmp('administration_snmp_trapPort').enable();
                                        }
                                    }
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Community'),
                        name : 'communityString',
                        id: 'administration_snmp_communityString',
                        value : this.getSnmpSettings().communityString,
                        allowBlank : false,
                        blankText : this.i18n._("An SNMP \"Community\" must be specified.")                        
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('System Contact'),
                        name : 'sysContact',
                        id: 'administration_snmp_sysContact',
                        value : this.getSnmpSettings().sysContact
                        //vtype : 'email'
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('System Location'),
                        name : 'sysLocation',
                        id: 'administration_snmp_sysLocation',
                        value : this.getSnmpSettings().sysLocation
                    },{
                        border: false,
                        html : '<hr>'
                    },{
                        xtype : 'radio',
                        boxLabel : i18n.sprintf(this.i18n._('%sDisable Traps%s so no trap events are generated.  (This is the default setting.)'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'sendTraps',
                        id: 'administration_snmp_sendTraps_disable',                        
                        checked : !this.getSnmpSettings().sendTraps,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    if (checked) {
                                        Ext.getCmp('administration_snmp_trapCommunity').disable();
                                        Ext.getCmp('administration_snmp_trapHost').disable();
                                        Ext.getCmp('administration_snmp_trapPort').disable();
                                    }
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'radio',
                        boxLabel : i18n.sprintf(this.i18n._('%sEnable Traps%s so trap events are sent when they are generated.'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'sendTraps',
                        id: 'administration_snmp_sendTraps_enable',
                        checked : this.getSnmpSettings().sendTraps,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    if (this.getSnmpSettings().enabled && checked) {
                                        Ext.getCmp('administration_snmp_trapCommunity').enable();
                                        Ext.getCmp('administration_snmp_trapHost').enable();
                                        Ext.getCmp('administration_snmp_trapPort').enable();
                                    }
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Community'),
                        name : 'trapCommunity',
                        id: 'administration_snmp_trapCommunity',
                        value : this.getSnmpSettings().trapCommunity,
                        allowBlank : false,
                        blankText : this.i18n._("An Trap \"Community\" must be specified.")                        
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Host'),
                        name : 'trapHost',
                        id: 'administration_snmp_trapHost',
                        value : this.getSnmpSettings().trapHost,
                        allowBlank : false,
                        blankText : this.i18n._("An Trap \"Host\" must be specified.")                        
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Port'),
                        name : 'trapPort',
                        id: 'administration_snmp_trapPort',
                        value : this.getSnmpSettings().trapPort,
                        width: 50,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 1,                        
                        maxValue: 65535,
                        minText: i18n.sprintf(this.i18n._("The port must be an integer number between %d and %d."), 1, 65535),
                        maxText: i18n.sprintf(this.i18n._("The port must be an integer number between %d and %d."), 1, 65535)
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
                                    if (checked) {
                                        Ext.getCmp('administration_syslog_host').disable();
                                        Ext.getCmp('administration_syslog_port').disable();
                                        Ext.getCmp('administration_syslog_facility').disable();
                                        Ext.getCmp('administration_syslog_threshold').disable();
                                    }
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
                                    if (checked) {
                                        Ext.getCmp('administration_syslog_host').enable();
                                        Ext.getCmp('administration_syslog_port').enable();
                                        Ext.getCmp('administration_syslog_facility').enable();
                                        Ext.getCmp('administration_syslog_threshold').enable();
                                    }
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Host'),
                        name : 'syslogHost',
                        id : 'administration_syslog_host',
                        value : this.getLoggingSettings().syslogHost,
                        allowBlank : false,
                        blankText : this.i18n._("A \"Host\" must be specified.")
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Port'),
                        name : 'syslogPort',
                        id : 'administration_syslog_port',
                        value : this.getLoggingSettings().syslogPort,
                        width: 50,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: 1,                        
                        maxValue: 65535,
                        minText: i18n.sprintf(this.i18n._("The port must be an integer number between %d and %d."), 1, 65535),
                        maxText: i18n.sprintf(this.i18n._("The port must be an integer number between %d and %d."), 1, 65535)
                    },{
                        xtype : 'combo',
                        name : 'syslogFacility',
                        id : 'administration_syslog_facility',
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
                        value : this.getLoggingSettings().syslogFacility
                    },{
                        xtype : 'combo',
                        name : 'syslogThreshold',
                        id : 'administration_syslog_threshold',
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
                        value : this.getLoggingSettings().syslogThreshold
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
        // validation function
        validateClient : function() {
            return  this.validateAdminAccounts() && this.validateSnmp() && this.validateSyslog(); 
        },
        
        //validate Admin Accounts
        validateAdminAccounts : function() {
            var listAdminAccounts = this.gridAdminAccounts.getFullSaveList();
            var oneWritableAccount = false;
            
            // verify that the login name is not duplicated
            for(var i=0; i<listAdminAccounts.length;i++) {
                for(var j=i+1; j<listAdminAccounts.length;j++) {
                	if (listAdminAccounts[i].login == listAdminAccounts[j].login) {
                        Ext.MessageBox.alert('Warning', i18n.sprintf(this.i18n._("The login name: \"%s\" in row: %d  already exists."), listAdminAccounts[j].login, j+1));
                		return false;
                	}
                }
                
                if (!listAdminAccounts[i].readOnly) {
                    oneWritableAccount = true;
                }
            	
            }
            
            // verify that there is at least one valid entry after all operations
            if(listAdminAccounts.length <= 0 ){
                Ext.MessageBox.alert('Warning', this.i18n._("There must always be at least one valid account."));
                return false;
            }
        
            // verify that there was at least one non-read-only account
            if(!oneWritableAccount){
                Ext.MessageBox.alert('Warning', this.i18n._("There must always be at least one non-read-only (writable) account."));
                return false;
            }
            
        	return true;
        },

        //validate SNMP
        validateSnmp : function() {
            var isSnmpEnabled = this.getSnmpSettings().enabled;
            if (isSnmpEnabled) {
                var snmpCommunityCmp = Ext.getCmp('administration_snmp_communityString');
                if (!snmpCommunityCmp.isValid()) {
                    Ext.MessageBox.alert('Warning', this.i18n._("An SNMP \"Community\" must be specified."));
                    return false;
                }
                
                var sendTrapsEnableCmp = Ext.getCmp('administration_snmp_sendTraps_enable');
                var isTrapEnabled = sendTrapsEnableCmp.getValue();
                var snmpTrapCommunityCmp, snmpTrapHostCmp, snmpTrapPortCmp;                
                if (isTrapEnabled) {
                    snmpTrapCommunityCmp = Ext.getCmp('administration_snmp_trapCommunity');
                    if (!snmpTrapCommunityCmp.isValid()) {
                        Ext.MessageBox.alert('Warning', this.i18n._("An Trap \"Community\" must be specified."));
                        return false;
                    }
                    
                    snmpTrapHostCmp = Ext.getCmp('administration_snmp_trapHost');
                    if (!snmpTrapHostCmp.isValid()) {
                        Ext.MessageBox.alert('Warning', this.i18n._("An Trap \"Host\" must be specified."));
                        return false;
                    }
                    
                    snmpTrapPortCmp = Ext.getCmp('administration_snmp_trapPort');
                    if (!snmpTrapPortCmp.isValid()) {
                        Ext.MessageBox.alert('Warning', i18n.sprintf(this.i18n._("The port must be an integer number between %d and %d."), 1, 65535));
                        return false;
                    }
                }
                
                //prepare for save
                var snmpSysContactCmp = Ext.getCmp('administration_snmp_sysContact');
                var snmpSysLocationCmp = Ext.getCmp('administration_snmp_sysLocation');
                
                this.getSnmpSettings().communityString = snmpCommunityCmp.getValue();
                this.getSnmpSettings().sysContact = snmpSysContactCmp.getValue();
                this.getSnmpSettings().sysLocation = snmpSysLocationCmp.getValue();
                this.getSnmpSettings().sendTraps = isTrapEnabled;
                if (isTrapEnabled) {
                    this.getSnmpSettings().trapCommunity = snmpTrapCommunityCmp.getValue();
                    this.getSnmpSettings().trapHost = snmpTrapHostCmp.getValue();
                    this.getSnmpSettings().trapPort = snmpTrapPortCmp.getValue();
                }
            }
            return true;
        },
        
        //validate Syslog
        validateSyslog : function() {
        	var isSyslogEnabled = this.getLoggingSettings().syslogEnabled;
        	if (isSyslogEnabled) {
                var syslogHostCmp = Ext.getCmp('administration_syslog_host');
                if (!syslogHostCmp.isValid()) {
                    Ext.MessageBox.alert('Warning', this.i18n._("A \"Host\" must be specified."));
                    return false;
                }
                var syslogPortCmp = Ext.getCmp('administration_syslog_port');
                if (!syslogPortCmp.isValid()) {
                    Ext.MessageBox.alert('Warning', i18n.sprintf(this.i18n._("The port must be an integer number between %d and %d."), 1, 65535));
                    return false;
                }
                //prepare for save
                var syslogFacilityCmp = Ext.getCmp('administration_syslog_facility');
                var syslogThresholdCmp = Ext.getCmp('administration_syslog_threshold');
                
                this.getLoggingSettings().syslogHost = syslogHostCmp.getValue();
                this.getLoggingSettings().syslogPort = syslogPortCmp.getValue();
                this.getLoggingSettings().syslogFacility = syslogFacilityCmp.getValue();
                this.getLoggingSettings().syslogThreshold = syslogThresholdCmp.getValue();
        	}
        	return true;
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
            	this.saveSemaphore = 4;
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                
                var listAdministration=this.gridAdminAccounts.getFullSaveList();
                var setAdministration={};
                for(var i=0; i<listAdministration.length;i++) {
                    setAdministration[i]=listAdministration[i];
                }
                this.getAdminSettings().users.set=setAdministration;
                rpc.adminManager.setAdminSettings(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getAdminSettings());

               rpc.adminManager.getSnmpManager().setSnmpSettings(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getSnmpSettings());
                
                main.getLoggingManager().setLoggingSettings(function(result, exception) {
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