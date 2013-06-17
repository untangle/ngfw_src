// margin: 'top right bottom left'

if (!Ung.hasResource["Ung.Administration"]) {
    Ung.hasResource["Ung.Administration"] = true;

    Ext.namespace("Ung");
    Ext.namespace("Ung.Config");
    Ext.namespace("Ung.Config.Administration");

    Ext.define("Ung.Config.Administration.SkinManager", {
        constructor: function( config ) {
            /* List of stores to be refreshed, dynamically generated. */
            this.refreshList = [];
            this.i18n = config.i18n;
        },
        addRefreshableStore: function( store ) {
            this.refreshList.push( store );
        },
        uploadSkin: function( cmp, form ) {
            form.submit({
                parentId: cmp.getId(),
                waitMsg: this.i18n._('Please wait while your skin is uploaded...'),
                success: Ext.bind(this.uploadSkinSuccess, this ),
                failure: Ext.bind(this.uploadSkinFailure, this )
            });
        },
        uploadSkinSuccess: function( form, action ) {
            this.storeSemaphore = this.refreshList.length;

            var handler = Ext.bind(function() {
                this.storeSemaphore--;
                if (this.storeSemaphore == 0) {
                    Ext.MessageBox.alert( this.i18n._("Succeeded"), this.i18n._("Upload Skin Succeeded"));
                    var field = form.findField( "upload_skin_textfield" );
                    if ( field != null ) field.reset();
                }
            }, this);

            for ( var c = 0 ; c < this.storeSemaphore ; c++ ) this.refreshList[c].load({callback:handler});
        },
        uploadSkinFailure: function( form, action ) {
            var cmp = Ext.getCmp(action.parentId);
            var errorMsg = cmp.i18n._("Upload Skin Failed");
            if (action.result && action.result.msg) {
                switch (action.result.msg) {
                case 'Invalid Skin':
                    errorMsg = cmp.i18n._("Invalid Skin");
                    break;
                case 'The default skin can not be overwritten':
                    errorMsg = cmp.i18n._("The default skin can not be overwritten");
                    break;
                case 'Error creating skin folder':
                    errorMsg = cmp.i18n._("Error creating skin folder");
                    break;
                default:
                    errorMsg = cmp.i18n._("Upload Skin Failed");
                }
            }
            Ext.MessageBox.alert(cmp.i18n._("Failed"), errorMsg);
        }
    });

    Ext.define("Ung.Administration", {
        extend: "Ung.ConfigWin",
        panelAdministration: null,
        panelPublicAddress: null,
        panelCertificates: null,
        panelSnmp: null,
        panelSkins: null,
        uploadedCustomLogo: false,
        initComponent: function() {
            this.breadcrumbs = [{
                title: i18n._("Configuration"),
                action: Ext.bind(function() {
                    this.cancelAction();
                }, this)
            }, {
                title: i18n._('Administration')
            }];
            this.skinManager = Ext.create('Ung.Config.Administration.SkinManager',{ 'i18n':  i18n });
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
            this.callParent(arguments);
        },
        // get base settings object
        getSkinSettings: function(forceReload) {
            if (forceReload || this.rpc.skinSettings === undefined) {
                try {
                    this.rpc.skinSettings = rpc.skinManager.getSettings();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
            }
            return this.rpc.skinSettings;
        },
        // get admin settings
        getAdminSettings: function(forceReload) {
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
        getSystemSettings: function(forceReload) {
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
        getCertificateInformation: function(forceReload) {
            if (forceReload || this.rpc.currentServerCertInfo === undefined) {
                try {
                    this.rpc.currentServerCertInfo = main.getCertificateManager().getCertificateInformation();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }

            }
            return this.rpc.currentServerCertInfo;
        },
        // get hostname
        getHostname: function(forceReload) {
            if ( forceReload || this.rpc.hostname === undefined || this.rpc.domainName === undefined ) {
                try {
                    this.rpc.hostname = rpc.networkManager.getNetworkSettings()['hostName'];
                    this.rpc.domainName = rpc.networkManager.getNetworkSettings()['domainName'];
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }

            }
            if ( this.rpc.domainName !== null && this.rpc.domainName !== "" )
                return this.rpc.hostname + "." + this.rpc.domainName;
            else
                return this.rpc.hostname;
        },

        buildAdministration: function() {
            // keep initial system and address settings
            this.initialSystemSettings = Ung.Util.clone(this.getSystemSettings());

            var changePasswordColumn = Ext.create("Ung.grid.EditColumn",{
                header: this.i18n._("change password"),
                width: 130,
                iconClass: 'icon-edit-row',
                handler: function(view, rowIndex, colIndex) {
                    // populate row editor
                    var rec = view.getStore().getAt(rowIndex);
                    this.grid.rowEditorChangePass.populate(rec);
                    this.grid.rowEditorChangePass.show();
                }
            });

            this.gridAdminAccounts=Ext.create('Ung.EditorGrid', {
                anchor: "100% -260",
                settingsCmp: this,
                title: this.i18n._("Admin Accounts"),
                bodyStyle: 'padding-bottom:30px;',
                autoScroll: true,
                hasEdit: false,
                name: 'gridAdminAccounts',
                recordJavaClass: "com.untangle.uvm.AdminUserSettings",
                emptyRow: {
                    "username": this.i18n._("[no username]"),
                    "description": this.i18n._("[no description]"),
                    "emailAddress": this.i18n._("[no email]"),
                    "password": null,
                    "passwordHashBase64": null
                },
                data: this.getAdminSettings().users.list,
                paginated: false,
                // the list of fields; we need all as we get/set all records once
                fields: [{
                    name: 'username'
                }, {
                    name: 'description'
                }, {
                    name: 'emailAddress'
                }, {
                    name: 'password'
                }, {
                    name: 'passwordHashBase64'
                }],
                // the list of columns for the column model
                columns: [{
                    header: this.i18n._("Username"),
                    width: 200,
                    dataIndex: 'username',
                    field:{
                        xtype:'textfield',
                        allowBlank: false,
                        blankText: this.i18n._("The username cannot be blank.")
                    }
                }, {
                    header: this.i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex: 1,
                    editor:{
                        xtype:'textfield',
                        allowBlank: false
                    }
                },{
                    header: this.i18n._("Email"),
                    width: 200,
                    dataIndex: 'emailAddress',
                    editor: {
                        xtype:'textfield',
                        allowBlank: false
                    }
                }, changePasswordColumn
                         ],
                sortField: 'username',
                columnsDefaultSortable: true,
                plugins: [changePasswordColumn],
                // the row input lines used by the row editor window
                rowEditorInputLines: [{
                    xtype: "textfield",
                    name: "Username",
                    dataIndex: "username",
                    fieldLabel: this.i18n._("Username"),
                    allowBlank: false,
                    blankText: this.i18n._("The username cannot be blank."),
                    width: 400
                }, {
                    xtype: "textfield",
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    allowBlank: false,
                    width: 400
                },{
                    xtype: "textfield",
                    name: "Email",
                    dataIndex: "emailAddress",
                    fieldLabel: this.i18n._("Email"),
                    width: 400
                },{
                    xtype: "textfield",
                    inputType: 'password',
                    name: "password",
                    dataIndex: "password",
                    fieldLabel: this.i18n._("Password"),
                    width: 400,
                    minLength: 3,
                    minLengthText: Ext.String.format(this.i18n._("The password is shorter than the minimum {0} characters."), 3)
                },{
                    xtype: "textfield",
                    inputType: 'password',
                    name: "confirmPassword",
                    dataIndex: "password",
                    fieldLabel: this.i18n._("Confirm Password"),
                    width: 400
                }]
            });

            this.gridAdminAccounts.rowEditorChangePass = Ext.create("Ung.RowEditorWindow",{
                grid: this.gridAdminAccounts,
                inputLines: [{
                    xtype: "textfield",
                    inputType: 'password',
                    name: "password",
                    dataIndex: "password",
                    fieldLabel: this.i18n._("Password"),
                    width: 400,
                    minLength: 3,
                    minLengthText: Ext.String.format(this.i18n._("The password is shorter than the minimum {0} characters."), 3)
                }, {
                    xtype: "textfield",
                    inputType: 'password',
                    name: "confirmPassword",
                    dataIndex: "password",
                    fieldLabel: this.i18n._("Confirm Password"),
                    width: 400
                }],
                validate: Ext.bind(function(items) {
                    //validate password match
                    var pwd = this.gridAdminAccounts.rowEditorChangePass.query('textfield[name="password"]')[0];
                    var confirmPwd = this.gridAdminAccounts.rowEditorChangePass.query('textfield[name="confirmPassword"]')[0];
                    if(pwd.getValue() != confirmPwd.getValue()) {
                        pwd.markInvalid();
                        return this.i18n._('Passwords do not match');
                    } else {
                        return true;
                    }
                }, this)
            });

            this.gridAdminAccounts.subCmps.push(this.gridAdminAccounts.rowEditorChangePass);

            this.panelAdministration = Ext.create('Ext.panel.Panel',{
                name: 'panelAdministration',
                helpSource: 'administration',
                // private fields
                parentId: this.getId(),
                title: this.i18n._('Administration'),
                layout: "anchor",
                cls: 'ung-panel',
                items: [
                    this.gridAdminAccounts, {
                        xtype: 'checkbox',
                        fieldLabel: this.i18n._('Allow HTTP Administration'),
                        labelWidth: 200,
                        style: "margin-top: 10px",
                        checked: this.getSystemSettings().httpAdministrationAllowed,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getSystemSettings().httpAdministrationAllowed = newValue;
                                }, this)
                            }
                        }
                    }, {
                        xtype:'fieldset',
                        title: this.i18n._('Note:'),
                        items: [{
                            xtype: 'label',
                            html: this.i18n._('HTTP is open on non-WANs (internal interfaces) for blockpages and other services.') + "<br/>" +
                                this.i18n._('This settings only controls the availability of <b>administration</b> via HTTP.')
                        }]
                    }]
            });
        },

        buildPublicAddress: function() {
            this.panelPublicAddress = Ext.create('Ext.panel.Panel',{
                name: 'panelPublicAddress',
                helpSource: 'public_address',
                // private fields
                parentId: this.getId(),
                title: this.i18n._('Public Address'),
                cls: 'ung-panel',
                autoScroll: true,
                items: {
                    xtype: 'fieldset',
                    items: [{
                        cls: 'description',
                        html: Ext.String.format(this.i18n._('The Public Address is the address/URL that provides a public location for the {0} Server. This address will be used in emails sent by the {0} Server to link back to services hosted on the {0} Server such as Quarantine Digests and OpenVPN Client emails.'), main.getBrandingManager().getCompanyName()),
                        bodyStyle: 'padding-bottom:10px;',
                        border: false
                    },{
                        xtype: 'radio',
                        boxLabel: this.i18n._('Use IP address from External interface (default)'),
                        hideLabel: true,
                        name: 'publicUrl',
                        checked: this.getSystemSettings().publicUrlMethod == "external",
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    if (checked) {
                                        this.getSystemSettings().publicUrlMethod = "external";
                                        this.panelPublicAddress.query('textfield[name="publicUrlAddress"]')[0].disable();
                                        this.panelPublicAddress.query('numberfield[name="publicUrlPort"]')[0].disable();
                                    }
                                }, this)
                            }
                        }
                    },{
                        cls: 'description',
                        html: Ext.String.format(this.i18n._('This works if your {0} Server has a routable public static IP address.'), main.getBrandingManager().getCompanyName()),
                        bodyStyle: 'padding:0px 5px 10px 25px;',
                        border: false
                    },{
                        xtype: 'radio',
                        boxLabel: this.i18n._('Use Hostname'),
                        hideLabel: true,
                        name: 'publicUrl',
                        checked: this.getSystemSettings().publicUrlMethod == "hostname",
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    if (checked) {
                                        this.getSystemSettings().publicUrlMethod = "hostname";
                                        this.panelPublicAddress.query('textfield[name="publicUrlAddress"]')[0].disable();
                                        this.panelPublicAddress.query('numberfield[name="publicUrlPort"]')[0].disable();
                                    }
                                }, this)
                            }
                        }
                    },{
                        cls: 'description',
                        html: Ext.String.format(this.i18n._('This is recommended if the {0} Server\'s fully qualified domain name looks up to its IP address both internally and externally.'),
                                                 main.getBrandingManager().getCompanyName()),
                        bodyStyle: 'padding:0px 5px 5px 25px;',
                        border: false
                    }, {
                        cls: 'description',
                        html: Ext.String.format( this.i18n._( 'Current Hostname: {0}'), '<i>' + this.getHostname(true) + '</i>' ),
                        bodyStyle: 'padding:0px 5px 10px 25px;',
                        border: false
                    }, {
                        xtype: 'radio',
                        boxLabel: this.i18n._('Use Manually Specified Address'),
                        hideLabel: true,
                        name: 'publicUrl',
                        checked: this.getSystemSettings().publicUrlMethod == "address_and_port",
                        listeners: {
                            "afterrender": {
                                fn: Ext.bind(function(elem) {
                                    if(elem.getValue()) {
                                        this.panelPublicAddress.query('textfield[name="publicUrlAddress"]')[0].enable();
                                        this.panelPublicAddress.query('numberfield[name="publicUrlPort"]')[0].enable();
                                    } else {
                                        this.panelPublicAddress.query('textfield[name="publicUrlAddress"]')[0].disable();
                                        this.panelPublicAddress.query('numberfield[name="publicUrlPort"]')[0].disable();
                                    }
                                }, this)
                            },
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    if (checked) {
                                        this.getSystemSettings().publicUrlMethod = "address_and_port";
                                        this.panelPublicAddress.query('textfield[name="publicUrlAddress"]')[0].enable();
                                        this.panelPublicAddress.query('numberfield[name="publicUrlPort"]')[0].enable();
                                    }
                                }, this)
                            }
                        }
                    },{
                        cls: 'description',
                        html: Ext.String.format(this.i18n._('This is recommended if the {0} Server is installed behind another firewall with a port forward from the specified hostname/IP that redirects traffic to the {0} Server.'),
                                                 main.getBrandingManager().getCompanyName()),
                        bodyStyle: 'padding:0px 5px 5px 25px;',
                        border: false
                    },{
                        xtype: 'panel',
                        bodyStyle: 'padding-left:25px;',
                        border: false,
                        items: [{
                            xtype: 'textfield',
                            fieldLabel: this.i18n._('IP/Hostname'),
                            name: 'publicUrlAddress',
                            value: this.getSystemSettings().publicUrlAddress,
                            allowBlank: false,
                            width: 400,
                            blankText: this.i18n._("You must provide a valid IP Address or hostname."),
                            disabled: this.getSystemSettings().publicUrlMethod != "address_and_port"
                        },{
                            xtype: 'numberfield',
                            fieldLabel: this.i18n._('Port'),
                            name: 'publicUrlPort',
                            value: this.getSystemSettings().publicUrlPort,
                            allowDecimals: false,
                            minValue: 0,
                            allowBlank: false,
                            width: 210,
                            blankText: this.i18n._("You must provide a valid port."),
                            vtype: 'port',
                            disabled: this.getSystemSettings().publicUrlMethod != "address_and_port"
                        }]
                    }]
                }
            });
        },

        buildCertificates: function() {
            this.panelCertificates = Ext.create('Ext.panel.Panel',{
                name: 'panelCertificates',
                helpSource: 'certificates',
                // private fields
                parentId: this.getId(),
                winGenerateSelfSignedCertificate: null,
                winGenerateCertGenTrusted: null,
                winCertImportTrusted: null,

                title: this.i18n._('Certificates'),
                layout: "anchor",
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    anchor: '98%',
                    xtype: 'fieldset'
                },
                items: [{
                    title: this.i18n._('Internal Certificate Authority'),
                    defaults: { labelWidth: 150 },
                    items: [{
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('Valid starting'),
                        labelStyle: 'font-weight:bold',
                        id: 'rootca_status_notBefore',
                        value: this.getCertificateInformation() == null ? "" : i18n.timestampFormat(this.getCertificateInformation().rootcaDateValid),
                        disabled: true,
                        anchor:'100%'
                    },{
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('Valid until'),
                        labelStyle: 'font-weight:bold',
                        id: 'rootca_status_notAfter',
                        value: this.getCertificateInformation() == null ? "" : i18n.timestampFormat(this.getCertificateInformation().rootcaDateExpires),
                        disabled: true,
                        anchor:'100%'
                    },{
                        xtype: 'textarea',
                        fieldLabel: this.i18n._('Subject DN'),
                        labelStyle: 'font-weight:bold',
                        id: 'rootca_status_subjectDN',
                        value: this.getCertificateInformation() == null ? "" : this.getCertificateInformation().rootcaSubject,
                        disabled: true,
                        anchor:'100%',
                        height: 40
                    },{
                        xtype: 'displayfield',
                        margin: '10 0 10 0',
                        value:  'The Internal Certificate Authority is used to create and sign the HTTPS certificates used by the internal web ' +
                                'server as well as certificates used by other applications and services such as HTTPS Inspector and Captive Portal. ' +
                                'To eliminate certificate security warnings on client computers and devices, you should download the root certificate ' +
                                'and add it to the list of trusted authorities on each client conneced to your network.'
                    },{
                        xtype: 'fieldset',
                        layout: 'column',
                        items: [{
                            xtype: 'button',
                            margin: '0 10 0 10',
                            minWidth: 200,
                            text: 'Download Root Certificate',
                            iconCls: 'action-icon',
                            handler: Ext.bind(function() {
                                var downloadForm = document.getElementById('downloadForm');
                                downloadForm["type"].value = "root_download"
                                downloadForm.submit();
                                }, this)
                        },{
                            xtype: 'displayfield',
                            margin: '0 10 0 10',
                            columnWidth: 1,
                            height: 30,
                            value: 'Click here to download the root certificate.  Installing this certificate on client devices will allow them to trust certificates generated by this server.'
                        }]
                    },{
                        xtype: 'fieldset',
                        layout: 'column',
                        items: [{
                            xtype: 'button',
                            margin: '0 10 0 10',
                            minWidth: 200,
                            text: 'Generate Certificate Authority',
                            iconCls: 'action-icon',
                            handler: Ext.bind(function() {
                                this.certGeneratorPopup("ROOT", null, 'Generate Certificate Authority');
                            }, this)
                        },{
                            xtype: 'displayfield',
                            margin: '0 10 0 10',
                            columnWidth: 1,
                            height: 30,
                            value: 'Click here to re-create the internal certificate authority.  Use this to change the information in the Subject DN of the root certificate.'
                        }]
                    },{
                        xtype: 'fieldset',
                        layout: 'column',
                        items: [{
                            xtype: 'button',
                            margin: '0 10 0 10',
                            minWidth: 200,
                            text: 'Generate Server Certificate',
                            iconCls: 'action-icon',
                            handler: Ext.bind(function() {
                                this.certGeneratorPopup("SERVER", this.getHostname(true), 'Generate Server Certificate');
                            }, this)
                        },{
                            xtype: 'displayfield',
                            margin: '0 10 0 10',
                            columnWidth: 1,
                            height: 30,
                            value: 'Click here to re-create the server certificate.  Use this to change the information in the Subject DN of the internal web server certificate.'
                        }]
                    }]
                },{
                    title: this.i18n._('Server Certificate'),
                    defaults: { labelWidth: 150 },
                    items: [{
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('Valid starting'),
                        labelStyle: 'font-weight:bold',
                        id: 'server_status_notBefore',
                        value: this.getCertificateInformation() == null ? "" : i18n.timestampFormat(this.getCertificateInformation().serverDateValid),
                        disabled: true,
                        anchor:'100%'
                    },{
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('Valid until'),
                        labelStyle: 'font-weight:bold',
                        id: 'server_status_notAfter',
                        value: this.getCertificateInformation() == null ? "" : i18n.timestampFormat(this.getCertificateInformation().serverDateExpires),
                        disabled: true,
                        anchor:'100%'
                    },{
                        xtype: 'textarea',
                        fieldLabel: this.i18n._('Issuer DN'),
                        labelStyle: 'font-weight:bold',
                        id: 'server_status_issuerDN',
                        value: this.getCertificateInformation() == null ? "" : this.getCertificateInformation().serverIssuer,
                        disabled: true,
                        anchor:'100%',
                        height: 40
                    },{
                        xtype: 'textarea',
                        fieldLabel: this.i18n._('Subject DN'),
                        labelStyle: 'font-weight:bold',
                        id: 'server_status_subjectDN',
                        value: this.getCertificateInformation() == null ? "" : this.getCertificateInformation().serverSubject,
                        disabled: true,
                        anchor:'100%',
                        height: 40
                    }]
                },{
                    title: this.i18n._('External Certificate Authority'),
                    defaults: {
                        xtype: 'fieldset',
                        layout: 'column'
                    },
                    items: [{
                            cls: 'description',
                            html: this.i18n._('You must complete each of these steps in order every time you import a new signed certificate!'),
                            bodyStyle: 'padding-bottom:10px;',
                            border: false
                        },{
                        items: [{
                            border: false,
                            width: 30,
                            html:'<div class="step_counter">1</div>'
                        },{
                            border: false,
                            width: 270,
                            items: [{
                                xtype: 'button',
                                text: Ext.String.format(this.i18n._('Generate a {0}CSR{1}'), '<b>', '</b>'),
                                minWidth: 250,
                                name: 'Generate a Self-Signed Certificate',
                                iconCls: 'action-icon',
                                handler: Ext.bind(function() {
                                    this.onGenerateCertGenTrusted() ;
                                }, this)
                            }]
                        },{
                            border: false,
                            columnWidth: 1,
                            items: [{
                                cls: 'description',
                                html: this.i18n._("Click this button to generate a Certificate Signature Request (CSR), which you can then copy and paste for use by certificate authorities such as Thawte, Verisign, etc."),
                                border: false
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
                                xtype: 'button',
                                text: Ext.String.format(this.i18n._('Import a {0}Signed Certificate{1}'), '<b>', '</b>'),
                                minWidth: 250,
                                name: 'Generate a Self-Signed Certificate',
                                iconCls: 'action-icon',
                                handler: Ext.bind(function() {
                                    this.onCertImportTrusted();
                                }, this)
                            }]
                        },{
                            border: false,
                            columnWidth: 1,
                            items: [{
                                cls: 'description',
                                html: Ext.String.format(this.i18n._("Click this button to import a signed certificate which has been generated by a certificate authority, and was based on a previous signature request from {0}."), main.getBrandingManager().getCompanyName()),
                                border: false
                            }]
                        }]
                    }]
                }]
            });
        },

        certGeneratorPopup: function(certMode, hostName, titleText)
        {
            popup = new Ext.Window({
                title: titleText,
                layout: 'fit',
                width: 600,
                height: 400,
                border: true,
                xtype: 'form',
                items: [{
                    xtype: "form",
                    id: "cert_info_form",
                    border: false,
                    items: [{
                        xtype: 'combo',
                        fieldLabel: this.i18n._('Country') + " (C)",
                        labelWidth: 150,
                        name: 'Country',
                        id: 'Country',
                        margin: "10 10 10 10",
                        size: 50,
                        allowBlank: true,
                        store: Ung.Country.getCountryStore(i18n),
                        queryMode: 'local',
                        editable: false
                    },{
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('State') + " (ST)",
                        labelWidth: 150,
                        name: "State",
                        id: "State",
                        margin: "10 10 10 10",
                        size: 200,
                        allowBlank: true
                    },{
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('Locality') + " (L)",
                        labelWidth: 150,
                        name: "Locality",
                        id: "Locality",
                        margin: "10 10 10 10",
                        size: 200,
                        allowBlank: true
                    },{
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('Organization') + " (O)",
                        labelWidth: 150,
                        name: "Organization",
                        id: "Organization",
                        margin: "10 10 10 10",
                        size: 200,
                        allowBlank: true
                    },{
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('Organaizationl Unit') + " (OU)",
                        labelWidth: 150,
                        name: "OrganizationalUnit",
                        id: "OrganizationalUnit",
                        margin: "10 10 10 10",
                        size: 200,
                        allowBlank: true
                    },{
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('Common Name') + " (CN)",
                        labelWidth: 150,
                        name: "CommonName",
                        id: "CommonName",
                        margin: "10 10 10 10",
                        size: 200,
                        allowBlank: false,
                        disabled: (hostName === null ? false : true),
                        value: hostName
                    },{
                        xtype: "button",
                        text: this.i18n._("Accept"),
                        name: "Accept",
                        width: 100,
                        margin: "10 10 10 180",
                        handler: Ext.bind(function() {
                            this.certGeneratorWorker(certMode);
                        }, this)
                    },{
                        xtype: "button",
                        text: this.i18n._("Cancel"),
                        name: "Cancel",
                        width: 100,
                        margin: "10 10 10 10",
                        handler: Ext.bind(function() {
                            popup.close()
                        }, this)
                    }]
                }]
            });

            popup.show();
        },

        certGeneratorWorker: function(certMode)
        {
            var form_C = Ext.getCmp('Country');
            var form_ST = Ext.getCmp('State');
            var form_L = Ext.getCmp('Locality');
            var form_O = Ext.getCmp('Organization');
            var form_OU = Ext.getCmp('OrganizationalUnit');
            var form_CN = Ext.getCmp('CommonName');

            if (form_CN.getValue().length == 0)
            {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._('The Common Name field must not be empty'));
                return;
            }

            var certSubject = ("/CN=" + form_CN.getValue());
            if ((form_C.getValue()) && (form_C.getValue().length > 0)) certSubject += ("/C=" + form_C.getValue());
            if ((form_ST.getValue()) && (form_ST.getValue().length > 0)) certSubject += ("/ST=" + form_ST.getValue());
            if ((form_L.getValue()) && (form_L.getValue().length > 0)) certSubject += ("/L=" + form_L.getValue());
            if ((form_O.getValue()) && (form_O.getValue().length > 0)) certSubject += ("/O=" + form_O.getValue());
            if ((form_OU.getValue()) && (form_OU.getValue().length > 0)) certSubject += ("/OU=" + form_OU.getValue());

            if (certMode === "ROOT") certFunction = main.getCertificateManager().generateCertificateAuthority;
            if (certMode == "SERVER") certFunction = main.getCertificateManager().generateServerCertificate;

            certFunction(Ext.bind(function(result, exception)
            {
                popup.close();

                if(Ung.Util.handleException(exception)) return;

                if (result)
                {
                    this.updateCertificatesStatus();
                    Ext.MessageBox.alert(this.i18n._("Success"), this.i18n._("Certificate generation successfully completed"));
                }
                else
                {
                    Ext.MessageBox.alert(i18n._("Failure"), this.i18n._("Error during certificate generation"));
                }
            }, this), certSubject);
        },

        // Generate Signature Request
        onGenerateCertGenTrusted: function() {
            if(!this.winGenerateCertGenTrusted) {
                this.winGenerateCertGenTrusted = Ext.create('Ung.CertGenerateWindow', {
                    settingsCmp: this,
                    breadcrumbs: [{
                        title: i18n._("Configuration"),
                        action: Ext.bind(function() {
                            this.winGenerateCertGenTrusted.cancelAction();
                            this.cancelAction();
                        }, this)
                    }, {
                        title: i18n._('Administration'),
                        action: Ext.bind(function() {
                            this.winGenerateCertGenTrusted.cancelAction();
                        }, this)
                    }, {
                        title: i18n._('Certificates'),
                        action: Ext.bind(function() {
                            this.winGenerateCertGenTrusted.cancelAction();
                        }, this)
                    }, {
                        title: this.i18n._("Generate a Certificate Signature Request")
                    }],
                    items: {
                        xtype: 'panel',
                        name: 'panelGenerateCertGenTrusted',
                        layout: "anchor",
                        title: this.i18n._('Generate a Certificate Signature Request'),
                        cls: 'ung-panel',
                        autoScroll: true,
                        items: [{
                            cls: 'description',
                            html: this.i18n._('Click the Proceed button to generate a signature below. Copy the signature (Control-C), and paste it into the necessary form from your Certificate Authority (Verisign, Thawte, etc.).'),
                            bodyStyle: 'padding-bottom:10px;',
                            border: false
                        },{
                            xtype: 'textarea',
                            name: 'crs',
                            id: 'administration_crs',
                            anchor:'98%',
                            height: 200,
                            hideLabel: true
                        }]
                    },
                    proceedAction: Ext.bind(function() {
                        Ext.MessageBox.wait(this.i18n._("Generating Certificate..."), i18n._("Please wait"));

                        // generate certificate request
                        main.getCertificateManager().generateCSR(Ext.bind(function(result, exception) {
                            if(Ung.Util.handleException(exception)) return;
                            if (result != null) {
                                //success
                                Ext.MessageBox.alert(this.i18n._("Succeeded"), this.i18n._("Certificate Signature Request Successfully Generated"),
                                    Ext.bind(function () {
                                        var crsCmp = Ext.getCmp('administration_crs');
                                        crsCmp.setValue(result);
                                        crsCmp.focus(true);
                                    }, this)
                                );
                            } else {
                                //failed
                                Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("Error generating certificate signature request"));
                                return;
                            }
                        }, this));

                    }, this),
                    cancelAction: function() {
                        Ext.getCmp('administration_crs').reset();
                        this.hide();
                    }
                });
                this.subCmps.push(this.winGenerateCertGenTrusted);
            }
            this.winGenerateCertGenTrusted.show();
        },

        // Import Signed Certificate
        onCertImportTrusted: function() {
            if(!this.winCertImportTrusted) {
                this.winCertImportTrusted = Ext.create('Ung.CertGenerateWindow', {
                    settingsCmp: this,
                    breadcrumbs: [{
                        title: i18n._("Configuration"),
                        action: Ext.bind(function() {
                            this.winCertImportTrusted.cancelAction();
                            this.cancelAction();
                        }, this)
                    }, {
                        title: i18n._('Administration'),
                        action: Ext.bind(function() {
                            this.winCertImportTrusted.cancelAction();
                        }, this)
                    }, {
                        title: i18n._('Certificates'),
                        action: Ext.bind(function() {
                            this.winCertImportTrusted.cancelAction();
                        }, this)
                    }, {
                        title: this.i18n._("Import Signed Certificate")
                    }],
                    items: {
                        name: 'panelCertImportTrusted',
                        // private fields
                        layout: "anchor",
                        title: this.i18n._('Import Signed Certificate'),
                        cls: 'ung-panel',
                        autoScroll: true,
                            items: [{
                                cls: 'description',
                                html: this.i18n._('When your Certificate Authority (Verisign, Thawte, etc.) has sent your Signed Certificate, copy and paste it below (Control-V) then press the Proceed button.'),
                                bodyStyle: 'padding-bottom:10px;',
                                border: false
                            },{
                                xtype: 'textarea',
                                name: 'cert',
                                id: 'administration_import_cert',
                                anchor:'98%',
                                height: 200,
                                hideLabel: true
                            },{
                                cls: 'description',
                                html: this.i18n._('If your Certificate Authority (Verisign, Thawte, etc.) also send you an Intermediate Certificate, paste it below. Otherwise, do not paste anything below.'),
                                bodyStyle: 'padding:20px 0px 10px 0px;',
                                border: false
                            },{
                                xtype: 'textarea',
                                name: 'caCert',
                                id: 'administration_import_caCert',
                                anchor:'98%',
                                height: 200,
                                hideLabel: true
                            }]
                    },
                    proceedAction: Ext.bind(function() {
                        Ext.MessageBox.wait(this.i18n._("Importing Certificate..."), i18n._("Please wait"));

                        //get user values
                        var cert = Ext.getCmp('administration_import_cert').getValue();
                        var caCert = Ext.getCmp('administration_import_caCert').getValue();

                        // import certificate
                        main.getCertificateManager().importServerCert(Ext.bind(function(result, exception) {
                            if(Ung.Util.handleException(exception)) return;
                            if (result) { //true or false
                                //success

                                //update status
                                this.updateCertificatesStatus();

                                Ext.MessageBox.alert(this.i18n._("Succeeded"), this.i18n._("Certificate Successfully Imported"),
                                    Ext.bind(function () {
                                        this.winCertImportTrusted.cancelAction();
                                    }, this)
                                );
                            } else {
                                //failed
                                Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("Error importing certificate"));
                                return;
                            }
                        }, this), cert, caCert.length==0?null:caCert );

                    }, this),
                    cancelAction: function() {
                        Ext.getCmp('administration_import_cert').reset();
                        Ext.getCmp('administration_import_caCert').reset();
                        this.hide();
                    }
                });
                this.subCmps.push(this.winCertImportTrusted);
            }
            this.winCertImportTrusted.show();
        },

        updateCertificatesStatus: function() {
            var certInfo = this.getCertificateInformation(true);
            if (certInfo != null) {
                Ext.getCmp('rootca_status_notBefore').setValue(i18n.timestampFormat(certInfo.rootcaDateValid));
                Ext.getCmp('rootca_status_notAfter').setValue(i18n.timestampFormat(certInfo.rootcaDateExpires));
                Ext.getCmp('rootca_status_subjectDN').setValue(certInfo.rootcaSubject);

                Ext.getCmp('server_status_notBefore').setValue(i18n.timestampFormat(certInfo.serverDateValid));
                Ext.getCmp('server_status_notAfter').setValue(i18n.timestampFormat(certInfo.serverDateExpires));
                Ext.getCmp('server_status_subjectDN').setValue(certInfo.serverSubject);
                Ext.getCmp('server_status_issuerDN').setValue(certInfo.serverIssuer);
            }
        },

        buildSnmp: function() {
            this.panelSnmp = Ext.create('Ext.panel.Panel',{
                name: 'panelSnmp',
                helpSource: 'monitoring',
                // private fields
                parentId: this.getId(),
                title: this.i18n._('SNMP'),
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    xtype: 'fieldset'
                },
                items: [{
                    title: this.i18n._('SNMP'),
                    defaults: {
                        labelWidth: 150
                    },
                    items: [{
                        xtype: 'radio',
                        boxLabel: Ext.String.format(this.i18n._('{0}Disable{1} SNMP Monitoring. (This is the default setting.)'), '<b>', '</b>'),
                        hideLabel: true,
                        name: 'snmpEnabled',
                        checked: !this.getSystemSettings().snmpSettings.enabled,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
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
                                }, this)
                            }
                        }
                    },{
                        xtype: 'radio',
                        boxLabel: Ext.String.format(this.i18n._('{0}Enable{1} SNMP Monitoring.'), '<b>', '</b>'),
                        hideLabel: true,
                        name: 'snmpEnabled',
                        checked: this.getSystemSettings().snmpSettings.enabled,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    this.getSystemSettings().snmpSettings.enabled = checked;
                                    if (checked) {
                                        Ext.getCmp('administration_snmp_communityString').enable();
                                        Ext.getCmp('administration_snmp_sysContact').enable();
                                        Ext.getCmp('administration_snmp_sysLocation').enable();
                                        Ext.getCmp('administration_snmp_sendTraps_disable').enable();
                                        var sendTrapsEnableCmp = null;
                                        (sendTrapsEnableCmp = Ext.getCmp('administration_snmp_sendTraps_enable')).enable();
                                        if (sendTrapsEnableCmp.getValue()) {
                                            Ext.getCmp('administration_snmp_trapCommunity').enable();
                                            Ext.getCmp('administration_snmp_trapHost').enable();
                                            Ext.getCmp('administration_snmp_trapPort').enable();
                                        }
                                    }
                                }, this)
                            }
                        }
                    },{
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('Community'),
                        name: 'communityString',
                        itemCls: 'left-indent-1',
                        id: 'administration_snmp_communityString',
                        value: this.getSystemSettings().snmpSettings.communityString == 'CHANGE_ME' ? this.i18n._('CHANGE_ME'): this.getSystemSettings().snmpSettings.communityString,
                        allowBlank: false,
                        blankText: this.i18n._("An SNMP \"Community\" must be specified."),
                        disabled: !this.getSystemSettings().snmpSettings.enabled
                    },{
                        xtype: 'textfield',
                        itemCls: 'left-indent-1',
                        fieldLabel: this.i18n._('System Contact'),
                        name: 'sysContact',
                        id: 'administration_snmp_sysContact',
                        value: this.getSystemSettings().snmpSettings.sysContact == 'MY_CONTACT_INFO' ? this.i18n._('MY_CONTACT_INFO'): this.getSystemSettings().snmpSettings.sysContact,
                        disabled: !this.getSystemSettings().snmpSettings.enabled
                        //vtype: 'email'
                    },{
                        xtype: 'textfield',
                        itemCls: 'left-indent-1',
                        fieldLabel: this.i18n._('System Location'),
                        name: 'sysLocation',
                        id: 'administration_snmp_sysLocation',
                        value: this.getSystemSettings().snmpSettings.sysLocation == 'MY_LOCATION' ? this.i18n._('MY_LOCATION'): this.getSystemSettings().snmpSettings.sysLocation,
                        disabled: !this.getSystemSettings().snmpSettings.enabled
                    },{
                        xtype: 'radio',
                        itemCls: 'left-indent-1',
                        boxLabel: Ext.String.format(this.i18n._('{0}Disable Traps{1} so no trap events are generated.  (This is the default setting.)'), '<b>', '</b>'),
                        hideLabel: true,
                        name: 'sendTraps',
                        id: 'administration_snmp_sendTraps_disable',
                        checked: !this.getSystemSettings().snmpSettings.sendTraps,
                        disabled: !this.getSystemSettings().snmpSettings.enabled,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    if (checked) {
                                        Ext.getCmp('administration_snmp_trapCommunity').disable();
                                        Ext.getCmp('administration_snmp_trapHost').disable();
                                        Ext.getCmp('administration_snmp_trapPort').disable();
                                    }
                                }, this)
                            }
                        }
                    },{
                        xtype: 'radio',
                        itemCls: 'left-indent-1',
                        boxLabel: Ext.String.format(this.i18n._('{0}Enable Traps{1} so trap events are sent when they are generated.'), '<b>', '</b>'),
                        hideLabel: true,
                        name: 'sendTraps',
                        id: 'administration_snmp_sendTraps_enable',
                        checked: this.getSystemSettings().snmpSettings.sendTraps,
                        disabled: !this.getSystemSettings().snmpSettings.enabled,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, checked) {
                                    if (this.getSystemSettings().snmpSettings.enabled && checked) {
                                        Ext.getCmp('administration_snmp_trapCommunity').enable();
                                        Ext.getCmp('administration_snmp_trapHost').enable();
                                        Ext.getCmp('administration_snmp_trapPort').enable();
                                    }
                                }, this)
                            }
                        }
                    },{
                        xtype: 'textfield',
                        itemCls: 'left-indent-2',
                        fieldLabel: this.i18n._('Community'),
                        name: 'trapCommunity',
                        id: 'administration_snmp_trapCommunity',
                        value: this.getSystemSettings().snmpSettings.trapCommunity == 'MY_TRAP_COMMUNITY' ? this.i18n._('MY_TRAP_COMMUNITY'): this.getSystemSettings().snmpSettings.trapCommunity,
                        allowBlank: false,
                        blankText: this.i18n._("An Trap \"Community\" must be specified."),
                        disabled: !this.getSystemSettings().snmpSettings.enabled || !this.getSystemSettings().snmpSettings.sendTraps
                    },{
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('Host'),
                        itemCls: 'left-indent-2',
                        name: 'trapHost',
                        id: 'administration_snmp_trapHost',
                        value: this.getSystemSettings().snmpSettings.trapHost == 'MY_TRAP_HOST' ? this.i18n._('MY_TRAP_HOST'): this.getSystemSettings().snmpSettings.trapHost,
                        allowBlank: false,
                        blankText: this.i18n._("An Trap \"Host\" must be specified."),
                        disabled: !this.getSystemSettings().snmpSettings.enabled || !this.getSystemSettings().snmpSettings.sendTraps
                    },{
                        xtype: 'numberfield',
                        itemCls: 'left-indent-2',
                        fieldLabel: this.i18n._('Port'),
                        name: 'trapPort',
                        id: 'administration_snmp_trapPort',
                        value: this.getSystemSettings().snmpSettings.trapPort,
                        allowDecimals: false,
                        minValue: 0,
                        allowBlank: false,
                        blankText: this.i18n._("You must provide a valid port."),
                        vtype: 'port',
                        disabled: !this.getSystemSettings().snmpSettings.enabled || !this.getSystemSettings().snmpSettings.sendTraps
                    }]
                }]
            });
        },
        buildSkins: function() {
            // keep initial skin settings
            var adminSkinsStore = Ext.create("Ext.data.Store",{
                fields: [{
                    name: 'name'
                },{
                    name: 'displayName',
                    convert: Ext.bind(function(v) {
                        if ( v == "Default" ) return this.i18n._("Default");
                        return v;
                    }, this)
                }],
                proxy: Ext.create("Ext.data.proxy.Server",{
                    doRequest: function(operation, callback, scope) {
                        rpc.skinManager.getSkinsList(Ext.bind(function(result, exception) {
                            if(Ung.Util.handleException(exception)) return;
                            this.processResponse(exception==null, operation, null, result, callback, scope);
                        }, this));
                    },
                    reader: {
                        type: 'json',
                        root: 'list'
                    }
                })
            });

            this.skinManager.addRefreshableStore( adminSkinsStore );

            this.panelSkins = Ext.create('Ext.panel.Panel',{
                name: "panelSkins",
                helpSource: 'skins',
                // private fields
                parentId: this.getId(),
                title: this.i18n._('Skins'),
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    xtype: 'fieldset',
                    buttonAlign: 'left'
                },
                items: [{
                    title: this.i18n._('Administration Skin'),
                    items: [{
                        cls: 'description',
                        border: false,
                        html: this.i18n._("This skin will used in the administration client")
                    }, {
                        xtype: 'combo',
                        name: "skinName",
                        id: "administration_admin_client_skin_combo",
                        store: adminSkinsStore,
                        displayField: 'displayName',
                        valueField: 'name',
                        forceSelection: true,
                        editable: false,
                        queryMode: 'local',
                        selectOnFocus: true,
                        hideLabel: true,
                        width: 300,
                        listeners: {
                            "select": {
                                fn: Ext.bind(function(elem, record) {
                                    this.getSkinSettings().skinName = record[0].data.name;
                                }, this)
                            }
                        }
                    }]
                },{
                    title: this.i18n._('Upload New Skin'),
                    items: {
                        xtype: 'form',
                        id: 'upload_skin_form',
                        url: 'upload',
                        border: false,
                        items: [{
                            xtype: 'filefield',
                            fieldLabel: this.i18n._('File'),
                            name: 'upload_skin_textfield',
                            width: 500,
                            size: 50,
                            labelWidth: 50,
                            allowBlank: false
                        },{
                            xtype: 'button',
                            text: this.i18n._("Upload"),
                            handler: Ext.bind(function() {
                                this.panelSkins.onUpload();
                            }, this)
                        },{
                            xtype: 'hidden',
                            name: 'type',
                            value: 'skin'
                        }]
                    }
                }],
                onUpload: function() {
                    var prova = Ext.getCmp('upload_skin_form');
                    var cmp = Ext.getCmp(this.parentId);
                    var form = prova.getForm();

                    cmp.skinManager.uploadSkin( cmp, form );
                }
            });
            adminSkinsStore.load({
                callback: Ext.bind(function() {
                    var skinCombo=Ext.getCmp('administration_admin_client_skin_combo');
                    if(skinCombo!=null) {
                        skinCombo.setValue(this.getSkinSettings().skinName);
                        skinCombo.clearDirty();
                    }
                }, this)
            });
        },

        isBlankField: function (cmp, errMsg) {
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
        validate: function() {
            return  this.validateAdminAccounts() && this.validatePublicAddress() && this.validateSnmp();
        },

        //validate Admin Accounts
        validateAdminAccounts: function() {
            var listAdminAccounts = this.gridAdminAccounts.getPageList();
            var oneWritableAccount = false;

            // verify that the username is not duplicated
            for(var i=0; i<listAdminAccounts.length;i++) {
                for(var j=i+1; j<listAdminAccounts.length;j++) {
                    if (listAdminAccounts[i].username == listAdminAccounts[j].username) {
                        Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._("The username name: \"{0}\" in row: {1}  already exists."), listAdminAccounts[j].username, j+1),
                            Ext.bind(function () {
                                this.tabs.setActiveTab(this.panelAdministration);
                            }, this)
                        );
                        return false;
                    }
                }

                if (!listAdminAccounts[i].readOnly) {
                    oneWritableAccount = true;
                }

            }

            // verify that there is at least one valid entry after all operations
            if(listAdminAccounts.length <= 0 ) {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("There must always be at least one valid account."),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelAdministration);
                    }, this)
                );
                return false;
            }

            // verify that there was at least one non-read-only account
            if(!oneWritableAccount) {
                Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("There must always be at least one non-read-only (writable) account."),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelAdministration);
                    }, this)
                );
                return false;
            }

            return true;
        },

        //validate Public Address
        validatePublicAddress: function() {
            if (this.getSystemSettings().publicUrlMethod == "address_and_port") {
                var publicUrlAddressCmp = this.panelPublicAddress.query('textfield[name="publicUrlAddress"]')[0];
                if (!publicUrlAddressCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("You must provide a valid IP Address or hostname."),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelPublicAddress);
                            publicUrlAddressCmp.focus(true);
                        }, this)
                    );
                    return false;
                }
                var publicUrlPortCmp = this.panelPublicAddress.query('numberfield[name="publicUrlPort"]')[0];
                if (!publicUrlPortCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelPublicAddress);
                            publicUrlPortCmp.focus(true);
                        }, this)
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
        validateSnmp: function() {
            var isSnmpEnabled = this.getSystemSettings().snmpSettings.enabled;
            if (isSnmpEnabled) {
                var snmpCommunityCmp = Ext.getCmp('administration_snmp_communityString');
                if (!snmpCommunityCmp.isValid()) {
                    Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("An SNMP \"Community\" must be specified."),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelSnmp);
                            snmpCommunityCmp.focus(true);
                        }, this)
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
                            }, this)
                        );
                        return false;
                    }

                    snmpTrapHostCmp = Ext.getCmp('administration_snmp_trapHost');
                    if (!snmpTrapHostCmp.isValid()) {
                        Ext.MessageBox.alert(this.i18n._('Warning'), this.i18n._("An Trap \"Host\" must be specified."),
                            Ext.bind(function () {
                                this.tabs.setActiveTab(this.panelSnmp);
                                snmpTrapHostCmp.focus(true);
                            }, this)
                        );
                        return false;
                    }

                    snmpTrapPortCmp = Ext.getCmp('administration_snmp_trapPort');
                    if (!snmpTrapPortCmp.isValid()) {
                        Ext.MessageBox.alert(this.i18n._('Warning'), Ext.String.format(this.i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),
                            Ext.bind(function () {
                                this.tabs.setActiveTab(this.panelSnmp);
                                snmpTrapPortCmp.focus(true);
                            }, this)
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
        beforeSave: function(isApply, handler) {
            handler.call(this, isApply);
        },
        save: function(isApply) {
            this.saveSemaphore = 2;
            Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));

            this.getAdminSettings().users.list=this.gridAdminAccounts.getPageList();

            rpc.adminManager.setSettings(Ext.bind(function(result, exception) {
                this.afterSave(exception, isApply);
            }, this), this.getAdminSettings());

            rpc.skinManager.setSettings(Ext.bind(function(result, exception) {
                this.afterSave(exception, isApply);
            }, this), this.getSkinSettings());
        },
        afterSave: function(exception, isApply) {
            if(Ung.Util.handleException(exception)) return;
            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
                // access settings should be saved last as saving these changes may disconnect the user from the Untangle box
                rpc.systemManager.setSettings(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    //If skin changed it needs a refresh
                    if(this.initialSkin != this.getSkinSettings().skinName) {
                        Ung.Util.goToStartPage();
                        return;
                    }
                    if(isApply) {
                        this.gridAdminAccounts.reload({data: this.getAdminSettings(true).users.list});
                        this.initialSystemSettings = Ung.Util.clone(this.getSystemSettings(true));
                        this.getCertificateInformation(true);
                        this.getHostname(true);
                        this.clearDirty();
                        Ext.MessageBox.hide();
                    } else {
                        Ext.MessageBox.hide();
                        this.closeWindow();
                    }
                }, this), this.getSystemSettings());
            }
        }
    });

    // certificate generation window
    Ext.define("Ung.CertGenerateWindow", {
        extend: "Ung.Window",
        settingsCmp: null,
        initComponent: function() {
            this.bbar= ['->',{
                name: 'Cancel',
                iconCls: 'cancel-icon',
                text: i18n._('Cancel'),
                handler: Ext.bind(function() {
                    this.cancelAction();
                }, this)
            },{
                name: 'Proceed',
                iconCls: 'save-icon',
                text: this.settingsCmp.i18n._('Proceed'),
                handler: Ext.bind(function() {
                    this.proceedAction();
                }, this)
            }];
            Ung.Window.prototype.initComponent.call(this);
        }
    });
}
//@ sourceURL=administration.js
