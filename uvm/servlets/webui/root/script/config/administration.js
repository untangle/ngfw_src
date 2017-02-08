Ext.define('Webui.config.administration', {
    extend: 'Ung.ConfigWin',
    displayName: 'Administration',
    hasReports: true,
    reportCategory: 'Administration',
    panelAdmin: null,
    panelCertificates: null,
    gridCertList: null,
    certGeneratorWindow: null,
    panelSnmp: null,
    panelSkins: null,
    uploadedCustomLogo: false,
    initComponent: function() {
        this.countries = [
            [ "US", i18n._("United States") ], [ "AF", i18n._("Afghanistan") ], [ "AL", i18n._("Albania") ], [ "DZ", i18n._("Algeria") ],
            [ "AS", i18n._("American Samoa") ], [ "AD", i18n._("Andorra") ], [ "AO", i18n._("Angola") ], [ "AI", i18n._("Anguilla") ],
            [ "AQ", i18n._("Antarctica") ], [ "AG", i18n._("Antigua and Barbuda") ], [ "AR", i18n._("Argentina") ], [ "AM", i18n._("Armenia") ],
            [ "AW", i18n._("Aruba") ], [ "AU", i18n._("Australia") ], [ "AT", i18n._("Austria") ], [ "AZ", i18n._("Azerbaijan") ],
            [ "BS", i18n._("Bahamas") ], [ "BH", i18n._("Bahrain") ], [ "BD", i18n._("Bangladesh") ], [ "BB", i18n._("Barbados") ],
            [ "BY", i18n._("Belarus") ], [ "BE", i18n._("Belgium") ], [ "BZ", i18n._("Belize") ], [ "BJ", i18n._("Benin") ], [ "BM", i18n._("Bermuda") ],
            [ "BT", i18n._("Bhutan") ], [ "BO", i18n._("Bolivia") ], [ "BA", i18n._("Bosnia and Herzegovina") ], [ "BW", i18n._("Botswana") ],
            [ "BV", i18n._("Bouvet Island") ], [ "BR", i18n._("Brazil") ], [ "IO", i18n._("British Indian Ocean Territory") ],
            [ "VG", i18n._("British Virgin Islands") ], [ "BN", i18n._("Brunei") ], [ "BG", i18n._("Bulgaria") ], [ "BF", i18n._("Burkina Faso") ],
            [ "BI", i18n._("Burundi") ], [ "KH", i18n._("Cambodia") ], [ "CM", i18n._("Cameroon") ], [ "CA", i18n._("Canada") ],
            [ "CV", i18n._("Cape Verde") ], [ "KY", i18n._("Cayman Islands") ], [ "CF", i18n._("Central African Republic") ],
            [ "TD", i18n._("Chad") ], [ "CL", i18n._("Chile") ], [ "CN", i18n._("China") ], [ "CX", i18n._("Christmas Island") ],
            [ "CC", i18n._("Cocos Islands") ], [ "CO", i18n._("Colombia") ], [ "KM", i18n._("Comoros") ], [ "CG", i18n._("Congo - Brazzaville") ],
            [ "CK", i18n._("Cook Islands") ], [ "CR", i18n._("Costa Rica") ], [ "HR", i18n._("Croatia") ], [ "CU", i18n._("Cuba") ],
            [ "CY", i18n._("Cyprus") ], [ "CZ", i18n._("Czech Republic") ], [ "DK", i18n._("Denmark") ], [ "DJ", i18n._("Djibouti") ],
            [ "DM", i18n._("Dominica") ], [ "DO", i18n._("Dominican Republic") ], [ "EC", i18n._("Ecuador") ], [ "EG", i18n._("Egypt") ],
            [ "SV", i18n._("El Salvador") ], [ "GQ", i18n._("Equatorial Guinea") ], [ "ER", i18n._("Eritrea") ], [ "EE", i18n._("Estonia") ],
            [ "ET", i18n._("Ethiopia") ], [ "FK", i18n._("Falkland Islands") ], [ "FO", i18n._("Faroe Islands") ], [ "FJ", i18n._("Fiji") ],
            [ "FI", i18n._("Finland") ], [ "FR", i18n._("France") ], [ "GF", i18n._("French Guiana") ], [ "PF", i18n._("French Polynesia") ],
            [ "TF", i18n._("French Southern Territories") ], [ "GA", i18n._("Gabon") ], [ "GM", i18n._("Gambia") ], [ "GE", i18n._("Georgia") ],
            [ "DE", i18n._("Germany") ], [ "GH", i18n._("Ghana") ], [ "GI", i18n._("Gibraltar") ], [ "GR", i18n._("Greece") ],
            [ "GL", i18n._("Greenland") ], [ "GD", i18n._("Grenada") ], [ "GP", i18n._("Guadeloupe") ], [ "GU", i18n._("Guam") ],
            [ "GT", i18n._("Guatemala") ], [ "GN", i18n._("Guinea") ], [ "GW", i18n._("Guinea-Bissau") ], [ "GY", i18n._("Guyana") ],
            [ "HT", i18n._("Haiti") ], [ "HM", i18n._("Heard Island and McDonald Islands") ], [ "HN", i18n._("Honduras") ],
            [ "HK", i18n._("Hong Kong SAR China") ], [ "HU", i18n._("Hungary") ], [ "IS", i18n._("Iceland") ], [ "IN", i18n._("India") ],
            [ "ID", i18n._("Indonesia") ], [ "IR", i18n._("Iran") ], [ "IQ", i18n._("Iraq") ], [ "IE", i18n._("Ireland") ],
            [ "IL", i18n._("Israel") ], [ "IT", i18n._("Italy") ], [ "CI", i18n._("Ivory Coast") ], [ "JM", i18n._("Jamaica") ],
            [ "JP", i18n._("Japan") ], [ "JO", i18n._("Jordan") ], [ "KZ", i18n._("Kazakhstan") ], [ "KE", i18n._("Kenya") ],
            [ "KI", i18n._("Kiribati") ], [ "KW", i18n._("Kuwait") ], [ "KG", i18n._("Kyrgyzstan") ], [ "LA", i18n._("Laos") ],
            [ "LV", i18n._("Latvia") ], [ "LB", i18n._("Lebanon") ], [ "LS", i18n._("Lesotho") ], [ "LR", i18n._("Liberia") ],
            [ "LY", i18n._("Libya") ], [ "LI", i18n._("Liechtenstein") ], [ "LT", i18n._("Lithuania") ], [ "LU", i18n._("Luxembourg") ],
            [ "MO", i18n._("Macao SAR China") ], [ "MK", i18n._("Macedonia") ], [ "MG", i18n._("Madagascar") ], [ "MW", i18n._("Malawi") ],
            [ "MY", i18n._("Malaysia") ], [ "MV", i18n._("Maldives") ], [ "ML", i18n._("Mali") ], [ "MT", i18n._("Malta") ],
            [ "MH", i18n._("Marshall Islands") ], [ "MQ", i18n._("Martinique") ], [ "MR", i18n._("Mauritania") ], [ "MU", i18n._("Mauritius") ],
            [ "YT", i18n._("Mayotte") ], [ "FX", i18n._("Metropolitan France") ], [ "MX", i18n._("Mexico") ], [ "FM", i18n._("Micronesia") ],
            [ "MD", i18n._("Moldova") ], [ "MC", i18n._("Monaco") ], [ "MN", i18n._("Mongolia") ], [ "MS", i18n._("Montserrat") ],
            [ "MA", i18n._("Morocco") ], [ "MZ", i18n._("Mozambique") ], [ "MM", i18n._("Myanmar") ], [ "NA", i18n._("Namibia") ],
            [ "NR", i18n._("Nauru") ], [ "NP", i18n._("Nepal") ], [ "NL", i18n._("Netherlands") ], [ "AN", i18n._("Netherlands Antilles") ],
            [ "NC", i18n._("New Caledonia") ], [ "NZ", i18n._("New Zealand") ], [ "NI", i18n._("Nicaragua") ], [ "NE", i18n._("Niger") ],
            [ "NG", i18n._("Nigeria") ], [ "NU", i18n._("Niue") ], [ "NF", i18n._("Norfolk Island") ], [ "KP", i18n._("North Korea") ],
            [ "MP", i18n._("Northern Mariana Islands") ], [ "NO", i18n._("Norway") ], [ "OM", i18n._("Oman") ], [ "PK", i18n._("Pakistan") ],
            [ "PW", i18n._("Palau") ], [ "PA", i18n._("Panama") ], [ "PG", i18n._("Papua New Guinea") ], [ "PY", i18n._("Paraguay") ],
            [ "PE", i18n._("Peru") ], [ "PH", i18n._("Philippines") ], [ "PN", i18n._("Pitcairn") ], [ "PL", i18n._("Poland") ],
            [ "PT", i18n._("Portugal") ], [ "PR", i18n._("Puerto Rico") ], [ "QA", i18n._("Qatar") ], [ "RE", i18n._("Reunion") ],
            [ "RO", i18n._("Romania") ], [ "RU", i18n._("Russia") ], [ "RW", i18n._("Rwanda") ], [ "SH", i18n._("Saint Helena") ],
            [ "KN", i18n._("Saint Kitts and Nevis") ], [ "LC", i18n._("Saint Lucia") ], [ "PM", i18n._("Saint Pierre and Miquelon") ],
            [ "VC", i18n._("Saint Vincent and the Grenadines") ], [ "WS", i18n._("Samoa") ], [ "SM", i18n._("San Marino") ],
            [ "ST", i18n._("Sao Tome and Principe") ], [ "SA", i18n._("Saudi Arabia") ], [ "SN", i18n._("Senegal") ], [ "SC", i18n._("Seychelles") ],
            [ "SL", i18n._("Sierra Leone") ], [ "SG", i18n._("Singapore") ], [ "SK", i18n._("Slovakia") ], [ "SI", i18n._("Slovenia") ],
            [ "SB", i18n._("Solomon Islands") ], [ "SO", i18n._("Somalia") ], [ "ZA", i18n._("South Africa") ],
            [ "GS", i18n._("South Georgia and the South Sandwich Islands") ], [ "KR", i18n._("South Korea") ], [ "ES", i18n._("Spain") ],
            [ "LK", i18n._("Sri Lanka") ], [ "SD", i18n._("Sudan") ], [ "SR", i18n._("Suriname") ], [ "SJ", i18n._("Svalbard and Jan Mayen") ],
            [ "SZ", i18n._("Swaziland") ], [ "SE", i18n._("Sweden") ], [ "CH", i18n._("Switzerland") ], [ "SY", i18n._("Syria") ],
            [ "TW", i18n._("Taiwan") ], [ "TJ", i18n._("Tajikistan") ], [ "TZ", i18n._("Tanzania") ], [ "TH", i18n._("Thailand") ],
            [ "TG", i18n._("Togo") ], [ "TK", i18n._("Tokelau") ], [ "TO", i18n._("Tonga") ], [ "TT", i18n._("Trinidad and Tobago") ],
            [ "TN", i18n._("Tunisia") ], [ "TR", i18n._("Turkey") ], [ "TM", i18n._("Turkmenistan") ], [ "TC", i18n._("Turks and Caicos Islands") ],
            [ "TV", i18n._("Tuvalu") ], [ "VI", i18n._("U.S. Virgin Islands") ], [ "UG", i18n._("Uganda") ], [ "UA", i18n._("Ukraine") ],
            [ "AE", i18n._("United Arab Emirates") ], [ "GB", i18n._("United Kingdom") ], [ "UM", i18n._("United States Minor Outlying Islands") ],
            [ "UY", i18n._("Uruguay") ], [ "UZ", i18n._("Uzbekistan") ], [ "VU", i18n._("Vanuatu") ], [ "VA", i18n._("Vatican") ],
            [ "VE", i18n._("Venezuela") ], [ "VN", i18n._("Vietnam") ], [ "WF", i18n._("Wallis and Futuna") ], [ "EH", i18n._("Western Sahara") ],
            [ "YE", i18n._("Yemen") ], [ "ZM", i18n._("Zambia") ], [ "ZW", i18n._("Zimbabwe") ]
        ];
        this.breadcrumbs = [{
            title: i18n._("Configuration"),
            action: Ext.bind(function() {
                this.cancelAction();
            }, this)
        }, {
            title: i18n._('Administration')
        }];
        this.initialSkin = this.getSkinSettings().skinName;
        this.buildAdmin();
        this.buildCertificates();
        this.buildSnmp();
        this.buildSkins();

        // builds the tab panel with the tabs
        var adminTabs = [this.panelAdmin, this.panelCertificates, this.panelSnmp, this.panelSkins];
        this.buildTabPanel(adminTabs);
        this.tabs.setActiveTab(this.panelAdmin);
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
    // get root certificate details
    getRootCertificateInformation: function(forceReload) {
        if (forceReload || this.rpc.rootCertInfo === undefined) {
            try {
                this.rpc.rootCertInfo = Ung.Main.getCertificateManager().getRootCertificateInformation();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.rootCertInfo;
    },
    // get list of server certificates
    getServerCertificateList: function(forceReload) {
        if (forceReload || this.rpc.serverCertList === undefined) {
            try {
                this.rpc.serverCertList = Ung.Main.getCertificateManager().getServerCertificateList();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.serverCertList;
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
    buildAdmin: function() {
        var changePasswordColumn = Ext.create("Ung.grid.EditColumn",{
            header: i18n._("Change Password"),
            width: 130,
            iconClass: 'icon-edit-row',
            handler: function(view, rowIndex, colIndex, item, e, record) {
                // populate row editor
                this.grid.rowEditorChangePass.populate(record);
                this.grid.rowEditorChangePass.show();
            }
        });
        var passwordValidator = function (fieldValue) {
          //validate password match
            var panel = this.up("panel");
            var pwd = panel.down('textfield[name="password"]');
            var confirmPwd = panel.down('textfield[name="confirmPassword"]');
            if(pwd.getValue() != confirmPwd.getValue()) {
                pwd.markInvalid();
                return i18n._('Passwords do not match');
            }
            pwd.clearInvalid();
            confirmPwd.clearInvalid();
            return true;
        };
        var newAddHandler = function(button, e, rowData) {
            Ext.MessageBox.show({
                title: i18n._('Administrator Warning'),
                msg: i18n._('This action will add an ADMINISTRATOR account.') + '<br/>' + '<br/>' +
                    '<b>' + i18n._('ADMINISTRATORS (also sometimes known as admin or root or superuser) have ADMINISTRATOR access to the server.') + '</b>' + '<br/>' + '<br/>' +
                    i18n._('Administrator accounts have the ability to do anything including:') + '<br/>' +
                    '<ul>' +
                    '<li>' + i18n._("Read/Modify any setting") + '</li>' +
                    '<li>' + i18n._("Restore/Backup all settings") + '</li>' +
                    '<li>' + i18n._("Create more administrators") + '</li>' +
                    '<li>' + i18n._("Delete/Modify/Create any file") + '</li>' +
                    '<li>' + i18n._("Run any command") + '</li>' +
                    '<li>' + i18n._("Install any software") + '</li>' +
                    '<li>' + i18n._("Complete control and access identical to what you now possess") + '</li>' +
                    '</ul>' + '<br/>' +
                    i18n._('Do you understand the above statement?') + '<br/>' +
                    '<input type="checkbox" id="admin_understand"/> <i>' + i18n._('Yes, I understand.') + '</i>' + '<br/>' +
                    '<br/>' +
                    i18n._('Do you wish to continue?') + '<br/>',
                buttons: Ext.MessageBox.YESNO,
                fn: Ext.bind(function(btn) {
                    if (btn == "yes") {
                        if (Ext.get('admin_understand').dom.checked) {
                            Ung.grid.Panel.prototype.addHandler.call(this, button, e, rowData);
                        }
                    }
                }, this)});
        };
        this.gridAdminAccounts=Ext.create('Ung.grid.Panel', {
            flex: 1,
            settingsCmp: this,
            title: i18n._("Admin Accounts"),
            bodyStyle: 'padding-bottom:30px;',
            autoScroll: true,
            hasEdit: false,
            addHandler: newAddHandler,
            name: 'gridAdminAccounts',
            dataExpression: "getAdminSettings().users.list",
            recordJavaClass: "com.untangle.uvm.AdminUserSettings",
            emptyRow: {
                "username": "",
                "description": "",
                "emailAddress": "",
                "emailAlerts": true,
                // "emailSummaries": true,
                "passwordHashBase64": null,
                "passwordHashShadow": null,
                "password": null
            },
            sortField: 'username',
            plugins: [changePasswordColumn],
            fields: [{
                name: 'username'
            }, {
                name: 'description'
            }, {
                name: 'emailAddress'
            },{
                name: 'emailAlerts'
            // },{
            //     name: 'emailSummaries'
            }, {
                name: 'passwordHashBase64'
            }, {
                name: 'passwordHashShadow'
            }, {
                name: 'password'
            }],
            columns: [{
                header: i18n._("Username"),
                width: 200,
                dataIndex: 'username',
                field:{
                    xtype:'textfield',
                    allowBlank: false,
                    emptyText: i18n._("[enter username]"),
                    blankText: i18n._("The username cannot be blank.")
                }
            }, {
                header: i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex: 1,
                editor:{
                    xtype:'textfield',
                    emptyText: i18n._("[no description]")
                }
            },{
                header: i18n._("Email Address"),
                width: 200,
                dataIndex: 'emailAddress',
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[no email]"),
                    vtype: 'email'
                }
            }, {
                xtype:'checkcolumn',
                header: i18n._("Email Events"),
                dataIndex: "emailAlerts",
                width: 150,
                resizable: false
            // }, {
            //     xtype:'checkcolumn',
            //     header: i18n._("Email Summaries"),
            //     dataIndex: "emailSummaries",
            //     width: 150,
            //     resizable: false
            }, 
            changePasswordColumn],
            rowEditorInputLines: [{
                xtype: "textfield",
                name: "Username",
                dataIndex: "username",
                fieldLabel: i18n._("Username"),
                emptyText: i18n._("[enter username]"),
                allowBlank: false,
                blankText: i18n._("The username cannot be blank."),
                width: 400
            }, {
                xtype: "textfield",
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
                width: 400
            },{
                xtype: "textfield",
                name: "Email",
                dataIndex: "emailAddress",
                fieldLabel: i18n._("Email Address"),
                emptyText: i18n._("[no email address]"),
                vtype: 'email',
                width: 400
            },{
                xtype:'checkbox',
                dataIndex: "emailAlerts",
                fieldLabel: i18n._("Email Events"),
                width: 300
            // },{
            //     xtype:'checkbox',
            //     dataIndex: "emailSummaries",
            //     fieldLabel: i18n._("Email Summaries"),
            //     width: 300
            },{
                xtype: "textfield",
                inputType: 'password',
                name: "password",
                dataIndex: "password",
                fieldLabel: i18n._("Password"),
                width: 400,
                minLength: 3,
                minLengthText: Ext.String.format(i18n._("The password is shorter than the minimum {0} characters."), 3)
            },{
                xtype: "textfield",
                inputType: 'password',
                name: "confirmPassword",
                dataIndex: "password",
                fieldLabel: i18n._("Confirm Password"),
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
                fieldLabel: i18n._("Password"),
                width: 400,
                minLength: 3,
                minLengthText: Ext.String.format(i18n._("The password is shorter than the minimum {0} characters."), 3),
                validator: passwordValidator
            }, {
                xtype: "textfield",
                inputType: 'password',
                name: "confirmPassword",
                dataIndex: "password",
                fieldLabel: i18n._("Confirm Password"),
                width: 400,
                validator: passwordValidator
            }]
        });

        this.gridAdminAccounts.subCmps.push(this.gridAdminAccounts.rowEditorChangePass);

        this.panelAdmin = Ext.create('Ext.panel.Panel',{
            name: 'panelAdmin',
            helpSource: 'administration_admin',
            title: i18n._('Admin'),
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [
                this.gridAdminAccounts, {
                    xtype: 'checkbox',
                    fieldLabel: i18n._('Allow HTTP Administration'),
                    labelWidth: 250,
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
                    xtype: 'textfield',
                    fieldLabel: i18n._('Default Administration Username Text'),
                    maxWidth: 400,
                    labelWidth: 250,
                    style: "margin-top: 10px",
                    value: this.getAdminSettings().defaultUsername,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.getAdminSettings().defaultUsername = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype:'fieldset',
                    title: i18n._('Note:'),
                    items: [{
                        xtype: 'label',
                        html: i18n._('HTTP is open on non-WANs (internal interfaces) for blockpages and other services.') + "<br/>" +
                            i18n._('This settings only controls the availability of <b>administration</b> via HTTP.')
                    }]
                }]
        });
    },

    buildCertificates: function() {
        this.buildCertGrid();

        this.panelCertificates = Ext.create('Ext.panel.Panel', {
            name: 'panelCertificates',
            helpSource: 'administration_certificates',
            title: i18n._('Certificates'),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                title: i18n._('Certificate Authority'),
                defaults: { labelWidth: 150 },
                items: [{
                    xtype: 'component',
                    margin: '5 0 5 0',
                    html:  i18n._("The Certificate Authority is used to create and sign SSL certificates used by several applications and services such as SSL Inspector and Captive Portal.  It can also be used to sign the internal web server certificate. To eliminate certificate security warnings on client computers and devices, you should download the root certificate and add it to the list of trusted authorities on each client connected to your network.")
                },{
                    xtype: 'displayfield',
                    fieldLabel: i18n._('Valid starting'),
                    id: 'rootca_status_notBefore',
                    value: this.getRootCertificateInformation() == null ? "" : i18n.timestampFormat(this.getRootCertificateInformation().dateValid)
                },{
                    xtype: 'displayfield',
                    fieldLabel: i18n._('Valid until'),
                    id: 'rootca_status_notAfter',
                    value: this.getRootCertificateInformation() == null ? "" : i18n.timestampFormat(this.getRootCertificateInformation().dateExpires)
                },{
                    xtype: 'displayfield',
                    fieldLabel: i18n._('Subject DN'),
                    id: 'rootca_status_subjectDN',
                    value: this.getRootCertificateInformation() == null ? "" : this.getRootCertificateInformation().certSubject
                },{
                    xtype: 'fieldset',
                    layout: 'column',
                    items: [{
                        xtype: 'button',
                        margin: '0 5 0 5',
                        minWidth: 200,
                        text: i18n._('Generate Certificate Authority'),
                        iconCls: 'action-icon',
                        handler: Ext.bind(function() {
                            this.certGeneratorPopup("ROOT", null, i18n._('Generate Certificate Authority'));
                        }, this)
                    },{
                        xtype: 'component',
                        margin: '0 5 0 5',
                        columnWidth: 1,
                        html: i18n._('Click here to re-create the internal certificate authority.  Use this to change the information in the Subject DN of the root certificate.')
                    }]
                },{
                    xtype: 'fieldset',
                    layout: 'column',
                    items: [{
                        xtype: 'button',
                        margin: '0 5 0 5',
                        minWidth: 200,
                        text: i18n._('Download Root Certificate'),
                        iconCls: 'action-icon',
                        handler: Ext.bind(function() {
                            var downloadForm = document.getElementById('downloadForm');
                            downloadForm["type"].value = "root_certificate_download";
                            downloadForm.submit();
                        }, this)
                    },{
                        xtype: 'component',
                        margin: '0 5 0 5',
                        columnWidth: 1,
                        html: i18n._('Click here to download the root certificate.  Installing this certificate on client devices will allow them to trust certificates generated by this server.')
                    }]
                },{
                    xtype: 'fieldset',
                    layout: 'column',
                    items: [{
                        xtype: 'button',
                        margin: '0 5 0 5',
                        minWidth: 200,
                        text: i18n._('Download Root Certificate Installer'),
                        iconCls: 'action-icon',
                        handler: Ext.bind(function() {
                            var downloadForm = document.getElementById('downloadForm');
                            downloadForm["type"].value = "root_certificate_installer_download";
                            downloadForm.submit();
                        }, this)
                    },{
                        xtype: 'component',
                        margin: '0 5 0 5',
                        columnWidth: 1,
                        html: i18n._('Click here to download the root certificate installer.  It installs the root certificate appropriately on a Windows device.')
                    }]
                }]
            },{
                title: i18n._('Server Certificates'),
                defaults: { labelWidth: 150 },
                items: [{
                    xtype: 'component',
                    margin: '5 0 10 0',
                    html: i18n._("The Server Certificates list is used to select the SSL certificate to be used for each service provided by this server.  The <B>HTTPS</B> column selects the certificate used by the internal web server.  The <B>SMTPS</B> column selects the certificate to use for SMTP+STARTTLS when using SSL Inspector to scan inbound email.  The <B>IPSEC</B> column selects the certificate to use for the IPsec IKEv2 VPN server.")
                }, this.gridCertList, {
                    xtype: 'button',
                    margin: '5 5 0 5',
                    minWidth: 200,
                    text: i18n._('Generate Server Certificate'),
                    iconCls: 'action-icon',
                    handler: Ext.bind(function() {
                        this.certGeneratorPopup("SERVER", this.getHostname(true), i18n._('Generate Server Certificate'));
                    }, this)
                },{
                    xtype: 'button',
                    margin: '5 5 0 5',
                    minWidth: 200,
                    text: i18n._('Upload Server Certificate'),
                    iconCls: 'action-icon',
                    handler: Ext.bind(function() {
                        this.handleServerCertificateUpload();
                    }, this)
                },{
                    xtype: 'button',
                    margin: '5 5 0 5',
                    minWidth: 200,
                    text: i18n._('Create Certificate Signing Request'),
                    iconCls: 'action-icon',
                    handler: Ext.bind(function() {
                        this.certGeneratorPopup("CSR", this.getHostname(true), i18n._("Create Certificate Signing Request"));
                    }, this)
                }]
            },{
                xtype: 'component',
                name: 'validateMessage',
                margin: '10 0 0 20',
                html: Ung.Main.getCertificateManager().validateActiveInspectorCertificates()
            }]
        });
    },

    buildCertGrid: function() {
            var viewCertificateColumn = Ext.create("Ung.grid.EditColumn",{
            header: i18n._("View"),
            width: 60,
            iconClass: 'icon-edit-row',
            handler: function(view, rowIndex, colIndex, item, e, record) {
                var detail = "";
                detail += "<b>VALID:</b> " + i18n.timestampFormat(record.get("dateValid")) + "<br><br>";
                detail += "<b>EXPIRES:</b> " + i18n.timestampFormat(record.get("dateExpires")) + "<br><br>";
                detail += "<b>ISSUER:</b> " + record.get("certIssuer") + "<br><br>";
                detail += "<b>SUBJECT:</b> " + record.get("certSubject") + "<br><br>";
                detail += "<b>SAN:</b> " + record.get("certNames") + "<br><br>";
                detail += "<b>EKU:</b> " + record.get("certUsage") + "<br><br>";
                Ext.MessageBox.alert({ buttons: Ext.Msg.OK, maxWidth: 1024, title: "Certificate Details", msg: "<tt>" + detail + "</tt>" });
            }
        });

        this.gridCertList = Ext.create('Ung.grid.Panel',{
            title: i18n._("Server Certificates"),
            settingsCmp: this,
            autoGenerateId: true,
            height: 250,
            hasDelete: false,
            hasEdit: false,
            hasAdd: false,
            dataExpression: "getServerCertificateList().list",
            recordJavaClass: "com.untangle.uvm.CertificateInformation",
            plugins: [viewCertificateColumn],
            fields: [{
                name: 'certSubject',
            }, {
                name: 'certIssuer',
            }, {
                name: 'dateValid',
            }, {
                name: 'dateExpires',
            }, {
                name: 'httpsServer',
            }, {
                name: 'smtpsServer',
            }, {
                name: 'ipsecServer',
            }],
            columns: [{
                header: i18n._("Subject"),
                flex: 1,
                width: 220,
                sortable: true,
                dataIndex: 'certSubject'
            }, {
                header: i18n._("Issued By"),
                flex: 1,
                width: 220,
                sortable: true,
                dataIndex: 'certIssuer'
            }, {
                header: i18n._("Date Valid"),
                width: 140,
                sortable: true,
                dataIndex: 'dateValid',
                renderer: function(value) { return(i18n.timestampFormat(value)); }
            }, {
                header: i18n._("Date Expires"),
                width: 140,
                sortable: true,
                dataIndex: 'dateExpires',
                renderer: function(value) { return(i18n.timestampFormat(value)); }
            }, {
                header: i18n._("HTTPS"),
                xtype: 'checkcolumn',
                width: 60,
                dataIndex: 'httpsServer',
                listeners: {
                    // don't allow uncheck - they must pick a different cert
                    beforecheckchange: Ext.bind(function(elem, rowIndex, checked) {
                        if (checked === false) return(false);
                        return(true);
                    }, this),
                    // when a new cert is selected uncheck all others
                    checkchange: Ext.bind(function(elem, rowIndex, checked) {
                        var records=elem.up("grid").getStore().getRange();
                        for(var i=0; i<records.length; i++) {
                            if (i === rowIndex) continue;
                            records[i].set('httpsServer', false);
                        }
                    }, this)
                }
            }, {
                header: i18n._("SMTPS"),
                xtype: 'checkcolumn',
                width: 60,
                dataIndex: 'smtpsServer',
                listeners: {
                    // don't allow uncheck - they must pick a different cert
                    beforecheckchange: Ext.bind(function(elem, rowIndex, checked) {
                        if (checked === false) return(false);
                        return(true);
                    }, this),
                    // when a new cert is selected uncheck all others
                    checkchange: Ext.bind(function(elem, rowIndex, checked) {
                        var records=elem.up("grid").getStore().getRange();
                        for(var i=0; i<records.length; i++) {
                            if (i === rowIndex) continue;
                            records[i].set('smtpsServer', false);
                        }
                    }, this)
                }
            }, {
                header: i18n._("IPSEC"),
                xtype: 'checkcolumn',
                width: 60,
                dataIndex: 'ipsecServer',
                listeners: {
                    // don't allow uncheck - they must pick a different cert
                    beforecheckchange: Ext.bind(function(elem, rowIndex, checked) {
                        if (checked === false) return(false);
                        return(true);
                    }, this),
                    // when a new cert is selected uncheck all others
                    checkchange: Ext.bind(function(elem, rowIndex, checked) {
                        var records=elem.up("grid").getStore().getRange();
                        for(var i=0; i<records.length; i++) {
                            if (i === rowIndex) continue;
                            records[i].set('ipsecServer', false);
                        }
                    }, this)
                }
            }, viewCertificateColumn, {
                header: i18n._("Delete"),
                xtype: 'actioncolumn',
                width: 60,
                items: [{
                    id: 'certRemove',
                    iconCls: 'icon-delete-row',
                    tooltip: i18n._("Click to delete"),
                    handler: Ext.bind(function(view, rowIndex, colIndex, item, e, record) {
                        if (record.get("fileName") === "apache.pem") {
                            Ext.MessageBox.alert("System Certificate","This is the default system certificate and cannot be removed.");
                            return;
                        }
                        if (record.get("httpsServer") || record.get("smtpsServer") || record.get("ipsecServer")) {
                            Ext.MessageBox.alert("Certificate In Use","You can not delete a certificate that is assigned to one or more services.");
                            return;
                        }
                        if (this.isDirty())
                        {
                            Ext.MessageBox.alert("Unsaved Changes","You must apply unsaved changes changes before you can delete this certificate.");
                            return;
                        }
                        Ext.MessageBox.confirm("Are you sure you want to delete this certificate?", "<B>SUBJECT:</B> " + record.get("certSubject") + "<BR><BR><B>ISSUER:</B> " + record.get("certIssuer"), function(button) {
                            if (button === 'yes')
                            {
                            Ung.Main.getCertificateManager().removeServerCertificate(record.get("fileName"));
                            this.getServerCertificateList(true);
                            this.gridCertList.reload();
                            }
                        }, this);
                    }, this)
                }]
            }]
        });
    },

    certGeneratorPopup: function(certMode, hostName, titleText) {
        var helptipRenderer = function(c) {
            Ext.create('Ext.tip.ToolTip', {
                target: c.getEl(),
                html: c.helptip,
                dismissDelay: 0,
                anchor: 'bottom'
            });
        };

        try {
            netStatus = Ung.Main.getNetworkManager().getInterfaceStatus();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }

        addressList = "";
        addressList += hostName;

        for( x = 0 ; x < netStatus.list.length ; x++)
        {
            var netItem = netStatus.list[x];
            if (netItem.v4Address === null) { continue; }
            addressList += ",";
            addressList += netItem.v4Address;
        }

        this.certGeneratorWindow = Ext.create("Ext.Window", {
            title: titleText,
            layout: 'fit',
            width: 600,
            height: (certMode === "ROOT" ? 320 : 360),
            border: true,
            modal: true,
            items: [{
                xtype: "form",
                layout: 'anchor',
                border: false,
                defaults: {
                    anchor: '98%',
                    margin: "10 10 10 10",
                    labelWidth: 150,
                    listeners: {
                        render: helptipRenderer
                    }
                },
                items: [{
                    xtype: 'combo',
                    fieldLabel: i18n._('Country') + " (C)",
                    name: 'Country',
                    id: 'Country',
                    helptip: i18n._("Select the country in which your organization is legally registered."),
                    allowBlank: true,
                    store: this.countries,
                    queryMode: 'local',
                    editable: false
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('State/Province') + " (ST)",
                    name: "State",
                    helptip: i18n._('Name of state, province, region, territory where your organization is located. Please enter the full name. Do not abbreviate.'),
                    allowBlank: false

                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('City/Locality') + " (L)",
                    name: "Locality",
                    helptip: i18n._('Name of the city/locality in which your organization is registered/located. Please spell out the name of the city/locality. Do not abbreviate.'),
                    allowBlank: false
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('Organization') + " (O)",
                    name: "Organization",
                    helptip: i18n._("The name under which your business is legally registered. The listed organization must be the legal registrant of the domain name in the certificate request. If you are enrolling as a small business/sole proprietor, please enter the certificate requester's name in the Organization field, and the DBA (doing business as) name in the Organizational Unit field."),
                    allowBlank: false
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('Organizational Unit') + " (OU)",
                    name: "OrganizationalUnit",
                    helptip: i18n._("Optional. Use this field to differentiate between divisions within an organization. If applicable, you may enter the DBA (doing business as) name in this field."),
                    allowBlank: true
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('Common Name') + " (CN)",
                    name: "CommonName",
                    helptip: i18n._("The name entered in the CN (common name) field MUST be the fully-qualified domain name of the website for which you will be using the certificate (example.com). Do not include the http:// or https:// prefixes in your common name. Do NOT enter your personal name in this field."),
                    allowBlank: false,
                    value: hostName
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('Subject Alternative Names'),
                    name: "AltNames",
                    helptip: i18n._("Optional. Use this field to enter a comma seperated list of one or more alternative host names or IP addresses that may be used to access the website for which you will be using the certificate."),
                    allowBlank: true,
                    value: (certMode === "ROOT" ? "" : addressList),
                    hidden: (certMode === "ROOT" ? true : false)
                }],
                buttons: [{
                    xtype: "button",
                    text: i18n._("Generate"),
                    name: "Accept",
                    width: 120,
                    handler: Ext.bind(function() {
                        this.certGeneratorWorker(certMode);
                    }, this)
                },{
                    xtype: "button",
                    text: i18n._("Cancel"),
                    name: "Cancel",
                    width: 120,
                    handler: Ext.bind(function() {
                        this.certGeneratorWindow.close();
                    }, this)
                }]
            }]
        });

        this.certGeneratorWindow.show();
    },

    certGeneratorWorker: function(certMode)
    {
        var form_C = this.certGeneratorWindow.down('[name="Country"]');
        var form_ST = this.certGeneratorWindow.down('[name="State"]');
        var form_L = this.certGeneratorWindow.down('[name="Locality"]');
        var form_O = this.certGeneratorWindow.down('[name="Organization"]');
        var form_OU = this.certGeneratorWindow.down('[name="OrganizationalUnit"]');
        var form_CN = this.certGeneratorWindow.down('[name="CommonName"]');
        var form_SAN = this.certGeneratorWindow.down('[name="AltNames"]');

        if (form_C.getValue() == null)  { Ext.MessageBox.alert(i18n._('Warning'), i18n._('The Country field must not be empty')); return; }
        if (form_ST.getValue().length == 0) { Ext.MessageBox.alert(i18n._('Warning'), i18n._('The State field must not be empty')); return; }
        if (form_L.getValue().length == 0)  { Ext.MessageBox.alert(i18n._('Warning'), i18n._('The Locality field must not be empty')); return; }
        if (form_O.getValue().length == 0)  { Ext.MessageBox.alert(i18n._('Warning'), i18n._('The Organization field must not be empty')); return; }
        if (form_CN.getValue().length == 0) { Ext.MessageBox.alert(i18n._('Warning'), i18n._('The Common Name field must not be empty')); return; }

        var certSubject = ("/CN=" + form_CN.getValue());
        if ((form_C.getValue()) && (form_C.getValue().length > 0)) certSubject += ("/C=" + form_C.getValue());
        if ((form_ST.getValue()) && (form_ST.getValue().length > 0)) certSubject += ("/ST=" + form_ST.getValue());
        if ((form_L.getValue()) && (form_L.getValue().length > 0)) certSubject += ("/L=" + form_L.getValue());
        if ((form_O.getValue()) && (form_O.getValue().length > 0)) certSubject += ("/O=" + form_O.getValue());
        if ((form_OU.getValue()) && (form_OU.getValue().length > 0)) certSubject += ("/OU=" + form_OU.getValue());

        altNames = "";
        if ((form_SAN.getValue()) && (form_SAN.getValue().length > 0)) {
            altNames = form_SAN.getValue();
            var hostnameRegex = /^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$/;
            // Parse subject alt name list. For IP's prefix with both DNS: and IP:, for hostnames prefix with DNS:, otherwise is left unchanged
            var altNameTokens = altNames.split(',');
            var altNamesArray=[];
            for(var i=0; i<altNameTokens.length; i++) {
                var altName = altNameTokens[i].trim();
                if(Ext.form.VTypes.ipAddress(altName)) {
                    altName="IP:"+altName+",DNS:"+altName;
                } else if(hostnameRegex.test(altName)) {
                    altName="DNS:"+altName;
                }
                altNamesArray.push(altName);
            }
            altNames = altNamesArray.join(',');
        }

        // for a CSR we handle it like a file download which will cause the
        // client browser to prompt the user to save the resulting file
        if (certMode === "CSR")
        {
            this.certGeneratorWindow.close();
            var downloadForm = document.getElementById('downloadForm');
            downloadForm["type"].value = "certificate_request_download";
            downloadForm["arg1"].value = certSubject;
            downloadForm["arg2"].value = altNames;
            downloadForm.submit();
            return;
        }

        // for ROOT mode we just throw up a success dialog and refresh the display
        if (certMode === "ROOT")
        {
        certFunction = Ung.Main.getCertificateManager().generateCertificateAuthority;

            certFunction(Ext.bind(function(result)
            {
            this.certGeneratorWindow.close();
            refreshDisplay = this.updateCertificateDisplay();

                if (result)
                {
                Ext.MessageBox.alert(i18n._("Success"), i18n._("Certificate Authority generation successfully completed. Click OK to continue."), refreshDisplay);
                }
                else
                {
                Ext.MessageBox.alert(i18n._("Failure"), i18n._("Error during Certificate Authority generation.  Click OK to continue."), refreshDisplay);
                }

            }, this), certSubject, altNames);
        }

        // deal with restarting apache when creating a new server certificate
        if (certMode === "SERVER")
        {
        certFunction = Ung.Main.getCertificateManager().generateServerCertificate;

            certFunction(Ext.bind(function(result)
            {
            this.certGeneratorWindow.close();

                if (result)
                {
                    this.getServerCertificateList(true);
                    this.gridCertList.reload();
                }

                else
                {
                    Ext.MessageBox.alert(i18n._("Failure"), i18n._("Error during certificate generation."));
                }
            }, this), certSubject, altNames);
        }
    },

    handleServerCertificateUpload: function() {
        master = this;
        popup = new Ext.Window({
            title: i18n._("Upload Server Certificate"),
            layout: 'fit',
            width: 600,
            height: 120,
            border: true,
            xtype: 'form',
            items: [{
                xtype: "form",
                id: "upload_server_cert_form",
                url: "upload",
                border: false,
                items: [{
                    xtype: 'filefield',
                    fieldLabel: i18n._("File"),
                    name: "filename",
                    id: "filename",
                    margin: "10 10 10 10",
                    width: 560,
                    labelWidth: 50,
                    allowBlank: false,
                    validateOnBlur: false
                }, {
                    xtype: "hidden",
                    name: "type",
                    value: "server_cert"
                    }]
                }],
                buttons: [{
                    xtype: "button",
                    text: i18n._("Upload Certificate"),
                    name: "Upload Certificate",
                    width: 200,
                    handler: Ext.bind(function() {
                        this.handleServerFileUpload();
                    }, this)
                }, {
                    xtype: "button",
                    text: i18n._("Cancel"),
                    name: "Cancel",
                    width: 100,
                    handler: Ext.bind(function() {
                        popup.close();
                    }, this)
                }]
        });

        popup.show();
    },

    handleServerFileUpload: function()
    {
        var prova = Ext.getCmp("upload_server_cert_form");
        var fileText = prova.items.get(0);
        var form = prova.getForm();
        var parent = this;

        if (fileText.getValue().length === 0)
        {
            Ext.MessageBox.alert(i18n._("Invalid or missing File"), i18n._("Please select a certificate to upload."));
            return false;
        }

        form.submit({
            success: function(form, action) {
                popup.close();
                parent.getServerCertificateList(true);
                parent.gridCertList.reload();
                },
            failure: function(form, action) {
                popup.close();
                Ext.MessageBox.alert(i18n._("Failure"), action.result.msg);
                }
            });

        return true;
    },

    updateCertificateDisplay: function() {
        var certInfo = this.getRootCertificateInformation(true);
        if (certInfo != null) {
            Ext.getCmp('rootca_status_notBefore').setValue(i18n.timestampFormat(certInfo.dateValid));
            Ext.getCmp('rootca_status_notAfter').setValue(i18n.timestampFormat(certInfo.dateExpires));
            Ext.getCmp('rootca_status_subjectDN').setValue(certInfo.certSubject);
        }
    },

    buildSnmp: function() {
        var passwordValidator = function () {
            var name = this.name;
            var confirmPos = name.search("Confirm");
            if( confirmPos != -1 ){
                name = name.substring( 0, confirmPos );
            }
            var panel = this.up("panel");
            var pwd = panel.down('textfield[name="' + name + '"]');
            var confirmPwd = panel.down('textfield[name="' + name + 'Confirm"]');
            if(pwd.getValue() != confirmPwd.getValue()) {
                pwd.markInvalid();
                return i18n._('Passwords do not match');
            }
            if( pwd.getValue().length < 8 ){
                pwd.markInvalid();
                return i18n._('Password is too short.');
            }
            pwd.clearInvalid();
            confirmPwd.clearInvalid();
            return true;
        };

        this.panelSnmp = Ext.create('Ext.panel.Panel',{
            name: 'panelSnmp',
            helpSource: 'administration_snmp',
            title: i18n._('SNMP'),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                title: i18n._('SNMP'),
                defaults: {
                    labelWidth: 200
                },
                items: [{
                    xtype: 'checkbox',
                    boxLabel: i18n._('Enable SNMP Monitoring'),
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
                                    Ext.getCmp('administration_snmp_v3enabled').enable();
                                    var v3EnabledCmp = Ext.getCmp('administration_snmp_v3enabled');
                                    if (v3EnabledCmp.getValue()) {
                                        Ext.getCmp('administration_snmp_v3required').enable();
                                        Ext.getCmp('administration_snmp_v3username').enable();
                                        Ext.getCmp('administration_snmp_v3authenticationProtocol').enable();
                                        Ext.getCmp('administration_snmp_v3authenticationPassphrase').enable();
                                        Ext.getCmp('administration_snmp_v3authenticationPassphraseConfirm').enable();
                                        Ext.getCmp('administration_snmp_v3privacyProtocol').enable();
                                        Ext.getCmp('administration_snmp_v3privacyPassphrase').enable();
                                        Ext.getCmp('administration_snmp_v3privacyPassphraseConfirm').enable();
                                    }
                                    Ext.getCmp('administration_snmp_sendTraps').enable();
                                    var sendTrapsCmp = Ext.getCmp('administration_snmp_sendTraps');
                                    if (sendTrapsCmp.getValue()) {
                                        Ext.getCmp('administration_snmp_trapCommunity').enable();
                                        Ext.getCmp('administration_snmp_trapHost').enable();
                                        Ext.getCmp('administration_snmp_trapPort').enable();
                                    }
                                } else {
                                    Ext.getCmp('administration_snmp_communityString').disable();
                                    Ext.getCmp('administration_snmp_sysContact').disable();
                                    Ext.getCmp('administration_snmp_sysLocation').disable();
                                    Ext.getCmp('administration_snmp_v3enabled').disable();
                                    Ext.getCmp('administration_snmp_v3required').disable();
                                    Ext.getCmp('administration_snmp_v3username').disable();
                                    Ext.getCmp('administration_snmp_v3authenticationProtocol').disable();
                                    Ext.getCmp('administration_snmp_v3authenticationPassphrase').disable();
                                    Ext.getCmp('administration_snmp_v3authenticationPassphraseConfirm').disable();
                                    Ext.getCmp('administration_snmp_v3privacyProtocol').disable();
                                    Ext.getCmp('administration_snmp_v3privacyPassphrase').disable();
                                    Ext.getCmp('administration_snmp_v3privacyPassphraseConfirm').disable();
                                    Ext.getCmp('administration_snmp_sendTraps').disable();
                                    Ext.getCmp('administration_snmp_trapCommunity').disable();
                                    Ext.getCmp('administration_snmp_trapHost').disable();
                                    Ext.getCmp('administration_snmp_trapPort').disable();
                                }
                            }, this)
                        }
                    }
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('Community'),
                    name: 'communityString',
                    id: 'administration_snmp_communityString',
                    value: this.getSystemSettings().snmpSettings.communityString == 'CHANGE_ME' ? i18n._('CHANGE_ME'): this.getSystemSettings().snmpSettings.communityString,
                    allowBlank: false,
                    blankText: i18n._("An SNMP Community must be specified."),
                    disabled: !this.getSystemSettings().snmpSettings.enabled
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('System Contact'),
                    name: 'sysContact',
                    id: 'administration_snmp_sysContact',
                    value: this.getSystemSettings().snmpSettings.sysContact == 'MY_CONTACT_INFO' ? i18n._('MY_CONTACT_INFO'): this.getSystemSettings().snmpSettings.sysContact,
                    disabled: !this.getSystemSettings().snmpSettings.enabled
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('System Location'),
                    name: 'sysLocation',
                    id: 'administration_snmp_sysLocation',
                    value: this.getSystemSettings().snmpSettings.sysLocation == 'MY_LOCATION' ? i18n._('MY_LOCATION'): this.getSystemSettings().snmpSettings.sysLocation,
                    disabled: !this.getSystemSettings().snmpSettings.enabled
                },{
                    xtype: 'checkbox',
                    boxLabel: i18n._('Enable Traps'),
                    hideLabel: true,
                    name: 'sendTraps',
                    id: 'administration_snmp_sendTraps',
                    checked: this.getSystemSettings().snmpSettings.sendTraps,
                    disabled: !this.getSystemSettings().snmpSettings.enabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                this.getSystemSettings().snmpSettings.sendTraps = checked;
                                if (checked) {
                                    Ext.getCmp('administration_snmp_trapCommunity').enable();
                                    Ext.getCmp('administration_snmp_trapHost').enable();
                                    Ext.getCmp('administration_snmp_trapPort').enable();
                                } else {
                                    Ext.getCmp('administration_snmp_trapCommunity').disable();
                                    Ext.getCmp('administration_snmp_trapHost').disable();
                                    Ext.getCmp('administration_snmp_trapPort').disable();
                                }
                            }, this)
                        }
                    }
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('Community'),
                    name: 'trapCommunity',
                    id: 'administration_snmp_trapCommunity',
                    value: this.getSystemSettings().snmpSettings.trapCommunity == 'MY_TRAP_COMMUNITY' ? i18n._('MY_TRAP_COMMUNITY'): this.getSystemSettings().snmpSettings.trapCommunity,
                    allowBlank: false,
                    blankText: i18n._("An Trap Community must be specified."),
                    disabled: !this.getSystemSettings().snmpSettings.enabled || !this.getSystemSettings().snmpSettings.sendTraps
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('Host'),
                    name: 'trapHost',
                    id: 'administration_snmp_trapHost',
                    value: this.getSystemSettings().snmpSettings.trapHost == 'MY_TRAP_HOST' ? i18n._('MY_TRAP_HOST'): this.getSystemSettings().snmpSettings.trapHost,
                    allowBlank: false,
                    blankText: i18n._("An Trap Host must be specified."),
                    disabled: !this.getSystemSettings().snmpSettings.enabled || !this.getSystemSettings().snmpSettings.sendTraps
                },{
                    xtype: 'numberfield',
                    fieldLabel: i18n._('Port'),
                    name: 'trapPort',
                    id: 'administration_snmp_trapPort',
                    value: this.getSystemSettings().snmpSettings.trapPort,
                    allowDecimals: false,
                    minValue: 0,
                    allowBlank: false,
                    blankText: i18n._("You must provide a valid port."),
                    vtype: 'port',
                    disabled: !this.getSystemSettings().snmpSettings.enabled || !this.getSystemSettings().snmpSettings.sendTraps
                },{
                    xtype: 'checkbox',
                    boxLabel: i18n._('Enable SNMP v3'),
                    hideLabel: true,
                    name: 'snmpv3Enabled',
                    id: 'administration_snmp_v3enabled',
                    checked: this.getSystemSettings().snmpSettings.v3Enabled,
                    disabled: !this.getSystemSettings().snmpSettings.enabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                this.getSystemSettings().snmpSettings.v3enabled = checked;
                                if (checked) {
                                        Ext.getCmp('administration_snmp_v3required').enable();
                                        Ext.getCmp('administration_snmp_v3username').enable();
                                        Ext.getCmp('administration_snmp_v3authenticationPassphrase').enable();
                                        Ext.getCmp('administration_snmp_v3authenticationProtocol').enable();
                                        Ext.getCmp('administration_snmp_v3authenticationPassphraseConfirm').enable();
                                        Ext.getCmp('administration_snmp_v3privacyProtocol').enable();
                                        Ext.getCmp('administration_snmp_v3privacyPassphrase').enable();
                                        Ext.getCmp('administration_snmp_v3privacyPassphraseConfirm').enable();
                                } else {
                                    Ext.getCmp('administration_snmp_v3required').disable();
                                    Ext.getCmp('administration_snmp_v3username').disable();
                                    Ext.getCmp('administration_snmp_v3authenticationProtocol').disable();
                                    Ext.getCmp('administration_snmp_v3authenticationPassphrase').disable();
                                    Ext.getCmp('administration_snmp_v3authenticationPassphraseConfirm').disable();
                                    Ext.getCmp('administration_snmp_v3privacyProtocol').disable();
                                    Ext.getCmp('administration_snmp_v3privacyPassphrase').disable();
                                    Ext.getCmp('administration_snmp_v3privacyPassphraseConfirm').disable();
                                }
                            }, this)
                        }
                    }
                },{
                    xtype: 'textfield',
                    fieldLabel: i18n._('Username'),
                    name: 'snmpv3Username',
                    id: 'administration_snmp_v3username',
                    value: this.getSystemSettings().snmpSettings.v3Username,
                    allowBlank: false,
                    blankText: i18n._("Username must be specified."),
                    disabled: !this.getSystemSettings().snmpSettings.v3Enabled || !this.getSystemSettings().snmpSettings.enabled
                },{
                    xtype: 'combo',
                    fieldLabel: i18n._('Authentication Protocol'),
                    name: "snmpv3AuthenticationProtocol",
                    id: "administration_snmp_v3authenticationProtocol",
                    store: [
                        ["sha", i18n._("SHA") ],
                        ["md5", i18n._("MD5") ]
                    ],
                    editable: false,
                    queryMode: 'local',
                    value: this.getSystemSettings().snmpSettings.v3AuthenticationProtocol ? this.getSystemSettings().snmpSettings.v3AuthenticationProtocol : "sha",
                    disabled: !this.getSystemSettings().snmpSettings.v3Enabled || !this.getSystemSettings().snmpSettings.enabled
                },{
                    xtype: 'textfield',
                    inputType: 'password',
                    fieldLabel: i18n._('Authentication Passphrase'),
                    name: 'snmpv3AuthenticationPassphrase',
                    id: 'administration_snmp_v3authenticationPassphrase',
                    value: this.getSystemSettings().snmpSettings.v3AuthenticationPassphrase,
                    allowBlank: false,
                    blankText: i18n._("Authentication Passphrase must be specified."),
                    validator: passwordValidator,
                    disabled: !this.getSystemSettings().snmpSettings.v3Enabled || !this.getSystemSettings().snmpSettings.enabled
                },{
                    xtype: 'textfield',
                    inputType: 'password',
                    fieldLabel: i18n._('Confirm Authentication Passphrase'),
                    name: 'snmpv3AuthenticationPassphraseConfirm',
                    id: 'administration_snmp_v3authenticationPassphraseConfirm',
//                        value: this.getSystemSettings().snmpSettings.v3AuthenticationPassphrase,
                    allowBlank: false,
                    blankText: i18n._("Confirm Authentication Passphrase must be specified."),
                    validator: passwordValidator,
                    disabled: !this.getSystemSettings().snmpSettings.v3Enabled || !this.getSystemSettings().snmpSettings.enabled
                },{
                    xtype: 'combo',
                    fieldLabel: i18n._('Privacy Protocol'),
                    name: "snmpv3PrivacyProtocol",
                    id: "administration_snmp_v3privacyProtocol",
                    store: [
                        ["des", i18n._("DES") ],
                        ["aes", i18n._("AES") ]
                    ],
                    editable: false,
                    queryMode: 'local',
                    value: this.getSystemSettings().snmpSettings.v3PrivacyProtocol ? this.getSystemSettings().snmpSettings.v3PrivacyProtocol : "des",
                    disabled: !this.getSystemSettings().snmpSettings.v3Enabled || !this.getSystemSettings().snmpSettings.enabled
                },{
                    xtype: 'textfield',
                    inputType: 'password',
                    fieldLabel: i18n._('Privacy Passphrase'),
                    name: 'snmpv3PrivacyPassphrase',
                    id: 'administration_snmp_v3privacyPassphrase',
                    value: this.getSystemSettings().snmpSettings.v3PrivacyPassphrase,
                    allowBlank: false,
                    blankText: i18n._("Privacy Passphrase must be specified."),
                    validator: passwordValidator,
                    disabled: !this.getSystemSettings().snmpSettings.v3Enabled || !this.getSystemSettings().snmpSettings.enabled
                },{
                    xtype: 'textfield',
                    inputType: 'password',
                    fieldLabel: i18n._('Confirm Privacy Passphrase'),
                    name: 'snmpv3PrivacyPassphraseConfirm',
                    id: 'administration_snmp_v3privacyPassphraseConfirm',
//                        value: this.getSystemSettings().snmpSettings.v3PrivacyPassphrase,
                    allowBlank: false,
                    blankText: i18n._("Confirm Privacy Passphrase must be specified."),
                    validator: passwordValidator,
                    disabled: !this.getSystemSettings().snmpSettings.v3Enabled || !this.getSystemSettings().snmpSettings.enabled
                },{
                    xtype: 'checkbox',
                    hideEmptyLabel: false,
                    boxLabel: i18n._('Require only SNMP v3'),
                    name: 'snmpv3Require',
                    id: 'administration_snmp_v3required',
                    checked: this.getSystemSettings().snmpSettings.v3Required,
                    validator: passwordValidator,
                    disabled: !this.getSystemSettings().snmpSettings.v3Enabled || !this.getSystemSettings().snmpSettings.enabled
                }]
            }]
        });
    },

    buildSkins: function() {
        this.adminSkinsStore = Ext.create("Ext.data.JsonStore",{
            fields: [{
                name: 'name'
            },{
                name: 'displayName',
                convert: function(v) {
                    if ( v == "Default" ) return i18n._("Default");
                    return v;
                }
            }]
        });

        this.panelSkins = Ext.create('Ext.panel.Panel',{
            name: "panelSkins",
            helpSource: 'administration_skins',
            title: i18n._('Skins'),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset'
            },
            items: [{
                title: i18n._('Administration Skin'),
                items: [{
                    xtype: 'combo',
                    name: "skinName",
                    store: this.adminSkinsStore,
                    displayField: 'displayName',
                    valueField: 'name',
                    forceSelection: true,
                    editable: false,
                    queryMode: 'local',
                    hideLabel: true,
                    width: 300,
                    listeners: {
                        "select": {
                            fn: Ext.bind(function(elem, record) {
                                this.getSkinSettings().skinName = record.get("name");
                            }, this)
                        }
                    }
                }]
            },{
                title: i18n._('Upload New Skin'),
                items: {
                    xtype: 'form',
                    name: 'uploadSkinForm',
                    url: 'upload',
                    border: false,
                    items: [{
                        xtype: 'filefield',
                        fieldLabel: i18n._('File'),
                        name: 'uploadSkinFile',
                        width: 500,
                        labelWidth: 50,
                        allowBlank: false,
                        validateOnBlur: false
                    },{
                        xtype: 'button',
                        text: i18n._("Upload"),
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
            onUpload: Ext.bind(function() {
                var form = this.panelSkins.down('form[name="uploadSkinForm"]');
                form.submit({
                    waitMsg: i18n._('Please wait while your skin is uploaded...'),
                    success: Ext.bind(function( form, action ) {
                        rpc.skinManager.getSkinsList(Ext.bind(function(result, exception) {
                            if(Ung.Util.handleException(exception)) return;
                            var filefield = this.panelSkins.down('filefield[name="uploadSkinFile"]');
                            if ( filefield) {
                                filefield.reset();
                            }
                            this.adminSkinsStore.loadData(result.list);
                            Ext.MessageBox.alert( i18n._("Succeeded"), i18n._("Upload Skin Succeeded"));
                        }, this));
                    }, this ),
                    failure: Ext.bind(function( form, action ) {
                        var errorMsg = i18n._("Upload Skin Failed");
                        if (action.result && action.result.msg) {
                            switch (action.result.msg) {
                            case 'Invalid Skin':
                                errorMsg = i18n._("Invalid Skin");
                                break;
                            case 'The default skin can not be overwritten':
                                errorMsg = i18n._("The default skin can not be overwritten");
                                break;
                            case 'Error creating skin folder':
                                errorMsg = i18n._("Error creating skin folder");
                                break;
                            default:
                                errorMsg = i18n._("Upload Skin Failed");
                            }
                        }
                        Ext.MessageBox.alert(i18n._("Failed"), errorMsg);
                    }, this )
                });
            }, this)
        });
        rpc.skinManager.getSkinsList(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            this.adminSkinsStore.loadData(result.list);
            var skinCombo=this.panelSkins.down('combo[name="skinName"]');
            if(skinCombo!=null) {
                skinCombo.setValue(this.getSkinSettings().skinName);
                skinCombo.clearDirty();
            }
        }, this));
    },

    // validation function
    validate: function() {
        return  this.validateAdminAccounts() && this.validatePublicAddress() && this.validateSnmp() && this.validateCertificates();
    },

    //validate Admin Accounts
    validateAdminAccounts: function() {
        var listAdminAccounts = this.gridAdminAccounts.getList();
        var oneWritableAccount = false;

        // verify that the username is not duplicated
        for(var i=0; i<listAdminAccounts.length;i++) {
            for(var j=i+1; j<listAdminAccounts.length;j++) {
                if (listAdminAccounts[i].username == listAdminAccounts[j].username) {
                    Ext.MessageBox.alert(i18n._('Warning'), i18n._("The username") + ": " + listAdminAccounts[j].username + " " + i18n._("already exists in row") + " " + (j+1),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelAdmin);
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
            Ext.MessageBox.alert(i18n._('Warning'), i18n._("There must always be at least one valid account."),
                Ext.bind(function () {
                    this.tabs.setActiveTab(this.panelAdmin);
                }, this)
            );
            return false;
        }

        // verify that there was at least one non-read-only account
        if(!oneWritableAccount) {
            Ext.MessageBox.alert(i18n._('Warning'), i18n._("There must always be at least one non-read-only (writable) account."),
                Ext.bind(function () {
                    this.tabs.setActiveTab(this.panelAdmin);
                }, this)
            );
            return false;
        }

        return true;
    },

    //validate Public Address
    validatePublicAddress: function() {
        if (this.getSystemSettings().publicUrlMethod == "address_and_port") {
            var publicUrlAddressCmp = this.panelPublicAddress.down('textfield[name="publicUrlAddress"]');
            if (!publicUrlAddressCmp.isValid()) {
                Ext.MessageBox.alert(i18n._('Warning'), i18n._("You must provide a valid IP Address or hostname."),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelPublicAddress);
                        publicUrlAddressCmp.focus(true);
                    }, this)
                );
                return false;
            }
            var publicUrlPortCmp = this.panelPublicAddress.down('numberfield[name="publicUrlPort"]');
            if (!publicUrlPortCmp.isValid()) {
                Ext.MessageBox.alert(i18n._('Warning'), Ext.String.format(i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),
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
                Ext.MessageBox.alert(i18n._('Warning'), i18n._("An SNMP Community must be specified."),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelSnmp);
                        snmpCommunityCmp.focus(true);
                    }, this)
                );
                return false;
            }

            var sendTrapsCmp = Ext.getCmp('administration_snmp_sendTraps');
            var isTrapEnabled = sendTrapsCmp.getValue();
            var snmpTrapCommunityCmp, snmpTrapHostCmp, snmpTrapPortCmp;
            if (isTrapEnabled) {
                snmpTrapCommunityCmp = Ext.getCmp('administration_snmp_trapCommunity');
                if (!snmpTrapCommunityCmp.isValid()) {
                    Ext.MessageBox.alert(i18n._('Warning'), i18n._("An Trap Community must be specified."),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelSnmp);
                            snmpTrapCommunityCmp.focus(true);
                        }, this)
                    );
                    return false;
                }

                snmpTrapHostCmp = Ext.getCmp('administration_snmp_trapHost');
                if (!snmpTrapHostCmp.isValid()) {
                    Ext.MessageBox.alert(i18n._('Warning'), i18n._("An Trap Host must be specified."),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelSnmp);
                            snmpTrapHostCmp.focus(true);
                        }, this)
                    );
                    return false;
                }

                snmpTrapPortCmp = Ext.getCmp('administration_snmp_trapPort');
                if (!snmpTrapPortCmp.isValid()) {
                    Ext.MessageBox.alert(i18n._('Warning'), Ext.String.format(i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelSnmp);
                            snmpTrapPortCmp.focus(true);
                        }, this)
                    );
                    return false;
                }
            }

            var v3EnabledCmp = Ext.getCmp('administration_snmp_v3enabled');
            var isV3Enabled = v3EnabledCmp.getValue();

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

            this.getSystemSettings().snmpSettings.v3Enabled = isV3Enabled;
            if( isV3Enabled ){
                this.getSystemSettings().snmpSettings.v3Required = Ext.getCmp('administration_snmp_v3required').getValue();
                this.getSystemSettings().snmpSettings.v3Username = Ext.getCmp('administration_snmp_v3username').getValue();
                this.getSystemSettings().snmpSettings.v3AuthenticationProtocol = Ext.getCmp('administration_snmp_v3authenticationProtocol').getValue();
                this.getSystemSettings().snmpSettings.v3AuthenticationPassphrase = Ext.getCmp('administration_snmp_v3authenticationPassphrase').getValue();
                this.getSystemSettings().snmpSettings.v3PrivacyProtocol = Ext.getCmp('administration_snmp_v3privacyProtocol').getValue();
                this.getSystemSettings().snmpSettings.v3PrivacyPassphrase = Ext.getCmp('administration_snmp_v3privacyPassphrase').getValue();
            }

        }
        return true;
    },
    validateCertificates: function() {
        var rows = this.gridCertList.getStore().getRange();
        for(var i = 0;i < rows.length;i++) {
            var record = this.gridCertList.getStore().getAt(i);
            if (record.data.httpsServer === true) this.getSystemSettings().webCertificate = record.data.fileName;
            if (record.data.smtpsServer === true) this.getSystemSettings().mailCertificate = record.data.fileName;
            if (record.data.ipsecServer === true) this.getSystemSettings().ipsecCertificate = record.data.fileName;
        }
        return true;
    },
    beforeSave: function(isApply, handler) {
        handler.call(this, isApply);
    },
    save: function(isApply) {
        this.saveSemaphore = 2;
        Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));

        this.getAdminSettings().users.list=this.gridAdminAccounts.getList();

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

                if(this.initialSkin != this.getSkinSettings().skinName) {
                    Ung.Util.goToStartPage();
                    return;
                }
                if(isApply) {
                    var vmess = this.panelCertificates.down('component[name="validateMessage"]');
                    vmess.update(Ung.Main.getCertificateManager().validateActiveInspectorCertificates());
                    this.getAdminSettings(true);
                    this.getRootCertificateInformation(true);
                    this.getServerCertificateList(true);
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
//# sourceURL=administration.js
