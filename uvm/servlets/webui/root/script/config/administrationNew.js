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
        panelSnmp : null,
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
            this.initialSkin = this.getSkinSettings().skinName;
            this.buildAdministration();
            this.buildPublicAddress();
            this.buildCertificates();
            this.buildSnmp();
            this.buildSkins();

            // builds the tab panel with the tabs
            var adminTabs = [this.panelAdministration, this.panelPublicAddress, this.panelCertificates, this.panelSnmp, this.panelSkins];
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
                    this.rpc.adminSettings = rpc.adminManager.getSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }

            }
            return this.rpc.adminSettings;
        },
        // get system settings
        getSystemSettings : function(forceReload) {
            if (forceReload || this.rpc.systemSettings === undefined) {
                try {
                    this.rpc.systemSettings = rpc.systemManager.getSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
            }
            return this.rpc.systemSettings;
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
            // keep initial system and address settings
            this.initialSystemSettings = Ung.Util.clone(this.getSystemSettings());

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
                    height : 200,
                    bodyStyle : 'padding-bottom:30px;',
                    autoScroll : true,
                    hasEdit : false,
                    name : 'gridAdminAccounts',
                    recordJavaClass : "com.untangle.uvm.AdminUserSettings",
                    emptyRow : {
                        "username" : this.i18n._("[no username]"),
                        "description" : this.i18n._("[no description]"),
                        "emailAddress" : this.i18n._("[no email]"),
                        "password"       : null,
                        "passwordHashBase64"   : null
                    },
                    ignoreServerIds: true,
                    data : this.getAdminSettings().users.list,
                    // the list of fields; we need all as we get/set all records once
                    fields : [{
                        name : 'id'
                    }, {
                        name : 'username'
                    }, {
                        name : 'description'
                    }, {
                        name : 'emailAddress'
                    }, {
                        name : 'password'
                    }, {
                        name : 'passwordHashBase64'
                    }],
                    // the list of columns for the column model
                    columns : [{
                        header : this.i18n._("Username"),
                        width : 200,
                        dataIndex : 'username',
                        field :{
                            xtype:'textfield',
                            allowBlank : false,
                            blankText : this.i18n._("The username cannot be blank.")
                        }
                    }, {
                        header : this.i18n._("Description"),
                        width : 200,
                        dataIndex : 'description',
                        flex: 1,
                        editor:{
                            xtype:'textfield',
                            allowBlank : false
                        }
                    },{
                        header : this.i18n._("Email"),
                        width : 200,
                        dataIndex : 'emailAddress',
                        editor: {
                            xtype:'textfield',
                            allowBlank : false
                        }
                    }, changePasswordColumn
                    ],
                    sortField : 'username',
                    columnsDefaultSortable : true,
                    plugins : [changePasswordColumn],
                    // the row input lines used by the row editor window
                    rowEditorInputLines : [{
                        xtype: "textfield",
                        name : "Username",
                        dataIndex : "username",
                        fieldLabel : this.i18n._("Username"),
                        allowBlank : false,
                        blankText : this.i18n._("The username cannot be blank."),
                        width : 400
                    }, {
                        xtype: "textfield",
                        name : "Description",
                        dataIndex : "description",
                        fieldLabel : this.i18n._("Description"),
                        allowBlank : false,
                        width : 400
                    },{
                        xtype: "textfield",
                        name : "Email",
                        dataIndex : "emailAddress",
                        fieldLabel : this.i18n._("Email"),
                        width : 400
                    },{
                        xtype: "textfield",
                        inputType: 'password',
                        name : "Password",
                        dataIndex : "password",
                        id : 'administration_rowEditor_password_'+ fieldID,
                        fieldLabel : this.i18n._("Password"),
                        width : 400,
                        minLength : 3,
                        minLengthText : Ext.String.format(this.i18n._("The password is shorter than the minimum {0} characters."), 3)
                    },{
                        xtype: "textfield",
                        inputType: 'password',
                        name : "Confirm Password",
                        dataIndex : "password",
                        id : 'administration_rowEditor_confirm_password_'+ fieldID,
                        fieldLabel : this.i18n._("Confirm Password"),
                        width : 400
                    }]
                }), {
                    xtype : 'fieldset',
                    id:'external_administration_fieldset',
                    title : this.i18n._('Services Access'),
                    autoHeight : true,
                    labelWidth: 150,
                    items : [{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('HTTPS port'),
                        name : 'httpsPort',
                        id: 'administration_httpsPort',
                        value : this.getSystemSettings().httpsPort,
                        allowDecimals: false,
                        allowNegative: false,
                        allowBlank : false,
                        blankText : this.i18n._("You must provide a valid port."),
                        vtype : 'port',
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getSystemSettings().httpsPort = newValue;
                                },this)
                            }
                        }
                    }, {
                        xtype : 'checkbox',
                        name : 'Enable Outside HTTPS',
                        id : 'enable-wan-https',
                        boxLabel : this.i18n._('Enable Outside HTTPS'),
                        hideLabel : true,
                        checked : this.getSystemSettings().outsideHttpsEnabled,
                        listeners : {
                            "render" : {
                                fn : Ext.bind(function(elem) {
                                    if(elem.getValue()){
                                        Ext.getCmp('administration_outsideHttpsAdministrationEnabled').enable();
                                        Ext.getCmp('administration_outsideHttpsReportingEnabled').enable();
                                        Ext.getCmp('administration_outsideHttpsQuarantineEnabled').enable();
                                    }else{
                                        Ext.getCmp('administration_outsideHttpsAdministrationEnabled').disable();
                                        Ext.getCmp('administration_outsideHttpsReportingEnabled').disable();
                                        Ext.getCmp('administration_outsideHttpsQuarantineEnabled').disable();
                                    }
                                },this)
                            },
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getSystemSettings().outsideHttpsEnabled = newValue;
                                    if(newValue){
                                        Ext.getCmp('administration_outsideHttpsAdministrationEnabled').enable();
                                        Ext.getCmp('administration_outsideHttpsReportingEnabled').enable();
                                        Ext.getCmp('administration_outsideHttpsQuarantineEnabled').enable();
                                    }else{
                                        Ext.getCmp('administration_outsideHttpsAdministrationEnabled').disable();
                                        Ext.getCmp('administration_outsideHttpsReportingEnabled').disable();
                                        Ext.getCmp('administration_outsideHttpsQuarantineEnabled').disable();
                                    }
                                },this)
                            }
                        }
                    }, {
                        xtype : 'checkbox',
                        id : 'administration_outsideHttpsAdministrationEnabled',
                        name : 'outsideHttpsAdministrationEnabled',
                        boxLabel : this.i18n._('Enable Outside HTTPS Administration'),
                        hideLabel : true,
                        checked : this.getSystemSettings().outsideHttpsAdministrationEnabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getSystemSettings().outsideHttpsAdministrationEnabled = newValue;
                                },this)
                            }
                        }
                    },{
                        xtype : 'checkbox',
                        id : 'administration_outsideHttpsReportingEnabled',
                        name : 'outsideHttpsReportingEnabled',
                        boxLabel : this.i18n._('Enable Outside HTTPS Report Viewing'),
                        hideLabel : true,
                        checked : this.getSystemSettings().outsideHttpsReportingEnabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getSystemSettings().outsideHttpsReportingEnabled = newValue;
                                },this)
                            }
                        }
                    },{
                        xtype : 'checkbox',
                        id : 'administration_outsideHttpsQuarantineEnabled',
                        name : 'outsideHttpsQuarantineEnabled',
                        boxLabel : this.i18n._('Enable Outside HTTPS Quarantine Viewing'),
                        hideLabel : true,
                        checked : this.getSystemSettings().outsideHttpsQuarantineEnabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getSystemSettings().outsideHttpsQuarantineEnabled = newValue;
                                },this)
                            }
                        }
                    },{
                        xtype : 'checkbox',
                        id : 'administration_isInsideInsecureEnabled',
                        name : 'isInsideInsecureEnabled',
                        boxLabel : this.i18n._('Enable Inside HTTP Administration'),
                        hideLabel : true,
                        checked : this.getSystemSettings().isInsideInsecureEnabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getSystemSettings().isInsideInsecureEnabled = newValue;
                                },this)
                            }
                        }
                    },{
                        border: false,
                        cls: 'description',
                        html : this.i18n._('Note:') + "<br/>" +
                            this.i18n._('HTTP  Outside administration is always disabled.') + "<br/>" +
                            this.i18n._('HTTPS Inside administration is always enabled.') + "<br/>" +
                            this.i18n._('HTTP  Inside port is always open for block pages even when administration is disabled.') + "<br/>"
                    }]
                }]
            });
            this.gridAdminAccounts.rowEditorChangePass = Ext.create("Ung.RowEditorWindow",{
                grid : this.gridAdminAccounts,
                inputLines : [{
                    xtype: "textfield",
                    inputType: 'password',
                    name : "Password",
                    dataIndex : "password",
                    id : 'administration_rowEditor1_password_'+ fieldID,
                    fieldLabel : this.i18n._("Password"),
                    width : 400,
                    minLength : 3,
                    minLengthText : Ext.String.format(this.i18n._("The password is shorter than the minimum {0} characters."), 3)
                }, {
                    xtype: "textfield",
                    inputType: 'password',
                    name : "Confirm Password",
                    dataIndex : "password",
                    id : 'administration_rowEditor1_confirm_password_'+ fieldID,
                    fieldLabel : this.i18n._("Confirm Password"),
                    width : 400
                }],
                validate: Ext.bind(function(inputLines) {
                    //validate password match
                    var pwd = Ext.getCmp("administration_rowEditor1_password_" + fieldID);
                    var confirmPwd = Ext.getCmp("administration_rowEditor1_confirm_password_" + fieldID);
                    if(pwd.getValue() != confirmPwd.getValue()) {
                        pwd.markInvalid();
                        return this.i18n._('Passwords do not match');
                    } else {
                        return true;
                    }
                },this)
            });
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
                        html : Ext.String.format(this.i18n._('The Public Address is the address/URL that provides a public location for the {0} Server. This address will be used in emails sent by the {0} Server to link back to services hosted on the {0} Server such as Quarantine Digests and OpenVPN Client emails.'), main.getBrandingManager().getCompanyName()),
                        bodyStyle : 'padding-bottom:10px;',
                        border : false
                    },{
                        xtype : 'radio',
                        boxLabel : this.i18n._('Use External IP address (default)'),
                        hideLabel : true,
                        name : 'publicUrl',
                        checked : this.getSystemSettings().publicUrlMethod == "external",
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    if (checked) {
                                        this.getSystemSettings().publicUrlMethod = "external";
                                        Ext.getCmp('administration_publicUrlAddress').disable();
                                        Ext.getCmp('administration_publicUrlPort').disable();
                                    }
                                },this)
                            }
                        }
                    },{
                        cls: 'description',
                        html : Ext.String.format(this.i18n._('This works if your {0} Server has a routable public static IP address.'), main.getBrandingManager().getCompanyName()),
                        bodyStyle : 'padding:0px 5px 10px 25px;',
                        border : false
                    },{
                        xtype : 'radio',
                        boxLabel : this.i18n._('Use Hostname'),
                        hideLabel : true,
                        name : 'publicUrl',
                        checked : this.getSystemSettings().publicUrlMethod == "hostname",
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    if (checked) {
                                        this.getSystemSettings().publicUrlMethod = "hostname";
                                        Ext.getCmp('administration_publicUrlAddress').disable();
                                        Ext.getCmp('administration_publicUrlPort').disable();
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
                        boxLabel : this.i18n._('Use Manually Specified Address'),
                        hideLabel : true,
                        name : 'publicUrl',
                        checked : this.getSystemSettings().publicUrlMethod == "address_and_port",
                        listeners : {
                            "render" : {
                                fn : Ext.bind(function(elem) {
                                    if(elem.getValue()){
                                        Ext.getCmp('administration_publicUrlAddress').enable();
                                        Ext.getCmp('administration_publicUrlPort').enable();
                                    }else{
                                        Ext.getCmp('administration_publicUrlAddress').disable();
                                        Ext.getCmp('administration_publicUrlPort').disable();
                                    }
                                },this)
                            },
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    if (checked) {
                                        this.getSystemSettings().publicUrlMethod = "address_and_port";
                                        Ext.getCmp('administration_publicUrlAddress').enable();
                                        Ext.getCmp('administration_publicUrlPort').enable();
                                    }
                                },this)
                            }
                        }
                    },{
                        cls: 'description',
                        html : Ext.String.format(this.i18n._('This is recommended if the {0} Server is installed behind another firewall with a port forward from the specified hostname/IP that redirects traffic to the {0} Server.'),
                                                 main.getBrandingManager().getCompanyName()),
                        bodyStyle : 'padding:0px 5px 5px 25px;',
                        border : false
                    },{
                        xtype : 'panel',
                        bodyStyle : 'padding-left:25px;',
                        border : false,
                        items : [{
                            xtype : 'textfield',
                            fieldLabel : this.i18n._('IP/Hostname'),
                            name : 'publicUrlAddress',
                            id: 'administration_publicUrlAddress',
                            value : this.getSystemSettings().publicUrlAddress,
                            allowBlank : false,
                            blankText : this.i18n._("You must provide a valid IP Address or hostname."),
                            disabled : !this.getSystemSettings().publicUrlMethod == "address_and_port"
                        },{
                            xtype : 'numberfield',
                            fieldLabel : this.i18n._('Port'),
                            name : 'publicUrlPort',
                            id: 'administration_publicUrlPort',
                            value : this.getSystemSettings().publicUrlPort,
                            allowDecimals: false,
                            allowNegative: false,
                            allowBlank : false,
                            blankText : this.i18n._("You must provide a valid port."),
                            vtype : 'port',
                            disabled : !this.getSystemSettings().publicUrlMethod == "address_and_port"
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
                    
                    this.winGenerateSelfSignedCertificate.show();
                },

                onGenerateCertGenTrusted : function() {
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
                    this.winGenerateCertGenTrusted.show();
                },

                onCertImportTrusted : function() {
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

        buildSnmp : function() {
            this.panelSnmp = Ext.create('Ext.panel.Panel',{
                name : 'panelSnmp',
                helpSource : 'monitoring',
                // private fields
                parentId : this.getId(),
                title : this.i18n._('SNMP'),
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
                        checked : !this.getSystemSettings().snmpSettings.enabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    this.getSystemSettings().snmpSettings.enabled = !checked;
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
                        checked : this.getSystemSettings().snmpSettings.enabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    this.getSystemSettings().snmpSettings.enabled = checked;
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
                        value : this.getSystemSettings().snmpSettings.communityString == 'CHANGE_ME' ? this.i18n._('CHANGE_ME') : this.getSystemSettings().snmpSettings.communityString,
                        allowBlank : false,
                        blankText : this.i18n._("An SNMP \"Community\" must be specified."),
                        disabled : !this.getSystemSettings().snmpSettings.enabled
                    },{
                        xtype : 'textfield',
                        itemCls : 'left-indent-1',
                        fieldLabel : this.i18n._('System Contact'),
                        name : 'sysContact',
                        id: 'administration_snmp_sysContact',
                        value : this.getSystemSettings().snmpSettings.sysContact == 'MY_CONTACT_INFO' ? this.i18n._('MY_CONTACT_INFO') : this.getSystemSettings().snmpSettings.sysContact,
                        disabled : !this.getSystemSettings().snmpSettings.enabled
                        //vtype : 'email'
                    },{
                        xtype : 'textfield',
                        itemCls : 'left-indent-1',
                        fieldLabel : this.i18n._('System Location'),
                        name : 'sysLocation',
                        id: 'administration_snmp_sysLocation',
                        value : this.getSystemSettings().snmpSettings.sysLocation == 'MY_LOCATION' ? this.i18n._('MY_LOCATION') : this.getSystemSettings().snmpSettings.sysLocation,
                        disabled : !this.getSystemSettings().snmpSettings.enabled
                    },{
                        xtype : 'radio',
                        itemCls : 'left-indent-1',
                        boxLabel : Ext.String.format(this.i18n._('{0}Disable Traps{1} so no trap events are generated.  (This is the default setting.)'), '<b>', '</b>'),
                        hideLabel : true,
                        name : 'sendTraps',
                        id: 'administration_snmp_sendTraps_disable',
                        checked : !this.getSystemSettings().snmpSettings.sendTraps,
                        disabled : !this.getSystemSettings().snmpSettings.enabled,
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
                        checked : this.getSystemSettings().snmpSettings.sendTraps,
                        disabled : !this.getSystemSettings().snmpSettings.enabled,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, checked) {
                                    if (this.getSystemSettings().snmpSettings.enabled && checked) {
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
                        value : this.getSystemSettings().snmpSettings.trapCommunity == 'MY_TRAP_COMMUNITY' ? this.i18n._('MY_TRAP_COMMUNITY') : this.getSystemSettings().snmpSettings.trapCommunity,
                        allowBlank : false,
                        blankText : this.i18n._("An Trap \"Community\" must be specified."),
                        disabled : !this.getSystemSettings().snmpSettings.enabled || !this.getSystemSettings().snmpSettings.sendTraps
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Host'),
                        itemCls : 'left-indent-2',
                        name : 'trapHost',
                        id: 'administration_snmp_trapHost',
                        value : this.getSystemSettings().snmpSettings.trapHost == 'MY_TRAP_HOST' ? this.i18n._('MY_TRAP_HOST') : this.getSystemSettings().snmpSettings.trapHost,
                        allowBlank : false,
                        blankText : this.i18n._("An Trap \"Host\" must be specified."),
                        disabled : !this.getSystemSettings().snmpSettings.enabled || !this.getSystemSettings().snmpSettings.sendTraps
                    },{
                        xtype : 'numberfield',
                        itemCls : 'left-indent-2',
                        fieldLabel : this.i18n._('Port'),
                        name : 'trapPort',
                        id: 'administration_snmp_trapPort',
                        value : this.getSystemSettings().snmpSettings.trapPort,
                        allowDecimals: false,
                        allowNegative: false,
                        allowBlank : false,
                        blankText : this.i18n._("You must provide a valid port."),
                        vtype : 'port',
                        disabled : !this.getSystemSettings().snmpSettings.enabled || !this.getSystemSettings().snmpSettings.sendTraps
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
                        name : "skinName",
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
                                    this.getSkinSettings().skinName = record[0].data.name;
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
                        skinCombo.setValue(this.getSkinSettings().skinName);
                        skinCombo.clearDirty();
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
                this.validatePublicAddress() && this.validateSnmp();
        },

        //validate Admin Accounts
        validateAdminAccounts : function() {
            var listAdminAccounts = this.gridAdminAccounts.getFullSaveList();
            var oneWritableAccount = false;

            // verify that the username is not duplicated
            for(var i=0; i<listAdminAccounts.length;i++) {
                for(var j=i+1; j<listAdminAccounts.length;j++) {
                    if (listAdminAccounts[i].username == listAdminAccounts[j].username) {
                        Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._("The username name: \"{0}\" in row: {1}  already exists."), listAdminAccounts[j].username, j+1),
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

            var outsideAccessRestricted = this.getSystemSettings().outsideAccessRestricted;
            if (outsideAccessRestricted) {
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
                this.getSystemSettings().outsideNetwork = outsideNetworkCmp.getValue();
                this.getSystemSettings().outsideNetmask = outsideNetmaskCmp.getValue();
            }

            return true;
        },

        //validate Public Address
        validatePublicAddress : function() {
            if (this.getSystemSettings().publicUrlMethod == "address_and_port") {
                var publicUrlAddressCmp = Ext.getCmp('administration_publicUrlAddress');
                if (!publicUrlAddressCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("You must provide a valid IP Address or hostname."),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelPublicAddress);
                            publicUrlAddressCmp.focus(true);
                        },this)
                    );
                    return false;
                }
                var publicUrlPortCmp = Ext.getCmp('administration_publicUrlPort');
                if (!publicUrlPortCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelPublicAddress);
                            publicUrlPortCmp.focus(true);
                        },this)
                    );
                    return false;
                }
                //prepare for save
                this.getSystemSettings().publicUrlAddress = publicUrlAddressCmp.getValue();
                this.getSystemSettings().publicUrlPort = publicUrlPortCmp.getValue();
            }

            return true;
        },

        //validate SNMP
        validateSnmp : function() {
            var isSnmpEnabled = this.getSystemSettings().snmpSettings.enabled;
            if (isSnmpEnabled) {
                var snmpCommunityCmp = Ext.getCmp('administration_snmp_communityString');
                if (!snmpCommunityCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("An SNMP \"Community\" must be specified."),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelSnmp);
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
                                this.tabs.setActiveTab(this.panelSnmp);
                                snmpTrapCommunityCmp.focus(true);
                            },this)
                        );
                        return false;
                    }

                    snmpTrapHostCmp = Ext.getCmp('administration_snmp_trapHost');
                    if (!snmpTrapHostCmp.isValid()) {
                        Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("An Trap \"Host\" must be specified."),
                            Ext.bind(function () {
                                this.tabs.setActiveTab(this.panelSnmp);
                                snmpTrapHostCmp.focus(true);
                            },this)
                        );
                        return false;
                    }

                    snmpTrapPortCmp = Ext.getCmp('administration_snmp_trapPort');
                    if (!snmpTrapPortCmp.isValid()) {
                        Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),
                            Ext.bind(function () {
                                this.tabs.setActiveTab(this.panelSnmp);
                                snmpTrapPortCmp.focus(true);
                            },this)
                        );
                        return false;
                    }
                }

                //prepare for save
                var snmpSysContactCmp = Ext.getCmp('administration_snmp_sysContact');
                var snmpSysLocationCmp = Ext.getCmp('administration_snmp_sysLocation');

                this.getSystemSettings().snmpSettings.communityString = snmpCommunityCmp.getValue();
                this.getSystemSettings().snmpSettings.sysContact = snmpSysContactCmp.getValue();
                this.getSystemSettings().snmpSettings.sysLocation = snmpSysLocationCmp.getValue();
                this.getSystemSettings().snmpSettings.sendTraps = isTrapEnabled;
                if (isTrapEnabled) {
                    this.getSystemSettings().snmpSettings.trapCommunity = snmpTrapCommunityCmp.getValue();
                    this.getSystemSettings().snmpSettings.trapHost = snmpTrapHostCmp.getValue();
                    this.getSystemSettings().snmpSettings.trapPort = snmpTrapPortCmp.getValue();
                }
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

            this.initialSkinSettings = Ung.Util.clone( this.getSkinSettings(true) );
            this.gridAdminAccounts.store.loadData( this.getAdminSettings(true).users.list );
            this.initialSystemSettings = Ung.Util.clone(this.getSystemSettings(true));
            this.getCurrentServerCertInfo(true);
            this.getHostname(true);

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
            if (this.hasDangerousChanges()) {
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
                this.saveSemaphore = 2;
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));

                this.getAdminSettings().users.list=this.gridAdminAccounts.getFullSaveList();
                
                rpc.adminManager.setSettings(Ext.bind(function(result, exception) {
                    this.afterSave(exception, callback);
                },this), this.getAdminSettings());

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
                rpc.systemManager.setSettings(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    this.finalizeSave(callback);
                },this), this.getSystemSettings());
            }
        },
        finalizeSave : function(callback)
        {
            this.needRefresh = this.initialSkin != this.getSkinSettings().skinName;
            callback();
        },
        closeWindow : function() {
            Ung.Administration.superclass.closeWindow.call(this);
            if (this.needRefresh) {
                Ung.Util.goToStartPage();
            }
        },
        // tests for changes that might disconnect the UI from the server if saved
        hasDangerousChanges : function()
        {
            var i_systemSettings = this.initialSystemSettings;
            var c_systemSettings = this.getSystemSettings();
            
            //external administration
            if ( i_systemSettings.outsideHttpsAdministrationEnabled != c_systemSettings.outsideHttpsAdministrationEnabled ) {
                return true;
            }
            
            //internal administration
            if ( i_systemSettings.isInsideInsecureEnabled != c_systemSettings.isInsideInsecureEnabled ) {
                return true;
            }
            
            if ( i_systemSettings.httpsPort != this.getSystemSettings().httpsPort ) {
                return true;
            }
            
            if ( !i_systemSettings.outsideAccessRestricted && c_systemSettings.outsideAccessRestricted ) {
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
//@ sourceURL=administrationNew.js
