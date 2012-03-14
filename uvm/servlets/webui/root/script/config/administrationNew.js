if (!Ung.hasResource["Ung.Administration"]) {
    Ung.hasResource["Ung.Administration"] = true;

    Ext.namespace("Ung");
    Ext.namespace("Ung.Config");
    Ext.namespace("Ung.Config.Administration");

    Ext.define( "Ung.Config.Administration.SkinManager", {
        constructor : function( config )
        {
            /* List of stores to be refreshed, dynamically generated. */
            this.refreshList = [];
            this.i18n = config.i18n;
        },

        addRefreshableStore : function( store )
        {
            this.refreshList.push( store );
        },

        uploadSkin : function( cmp, form )
        {
            form.submit({
                parentId : cmp.getId(),
                waitMsg : this.i18n._('Please wait while your skin is uploaded...'),
                success : Ext.bind(this.uploadSkinSuccess, this ),
                failure : Ext.bind(this.uploadSkinFailure, this )
            });
        },

        uploadSkinSuccess : function( form, action )
        {
            this.storeSemaphore = this.refreshList.length;

            var handler = Ext.bind(function() {
                this.storeSemaphore--;
                if (this.storeSemaphore == 0) {
                    Ext.MessageBox.alert( this.i18n._("Succeeded"), this.i18n._("Upload Skin Succeeded"));
                    var field = form.findField( "upload_skin_textfield" );
                    if ( field != null ) field.reset();
                }
            },this);

            for ( var c = 0 ; c < this.storeSemaphore ; c++ ) this.refreshList[c].load({callback:handler});
        },

        uploadSkinFailure : function( form, action )
        {
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

    Ext.define("Ung.Administration", {
    	extend: "Ung.ConfigWin",
        panelAdministration : null,
        panelPublicAddress : null,
        panelCertificates : null,
        panelMonitoring : null,
        panelSkins : null,
        uploadedCustomLogo : false,
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : Ext.bind(function() {
                    this.cancelAction();
                },this)
            },{
                title : i18n._('Administration')
            }];
            this.skinManager = Ext.create('Ung.Config.Administration.SkinManager',{ 'i18n' :  i18n });
            this.initialSkin = this.getSkinSettings().administrationClientSkin;
            this.buildAdministration();
            this.buildPublicAddress();
            this.buildCertificates();
            this.buildMonitoring();
            this.buildSkins();

            // builds the tab panel with the tabs
            var adminTabs = [this.panelAdministration, this.panelPublicAddress, this.panelCertificates, this.panelMonitoring, this.panelSkins];
            this.buildTabPanel(adminTabs);
            this.tabs.setActiveTab(this.panelAdministration);
            Ung.Administration.superclass.initComponent.call(this);
        },
        // get base settings object
        getSkinSettings : function(forceReload) {
            if (forceReload || this.rpc.skinSettings === undefined) {
                try {
                    this.rpc.skinSettings = rpc.skinManager.getSkinSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
            }
            return this.rpc.skinSettings;
        },
        // get admin settings
        getAdminSettings : function(forceReload) {
            if (forceReload || this.rpc.adminSettings === undefined) {
                try {
                    this.rpc.adminSettings = rpc.adminManager.getAdminSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }

            }
            return this.rpc.adminSettings;
        },
        // get access settings
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
        // get address settings
        getAddressSettings : function(forceReload) {
            if (forceReload || this.rpc.addressSettings === undefined) {
                try {
                    this.rpc.addressSettings = rpc.networkManager.getAddressSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
            }
            return this.rpc.addressSettings;
        },
        // get snmp settings
        getSnmpSettings : function(forceReload) {
            if (forceReload || this.rpc.snmpSettings === undefined) {
                try {
                    this.rpc.snmpSettings = rpc.adminManager.getSnmpManager().getSnmpSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }

            }
            return this.rpc.snmpSettings;
        },
        // get logging settings
        getLoggingSettings : function(forceReload) {
            if (forceReload || this.rpc.loggingSettings === undefined) {
                try {
                    this.rpc.loggingSettings = main.getLoggingManager().getLoggingSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }

            }
            return this.rpc.loggingSettings;
        },
        // get Current Server CertInfo
        getCurrentServerCertInfo : function(forceReload) {
            if (forceReload || this.rpc.currentServerCertInfo === undefined) {
                try {
                    this.rpc.currentServerCertInfo = main.getAppServerManager().getCurrentServerCertInfo();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }

            }
            return this.rpc.currentServerCertInfo;
        },
        // get hostname
        getHostname : function(forceReload) {
            if (forceReload || this.rpc.hostname === undefined) {
                try {
                    this.rpc.hostname = rpc.networkManager.getHostname();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }

            }
            return this.rpc.hostname;
        },
        buildAdministration : function() {
            // keep initial access and address settings
            this.initialAccessSettings = Ung.Util.clone(this.getAccessSettings());
            this.initialAddressSettings = Ung.Util.clone(this.getAddressSettings());

            var changePasswordColumn = Ext.create("Ung.grid.EditColumn",{
                header : this.i18n._("change password"),
                width : 130,
                iconClass : 'icon-edit-row',
                handler : function(view, rowIndex, colIndex) {
                    // populate row editor
                	var rec = view.getStore().getAt(rowIndex);
                    this.grid.rowEditorChangePass.populate(rec);
                    this.grid.rowEditorChangePass.show();
                }
            });

          
            /* getID returns the same value, and this causes the password
             * field to not be rendered the second time around since it has an
             * existing ID. */
            var fieldID = "" + Math.round( Math.random() * 1000000 );

            this.panelAdministration = Ext.create('Ext.panel.Panel',{
                name : 'panelAdministration',
                helpSource : 'administration',
                // private fields
                parentId : this.getId(),
                title : this.i18n._('Administration'),
                layout : "anchor",
                defaults: {
                    anchor: '98%'
                },
                cls: 'ung-panel',
                autoScroll : true,
                items : [this.gridAdminAccounts=Ext.create("Ung.EditorGrid",{
                    settingsCmp : this,
                    title : this.i18n._("Admin Accounts"),
                    height : 300,
                    bodyStyle : 'padding-bottom:30px;',
                    autoScroll : true,
                    hasEdit : false,
                    name : 'gridAdminAccounts',
                    recordJavaClass : "com.untangle.uvm.User",
                    emptyRow : {
                        "login" : this.i18n._("[no login]"),
                        "name" : this.i18n._("[no description]"),
                        "hasWriteAccess" : true,
                        "hasReportsAccess" : true,
                        "sendAlerts":false,
                        "notes":this.i18n._("[no description]"),
                        "email" : this.i18n._("[no email]"),
                        "clearPassword" : "",
                        "javaClass" : "com.untangle.uvm.User"
                    },
                    ignoreServerIds:false,
                    dataFn : Ext.bind(function() { 
                        return this.buildUserList(false);
                    }, this),
                    dataRoot:'',
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
                        name : 'hasWriteAccess'
                    },{
                        name : "hasReportsAccess"
                    },{
                        name : 'javaClass' //needed as users is a set
                    }],
                    // the list of columns for the column model
                    columns : [{
                        header : this.i18n._("login"),
                        width : 200,
                        dataIndex : 'login',
                        field :{
                            xtype:'textfield',
                            allowBlank : false,
                            blankText : this.i18n._("The login name cannot be blank.")
                        }
                    }, {
                        header : this.i18n._("description"),
                        width : 200,
                        dataIndex : 'name',
                        flex: 1,
                        field:{
                            xtype:'textfield',
                            allowBlank : false
                        }
                    },{
                        header : this.i18n._("email"),
                        width : 200,
                        dataIndex : 'email',
                        field: {
                            xtype:'textfield',
                            allowBlank : false
                        }
                    }, changePasswordColumn
                    ],
                    sortField : 'login',
                    columnsDefaultSortable : true,
                    plugins : [changePasswordColumn],
                    // the row input lines used by the row editor window
                    rowEditorInputLines : [{
                    	xtype: "textfield",
                        name : "Login",
                        dataIndex : "login",
                        fieldLabel : this.i18n._("Login"),
                        allowBlank : false,
                        blankText : this.i18n._("The login name cannot be blank."),
                        width : 400
                    }, {
                    	xtype: "textfield",
                        name : "Name",
                        dataIndex : "name",
                        fieldLabel : this.i18n._("Description"),
                        allowBlank : false,
                        width : 400
                    },{
                    	xtype: "textfield",
                        name : "Email",
                        dataIndex : "email",
                        fieldLabel : this.i18n._("Email"),
                        width : 400
                    },{
                    	xtype: "textfield",
                        inputType: 'password',
                        name : "Password",
                        dataIndex : "clearPassword",
                        id : 'administration_rowEditor_password_'+ fieldID,
                        fieldLabel : this.i18n._("Password"),
                        width : 400,
                        minLength : 3,
                        minLengthText : Ext.String.format(this.i18n._("The password is shorter than the minimum {0} characters."), 3)
                    },{
                    	xtype: "textfield",
                        inputType: 'password',
                        name : "Confirm Password",
                        dataIndex : "clearPassword",
                        vtype: 'password',
                        initialPassField: 'administration_rowEditor_password_'+ fieldID, // id of the initial password field
                        fieldLabel : this.i18n._("Confirm Password"),
                        width : 400
                    }],
                    // the row input lines used by the change password window
                    rowEditorChangePassInputLines : [{
                    	xtype: "textfield",
                        inputType: 'password',
                        name : "Password",
                        dataIndex : "clearPassword",
                        id : 'administration_rowEditor1_password_'+ fieldID,
                        fieldLabel : this.i18n._("Password"),
                        width : 400,
                        minLength : 3,
                        minLengthText : Ext.String.format(this.i18n._("The password is shorter than the minimum {0} characters."), 3)
                    }, {
                    	xtype: "textfield",
                        inputType: 'password',
                        name : "Confirm Password",
                        dataIndex : "clearPassword",
                        vtype: 'password',
                        initialPassField: 'administration_rowEditor1_password_'+ fieldID, // id of the initial password field
                        fieldLabel : this.i18n._("Confirm Password"),
                        width : 400
                    }]
                }), {
                    xtype : 'fieldset',
                    id:'external_administration_fieldset',
                    title : this.i18n._('External Administration'),
                    autoHeight : true,
                    labelWidth: 150,
                    items : [{
                        xtype : 'checkbox',
                        name : 'isOutsideAdministrationEnabled',
                        boxLabel : this.i18n._('Enable External Administration.'),
                        hideLabel : true,
                        checked : this.getAccessSettings().isOutsideAdministrationEnabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getAccessSettings().isOutsideAdministrationEnabled = newValue;
                                },this)
                            }
                        }
                    },{
                        xtype : 'checkbox',
                        name : 'isOutsideReportingEnabled',
                        boxLabel : this.i18n._('Enable External Report Viewing.'),
                        hideLabel : true,
                        checked : this.getAccessSettings().isOutsideReportingEnabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getAccessSettings().isOutsideReportingEnabled = newValue;
                                },this)
                            }
                        }
                    },{
                        xtype : 'checkbox',
                        name : 'isOutsideQuarantineEnabled',
                        boxLabel : this.i18n._('Enable External Quarantine Viewing.'),
                        hideLabel : true,
                        checked : this.getAccessSettings().isOutsideQuarantineEnabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getAccessSettings().isOutsideQuarantineEnabled = newValue;
                                },this)
                            }
                        }
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('External HTTPS port'),
                        name : 'httpsPort',
                        id: 'administration_httpsPort',
                        value : this.getAddressSettings().httpsPort,
                        allowDecimals: false,
                        allowNegative: false,
                        allowBlank : false,
                        blankText : this.i18n._("You must provide a valid port."),
                        vtype : 'port',
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getAddressSettings().httpsPort = newValue;
                                },this)
                            }
                        }
                    },{
                        xtype : 'radio',
                        boxLabel : this.i18n._('Allow external access from any IP address.'),
                        hideLabel : true,
                        name : 'isOutsideAccessRestricted',
                        checked : !this.getAccessSettings().isOutsideAccessRestricted,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    this.getAccessSettings().isOutsideAccessRestricted = !checked;
                                    if (checked) {
                                        Ext.getCmp('administration_outsideNetwork').disable();
                                        Ext.getCmp('administration_outsideNetmask').disable();
                                    }
                                },this)
                            }
                        }
                    },{
                        xtype : 'radio',
                        boxLabel : this.i18n._('Restrict external access to these external IP address(es).'),
                        hideLabel : true,
                        name : 'isOutsideAccessRestricted',
                        checked : this.getAccessSettings().isOutsideAccessRestricted,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    this.getAccessSettings().isOutsideAccessRestricted = checked;
                                    if (checked) {
                                        Ext.getCmp('administration_outsideNetwork').enable();
                                        Ext.getCmp('administration_outsideNetmask').enable();
                                    }
                                },this)
                            }
                        }
                    },{
                        border: false,
                        layout:'column',
                        cls : 'administration_network',
                        items: [{
                            border: false,
                            columnWidth:.35,
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
                            columnWidth:.35,
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
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    this.getAccessSettings().isInsideInsecureEnabled = checked;
                                },this)
                            }
                        }
                    },{
                        xtype : 'radio',
                        boxLabel : this.i18n._('Disable HTTP administration inside the local network'),
                        hideLabel : true,
                        name : 'isInsideInsecureEnabled',
                        checked : !this.getAccessSettings().isInsideInsecureEnabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    this.getAccessSettings().isInsideInsecureEnabled = !checked;
                                },this)
                            }
                        }
                    },{
                        border: false,
                        cls: 'description',
                        html : this.i18n._('Note: HTTPS administration is always enabled internally')
                    }]
                }]
            });
            if ( this.gridAdminAccounts.rowEditorChangePassInputLines != null) {
                 this.gridAdminAccounts.rowEditorChangePass = Ext.create("Ung.RowEditorWindow",{
                    grid : this.gridAdminAccounts,
                    inputLines : this.gridAdminAccounts.rowEditorChangePassInputLines
                });
            }
        },
        buildPublicAddress : function() {
            var hostname = this.getHostname(true);

            var currentHostnameMessage = Ext.String.format( this.i18n._( 'Current Hostname: {0}'), '<i>' + hostname + '</i>' );
            if ( hostname.indexOf( "." ) < 0 ) {
            	currentHostnameMessage += Ext.String.format( this.i18n._( '{0}The current hostname is not a qualified hostname, click {1}here{2} to fix it{3}' ),
                                          '<br/><span class="warning">',
                                          '<a href="/alpaca/hostname/index" target="_blank">',
                                          '</a>', '</span>');
            }

            this.panelPublicAddress = Ext.create('Ext.panel.Panel',{
                name : 'panelPublicAddress',
                helpSource : 'public_address',
                // private fields
                parentId : this.getId(),
                title : this.i18n._('Public Address'),
                cls: 'ung-panel',
                autoScroll : true,
                items: {
                    xtype: 'fieldset',
                    autoHeight : true,
                    items : [{
                        cls: 'description',
                        html : Ext.String.format(this.i18n._('The Public Address is the address or hostname that provides a public routable address for the {0} Server. This address will be used in emails sent by the {0} Server to link back to services hosted on the {0} Server such as Quarantine Digests and OpenVPN Client emails.'),
                                             main.getBrandingManager().getCompanyName()),
                        bodyStyle : 'padding-bottom:10px;',
                        border : false
                    },{
                        xtype : 'radio',
                        boxLabel : this.i18n._('Use External IP address (default)'),
                        hideLabel : true,
                        name : 'publicAddress',
                        checked : !this.getAddressSettings().isPublicAddressEnabled && !this.getAddressSettings().isHostNamePublic,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    if (checked) {
                                        Ext.getCmp('administration_publicIPAddress').disable();
                                        Ext.getCmp('administration_publicPort').disable();
                                    }
                                },this)
                            }
                        }
                    },{
                        cls: 'description',
                        html : Ext.String.format(this.i18n._('This works if your {0} Server has a public static IP address.'),
                                   main.getBrandingManager().getCompanyName()),
                        bodyStyle : 'padding:0px 5px 10px 25px;',
                        border : false
                    },{
                        xtype : 'radio',
                        boxLabel : this.i18n._('Use Hostname'),
                        hideLabel : true,
                        name : 'publicAddress',
                        checked : this.getAddressSettings().isHostNamePublic,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    this.getAddressSettings().isHostNamePublic = checked;
                                    if (checked) {
                                        Ext.getCmp('administration_publicIPAddress').disable();
                                        Ext.getCmp('administration_publicPort').disable();
                                    }
                                },this)
                            }
                        }
                    },{
                        cls: 'description',
                        html : Ext.String.format(this.i18n._('This is recommended if the {0} Server\'s fully qualified domain name looks up to its IP address both internally and externally.'),
                                             main.getBrandingManager().getCompanyName()),
                        bodyStyle : 'padding:0px 5px 5px 25px;',
                        border : false
                    }, {
                        cls: 'description',
                        html : currentHostnameMessage,
                        bodyStyle : 'padding:0px 5px 10px 25px;',
                        border : false
                    }, {
                        xtype : 'radio',
                        boxLabel : this.i18n._('Use Manually Specified IP'),
                        hideLabel : true,
                        name : 'publicAddress',
                        checked : this.getAddressSettings().isPublicAddressEnabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    this.getAddressSettings().isPublicAddressEnabled = checked;
                                    if (checked) {
                                        Ext.getCmp('administration_publicIPAddress').enable();
                                        Ext.getCmp('administration_publicPort').enable();
                                    }
                                },this)
                            }
                        }
                    },{
                        cls: 'description',
                        html : Ext.String.format(this.i18n._('This is recommended if the {0} Server is installed behind another firewall with a port forward from the specified IP that redirects traffic to the {0} Server.'),
                                    main.getBrandingManager().getCompanyName()),
                        bodyStyle : 'padding:0px 5px 5px 25px;',
                        border : false
                    },{
                        xtype : 'panel',
                        bodyStyle : 'padding-left:25px;',
                        border : false,
                        items : [{
                            xtype : 'textfield',
                            fieldLabel : this.i18n._('Address'),
                            name : 'publicIPAddress',
                            id: 'administration_publicIPAddress',
                            value : this.getAddressSettings().publicIPAddress,
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
            this.panelCertificates = Ext.create('Ext.panel.Panel',{
                name : 'panelCertificates',
                helpSource : 'certificates',
                // private fields
                parentId : this.getId(),
                winGenerateSelfSignedCertificate : null,
                winGenerateCertGenTrusted : null,
                winCertImportTrusted : null,

                title : this.i18n._('Certificates'),
                layout : "anchor",
                cls: 'ung-panel',
                autoScroll : true,
                defaults : {
                    anchor: '98%',
                    xtype : 'fieldset',
                    autoHeight : true
                },
                items: [{
                    title: this.i18n._('Status'),
                    labelWidth: 150,
                    items : [{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Valid starting'),
                        labelStyle: 'font-weight:bold',
                        id : 'administration_status_notBefore',
                        value : this.getCurrentServerCertInfo() == null ? "" : i18n.timestampFormat(this.getCurrentServerCertInfo().notBefore),
                        disabled : true,
                        anchor:'100%'
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Valid until'),
                        labelStyle: 'font-weight:bold',
                        id : 'administration_status_notAfter',
                        value : this.getCurrentServerCertInfo() == null ? "" : i18n.timestampFormat(this.getCurrentServerCertInfo().notAfter),
                        disabled : true,
                        anchor:'100%'
                    },{
                        xtype : 'textarea',
                        fieldLabel : this.i18n._('Subject DN'),
                        labelStyle: 'font-weight:bold',
                        id : 'administration_status_subjectDN',
                        value : this.getCurrentServerCertInfo() == null ? "" : this.getCurrentServerCertInfo().subjectDN,
                        disabled : true,
                        anchor:'100%',
                        height : 40
                    },{
                        xtype : 'textarea',
                        fieldLabel : this.i18n._('Issuer DN'),
                        labelStyle: 'font-weight:bold',
                        id : 'administration_status_issuerDN',
                        value : this.getCurrentServerCertInfo() == null ? "" : this.getCurrentServerCertInfo().issuerDN,
                        disabled : true,
                        anchor:'100%',
                        height : 40
                    }]
                },{
                    title: this.i18n._('Generation'),
                    defaults : {
                        xtype : 'fieldset',
                        autoHeight : true,
                        layout:'column'
                    },
                    items : [{
	                        cls: 'description',
	                        html : this.i18n._('You must complete each of these steps in order every time you import a new signed certificate!'),
	                        bodyStyle : 'padding-bottom:10px;',
	                        border : false
                    	},{
                        items: [{
                            border: false,
                            width: 30,
                            html:'<div class="step_counter">1</div>'
                        },{
                            border: false,
                            width: 270,
                            items: [{
                                xtype : 'button',
                                text : Ext.String.format(this.i18n._('Generate a {0}Certificate{1}'), '<b>', '</b>'),
                                minWidth : 250,
                                name : 'Generate a Self-Signed Certificate',
                                iconCls : 'action-icon',
                                handler : Ext.bind(function() {
                                    this.panelCertificates.onGenerateSelfSignedCertificate();
                                },this)
                            }]
                        },{
                            border: false,
                            columnWidth:1,
                            items: [{
                                cls: 'description',
                                html : this.i18n._("Click here to create a new Certificate.  You should create a new Certificate to change any of the fields in the 'Subject DN'."),
                                border : false
                            }]
                        }]
                    },{
                        items: [{
                            border: false,
                            width: 30,
                            html:'<div class="step_counter">2</div>'
                        },{
                            border: false,
                            width: 270,
                            items: [{
                                xtype : 'button',
                                text : Ext.String.format(this.i18n._('Generate a {0}CSR{1}'), '<b>', '</b>'),
                                minWidth : 250,
                                name : 'Generate a Self-Signed Certificate',
                                iconCls : 'action-icon',
                                handler : Ext.bind(function() {
                                    this.panelCertificates.onGenerateCertGenTrusted();
                                },this)
                            }]
                        },{
                            border: false,
                            columnWidth : 1,
                            items: [{
                                cls: 'description',
                                html : this.i18n._("Click this button to generate a Certificate Signature Request (CSR), which you can then copy and paste for use by certificate authorities such as Thawte, Verisign, etc."),
                                border : false
                            }]
                        }]
                    },{
                        items: [{
                            border: false,
                            width: 30,
                            html:'<div class="step_counter">3</div>'
                        },{
                            border: false,
                            width: 270,
                            items: [{
                                xtype : 'button',
                                text : Ext.String.format(this.i18n._('Import a {0}Signed Certificate{1}'), '<b>', '</b>'),
                                minWidth : 250,
                                name : 'Generate a Self-Signed Certificate',
                                iconCls : 'action-icon',
                                handler : Ext.bind(function() {
                                    this.panelCertificates.onCertImportTrusted();
                                },this)
                            }]
                        },{
                            border: false,
                            columnWidth:1,
                            items: [{
                                cls: 'description',
                                html : Ext.String.format(this.i18n._("Click this button to import a signed certificate which has been generated by a certificate authority, and was based on a previous signature request from {0}."),
                                            main.getBrandingManager().getCompanyName()),
                                border : false
                            }]
                        }]
                    }]
                }],

                onGenerateSelfSignedCertificate : function() {
                    var settingsCmp = Ext.getCmp(this.parentId);

                    if (!this.winGenerateSelfSignedCertificate) {
                        settingsCmp.buildGenerateSelfSignedCertificate();
                        this.winGenerateSelfSignedCertificate = Ext.create('Ung.CertGenerateWindow',{
                            breadcrumbs : [{
                                title : i18n._("Configuration"),
                                action : Ext.bind(function() {
                                    this.panelCertificates.winGenerateSelfSignedCertificate.cancelAction();
                                    this.cancelAction();
                                },settingsCmp)
                            }, {
                                title : i18n._('Administration'),
                                action : Ext.bind(function() {
                                    this.panelCertificates.winGenerateSelfSignedCertificate.cancelAction();
                                },settingsCmp)
                            }, {
                                title : i18n._('Certificates'),
                                action : Ext.bind(function() {
                                    this.panelCertificates.winGenerateSelfSignedCertificate.cancelAction();
                                },settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Generate a Self-Signed Certificate")
                            }],
                            items : settingsCmp.panelGenerateSelfSignedCertificate,

                            proceedAction : Ext.bind(function() {
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
                                var distinguishedName = Ext.String.format("C={4},ST={3},L={2},OU={1},O={0}",
                                        organization, organizationUnit, city, state, country);

                                // generate certificate
                                main.getAppServerManager().regenCert(Ext.bind(function(result, exception) {
                                    if(Ung.Util.handleException(exception)) return;
                                    if (result) { //true or false
                                        //success

                                        //update status
                                        this.updateCertificatesStatus();

                                        Ext.MessageBox.alert(this.i18n._("Succeeded"), this.i18n._("Certificate Successfully Generated"),
                                        		Ext.bind(function () {
                                                this.panelCertificates.winGenerateSelfSignedCertificate.cancelAction();
                                            },this)
                                        );
                                    } else {
                                        //failed
                                        Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("Error generating self-signed certificate"));
                                        return;
                                    }
                                },this), distinguishedName, 5*365);

                            },settingsCmp)
                        });
                    } else {
                        /* Refresh the hostname */
                        var hostnameField = null;
                        hostnameField = settingsCmp.panelGenerateSelfSignedCertificate.find( "name", "hostname" )[0];
                        if ( hostnameField != null ) {
                            hostnameField.setValue( settingsCmp.getHostname( true ));
                        }
                    }
                    
                    this.winGenerateSelfSignedCertificate.show();
                },

                onGenerateCertGenTrusted : function() {
                    if (!this.winGenerateCertGenTrusted) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildGenerateCertGenTrusted();
                        this.winGenerateCertGenTrusted = Ext.create('Ung.CertGenerateWindow',{
                            breadcrumbs : [{
                                title : i18n._("Configuration"),
                                action : Ext.bind(function() {
                                    this.panelCertificates.winGenerateCertGenTrusted.cancelAction();
                                    this.cancelAction();
                                },settingsCmp)
                            }, {
                                title : i18n._('Administration'),
                                action : Ext.bind(function() {
                                    this.panelCertificates.winGenerateCertGenTrusted.cancelAction();
                                },settingsCmp)
                            }, {
                                title : i18n._('Certificates'),
                                action : Ext.bind(function() {
                                    this.panelCertificates.winGenerateCertGenTrusted.cancelAction();
                                },settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Generate a Certificate Signature Request")
                            }],
                            items : settingsCmp.panelGenerateCertGenTrusted,

                            proceedAction : Ext.bind(function() {
                                Ext.MessageBox.wait(this.i18n._("Generating Certificate..."), i18n._("Please wait"));

                                // generate certificate request
                                main.getAppServerManager().generateCSR(Ext.bind(function(result, exception) {
                                    if(Ung.Util.handleException(exception)) return;
                                    if (result != null) {
                                        //success
                                        Ext.MessageBox.alert(this.i18n._("Succeeded"), this.i18n._("Certificate Signature Request Successfully Generated"),
                                    		Ext.bind(function () {
                                                var crsCmp = Ext.getCmp('administration_crs');
                                                crsCmp.setValue(result);
                                                crsCmp.focus(true);
                                            },this)
                                        );
                                    } else {
                                        //failed
                                        Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("Error generating certificate signature request"));
                                        return;
                                    }
                                },this));

                            },settingsCmp)
                        });
                    }
                    this.winGenerateCertGenTrusted.show();
                },

                onCertImportTrusted : function() {
                    if (!this.winCertImportTrusted) {
                        var settingsCmp = Ext.getCmp(this.parentId);
                        settingsCmp.buildCertImportTrusted();
                        this.winCertImportTrusted = Ext.create('Ung.CertGenerateWindow',{
                            breadcrumbs : [{
                                title : i18n._("Configuration"),
                                action : Ext.bind(function() {
                                    this.panelCertificates.winCertImportTrusted.cancelAction();
                                    this.cancelAction();
                                },settingsCmp)
                            }, {
                                title : i18n._('Administration'),
                                action : Ext.bind(function() {
                                    this.panelCertificates.winCertImportTrusted.cancelAction();
                                },settingsCmp)
                            }, {
                                title : i18n._('Certificates'),
                                action : Ext.bind(function() {
                                    this.panelCertificates.winCertImportTrusted.cancelAction();
                                },settingsCmp)
                            }, {
                                title : settingsCmp.i18n._("Import Signed Certificate")
                            }],
                            items : settingsCmp.panelCertImportTrusted,

                            proceedAction : Ext.bind(function() {
                                Ext.MessageBox.wait(this.i18n._("Importing Certificate..."), i18n._("Please wait"));

                                //get user values
                                var cert = Ext.getCmp('administration_import_cert').getValue();
                                var caCert = Ext.getCmp('administration_import_caCert').getValue();

                                // import certificate
                                main.getAppServerManager().importServerCert(Ext.bind(function(result, exception) {
                                    if(Ung.Util.handleException(exception)) return;
                                    if (result) { //true or false
                                        //success

                                        //update status
                                        this.updateCertificatesStatus();

                                        Ext.MessageBox.alert(this.i18n._("Succeeded"), this.i18n._("Certificate Successfully Imported"),
                                    		Ext.bind(function () {
                                                this.panelCertificates.winCertImportTrusted.cancelAction();
                                            },this)
                                        );
                                    } else {
                                        //failed
                                        Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("Error importing certificate"));
                                        return;
                                    }
                                },this), cert, caCert.length==0?null:caCert );

                            },settingsCmp)
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

            });
        },

        // Generate Self-Signed certificate
        buildGenerateSelfSignedCertificate : function() {
            this.panelGenerateSelfSignedCertificate = Ext.create('Ext.panel.Panel',{
                name : 'panelGenerateSelfSignedCertificate',
                // private fields
                parentId : this.getId(),
                title : this.i18n._('Generate a Self-Signed Certificate'),
                cls: 'ung-panel',
                autoScroll : true,
                items: {
                    xtype: 'fieldset',
                    autoHeight : true,
                    labelWidth: 150,
                    items : [{
                        cls: 'description',
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
                        xtype : 'combo',
                        fieldLabel : this.i18n._('Country') + " (C)",
                        name : 'country',
                        id: 'administration_country',
                        width : 200,
                        listWidth : 205,
                        store : Ung.Country.getCountryStore(i18n),
                        mode : 'local',
                        triggerAction : 'all',
                        editable : false,
                        allowBlank : false,
                        blankText : this.i18n._("You must specify a country.")
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Hostname') + " (CN)",
                        name : 'hostname',
                        id: 'administration_hostname',
                        value : this.getHostname( true ),
                        disabled : true
                    }]
                }
            });
        },

        // Generate Signature Request
        buildGenerateCertGenTrusted : function() {
            this.panelGenerateCertGenTrusted = Ext.create('Ext.panel.Panel',{
                name : 'panelGenerateCertGenTrusted',
                // private fields
                parentId : this.getId(),
                layout: "anchor",
                title : this.i18n._('Generate a Certificate Signature Request'),
                cls: 'ung-panel',
                autoScroll : true,
                items: [{
                    cls: 'description',
                    html : this.i18n._('Click the Proceed button to generate a signature below. Copy the signature (Control-C), and paste it into the necessary form from your Certificate Authority (Verisign, Thawte, etc.).'),
                    bodyStyle : 'padding-bottom:10px;',
                    border : false
                },{
                    xtype : 'textarea',
                    name : 'crs',
                    id: 'administration_crs',
                    anchor:'98%',
                    height : 200,
                    hideLabel : true
                }]
            });
        },

        // Import Signed Certificate
        buildCertImportTrusted : function() {
            this.panelCertImportTrusted = Ext.create('Ext.panel.Panel',{
                name : 'panelCertImportTrusted',
                // private fields
                parentId : this.getId(),
                layout: "anchor",
                title : this.i18n._('Import Signed Certificate'),
                cls: 'ung-panel',
                autoScroll : true,
                    items : [{
                        cls: 'description',
                        html : this.i18n._('When your Certificate Authority (Verisign, Thawte, etc.) has sent your Signed Certificate, copy and paste it below (Control-V) then press the Proceed button.'),
                        bodyStyle : 'padding-bottom:10px;',
                        border : false
                    },{
                        xtype : 'textarea',
                        name : 'cert',
                        id: 'administration_import_cert',
                        anchor:'98%',
                        height : 200,
                        hideLabel : true
                    },{
                        cls: 'description',
                        html : this.i18n._('If your Certificate Authority (Verisign, Thawte, etc.) also send you an Intermediate Certificate, paste it below. Otherwise, do not paste anything below.'),
                        bodyStyle : 'padding:20px 0px 10px 0px;',
                        border : false
                    },{
                        xtype : 'textarea',
                        name : 'caCert',
                        id: 'administration_import_caCert',
                        anchor:'98%',
                        height : 200,
                        hideLabel : true
                    }]
            });
        },

        updateCertificatesStatus : function() {
            var certInfo = this.getCurrentServerCertInfo(true);
            if (certInfo != null) {
                Ext.getCmp('administration_status_notBefore').setValue(i18n.timestampFormat(certInfo.notBefore));
                Ext.getCmp('administration_status_notAfter').setValue(i18n.timestampFormat(certInfo.notAfter));
                Ext.getCmp('administration_status_subjectDN').setValue(certInfo.subjectDN);
                Ext.getCmp('administration_status_issuerDN').setValue(certInfo.issuerDN);
            }
        },

        buildMonitoring : function() {
            // keep initial settings
            this.initialSnmpSettings = Ung.Util.clone(this.getSnmpSettings());
            this.initialLoggingSettings = Ung.Util.clone(this.getLoggingSettings());

            this.panelMonitoring = Ext.create('Ext.panel.Panel',{
                name : 'panelMonitoring',
                helpSource : 'monitoring',
                // private fields
                parentId : this.getId(),
                title : this.i18n._('Monitoring'),
                cls: 'ung-panel',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true
                },
                items: [{
                    title: this.i18n._('SNMP'),
                    labelWidth: 150,
                    items : [{
                        xtype : 'radio',
                        boxLabel : Ext.String.format(this.i18n._('{0}Disable{1} SNMP Monitoring. (This is the default setting.)'), '<b>', '</b>'),
                        hideLabel : true,
                        name : 'snmpEnabled',
                        checked : !this.getSnmpSettings().enabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
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
                                },this)
                            }
                        }
                    },{
                        xtype : 'radio',
                        boxLabel : Ext.String.format(this.i18n._('{0}Enable{1} SNMP Monitoring.'), '<b>', '</b>'),
                        hideLabel : true,
                        name : 'snmpEnabled',
                        checked : this.getSnmpSettings().enabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
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
                                },this)
                            }
                        }
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Community'),
                        name : 'communityString',
                        itemCls : 'left-indent-1',
                        id: 'administration_snmp_communityString',
                        value : this.getSnmpSettings().communityString == 'CHANGE_ME' ? this.i18n._('CHANGE_ME') : this.getSnmpSettings().communityString,
                        allowBlank : false,
                        blankText : this.i18n._("An SNMP \"Community\" must be specified."),
                        disabled : !this.getSnmpSettings().enabled
                    },{
                        xtype : 'textfield',
                        itemCls : 'left-indent-1',
                        fieldLabel : this.i18n._('System Contact'),
                        name : 'sysContact',
                        id: 'administration_snmp_sysContact',
                        value : this.getSnmpSettings().sysContact == 'MY_CONTACT_INFO' ? this.i18n._('MY_CONTACT_INFO') : this.getSnmpSettings().sysContact,
                        disabled : !this.getSnmpSettings().enabled
                        //vtype : 'email'
                    },{
                        xtype : 'textfield',
                        itemCls : 'left-indent-1',
                        fieldLabel : this.i18n._('System Location'),
                        name : 'sysLocation',
                        id: 'administration_snmp_sysLocation',
                        value : this.getSnmpSettings().sysLocation == 'MY_LOCATION' ? this.i18n._('MY_LOCATION') : this.getSnmpSettings().sysLocation,
                        disabled : !this.getSnmpSettings().enabled
                    },{
                        xtype : 'radio',
                        itemCls : 'left-indent-1',
                        boxLabel : Ext.String.format(this.i18n._('{0}Disable Traps{1} so no trap events are generated.  (This is the default setting.)'), '<b>', '</b>'),
                        hideLabel : true,
                        name : 'sendTraps',
                        id: 'administration_snmp_sendTraps_disable',
                        checked : !this.getSnmpSettings().sendTraps,
                        disabled : !this.getSnmpSettings().enabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    if (checked) {
                                        Ext.getCmp('administration_snmp_trapCommunity').disable();
                                        Ext.getCmp('administration_snmp_trapHost').disable();
                                        Ext.getCmp('administration_snmp_trapPort').disable();
                                    }
                                },this)
                            }
                        }
                    },{
                        xtype : 'radio',
                        itemCls : 'left-indent-1',
                        boxLabel : Ext.String.format(this.i18n._('{0}Enable Traps{1} so trap events are sent when they are generated.'), '<b>', '</b>'),
                        hideLabel : true,
                        name : 'sendTraps',
                        id: 'administration_snmp_sendTraps_enable',
                        checked : this.getSnmpSettings().sendTraps,
                        disabled : !this.getSnmpSettings().enabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    if (this.getSnmpSettings().enabled && checked) {
                                        Ext.getCmp('administration_snmp_trapCommunity').enable();
                                        Ext.getCmp('administration_snmp_trapHost').enable();
                                        Ext.getCmp('administration_snmp_trapPort').enable();
                                    }
                                },this)
                            }
                        }
                    },{
                        xtype : 'textfield',
                        itemCls : 'left-indent-2',
                        fieldLabel : this.i18n._('Community'),
                        name : 'trapCommunity',
                        id: 'administration_snmp_trapCommunity',
                        value : this.getSnmpSettings().trapCommunity == 'MY_TRAP_COMMUNITY' ? this.i18n._('MY_TRAP_COMMUNITY') : this.getSnmpSettings().trapCommunity,
                        allowBlank : false,
                        blankText : this.i18n._("An Trap \"Community\" must be specified."),
                        disabled : !this.getSnmpSettings().enabled || !this.getSnmpSettings().sendTraps
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Host'),
                        itemCls : 'left-indent-2',
                        name : 'trapHost',
                        id: 'administration_snmp_trapHost',
                        value : this.getSnmpSettings().trapHost == 'MY_TRAP_HOST' ? this.i18n._('MY_TRAP_HOST') : this.getSnmpSettings().trapHost,
                        allowBlank : false,
                        blankText : this.i18n._("An Trap \"Host\" must be specified."),
                        disabled : !this.getSnmpSettings().enabled || !this.getSnmpSettings().sendTraps
                    },{
                        xtype : 'numberfield',
                        itemCls : 'left-indent-2',
                        fieldLabel : this.i18n._('Port'),
                        name : 'trapPort',
                        id: 'administration_snmp_trapPort',
                        value : this.getSnmpSettings().trapPort,
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
                        boxLabel : Ext.String.format(this.i18n._('{0}Disable{1} Syslog Monitoring. (This is the default setting.)'), '<b>', '</b>'),
                        hideLabel : true,
                        name : 'syslogEnabled',
                        checked : !this.getLoggingSettings().syslogEnabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    this.getLoggingSettings().syslogEnabled = !checked;
                                    if (checked) {
                                        Ext.getCmp('administration_syslog_host').disable();
                                        Ext.getCmp('administration_syslog_port').disable();
                                        Ext.getCmp('administration_syslog_facility').disable();
                                        Ext.getCmp('administration_syslog_threshold').disable();
                                        Ext.getCmp('administration_syslog_protocol').disable();
                                    }
                                },this)
                            }
                        }
                    },{
                        xtype : 'radio',
                        boxLabel : Ext.String.format(this.i18n._('{0}Enable{1} Syslog Monitoring.'), '<b>', '</b>'),
                        hideLabel : true,
                        name : 'syslogEnabled',
                        checked : this.getLoggingSettings().syslogEnabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    this.getLoggingSettings().syslogEnabled = checked;
                                    if (checked) {
                                        Ext.getCmp('administration_syslog_host').enable();
                                        Ext.getCmp('administration_syslog_port').enable();
                                        Ext.getCmp('administration_syslog_facility').enable();
                                        Ext.getCmp('administration_syslog_threshold').enable();
                                        Ext.getCmp('administration_syslog_protocol').enable();
                                    }
                                },this)
                            }
                        }
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Host'),
                        name : 'syslogHost',
                        itemCls : 'left-indent-1',
                        id : 'administration_syslog_host',
                        value : this.getLoggingSettings().syslogHost,
                        allowBlank : false,
                        blankText : this.i18n._("A \"Host\" must be specified."),
                        disabled : !this.getLoggingSettings().syslogEnabled
                    },{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Port'),
                        name : 'syslogPort',
                        itemCls : 'left-indent-1',
                        id : 'administration_syslog_port',
                        value : this.getLoggingSettings().syslogPort,
                        allowDecimals: false,
                        allowNegative: false,
                        allowBlank : false,
                        blankText : this.i18n._("You must provide a valid port."),
                        vtype : 'port',
                        disabled : !this.getLoggingSettings().syslogEnabled
                    },{
                        xtype : 'combo',
                        name : 'syslogFacility',
                        itemCls : 'left-indent-1',
                        id : 'administration_syslog_facility',
                        editable : false,
                        fieldLabel : this.i18n._('Facility'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        store : [
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
                        ],
                        value : this.getLoggingSettings().syslogFacility,
                        disabled : !this.getLoggingSettings().syslogEnabled
                    },{
                        xtype : 'combo',
                        name : 'syslogThreshold',
                        itemCls : 'left-indent-1',
                        id : 'administration_syslog_threshold',
                        editable : false,
                        fieldLabel : this.i18n._('Threshold'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        store : [
                                ["EMERGENCY", this.i18n._("emergency")],
                                ["ALERT", this.i18n._("alert")],
                                ["CRITICAL", this.i18n._("critical")],
                                ["ERROR", this.i18n._("error")],
                                ["WARNING", this.i18n._("warning")],
                                ["NOTICE", this.i18n._("notice")],
                                ["INFORMATIONAL", this.i18n._("informational")],
                                ["DEBUG", this.i18n._("debug")]
                        ],
                        displayField : 'name',
                        valueField : 'key',
                        value : this.getLoggingSettings().syslogThreshold,
                        disabled : !this.getLoggingSettings().syslogEnabled
                     },{
                        xtype : 'combo',
                        name : 'syslogProtocol',
                        itemCls : 'left-indent-1',
                        id : 'administration_syslog_protocol',
                        editable : false,
                        fieldLabel : this.i18n._('Protocol'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        store : [
                                ["UDP", this.i18n._("UDP")],
                                ["TCP", this.i18n._("TCP")]
                        ],
                        displayField : 'name',
                        valueField : 'key',
                        value : this.getLoggingSettings().syslogProtocol,
                        disabled : !this.getLoggingSettings().syslogEnabled
                    }]
                }]
            });
        },
        buildSkins : function() {
            // keep initial skin settings
            this.initialSkinSettings = Ung.Util.clone(this.getSkinSettings());

            var adminSkinsStore = Ext.create("Ext.data.Store",{
            	fields: [{
                    name: 'name'
                },{
                    name: 'displayName',
                    convert : Ext.bind(function(v) {
                        if ( v == "Default" ) return this.i18n._("Default");
                        return v;
                    },this)
                }],
                proxy: Ext.create("Ext.data.proxy.Server",{
                	doRequest: function(operation, callback, scope) {
                		rpc.skinManager.getSkinsList(Ext.bind(function(result, exception) {
                            if(Ung.Util.handleException(exception)) return;
                    		this.processResponse(exception==null, operation, null, result, callback, scope);
                		},this),true,false);
                	},
                    reader : {
                    	type: 'json',
                        root : 'list'
                    }
                })
            });

            this.skinManager.addRefreshableStore( adminSkinsStore );

            
            this.panelSkins = Ext.create('Ext.panel.Panel',{
                name : "panelSkins",
                helpSource : 'skins',
                // private fields
                parentId : this.getId(),
                title : this.i18n._('Skins'),
                cls: 'ung-panel',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Administration Skin'),
                    items : [{
                        cls: 'description',
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
                        editable : false,
                        typeAhead : true,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        selectOnFocus : true,
                        hideLabel : true,
                        listeners : {
                            "select" : {
                                fn : Ext.bind(function(elem, record) {
                                    this.getSkinSettings().administrationClientSkin = record[0].data.name;
                                },this)
                            }
                        }
                    }]
                },{
                    title : this.i18n._('Upload New Skin'),
                    items : {
                        fileUpload : true,
                        xtype : 'form',
                        id : 'upload_skin_form',
                        url : 'upload',
                        border : false,
                        items : [{
                            fieldLabel : this.i18n._('File'),
                            name : 'upload_skin_textfield',
                            inputType : 'file',
                            xtype : 'textfield',
                            allowBlank : false
                        },{
                            xtype : 'button',
                            text : this.i18n._("Upload"),
                            handler : Ext.bind(function() {
                                this.panelSkins.onUpload();
                            },this)
                        },{
                            xtype : 'hidden',
                            name : 'type',
                            value : 'skin'
                        }]
                    }
                }],
                onUpload : function() {
                    var prova = Ext.getCmp('upload_skin_form');
                    var cmp = Ext.getCmp(this.parentId);
                    var form = prova.getForm();

                    cmp.skinManager.uploadSkin( cmp, form );
                }
            });
            adminSkinsStore.load({
                callback : Ext.bind(function() {
                    var skinCombo=Ext.getCmp('administration_admin_client_skin_combo');
                    if(skinCombo!=null) {
                        skinCombo.setValue(this.getSkinSettings().administrationClientSkin);
                    }
                },this)
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
                        Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._("The login name: \"{0}\" in row: {1}  already exists."), listAdminAccounts[j].login, j+1),
                    		Ext.bind(function () {
                                this.tabs.setActiveTab(this.panelAdministration);
                            },this)
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
            		Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelAdministration);
                    },this)
                );
                return false;
            }

            // verify that there was at least one non-read-only account
            if(!oneWritableAccount){
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("There must always be at least one non-read-only (writable) account."),
            		Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelAdministration);
                    },this)
                );
                return false;
            }

            return true;
        },

        //validate External Administration
        validateExternalAdministration : function() {
            var httpsPortCmp = Ext.getCmp('administration_httpsPort');
            if (!httpsPortCmp.isValid()) {
                Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),
            		Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelAdministration);
                        httpsPortCmp.focus(true);
                    },this)
                );
                return false;
            }

            var isOutsideAccessRestricted = this.getAccessSettings().isOutsideAccessRestricted;
            if (isOutsideAccessRestricted) {
                var outsideNetworkCmp = Ext.getCmp('administration_outsideNetwork');
                if (!outsideNetworkCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('Invalid External Remote Administration \"IP Address\" specified.'),
                		Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelAdministration);
                            outsideNetworkCmp.focus(true);
                        },this)
                    );
                    return false;
                }
                var outsideNetmaskCmp = Ext.getCmp('administration_outsideNetmask');
                if (!outsideNetmaskCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("Invalid External Remote Administration \"Netmask\" specified."),
                		Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelAdministration);
                            outsideNetmaskCmp.focus(true);
                        },this)
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
                var publicIPAddressCmp = Ext.getCmp('administration_publicIPAddress');
                if (!publicIPAddressCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("You must provide a valid IP Address."),
                		Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelPublicAddress);
                            publicIPAddressCmp.focus(true);
                        },this)
                    );
                    return false;
                }
                var publicPortCmp = Ext.getCmp('administration_publicPort');
                if (!publicPortCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),
                		Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelPublicAddress);
                            publicPortCmp.focus(true);
                        },this)
                    );
                    return false;
                }
                //prepare for save
                this.getAddressSettings().publicIPAddress = publicIPAddressCmp.getValue();
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
                		Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelMonitoring);
                            snmpCommunityCmp.focus(true);
                        },this)
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
                    		Ext.bind(function () {
                                this.tabs.setActiveTab(this.panelMonitoring);
                                snmpTrapCommunityCmp.focus(true);
                            },this)
                        );
                        return false;
                    }

                    snmpTrapHostCmp = Ext.getCmp('administration_snmp_trapHost');
                    if (!snmpTrapHostCmp.isValid()) {
                        Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("An Trap \"Host\" must be specified."),
                    		Ext.bind(function () {
                                this.tabs.setActiveTab(this.panelMonitoring);
                                snmpTrapHostCmp.focus(true);
                            },this)
                        );
                        return false;
                    }

                    snmpTrapPortCmp = Ext.getCmp('administration_snmp_trapPort');
                    if (!snmpTrapPortCmp.isValid()) {
                        Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),
                    		Ext.bind(function () {
                                this.tabs.setActiveTab(this.panelMonitoring);
                                snmpTrapPortCmp.focus(true);
                            },this)
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
                		Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelMonitoring);
                            syslogHostCmp.focus(true);
                        },this)
                    );
                    return false;
                }
                if (syslogHostCmp.getValue() == 'localhost' || syslogHostCmp.getValue() == '127.0.0.1') {
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("The \"Host\" needs to be remote."),
                		Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelMonitoring);
                            syslogHostCmp.focus(true);
                        },this)
                    );
                    return false;
                }
                var syslogPortCmp = Ext.getCmp('administration_syslog_port');
                if (!syslogPortCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),
                		Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelMonitoring);
                            syslogPortCmp.focus(true);
                        },this)
                    );
                    return false;
                }
                //prepare for save
                var syslogFacilityCmp = Ext.getCmp('administration_syslog_facility');
                var syslogThresholdCmp = Ext.getCmp('administration_syslog_threshold');
                var syslogProtocolCmp = Ext.getCmp('administration_syslog_protocol');

                this.getLoggingSettings().syslogHost = syslogHostCmp.getValue();
                this.getLoggingSettings().syslogPort = syslogPortCmp.getValue();
                this.getLoggingSettings().syslogFacility = syslogFacilityCmp.getValue();
                this.getLoggingSettings().syslogThreshold = syslogThresholdCmp.getValue();
                this.getLoggingSettings().syslogProtocol = syslogProtocolCmp.getValue();
            }
            return true;
        },
        applyAction : function()
        {
            this.commitSettings(Ext.bind(this.reloadSettings,this));
        },
        reloadSettings : function()
        {
            if (this.needRefresh) {
                Ung.Util.goToStartPage();
                return;
            }

            this.initialSkinSettings = Ung.Util.clone(this.getSkinSettings(true));
            this.getAdminSettings(true);
            this.initialAccessSettings = Ung.Util.clone(this.getAccessSettings(true));
            this.initialAddressSettings = Ung.Util.clone(this.getAddressSettings(true));
            this.initialSnmpSettings = Ung.Util.clone(this.getSnmpSettings(true));
            this.initialLoggingSettings = Ung.Util.clone(this.getLoggingSettings(true));
            this.getCurrentServerCertInfo(true);
            this.getHostname(true);
            this.gridAdminAccounts.clearDirty();

            Ext.MessageBox.hide();
        },
        saveAction : function()
        {
            this.commitSettings(Ext.bind(this.completeSaveAction,this));
        },
        completeSaveAction : function()
        {
            Ext.MessageBox.hide();
            this.closeWindow();
        },
        // save function
        commitSettings : function(callback)
        {
            /* A hook for doing something in a node before attempting to save */

            //check to see if the remote administrative settings were changed in order to inform the user
            var remoteChanges = this.hasRemoteChanges();

            if (remoteChanges) {
                Ext.Msg.show({
                    title : this.i18n._("Warning"),
                    msg : Ext.String.format(this.i18n._("Changing the administration settings may disconnect you. Do you want to proceed?")),
                    buttons : Ext.Msg.YESNO,
                    icon : Ext.MessageBox.WARNING,
                    fn : Ext.bind(function (btn, text) {
                        if (btn == 'yes'){
                            this.completeCommitSettings(callback);
                        }
                    },this)
                });
            } else {
                this.completeCommitSettings(callback);
            }
        },
        completeCommitSettings : function(callback)
        {
            if (this.validate()) {
                this.saveSemaphore = 5;
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));

                var listAdministration=this.gridAdminAccounts.getFullSaveList();
                var setAdministration={};
                for(var i=0; i<listAdministration.length;i++) {
                    setAdministration[i]=listAdministration[i];
                }
                
                /* Add in the reporting only users */
                var storeDataSet=this.getAdminSettings().users.set;
                for ( var id in storeDataSet ) {
                    i++;

                    var user = storeDataSet[id];
                    if ( !user.hasWriteAccess ) {
                        /* So the settings always parse */
                        delete( user.password );
                        setAdministration[i] = user;
                    }
                }
                this.getAdminSettings().users.set=setAdministration;
                rpc.adminManager.setAdminSettings(Ext.bind(function(result, exception) {
                    this.afterSave(exception, callback);
                },this), this.getAdminSettings());

                rpc.networkManager.setAddressSettings(Ext.bind(function(result, exception) {
                    this.afterSave(exception, callback);
                 },this), this.getAddressSettings());

                rpc.adminManager.getSnmpManager().setSnmpSettings(Ext.bind(function(result, exception) {
                   this.afterSave(exception, callback);
                },this), this.getSnmpSettings());

                main.getLoggingManager().setLoggingSettings(Ext.bind(function(result, exception) {
                    this.afterSave(exception, callback);
                },this), this.getLoggingSettings());

                rpc.skinManager.setSkinSettings(Ext.bind(function(result, exception) {
                    this.afterSave(exception, callback);
                },this), this.getSkinSettings());

                this.afterSave(null,callback);
            }
        },
        afterSave : function(exception,callback)
        {
            if(Ung.Util.handleException(exception)) return;
            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
                // access settings should be saved last as saving these changes may disconnect the user from the Untangle box
                rpc.networkManager.setAccessSettings(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    this.finalizeSave(callback);
                },this), this.getAccessSettings());
            }
        },
        finalizeSave : function(callback)
        {
            this.needRefresh = this.initialSkin != this.getSkinSettings().administrationClientSkin;
            callback();
        },

        closeWindow : function() {
            Ung.Administration.superclass.closeWindow.call(this);
            if (this.needRefresh) {
                Ung.Util.goToStartPage();
            }
        },


        buildUserList : function(forceReload)
        {
            var storeData=[];
            var storeDataSet=this.getAdminSettings(forceReload).users.set;
            for(var id in storeDataSet) {
                var user = storeDataSet[id];
                
                if ( user.hasWriteAccess ) {
                    storeData.push(user);
                }
            }
            return storeData;
        },

        hasRemoteChanges : function()
        {
            i_accessSettings = this.initialAccessSettings;
            c_accessSettings = this.getAccessSettings();
            i_addressSettings = this.initialAddressSettings;
            
            //external administration
            if ( i_accessSettings.isOutsideAdministrationEnabled != c_accessSettings.isOutsideAdministrationEnabled ) {
                return true;
            }
            
            //internal administration
            if ( i_accessSettings.isInsideInsecureEnabled != c_accessSettings.isInsideInsecureEnabled ) {
                return true;
            }
            
            if ( c_accessSettings.isOutsideAccessRestricted 
                 && (i_accessSettings.outsideNetwork != Ext.getCmp('administration_outsideNetwork').getValue()
                     || i_accessSettings.outsideNetmask != Ext.getCmp('administration_outsideNetmask').getValue())) {
                return true;
            }
            
            if ( i_addressSettings.httpsPort != this.getAddressSettings().httpsPort ) {
                return true;
            }
            
            if ( !i_accessSettings.isOutsideAccessRestricted && c_accessSettings.isOutsideAccessRestricted ) {
                return true;
            }
            
            return false;
        }
    });

    // certificate generation window
    Ext.define("Ung.CertGenerateWindow", {
    	extend: "Ung.Window",
        initComponent : function() {
            var settingsCmp = Ext.getCmp(this.items.parentId);
            this.bbar= ['->',{
                name : 'Cancel',
                iconCls : 'cancel-icon',
                text : i18n._('Cancel'),
                handler : Ext.bind(function() {
                    this.cancelAction();
                },this)
            },{
                name : 'Proceed',
                iconCls : 'save-icon',
                text : settingsCmp.i18n._('Proceed'),
                handler : Ext.bind(function() {
                    this.proceedAction();
                },this)
            }];
            Ung.Window.prototype.initComponent.call(this);
        },
        // the proceed actions
        // to override
        proceedAction : function() {
            main.todo();
        }
    });
}
