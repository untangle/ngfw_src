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
            if (!this.isBrandingExpired()) {
                this.buildBranding();
            }
            
            // builds the tab panel with the tabs
            var adminTabs = [this.panelAdministration, this.panelPublicAddress, this.panelCertificates, this.panelMonitoring, this.panelSkins];
            if (!this.isBrandingExpired()) {
                adminTabs.push(this.panelBranding);
            }
            this.buildTabPanel(adminTabs);
            this.tabs.activate(this.panelAdministration);

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
        // get access settings
        getAccessSettings : function(forceReload) {
            if (forceReload || this.rpc.accessSettings === undefined) {
                this.rpc.accessSettings = rpc.networkManager.getAccessSettings();
            }
            return this.rpc.accessSettings;
        },
        // get address settings
        getAddressSettings : function(forceReload) {
            if (forceReload || this.rpc.addressSettings === undefined) {
                this.rpc.addressSettings = rpc.networkManager.getAddressSettings();
            }
            return this.rpc.addressSettings;
        },
        // get snmp settings
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
        // get Current Server CertInfo
        getCurrentServerCertInfo : function(forceReload) {
            if (forceReload || this.rpc.currentServerCertInfo === undefined) {
                this.rpc.currentServerCertInfo = main.getAppServerManager().getCurrentServerCertInfo();
            }
            return this.rpc.currentServerCertInfo;
        },
        // get hostname
        getHostname : function(forceReload) {
            if (forceReload || this.rpc.hostname === undefined) {
                this.rpc.hostname = rpc.networkManager.getHostname();
            }
            return this.rpc.hostname;
        },
        // is Branding Expired
        isBrandingExpired : function(forceReload) {
            if (forceReload || this.rpc.isBrandingExpired === undefined) {
                this.rpc.isBrandingExpired = main.getLicenseManager().getLicenseStatus('untangle-branding-manager').expired;
            }
            return this.rpc.isBrandingExpired;
        },
        // get branding settings
        getBrandingBaseSettings : function(forceReload) {
            if (forceReload || this.rpc.brandingBaseSettings === undefined) {
                this.rpc.brandingBaseSettings = main.getBrandingManager().getBaseSettings();
            }
            return this.rpc.brandingBaseSettings;
        },
        
        buildAdministration : function() {
            // read-only is a check column
            var readOnlyColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("read-only"),
                dataIndex : 'readOnly',
                fixed : true,
                width : 60
            });
            var changePasswordColumn = new Ext.grid.IconColumn({
                header : this.i18n._("change password"),
                width : 100,
                iconClass : 'iconEditRow',
                handle : function(record, index) {
                    // populate row editor
                    this.grid.rowEditorChangePass.populate(record);
                    this.grid.rowEditorChangePass.show();
                }
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
                autoScroll : true,
                items : [this.gridAdminAccounts=new Ung.EditorGrid({
                    settingsCmp : this,
                    title : this.i18n._("Admin Accounts"),
                    height : 300,
                    bodyStyle : 'padding-bottom:30px;',
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
                    }, changePasswordColumn
                    ],
                    sortField : 'login',
                    columnsDefaultSortable : true,
                    plugins : [readOnlyColumn,changePasswordColumn],
                    // the row input lines used by the row editor window
                    rowEditorInputLines : [new Ext.form.TextField({
                        name : "Login",
                        dataIndex : "login",
                        fieldLabel : this.i18n._("Login"),
                        allowBlank : false,
                        blankText : this.i18n._("The login name cannot be blank."),
                        width : 200
                    }), new Ext.form.TextField({
                        name : "Name",
                        dataIndex : "name",
                        fieldLabel : this.i18n._("Description"),
                        allowBlank : false,
                        width : 200
                    }), new Ext.form.Checkbox({
                        name : "Read-only",
                        dataIndex : "readOnly",
                        fieldLabel : this.i18n._("Read-only")
                    }), new Ext.form.TextField({
                        name : "Email",
                        dataIndex : "email",
                        fieldLabel : this.i18n._("Email"),
                        width : 200
                    }), new Ext.form.TextField({
                    	inputType: 'password',
                        name : "Password",
                        dataIndex : "clearPassword",
                        id : 'administration_rowEditor_password',
                        fieldLabel : this.i18n._("Password"),
                        width : 200,
                        minLength : 3,
                        minLengthText : String.format(this.i18n._("The password is shorter than the minimum {0} characters."), 3)
                    }), new Ext.form.TextField({
                        inputType: 'password',
                        name : "Confirm Password",
                        dataIndex : "clearPassword",
                        vtype: 'password',
                        initialPassField: 'administration_rowEditor_password', // id of the initial password field
                        fieldLabel : this.i18n._("Confirm Password"),
                        width : 200
                    })],
                    // the row input lines used by the change password window
                    rowEditorChangePassInputLines : [new Ext.form.TextField({
                        inputType: 'password',
                        name : "Password",
                        dataIndex : "clearPassword",
                        id : 'administration_rowEditor1_password',
                        fieldLabel : this.i18n._("Password"),
                        width : 200,
                        minLength : 3,
                        minLengthText : String.format(this.i18n._("The password is shorter than the minimum {0} characters."), 3)                        
                    }), new Ext.form.TextField({
                        inputType: 'password',
                        name : "Confirm Password",
                        dataIndex : "clearPassword",
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
                        xtype : 'checkbox',
                        name : 'isOutsideAdministrationEnabled',
                        boxLabel : this.i18n._('Enable External Administration.'),
                        hideLabel : true,
                        checked : this.getAccessSettings().isOutsideAdministrationEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getAccessSettings().isOutsideAdministrationEnabled = newValue;
                                }.createDelegate(this)
                            }
                        }
                	},{
                        xtype : 'checkbox',
                        name : 'isOutsideReportingEnabled',
                        boxLabel : this.i18n._('Enable External Report Viewing.'),
                        hideLabel : true,
                        checked : this.getAccessSettings().isOutsideReportingEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getAccessSettings().isOutsideReportingEnabled = newValue;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'checkbox',
                        name : 'isOutsideQuarantineEnabled',
                        boxLabel : this.i18n._('Enable External Quarantine Viewing.'),
                        hideLabel : true,
                        checked : this.getAccessSettings().isOutsideQuarantineEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getAccessSettings().isOutsideQuarantineEnabled = newValue;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        border: false,
                        html : '<hr>'
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('External HTTPS port'),
                        name : 'httpsPort',
                        id: 'administration_httpsPort',
                        value : this.getAddressSettings().httpsPort,
                        width: 50,
                        labelStyle: 'width:150px;',
                        allowDecimals: false,
                        allowNegative: false,
                        allowBlank : false,
                        blankText : this.i18n._("You must provide a valid port."),
                        vtype : 'port',
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getAddressSettings().httpsPort = newValue;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        border: false,
                        html : '<hr>'
                    },{
                        xtype : 'radio',
                        boxLabel : this.i18n._('Allow external access from any IP address.'), 
                        hideLabel : true,
                        name : 'isOutsideAccessRestricted',
                        checked : !this.getAccessSettings().isOutsideAccessRestricted,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getAccessSettings().isOutsideAccessRestricted = !checked;
                                    if (checked) {
                                        Ext.getCmp('administration_outsideNetwork').disable();
                                        Ext.getCmp('administration_outsideNetmask').disable();
                                    }
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'radio',
                        boxLabel : this.i18n._('Restrict external access to these external IP address(es).'), 
                        hideLabel : true,
                        name : 'isOutsideAccessRestricted',
                        checked : this.getAccessSettings().isOutsideAccessRestricted,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getAccessSettings().isOutsideAccessRestricted = checked;
                                    if (checked) {
                                        Ext.getCmp('administration_outsideNetwork').enable();
                                        Ext.getCmp('administration_outsideNetmask').enable();
                                    }
                                }.createDelegate(this)
                            }
                        }
                    },{
                        border: false,
                    	layout:'column',
                    	items: [{
                            border: false,
                            columnWidth:.3,
                            layout: 'form',
                            items: [{
                                xtype : 'textfield',
                                fieldLabel : this.i18n._('Address'),
                                name : 'outsideNetwork',
                                id : 'administration_outsideNetwork',
                                value : this.getAccessSettings().outsideNetwork,
                                allowBlank : false,
                                blankText : this.i18n._("A \"IP Address\" must be specified."),
                                vtype : 'ipAddress',
                                disabled : !this.getAccessSettings().isOutsideAccessRestricted
                            }]
                    	},{
                            border: false,
                            columnWidth:.3,
                            layout: 'form',
                            items: [{
                                xtype : 'textfield',
                                fieldLabel : '/', 
                                labelSeparator  : '',
                                name : 'outsideNetmask',
                                id : 'administration_outsideNetmask',
                                value : this.getAccessSettings().outsideNetmask,
                                allowBlank : false,
                                blankText : this.i18n._("A \"Netmask\" must be specified."),
                                vtype : 'ipAddress',
                                disabled : !this.getAccessSettings().isOutsideAccessRestricted                                
                            }]
                    	}]
                	}] 
                },{
                    xtype : 'fieldset',
                    title : this.i18n._('Internal Administration'),
                    autoHeight : true,
                    items : [{
                        xtype : 'radio',
                        boxLabel : this.i18n._('Enable HTTP administration inside the local network (default)'), 
                        hideLabel : true,
                        name : 'isInsideInsecureEnabled',
                        checked : this.getAccessSettings().isInsideInsecureEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getAccessSettings().isInsideInsecureEnabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'radio',
                        boxLabel : this.i18n._('Disable HTTP administration inside the local network'), 
                        hideLabel : true,
                        name : 'isInsideInsecureEnabled',
                        checked : !this.getAccessSettings().isInsideInsecureEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getAccessSettings().isInsideInsecureEnabled = !checked;
                                }.createDelegate(this)
                            }
                        }
                    },{
                    	border: false,
                        html : this.i18n._('Note: HTTPS administration is always enabled internally')
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
            this.panelPublicAddress = new Ext.Panel({

                name : 'panelMonitoring',
                // private fields
                parentId : this.getId(),

                title : this.i18n._('Public Address'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                items: {
                    xtype: 'fieldset',
                    autoHeight : true,
                    items : [{
                    	html : String.format(this.i18n._('The Public Address is the address or hostname that provides a public routable address for the {0} Server. This address will be used in emails sent by the {0} Server to link back to services hosted on the {0} Server such as Quarantine Digests and OpenVPN Client emails.'),
                    	           this.getBrandingBaseSettings().companyName),
                        bodyStyle : 'padding-bottom:10px;',
                        border : false
                    },{
                        xtype : 'radio',
                        boxLabel : this.i18n._('Use External IP address (default)'), 
                        hideLabel : true,
                        name : 'publicAddress',
                        checked : !this.getAddressSettings().isPublicAddressEnabled && !this.getAddressSettings().isHostNamePublic,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    if (checked) {
                                        Ext.getCmp('administration_publicIPaddr').disable();
                                        Ext.getCmp('administration_publicPort').disable();
                                    }
                                }.createDelegate(this)
                            }
                        }
                    },{
                    	html : String.format(this.i18n._('This works if your {0} Server has a public static IP address.'),
                    	           this.getBrandingBaseSettings().companyName),
                        bodyStyle : 'padding:0px 5px 10px 25px;',
                        border : false
                    },{
                        xtype : 'radio',
                        boxLabel : this.i18n._('Use Hostname'), 
                        hideLabel : true,
                        name : 'publicAddress',
                        checked : this.getAddressSettings().isHostNamePublic,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getAddressSettings().isHostNamePublic = checked;
                                    if (checked) {
                                        Ext.getCmp('administration_publicIPaddr').disable();
                                        Ext.getCmp('administration_publicPort').disable();
                                    }
                                }.createDelegate(this)
                            }
                        }
                    },{
                        html : String.format(this.i18n._('This is recommended if the {0} Server\'s fully qualified domain name looks up to its IP address both internally and externally.'),
                                    this.getBrandingBaseSettings().companyName),
                        bodyStyle : 'padding:0px 5px 5px 25px;',
                        border : false
                    },{
                        html : String.format(this.i18n._('Current Hostname: {0}'), '<i>' + this.getAddressSettings().hostName + '</i>'),
                        bodyStyle : 'padding:0px 5px 10px 25px;',
                        border : false
                    },{
                        xtype : 'radio',
                        boxLabel : this.i18n._('Use Manually Specified IP'), 
                        hideLabel : true,
                        name : 'publicAddress',
                        checked : this.getAddressSettings().isPublicAddressEnabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getAddressSettings().isPublicAddressEnabled = checked;
                                    if (checked) {
                                        Ext.getCmp('administration_publicIPaddr').enable();
                                        Ext.getCmp('administration_publicPort').enable();
                                    }
                                }.createDelegate(this)
                            }
                        }
                    },{
                        html : String.format(this.i18n._('This is recommended if the {0} Server is installed behind another firewall with a port forward from the specified IP that redirects traffic to the {0} Server.'),
                                    this.getBrandingBaseSettings().companyName),
                        bodyStyle : 'padding:0px 5px 5px 25px;',
                        border : false
                    },{
                    	xtype : 'panel',
                        bodyStyle : 'padding-left:25px;',
                        border : false,
                        layout : 'form',
                        items : [{
                            xtype : 'textfield',
                            fieldLabel : this.i18n._('Address'),
                            name : 'publicIPaddr',
                            id: 'administration_publicIPaddr',
                            value : this.getAddressSettings().publicIPaddr,
                            allowBlank : false,
                            blankText : this.i18n._("You must provide a valid IP Address."),
                            vtype : 'ipAddress',
                            disabled : !this.getAddressSettings().isPublicAddressEnabled
                        },{
                            xtype : 'numberfield',
                            fieldLabel : this.i18n._('Port'),
                            name : 'publicPort',
                            id: 'administration_publicPort',
                            value : this.getAddressSettings().publicPort,
                            width: 50,
                            allowDecimals: false,
                            allowNegative: false,
                            allowBlank : false,
                            blankText : this.i18n._("You must provide a valid port."),
                            vtype : 'port',
                            disabled : !this.getAddressSettings().isPublicAddressEnabled
                        }]
                    }]
                }
            });
        },
        buildCertificates : function() {
            this.panelCertificates = new Ext.Panel({
                name : 'panelCertificates',
                // private fields
                parentId : this.getId(),
                winGenerateSelfSignedCertificate : null,
                winGenerateCertGenTrusted : null,
                winCertImportTrusted : null,
                
                title : this.i18n._('Certificates'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true
                },
                items: [{
                    title: this.i18n._('Status'),
                    items : [{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Current Certificate Type'),
                        labelStyle: 'width:150px;',
                        id : 'administration_status_appearsSelfSigned',
                        value : this.getCurrentServerCertInfo() == null ? "" : (this.getCurrentServerCertInfo().appearsSelfSigned ? this.i18n._("Self-Signed") : this.i18n._("Signed / Trusted")),
                        disabled : true,
                        width: 300
                    },{
                        html : '<hr>',
                        border : false
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Valid starting'),
                        labelStyle: 'width:150px; font-weight:bold',
                        id : 'administration_status_notBefore',
                        value : this.getCurrentServerCertInfo() == null ? "" : i18n.timestampFormat(this.getCurrentServerCertInfo().notBefore),
                        disabled : true,
                        width: 300
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Valid until'),
                        labelStyle: 'width:150px; font-weight:bold',
                        id : 'administration_status_notAfter',
                        value : this.getCurrentServerCertInfo() == null ? "" : i18n.timestampFormat(this.getCurrentServerCertInfo().notAfter),
                        disabled : true,
                        width: 300
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Subject DN'),
                        labelStyle: 'width:150px; font-weight:bold',
                        id : 'administration_status_subjectDN',
                        value : this.getCurrentServerCertInfo() == null ? "" : this.getCurrentServerCertInfo().subjectDN,
                        disabled : true,
                        width: 300
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Issuer DN'),
                        labelStyle: 'width:150px; font-weight:bold',
                        id : 'administration_status_issuerDN',
                        value : this.getCurrentServerCertInfo() == null ? "" : this.getCurrentServerCertInfo().issuerDN,
                        disabled : true,
                        width: 300
                    }]
                }, {
                    title: this.i18n._('Generation'),
                    defaults : {
                        xtype : 'fieldset',
                        autoHeight : true,
                        layout:'column'
                    },
                    items : [{
                        items: [{
                            border: false,
                            width: 270,
                            layout: 'form',
                            items: [{
                                xtype : 'button',
                                text : String.format(this.i18n._('Generate a {0}Self-Signed Certificate{1}'), '<b>', '</b>'),
                                minWidth : 250,
                                name : 'Generate a Self-Signed Certificate',
                                iconCls : 'actionIcon',
                                handler : function() {
                                    this.panelCertificates.onGenerateSelfSignedCertificate();
                                }.createDelegate(this)
                            }]
                        },{
                            border: false,
                            columnWidth:1,
                            layout: 'form',
                            items: [{
                            	html : this.i18n._("Click this button if you have been using a signed certificate, and you want to go back to using a self-signed certificate."),
                            	border : false
                            }]
                        }]
                    },{
                        items: [{
                            border: false,
                            width: 270,
                            layout: 'form',
                            items: [{
                                xtype : 'button',
                                text : String.format(this.i18n._('Generate a {0}Certificate Signature Request{1}'), '<b>', '</b>'),
                                minWidth : 250,
                                name : 'Generate a Self-Signed Certificate',
                                iconCls : 'actionIcon',
                                handler : function() {
                                    this.panelCertificates.onGenerateCertGenTrusted();
                                }.createDelegate(this)
                            }]
                        },{
                            border: false,
                            columnWidth:1,
                            layout: 'form',
                            items: [{
                                html : this.i18n._("Click this button to generate a certificate signature request, which you can then copy and paste for use by certificate authorities such as Thawte, Verisign, etc."),
                                border : false
                            }]
                        }]
                    },{
                        items: [{
                            border: false,
                            width: 270,
                            layout: 'form',
                            items: [{
                                xtype : 'button',
                                text : String.format(this.i18n._('Import a {0}Signed Certificate{1}'), '<b>', '</b>'),
                                minWidth : 250,
                                name : 'Generate a Self-Signed Certificate',
                                iconCls : 'actionIcon',
                                handler : function() {
                                    this.panelCertificates.onCertImportTrusted();
                                }.createDelegate(this)
                            }]
                        },{
                            border: false,
                            columnWidth:1,
                            layout: 'form',
                            items: [{
                                html : String.format(this.i18n._("Click this button to import a signed certificate which has been generated by a certificate authority, and was based on a previous signature request from {0}."),
                                            this.getBrandingBaseSettings().companyName),
                                border : false
                            }]
                        }]
                    }]
                }],
                
                onGenerateSelfSignedCertificate : function() {
                    if (!this.winGenerateSelfSignedCertificate) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildGenerateSelfSignedCertificate();
                        this.winGenerateSelfSignedCertificate = new Ung.CertGenerateWindow({
                            breadcrumbs : [{
                                title : i18n._("Configuration"),
                                action : function() {
                                    this.panelCertificates.winGenerateSelfSignedCertificate.cancelAction();
                                    this.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : i18n._('Administration'),
                                action : function() {
                                    this.panelCertificates.winGenerateSelfSignedCertificate.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Generate a Self-Signed Certificate")
                            }],
                            certPanel : settingsCmp.panelGenerateSelfSignedCertificate,
                            
                            proceedAction : function() {
                                Ext.MessageBox.wait(this.i18n._("Generating Certificate..."), i18n._("Please wait"));
                                
                                //validation
                                var organizationCmp = Ext.getCmp('administration_organization');
                                var organizationUnitCmp = Ext.getCmp('administration_organizationUnit');
                                var cityCmp = Ext.getCmp('administration_city');
                                var stateCmp = Ext.getCmp('administration_state');
                                var countryCmp = Ext.getCmp('administration_country');
                                if (this.isBlankField(organizationCmp, this.i18n._("You must specify an organization.")) ||
                                    this.isBlankField(organizationUnitCmp, this.i18n._("You must specify an organization unit.")) ||
                                    this.isBlankField(cityCmp, this.i18n._("You must specify a city.")) ||
                                    this.isBlankField(stateCmp, this.i18n._("You must specify a state.")) ||
                                    this.isBlankField(countryCmp, this.i18n._("You must specify a country."))) {
                                    	return;
                                }
                                
                                //get user values
                                var organization = organizationCmp.getValue();
                                var organizationUnit = organizationUnitCmp.getValue();
                                var city = cityCmp.getValue();
                                var state = stateCmp.getValue();
                                var country = countryCmp.getValue();
                                var distinguishedName = String.format("O={0},OU={1},L={2},ST={3},C={4}",
                                        organization, organizationUnit, city, state, country);
                                
                                // generate certificate
                                main.getAppServerManager().regenCert(function(result, exception) {
                                    if (exception) {
                                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                                        return;
                                    }
                                    
                                    if (result) { //true or false
                                        //success
                                        
                                    	//update status
                                    	this.updateCertificatesStatus();
                                    	
                                        Ext.MessageBox.alert(this.i18n._("Succeeded"), this.i18n._("Certificate Successfully Generated"),
                                            function () {
                                                this.panelCertificates.winGenerateSelfSignedCertificate.cancelAction();
                                            }.createDelegate(this) 
                                        );
                                    } else {
                                        //failed
                                        Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("Error generating self-signed certificate"));
                                        return;
                                    }
                                }.createDelegate(this), distinguishedName, 5*365);
                            	
                            }.createDelegate(settingsCmp)
                        });
                    }
                    this.winGenerateSelfSignedCertificate.show();
                },
                
                onGenerateCertGenTrusted : function() {
                    if (!this.winGenerateCertGenTrusted) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildGenerateCertGenTrusted();
                        this.winGenerateCertGenTrusted = new Ung.CertGenerateWindow({
                            breadcrumbs : [{
                                title : i18n._("Configuration"),
                                action : function() {
                                    this.panelCertificates.winGenerateCertGenTrusted.cancelAction();
                                    this.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : i18n._('Administration'),
                                action : function() {
                                    this.panelCertificates.winGenerateCertGenTrusted.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Generate a Certificate Signature Request")
                            }],
                            certPanel : settingsCmp.panelGenerateCertGenTrusted,
                            
                            proceedAction : function() {
                                Ext.MessageBox.wait(this.i18n._("Generating Certificate..."), i18n._("Please wait"));
                                
                                // generate certificate request
                                main.getAppServerManager().generateCSR(function(result, exception) {
                                    if (exception) {
                                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                                        return;
                                    }
                                    
                                    if (result != null) { 
                                        //success
                                        Ext.MessageBox.alert(this.i18n._("Succeeded"), this.i18n._("Certificate Signature Request Successfully Generated"),
                                            function () {
                                                Ext.getCmp('administration_crs').setValue(result);
                                            }.createDelegate(this) 
                                        );
                                    } else {
                                        //failed
                                        Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("Error generating certificate signature request"));
                                        return;
                                    }
                                }.createDelegate(this));
                                
                            }.createDelegate(settingsCmp)
                        });
                    }
                    this.winGenerateCertGenTrusted.show();
                },
                
                onCertImportTrusted : function() {
                    if (!this.winCertImportTrusted) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildCertImportTrusted();
                        this.winCertImportTrusted = new Ung.CertGenerateWindow({
                            breadcrumbs : [{
                                title : i18n._("Configuration"),
                                action : function() {
                                    this.panelCertificates.winCertImportTrusted.cancelAction();
                                    this.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : i18n._('Administration'),
                                action : function() {
                                    this.panelCertificates.winCertImportTrusted.cancelAction();
                                }.createDelegate(settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Import Signed Certificate")
                            }],
                            certPanel : settingsCmp.panelCertImportTrusted,
                            
                            proceedAction : function() {
                                Ext.MessageBox.wait(this.i18n._("Importing Certificate..."), i18n._("Please wait"));
                                
                                //get user values
                                var cert = Ext.getCmp('administration_import_cert').getValue();
                                var caCert = Ext.getCmp('administration_import_caCert').getValue();
                                
                                // import certificate
                                main.getAppServerManager().importServerCert(function(result, exception) {
                                    if (exception) {
                                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                                        return;
                                    }
                                    
                                    if (result) { //true or false
                                        //success
                                        
                                        //update status
                                        this.updateCertificatesStatus();
                                        
                                        Ext.MessageBox.alert(this.i18n._("Succeeded"), this.i18n._("Certificate Successfully Imported"),
                                            function () {
                                                this.panelCertificates.winCertImportTrusted.cancelAction();
                                            }.createDelegate(this) 
                                        );
                                    } else {
                                        //failed
                                        Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("Error importing certificate"));
                                        return;
                                    }
                                }.createDelegate(this), cert, caCert.length==0?null:caCert );
                                
                            }.createDelegate(settingsCmp)
                        });
                    }
                    this.winCertImportTrusted.show();
                },
                
                beforeDestroy : function() {
                    Ext.destroy(this.winGenerateSelfSignedCertificate);
                    Ext.destroy(this.winGenerateCertGenTrusted);
                    Ext.destroy(this.winCertImportTrusted);
                    Ext.Panel.prototype.beforeDestroy.call(this);
                }
                
            })
        },
        
        // Generate Self-Signed certificate
        buildGenerateSelfSignedCertificate : function() {
            this.panelGenerateSelfSignedCertificate = new Ext.Panel({
                name : 'panelGenerateSelfSignedCertificate',
                // private fields
                parentId : this.getId(),

                title : this.i18n._('Generate a Self-Signed Certificate'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                items: {
                    xtype: 'fieldset',
                    autoHeight : true,
                    defaults : {
                        labelStyle: 'width:150px;'
                    },
                    items : [{
                        html : this.i18n._('Please fill out the following fields, which will be used to generate your self-signed certificate.'),
                        bodyStyle : 'padding-bottom:10px;',
                        border : false
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Organization') + " (O)",
                        name : 'organization',
                        id: 'administration_organization',
                        allowBlank : false,
                        blankText : this.i18n._("You must specify an organization.")
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Organization Unit') + " (OU)",
                        name : 'organizationUnit',
                        id: 'administration_organizationUnit',
                        allowBlank : false,
                        blankText : this.i18n._("You must specify an organization unit.")
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('City') + " (L)",
                        name : 'city',
                        id: 'administration_city',
                        allowBlank : false,
                        blankText : this.i18n._("You must specify a city.")
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('State') + " (ST)",
                        name : 'state',
                        id: 'administration_state',
                        allowBlank : false,
                        blankText : this.i18n._("You must specify a state.")
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Country') + " (C)",
                        name : 'country',
                        id: 'administration_country',
                        allowBlank : false,
                        blankText : this.i18n._("You must specify a country.")
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Hostname') + " (CN)",
                        name : 'hostname',
                        id: 'administration_hostname',
                        value : this.getHostname(),
                        disabled : true
                    }]
                }
            });
        },
        
        // Generate Signature Request
        buildGenerateCertGenTrusted : function() {
            this.panelGenerateCertGenTrusted = new Ext.Panel({
                name : 'panelGenerateCertGenTrusted',
                // private fields
                parentId : this.getId(),

                title : this.i18n._('Generate a Certificate Signature Request'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                items: {
                    xtype: 'fieldset',
                    autoHeight : true,
                    items : [{
                        html : this.i18n._('Click the Proceed button to generate a signature below. Copy the signature (Control-C), and paste it into the necessary form from your Certificate Authority (Verisign, Thawte, etc.).'),
                        bodyStyle : 'padding-bottom:10px;',
                        border : false
                    },{
                        xtype : 'textarea',
                        name : 'crs',
                        id: 'administration_crs',
                        anchor:'95%',
                        height : 200,
                        hideLabel : true
                    }]
                }
            });
        },
        
        // Import Signed Certificate
        buildCertImportTrusted : function() {
            this.panelCertImportTrusted = new Ext.Panel({
                name : 'panelCertImportTrusted',
                // private fields
                parentId : this.getId(),

                title : this.i18n._('Import Signed Certificate'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                    items : [{
                        html : this.i18n._('When your Certificate Authority (Verisign, Thawte, etc.) ' +
                        		'has sent your Signed Certificate, copy and paste it below (Control-V), ' +
                        		'then press the Proceed button.'),
                        bodyStyle : 'padding-bottom:10px;',
                        border : false
                    },{
                        xtype : 'textarea',
                        name : 'cert',
                        id: 'administration_import_cert',
                        anchor:'95%',
                        height : 200,
                        hideLabel : true
                    },{
                        html : this.i18n._('If your Certificate Authority (Verisign, Thawte, etc.) ' +
                        		'also send you an Intermediate Certificate, paste it below.  ' +
                        		'Otherwise, do not paste anything below.'),
                        bodyStyle : 'padding:20px 0px 10px 0px;',
                        border : false
                    },{
                        xtype : 'textarea',
                        name : 'caCert',
                        id: 'administration_import_caCert',
                        anchor:'95%',
                        height : 200,
                        hideLabel : true
                    }]
            });
        },
        
        updateCertificatesStatus : function() {
        	var certInfo = this.getCurrentServerCertInfo(true);
        	if (certInfo != null) {
                Ext.getCmp('administration_status_appearsSelfSigned').setValue(certInfo.appearsSelfSigned ? this.i18n._("Self-Signed") : this.i18n._("Signed / Trusted"));
                Ext.getCmp('administration_status_notBefore').setValue(i18n.timestampFormat(certInfo.notBefore));
                Ext.getCmp('administration_status_notAfter').setValue(i18n.timestampFormat(certInfo.notAfter));
                Ext.getCmp('administration_status_subjectDN').setValue(certInfo.subjectDN);
                Ext.getCmp('administration_status_issuerDN').setValue(certInfo.issuerDN);
        	}
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
                        boxLabel : String.format(this.i18n._('{0}Disable{1} SNMP Monitoring. (This is the default setting.)'), '<b>', '</b>'), 
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
                        boxLabel : String.format(this.i18n._('{0}Enable{1} SNMP Monitoring.'), '<b>', '</b>'), 
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
                        blankText : this.i18n._("An SNMP \"Community\" must be specified."),
                        disabled : !this.getSnmpSettings().enabled
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('System Contact'),
                        name : 'sysContact',
                        id: 'administration_snmp_sysContact',
                        value : this.getSnmpSettings().sysContact,
                        disabled : !this.getSnmpSettings().enabled                        
                        //vtype : 'email'
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('System Location'),
                        name : 'sysLocation',
                        id: 'administration_snmp_sysLocation',
                        value : this.getSnmpSettings().sysLocation,
                        disabled : !this.getSnmpSettings().enabled                        
                    },{
                        border: false,
                        html : '<hr>'
                    },{
                        xtype : 'radio',
                        boxLabel : String.format(this.i18n._('{0}Disable Traps{1} so no trap events are generated.  (This is the default setting.)'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'sendTraps',
                        id: 'administration_snmp_sendTraps_disable',                        
                        checked : !this.getSnmpSettings().sendTraps,
                        disabled : !this.getSnmpSettings().enabled,                        
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
                        boxLabel : String.format(this.i18n._('{0}Enable Traps{1} so trap events are sent when they are generated.'), '<b>', '</b>'), 
                        hideLabel : true,
                        name : 'sendTraps',
                        id: 'administration_snmp_sendTraps_enable',
                        checked : this.getSnmpSettings().sendTraps,
                        disabled : !this.getSnmpSettings().enabled,                        
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
                        blankText : this.i18n._("An Trap \"Community\" must be specified."),
                        disabled : !this.getSnmpSettings().enabled || !this.getSnmpSettings().sendTraps                     
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Host'),
                        name : 'trapHost',
                        id: 'administration_snmp_trapHost',
                        value : this.getSnmpSettings().trapHost,
                        allowBlank : false,
                        blankText : this.i18n._("An Trap \"Host\" must be specified."),                        
                        disabled : !this.getSnmpSettings().enabled || !this.getSnmpSettings().sendTraps                     
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Port'),
                        name : 'trapPort',
                        id: 'administration_snmp_trapPort',
                        value : this.getSnmpSettings().trapPort,
                        width: 50,
                        allowDecimals: false,
                        allowNegative: false,
                        allowBlank : false,
                        blankText : this.i18n._("You must provide a valid port."),
                        vtype : 'port',
                        disabled : !this.getSnmpSettings().enabled || !this.getSnmpSettings().sendTraps                     
                    }]
                }, {
                    title: this.i18n._('Syslog'),
                    items: [{
                        xtype : 'radio',
                        boxLabel : String.format(this.i18n._('{0}Disable{1} Syslog Monitoring. (This is the default setting.)'), '<b>', '</b>'), 
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
                        boxLabel : String.format(this.i18n._('{0}Enable{1} Syslog Monitoring.'), '<b>', '</b>'), 
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
                        blankText : this.i18n._("A \"Host\" must be specified."),
                        disabled : !this.getLoggingSettings().syslogEnabled
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Port'),
                        name : 'syslogPort',
                        id : 'administration_syslog_port',
                        value : this.getLoggingSettings().syslogPort,
                        width: 50,
                        allowDecimals: false,
                        allowNegative: false,
                        allowBlank : false,
                        blankText : this.i18n._("You must provide a valid port."),
                        vtype : 'port',
                        disabled : !this.getLoggingSettings().syslogEnabled                        
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
                                ["UUCP", this.i18n._("UUCP")],
                                ["CLOCK_0", this.i18n._("clock 0")],
                                ["SECURITY_1", this.i18n._("security 1")],
                                ["FTP", this.i18n._("FTP")],
                                ["NTP", this.i18n._("NTP")],
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
                        disabled : !this.getLoggingSettings().syslogEnabled                        
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
                        value : this.getLoggingSettings().syslogThreshold,
                        disabled : !this.getLoggingSettings().syslogEnabled                        
                    }]
                }]
            });
        },
        buildSkins : function() {
            var adminSkinsStore = new Ext.data.Store({
                proxy : new Ung.RpcProxy(rpc.skinManager.getSkinsList, [true, false], false),
                reader : new Ext.data.JsonReader({
                    root : 'list',
                    fields: [{
                        name: 'name'
                    },{
                        name: 'displayName',
                        convert : function(v) {
                            return this.i18n._(v)
                        }.createDelegate(this)
                    }]                    
                })
            });
            
            var userFacingSkinsStore = new Ext.data.Store({
                proxy : new Ung.RpcProxy(rpc.skinManager.getSkinsList, [false, true], false),
                reader : new Ext.data.JsonReader({
                    root : 'list',
                    fields: [{
                        name: 'name'
                    },{
                        name: 'displayName',
                        convert : function(v) {
                            return this.i18n._(v)
                        }.createDelegate(this)
                    }]                    
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
                        id : "administration_admin_client_skin_combo",
                        store : adminSkinsStore,
                        displayField : 'displayName',
                        valueField : 'name',
                        forceSelection : true,
                        typeAhead : true,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        selectOnFocus : true,
                        hideLabel : true,
                        listeners : {
                            "select" : {
                                fn : function(elem, record) {
                                    this.getSkinSettings().administrationClientSkin = record.data.name;
                                    Ext.MessageBox.alert(this.i18n._("Info"), this.i18n
                                            ._("Please note that you have to refresh the application after saving for the new skin to take effect."));
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
                        id : "administration_user_pages_skin_combo",
                        store : userFacingSkinsStore,
                        displayField : 'displayName',
                        valueField : 'name',
                        forceSelection : true,
                        typeAhead : true,
                        mode : 'local',
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
                            id : 'upload_skin_file_textfield',
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
                            var cmp = Ext.getCmp(action.options.parentId);
                        	cmp.storeSemaphore = 2;
                        	var handler = function() {
                                this.storeSemaphore--;
                        		if (this.storeSemaphore == 0) {
                                    Ext.MessageBox.alert(this.i18n._("Succeeded"), this.i18n._("Upload Skin Succeeded"),
                                        function() {
                                            Ext.getCmp('upload_skin_file_textfield').reset();
                                        } 
                                    );
                        		}
                        	}.createDelegate(cmp)
                            adminSkinsStore.load({callback:handler});
                            userFacingSkinsStore.load({callback:handler});
                        },
                        failure : function(form, action) {
                            var cmp = Ext.getCmp(action.options.parentId);
                            var errorMsg = cmp.i18n._("Upload Skin Failed");
                            if (action.result && action.result.msg) {
                                switch (action.result.msg) {
                                    case 'Invalid Skin' : 
                                        errorMsg = cmp.i18n._("Invalid Skin");
                                    break;
                                    case 'The default skin can not be overwritten' : 
                                        errorMsg = cmp.i18n._("The default skin can not be overwritten");
                                    break;
                                    case 'Error creating skin folder' : 
                                        errorMsg = cmp.i18n._("Error creating skin folder");
                                    break;
                                    default :
                                        errorMsg = cmp.i18n._("Upload Skin Failed");
                                }
                            }
                            Ext.MessageBox.alert(cmp.i18n._("Failed"), errorMsg);
                        }
                    });
                }
            });
            adminSkinsStore.load({
            	callback : function() {
            		Ext.getCmp('administration_admin_client_skin_combo').setValue(this.getSkinSettings().administrationClientSkin)
            	}.createDelegate(this)
            });
            userFacingSkinsStore.load({
                callback : function() {
                    Ext.getCmp('administration_user_pages_skin_combo').setValue(this.getSkinSettings().userPagesSkin)
                }.createDelegate(this)
            });
        },

        buildBranding : function() {
            var brandingBaseSettings = this.getBrandingBaseSettings();
            this.panelBranding = new Ext.Panel({
                // private fields
                name : 'Branding',
                parentId : this.getId(),
                title : this.i18n._('Branding'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                enableFileUpload : function(enabled) {
                    try {
                        var formItem = this.items.get(1).items.get(2);
                        if (enabled) {
                            formItem.items.get(0).enable();
                            formItem.buttons[0].enable();
                        } else {
                            formItem.items.get(0).disable();
                            formItem.buttons[0].disable();

                        }
                    } catch (e) {
                    }
                },
                items : [{
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 0px;',
                        border : false,
                        html : this.i18n
                                ._("The Branding Settings are used to set the logo and contact information that will be seen by users (e.g. reports).")
                    }]
                }, {
                    title : this.i18n._('Logo'),
                    items : [{
                        xtype : 'radio',
                        name : 'Logo',
                        hideLabel : true,
                        boxLabel : 'Use Default Logo',
                        value : 'default',
                        checked : brandingBaseSettings.defaultLogo,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    if (checked) {
                                        this.getBrandingBaseSettings().defaultLogo = true;
                                        this.panelBranding.enableFileUpload(false);
                                    }
                                    Ext.MessageBox.alert(this.i18n._("Info"), this.i18n
                                            ._("Please note that you have to refresh the application after saving for the new logo to take effect."));
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'radio',
                        name : 'Logo',
                        hideLabel : true,
                        boxLabel : 'Use Custom Logo',
                        value : 'custom',
                        checked : !brandingBaseSettings.defaultLogo,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    if (checked) {
                                        this.getBrandingBaseSettings().defaultLogo = false;
                                        this.panelBranding.enableFileUpload(true);
                                    }
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        fileUpload : true,
                        xtype : 'form',
                        bodyStyle : 'padding:0px 0px 0px 25px',
                        buttonAlign : 'left',
                        id : 'upload_logo_form',
                        url : 'upload',
                        border : false,
                        items : [{
                            fieldLabel : 'File',
                            name : 'File',
                            id : 'upload_logo_file_textfield',
                            inputType : 'file',
                            xtype : 'textfield',
                            disabled : brandingBaseSettings.defaultLogo,
                            allowBlank : false
                        }, {
                            xtype : 'hidden',
                            name : 'type',
                            value : 'logo'
                        }],
                        buttons : [{
                            text : this.i18n._("Upload"),
                            handler : function() {
                                this.panelBranding.onUpload();
                            }.createDelegate(this),
                            disabled : (brandingBaseSettings.defaultLogo)
                        }]
                    }]

                }, {
                    title : this.i18n._('Contact Information'),
                    defaults : {
                        width : 300
                    },
                    items : [{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Company Name'),
                        name : 'Company Name',
                        allowBlank : true,
                        value : brandingBaseSettings.companyName,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBrandingBaseSettings().companyName = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Company URL'),
                        name : 'Company URL',
                        allowBlank : true,
                        value : brandingBaseSettings.companyUrl,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBrandingBaseSettings().companyUrl = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Contact Name'),
                        name : 'Contact Name',
                        allowBlank : true,
                        value : brandingBaseSettings.contactName,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBrandingBaseSettings().contactName = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Contact Email'),
                        name : 'Contact Email',
                        allowBlank : true,
                        value : brandingBaseSettings.contactEmail,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBrandingBaseSettings().contactEmail = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]

                }],
                onUpload : function() {
                    var prova = Ext.getCmp('upload_logo_form');
                    var cmp = Ext.getCmp(this.parentId);
                    var fileText = prova.items.get(0);
                    if (fileText.getValue().length == 0) {
                        Ext.MessageBox.alert(cmp.i18n._("Failed"), cmp.i18n._('Please select an image to upload.'));
                        return false;
                    }
                    var form = prova.getForm();
                    form.submit({
                        parentId : cmp.getId(),
                        waitMsg : cmp.i18n._('Please wait while your logo image is uploaded...'),
                        success : function(form, action) {
                            var cmp = Ext.getCmp(action.options.parentId);
                            Ext.MessageBox.alert(cmp.i18n._("Succeeded"), cmp.i18n._("Upload Logo Succeeded"), 
                                function() {
                                    Ext.getCmp('upload_logo_file_textfield').reset();
                                } 
                            );
                        },
                        failure : function(form, action) {
                            var cmp = Ext.getCmp(action.options.parentId);
                            Ext.MessageBox.alert(cmp.i18n._("Failed"), cmp.i18n._("Upload Logo Failed"));
                        }
                    });

                }
            });
        },
        
        isBlankField : function (cmp, errMsg) {
            if (cmp.getValue().length == 0) {
                Ext.MessageBox.alert(this.i18n._('Warning'), errMsg ,
                    function () {
                        cmp.focus(true);
                    } 
                );
                return true;
            } else {
                return false;
            }
        },
        
        // validation function
        validateClient : function() {
            return  this.validateAdminAccounts() && this.validateExternalAdministration() && 
                this.validatePublicAddress() && this.validateSnmp() && this.validateSyslog(); 
        },
        
        //validate Admin Accounts
        validateAdminAccounts : function() {
            var listAdminAccounts = this.gridAdminAccounts.getFullSaveList();
            var oneWritableAccount = false;
            
            // verify that the login name is not duplicated
            for(var i=0; i<listAdminAccounts.length;i++) {
                for(var j=i+1; j<listAdminAccounts.length;j++) {
                	if (listAdminAccounts[i].login == listAdminAccounts[j].login) {
                        Ext.MessageBox.alert(this.i18n._('Warning'), String.format(this.i18n._("The login name: \"{0}\" in row: {1}  already exists."), listAdminAccounts[j].login, j+1),
                            function () {
                                this.tabs.activate(this.panelAdministration);
                            }.createDelegate(this) 
                        );
                		return false;
                	}
                }
                
                if (!listAdminAccounts[i].readOnly) {
                    oneWritableAccount = true;
                }
            	
            }
            
            // verify that there is at least one valid entry after all operations
            if(listAdminAccounts.length <= 0 ){
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("There must always be at least one valid account."),
                    function () {
                        this.tabs.activate(this.panelAdministration);
                    }.createDelegate(this) 
                );
                return false;
            }
        
            // verify that there was at least one non-read-only account
            if(!oneWritableAccount){
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("There must always be at least one non-read-only (writable) account."),
                    function () {
                        this.tabs.activate(this.panelAdministration);
                    }.createDelegate(this) 
                );
                return false;
            }
            
        	return true;
        },
        
        //validate External Administration
        validateExternalAdministration : function() {
            var httpsPortCmp = Ext.getCmp('administration_httpsPort');
            if (!httpsPortCmp.isValid()) {
                Ext.MessageBox.alert(this.i18n._('Warning'), String.format(this.i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),
                    function () {
                        this.tabs.activate(this.panelAdministration);
                        httpsPortCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            }
            
            var isOutsideAccessRestricted = this.getAccessSettings().isOutsideAccessRestricted;
            if (isOutsideAccessRestricted) {
                var outsideNetworkCmp = Ext.getCmp('administration_outsideNetwork');
                if (!outsideNetworkCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('Invalid External Remote Administration \"IP Address\" specified.'),
                        function () {
                            this.tabs.activate(this.panelAdministration);
                            outsideNetworkCmp.focus(true);
                        }.createDelegate(this) 
                    );
                    return false;
                }
                var outsideNetmaskCmp = Ext.getCmp('administration_outsideNetmask');
                if (!outsideNetmaskCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("Invalid External Remote Administration \"Netmask\" specified."),
                        function () {
                            this.tabs.activate(this.panelAdministration);
                            outsideNetmaskCmp.focus(true);
                        }.createDelegate(this) 
                    );
                    return false;
                }
                //prepare for save
                this.getAccessSettings().outsideNetwork = outsideNetworkCmp.getValue();
                this.getAccessSettings().outsideNetmask = outsideNetmaskCmp.getValue();
            }
            
            return true;
        },

        //validate Public Address
        validatePublicAddress : function() {
            var isPublicAddressEnabled = this.getAddressSettings().isPublicAddressEnabled;
            if (isPublicAddressEnabled) {
                var publicIPaddrCmp = Ext.getCmp('administration_publicIPaddr');
                if (!publicIPaddrCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("You must provide a valid IP Address."), 
                        function () {
                            this.tabs.activate(this.panelPublicAddress);
                            publicIPaddrCmp.focus(true);
                        }.createDelegate(this) 
                    );
                    return false;
                }
                var publicPortCmp = Ext.getCmp('administration_publicPort');
                if (!publicPortCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), String.format(this.i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),
                        function () {
                            this.tabs.activate(this.panelPublicAddress);
                            publicPortCmp.focus(true);
                        }.createDelegate(this) 
                    );
                    return false;
                }
                //prepare for save
                this.getAddressSettings().publicIPaddr = publicIPaddrCmp.getValue();
                this.getAddressSettings().publicPort = publicPortCmp.getValue();
            }
            
            return true;
        },
        
        //validate SNMP
        validateSnmp : function() {
            var isSnmpEnabled = this.getSnmpSettings().enabled;
            if (isSnmpEnabled) {
                var snmpCommunityCmp = Ext.getCmp('administration_snmp_communityString');
                if (!snmpCommunityCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("An SNMP \"Community\" must be specified."),
                        function () {
                            this.tabs.activate(this.panelMonitoring);
                            snmpCommunityCmp.focus(true);
                        }.createDelegate(this) 
                    );
                    return false;
                }
                
                var sendTrapsEnableCmp = Ext.getCmp('administration_snmp_sendTraps_enable');
                var isTrapEnabled = sendTrapsEnableCmp.getValue();
                var snmpTrapCommunityCmp, snmpTrapHostCmp, snmpTrapPortCmp;                
                if (isTrapEnabled) {
                    snmpTrapCommunityCmp = Ext.getCmp('administration_snmp_trapCommunity');
                    if (!snmpTrapCommunityCmp.isValid()) {
                        Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("An Trap \"Community\" must be specified."), 
                            function () {
                                this.tabs.activate(this.panelMonitoring);
                                snmpTrapCommunityCmp.focus(true);
                            }.createDelegate(this) 
                        );
                        return false;
                    }
                    
                    snmpTrapHostCmp = Ext.getCmp('administration_snmp_trapHost');
                    if (!snmpTrapHostCmp.isValid()) {
                        Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("An Trap \"Host\" must be specified."),
                            function () {
                                this.tabs.activate(this.panelMonitoring);
                                snmpTrapHostCmp.focus(true);
                            }.createDelegate(this) 
                        );
                        return false;
                    }
                    
                    snmpTrapPortCmp = Ext.getCmp('administration_snmp_trapPort');
                    if (!snmpTrapPortCmp.isValid()) {
                        Ext.MessageBox.alert(this.i18n._('Warning'), String.format(this.i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),
                            function () {
                                this.tabs.activate(this.panelMonitoring);
                                snmpTrapPortCmp.focus(true);
                            }.createDelegate(this) 
                        );
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
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("A \"Host\" must be specified."),
                        function () {
                            this.tabs.activate(this.panelMonitoring);
                            syslogHostCmp.focus(true);
                        }.createDelegate(this) 
                    );
                    return false;
                }
                var syslogPortCmp = Ext.getCmp('administration_syslog_port');
                if (!syslogPortCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), String.format(this.i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),
                        function () {
                            this.tabs.activate(this.panelMonitoring);
                            syslogPortCmp.focus(true);
                        }.createDelegate(this) 
                    );
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
            	this.saveSemaphore = 6;
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

               rpc.networkManager.setAccessSettings(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getAccessSettings());
                
               delete this.getAddressSettings().publicAddress;
               rpc.networkManager.setAddressSettings(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getAddressSettings());
                
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
                
                if (!this.isBrandingExpired()) {
                	this.saveSemaphore++;
                    main.getBrandingManager().setBaseSettings(function(result, exception) {
                        Ext.MessageBox.hide();
                        if (exception) {
                            Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                            return;
                        }
                        this.afterSave();
                    }.createDelegate(this), this.getBrandingBaseSettings());
                }
                
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
    
    // certificate generation window
    Ung.CertGenerateWindow = Ext.extend(Ung.ButtonsWindow, {
        // the certPanel
        certPanel : null,
        onRender : function(container, position) {
            Ung.CertGenerateWindow.superclass.onRender.call(this, container, position);
            this.initSubComponents.defer(1, this);
        },
        initSubComponents : function(container, position) {
            this.certPanel.render(this.getContentEl());
        },
        listeners : {
            'show' : {
                fn : function() {
                    this.certPanel.setHeight(this.getContentHeight());
                },
                delay : 1
            }
        },
        initButtons : function() {
            var settingsCmp = Ext.getCmp(this.certPanel.parentId);
            this.subCmps.push(new Ext.Button({
                name : 'Cancel',
                renderTo : 'button_inner_right_' + this.getId(),
                iconCls : 'cancelIcon',
                text : i18n._('Cancel'),
                handler : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }));
            this.subCmps.push(new Ext.Button({
                name : 'Proceed',
                renderTo : 'button_margin_right_' + this.getId(),
                iconCls : 'saveIcon',
                text : settingsCmp.i18n._('Proceed'),
                handler : function() {
                    this.proceedAction();
                }.createDelegate(this)
            }));
        },
        // the proceed actions
        // to override
        proceedAction : function() {
            main.todo();
        },
        beforeDestroy : function() {
            Ext.destroy(this.certPanel);
            Ung.ManageListWindow.superclass.beforeDestroy.call(this);
        }
    });
    
}