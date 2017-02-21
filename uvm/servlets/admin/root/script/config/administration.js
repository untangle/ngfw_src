Ext.define('Ung.config.administration.Administration', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.config.administration',
    requires: [
        'Ung.config.administration.AdministrationController',
        'Ung.config.administration.AdministrationModel',
        'Ung.store.Rule',
        'Ung.model.Rule',
        'Ung.cmp.Grid'
    ],
    controller: 'config.administration',
    viewModel: {
        type: 'config.administration'
    },
    dockedItems: [{
        xtype: 'toolbar',
        weight: -10,
        border: false,
        items: [{
            text: 'Back',
            iconCls: 'fa fa-arrow-circle-left fa-lg',
            hrefTarget: '_self',
            href: '#config'
        }, '-', {
            xtype: 'tbtext',
            html: '<strong>' + 'Administration'.t() + '</strong>'
        }],
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        border: false,
        items: ['->', {
            text: 'Apply Changes'.t(),
            scale: 'large',
            iconCls: 'fa fa-floppy-o fa-lg',
            handler: 'saveSettings'
        }]
    }],
    items: [
        { xtype: 'config.administration.admin' },
        { xtype: 'config.administration.certificates' },
        { xtype: 'config.administration.snmp' },
        { xtype: 'config.administration.skins' }
    ]
});
Ext.define('Ung.config.administration.AdministrationController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config.administration',
    control: {
        '#': {
            afterrender: 'loadAdmin',
            tabchange: 'onTabChange'
        },
        '#certificates': {
            beforerender: 'loadCertificates'
        }
    },
    countries: [
        [ 'US', 'United States'.t() ], [ 'AF', 'Afghanistan'.t() ], [ 'AL', 'Albania'.t() ], [ 'DZ', 'Algeria'.t() ],
        [ 'AS', 'American Samoa'.t() ], [ 'AD', 'Andorra'.t() ], [ 'AO', 'Angola'.t() ], [ 'AI', 'Anguilla'.t() ],
        [ 'AQ', 'Antarctica'.t() ], [ 'AG', 'Antigua and Barbuda'.t() ], [ 'AR', 'Argentina'.t() ], [ 'AM', 'Armenia'.t() ],
        [ 'AW', 'Aruba'.t() ], [ 'AU', 'Australia'.t() ], [ 'AT', 'Austria'.t() ], [ 'AZ', 'Azerbaijan'.t() ],
        [ 'BS', 'Bahamas'.t() ], [ 'BH', 'Bahrain'.t() ], [ 'BD', 'Bangladesh'.t() ], [ 'BB', 'Barbados'.t() ],
        [ 'BY', 'Belarus'.t() ], [ 'BE', 'Belgium'.t() ], [ 'BZ', 'Belize'.t() ], [ 'BJ', 'Benin'.t() ], [ 'BM', 'Bermuda'.t() ],
        [ 'BT', 'Bhutan'.t() ], [ 'BO', 'Bolivia'.t() ], [ 'BA', 'Bosnia and Herzegovina'.t() ], [ 'BW', 'Botswana'.t() ],
        [ 'BV', 'Bouvet Island'.t() ], [ 'BR', 'Brazil'.t() ], [ 'IO', 'British Indian Ocean Territory'.t() ],
        [ 'VG', 'British Virgin Islands'.t() ], [ 'BN', 'Brunei'.t() ], [ 'BG', 'Bulgaria'.t() ], [ 'BF', 'Burkina Faso'.t() ],
        [ 'BI', 'Burundi'.t() ], [ 'KH', 'Cambodia'.t() ], [ 'CM', 'Cameroon'.t() ], [ 'CA', 'Canada'.t() ],
        [ 'CV', 'Cape Verde'.t() ], [ 'KY', 'Cayman Islands'.t() ], [ 'CF', 'Central African Republic'.t() ],
        [ 'TD', 'Chad'.t() ], [ 'CL', 'Chile'.t() ], [ 'CN', 'China'.t() ], [ 'CX', 'Christmas Island'.t() ],
        [ 'CC', 'Cocos Islands'.t() ], [ 'CO', 'Colombia'.t() ], [ 'KM', 'Comoros'.t() ], [ 'CG', 'Congo - Brazzaville'.t() ],
        [ 'CK', 'Cook Islands'.t() ], [ 'CR', 'Costa Rica'.t() ], [ 'HR', 'Croatia'.t() ], [ 'CU', 'Cuba'.t() ],
        [ 'CY', 'Cyprus'.t() ], [ 'CZ', 'Czech Republic'.t() ], [ 'DK', 'Denmark'.t() ], [ 'DJ', 'Djibouti'.t() ],
        [ 'DM', 'Dominica'.t() ], [ 'DO', 'Dominican Republic'.t() ], [ 'EC', 'Ecuador'.t() ], [ 'EG', 'Egypt'.t() ],
        [ 'SV', 'El Salvador'.t() ], [ 'GQ', 'Equatorial Guinea'.t() ], [ 'ER', 'Eritrea'.t() ], [ 'EE', 'Estonia'.t() ],
        [ 'ET', 'Ethiopia'.t() ], [ 'FK', 'Falkland Islands'.t() ], [ 'FO', 'Faroe Islands'.t() ], [ 'FJ', 'Fiji'.t() ],
        [ 'FI', 'Finland'.t() ], [ 'FR', 'France'.t() ], [ 'GF', 'French Guiana'.t() ], [ 'PF', 'French Polynesia'.t() ],
        [ 'TF', 'French Southern Territories'.t() ], [ 'GA', 'Gabon'.t() ], [ 'GM', 'Gambia'.t() ], [ 'GE', 'Georgia'.t() ],
        [ 'DE', 'Germany'.t() ], [ 'GH', 'Ghana'.t() ], [ 'GI', 'Gibraltar'.t() ], [ 'GR', 'Greece'.t() ],
        [ 'GL', 'Greenland'.t() ], [ 'GD', 'Grenada'.t() ], [ 'GP', 'Guadeloupe'.t() ], [ 'GU', 'Guam'.t() ],
        [ 'GT', 'Guatemala'.t() ], [ 'GN', 'Guinea'.t() ], [ 'GW', 'Guinea-Bissau'.t() ], [ 'GY', 'Guyana'.t() ],
        [ 'HT', 'Haiti'.t() ], [ 'HM', 'Heard Island and McDonald Islands'.t() ], [ 'HN', 'Honduras'.t() ],
        [ 'HK', 'Hong Kong SAR China'.t() ], [ 'HU', 'Hungary'.t() ], [ 'IS', 'Iceland'.t() ], [ 'IN', 'India'.t() ],
        [ 'ID', 'Indonesia'.t() ], [ 'IR', 'Iran'.t() ], [ 'IQ', 'Iraq'.t() ], [ 'IE', 'Ireland'.t() ],
        [ 'IL', 'Israel'.t() ], [ 'IT', 'Italy'.t() ], [ 'CI', 'Ivory Coast'.t() ], [ 'JM', 'Jamaica'.t() ],
        [ 'JP', 'Japan'.t() ], [ 'JO', 'Jordan'.t() ], [ 'KZ', 'Kazakhstan'.t() ], [ 'KE', 'Kenya'.t() ],
        [ 'KI', 'Kiribati'.t() ], [ 'KW', 'Kuwait'.t() ], [ 'KG', 'Kyrgyzstan'.t() ], [ 'LA', 'Laos'.t() ],
        [ 'LV', 'Latvia'.t() ], [ 'LB', 'Lebanon'.t() ], [ 'LS', 'Lesotho'.t() ], [ 'LR', 'Liberia'.t() ],
        [ 'LY', 'Libya'.t() ], [ 'LI', 'Liechtenstein'.t() ], [ 'LT', 'Lithuania'.t() ], [ 'LU', 'Luxembourg'.t() ],
        [ 'MO', 'Macao SAR China'.t() ], [ 'MK', 'Macedonia'.t() ], [ 'MG', 'Madagascar'.t() ], [ 'MW', 'Malawi'.t() ],
        [ 'MY', 'Malaysia'.t() ], [ 'MV', 'Maldives'.t() ], [ 'ML', 'Mali'.t() ], [ 'MT', 'Malta'.t() ],
        [ 'MH', 'Marshall Islands'.t() ], [ 'MQ', 'Martinique'.t() ], [ 'MR', 'Mauritania'.t() ], [ 'MU', 'Mauritius'.t() ],
        [ 'YT', 'Mayotte'.t() ], [ 'FX', 'Metropolitan France'.t() ], [ 'MX', 'Mexico'.t() ], [ 'FM', 'Micronesia'.t() ],
        [ 'MD', 'Moldova'.t() ], [ 'MC', 'Monaco'.t() ], [ 'MN', 'Mongolia'.t() ], [ 'MS', 'Montserrat'.t() ],
        [ 'MA', 'Morocco'.t() ], [ 'MZ', 'Mozambique'.t() ], [ 'MM', 'Myanmar'.t() ], [ 'NA', 'Namibia'.t() ],
        [ 'NR', 'Nauru'.t() ], [ 'NP', 'Nepal'.t() ], [ 'NL', 'Netherlands'.t() ], [ 'AN', 'Netherlands Antilles'.t() ],
        [ 'NC', 'New Caledonia'.t() ], [ 'NZ', 'New Zealand'.t() ], [ 'NI', 'Nicaragua'.t() ], [ 'NE', 'Niger'.t() ],
        [ 'NG', 'Nigeria'.t() ], [ 'NU', 'Niue'.t() ], [ 'NF', 'Norfolk Island'.t() ], [ 'KP', 'North Korea'.t() ],
        [ 'MP', 'Northern Mariana Islands'.t() ], [ 'NO', 'Norway'.t() ], [ 'OM', 'Oman'.t() ], [ 'PK', 'Pakistan'.t() ],
        [ 'PW', 'Palau'.t() ], [ 'PA', 'Panama'.t() ], [ 'PG', 'Papua New Guinea'.t() ], [ 'PY', 'Paraguay'.t() ],
        [ 'PE', 'Peru'.t() ], [ 'PH', 'Philippines'.t() ], [ 'PN', 'Pitcairn'.t() ], [ 'PL', 'Poland'.t() ],
        [ 'PT', 'Portugal'.t() ], [ 'PR', 'Puerto Rico'.t() ], [ 'QA', 'Qatar'.t() ], [ 'RE', 'Reunion'.t() ],
        [ 'RO', 'Romania'.t() ], [ 'RU', 'Russia'.t() ], [ 'RW', 'Rwanda'.t() ], [ 'SH', 'Saint Helena'.t() ],
        [ 'KN', 'Saint Kitts and Nevis'.t() ], [ 'LC', 'Saint Lucia'.t() ], [ 'PM', 'Saint Pierre and Miquelon'.t() ],
        [ 'VC', 'Saint Vincent and the Grenadines'.t() ], [ 'WS', 'Samoa'.t() ], [ 'SM', 'San Marino'.t() ],
        [ 'ST', 'Sao Tome and Principe'.t() ], [ 'SA', 'Saudi Arabia'.t() ], [ 'SN', 'Senegal'.t() ], [ 'SC', 'Seychelles'.t() ],
        [ 'SL', 'Sierra Leone'.t() ], [ 'SG', 'Singapore'.t() ], [ 'SK', 'Slovakia'.t() ], [ 'SI', 'Slovenia'.t() ],
        [ 'SB', 'Solomon Islands'.t() ], [ 'SO', 'Somalia'.t() ], [ 'ZA', 'South Africa'.t() ],
        [ 'GS', 'South Georgia and the South Sandwich Islands'.t() ], [ 'KR', 'South Korea'.t() ], [ 'ES', 'Spain'.t() ],
        [ 'LK', 'Sri Lanka'.t() ], [ 'SD', 'Sudan'.t() ], [ 'SR', 'Suriname'.t() ], [ 'SJ', 'Svalbard and Jan Mayen'.t() ],
        [ 'SZ', 'Swaziland'.t() ], [ 'SE', 'Sweden'.t() ], [ 'CH', 'Switzerland'.t() ], [ 'SY', 'Syria'.t() ],
        [ 'TW', 'Taiwan'.t() ], [ 'TJ', 'Tajikistan'.t() ], [ 'TZ', 'Tanzania'.t() ], [ 'TH', 'Thailand'.t() ],
        [ 'TG', 'Togo'.t() ], [ 'TK', 'Tokelau'.t() ], [ 'TO', 'Tonga'.t() ], [ 'TT', 'Trinidad and Tobago'.t() ],
        [ 'TN', 'Tunisia'.t() ], [ 'TR', 'Turkey'.t() ], [ 'TM', 'Turkmenistan'.t() ], [ 'TC', 'Turks and Caicos Islands'.t() ],
        [ 'TV', 'Tuvalu'.t() ], [ 'VI', 'U.S. Virgin Islands'.t() ], [ 'UG', 'Uganda'.t() ], [ 'UA', 'Ukraine'.t() ],
        [ 'AE', 'United Arab Emirates'.t() ], [ 'GB', 'United Kingdom'.t() ], [ 'UM', 'United States Minor Outlying Islands'.t() ],
        [ 'UY', 'Uruguay'.t() ], [ 'UZ', 'Uzbekistan'.t() ], [ 'VU', 'Vanuatu'.t() ], [ 'VA', 'Vatican'.t() ],
        [ 'VE', 'Venezuela'.t() ], [ 'VN', 'Vietnam'.t() ], [ 'WF', 'Wallis and Futuna'.t() ], [ 'EH', 'Western Sahara'.t() ],
        [ 'YE', 'Yemen'.t() ], [ 'ZM', 'Zambia'.t() ], [ 'ZW', 'Zimbabwe'.t() ]
    ],
    onTabChange: function (tabPanel, newCard) {
        // window.location.hash = '#config/administration/' + newCard.getItemId();
        // Ung.app.redirectTo('#config/administration/' + newCard.getItemId(), false);
    },
    loadAdmin: function () {
        var v = this.getView(),
            vm = this.getViewModel();
        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.adminManager.getSettings'),
            Rpc.asyncPromise('rpc.systemManager.getSettings'),
            Rpc.asyncPromise('rpc.skinManager.getSkinsList'),
            Rpc.asyncPromise('rpc.skinManager.getSettings')
        ], this).then(function(result) {
            console.log(result);
            vm.set({
                adminSettings: result[0],
                systemSettings: result[1],
                skinsList: result[2],
                skinSettings: result[3]
            });
        }, function(ex) {
            console.error(ex);
            Util.exceptionToast(ex);
        }).always(function() {
            v.setLoading(false);
        });
    },
    loadCertificates: function () {
        var v = this.getView(),
            vm = this.getViewModel();
        v.setLoading(true);
        rpc.certificateManager = rpc.UvmContext.certificateManager();
        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.certificateManager.getServerCertificateList'),
            Rpc.asyncPromise('rpc.certificateManager.getRootCertificateInformation'),
            Rpc.asyncPromise('rpc.certificateManager.validateActiveInspectorCertificates'),
        ], this).then(function(result) {
            vm.set({
                serverCertificates: result[0],
                rootCertificateInformation: result[1],
                serverCertificateVerification: result[2],
            });
        }, function(ex) {
            console.error(ex);
            Util.exceptionToast(ex);
        }).always(function() {
            v.setLoading(false);
        });
    },
    saveSettings: function () {
        var me = this,
            view = this.getView(),
            vm = this.getViewModel();
        if (!Util.validateForms(view)) {
            return;
        }
        view.setLoading('Saving ...');
        view.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            /**
             * Important!
             * update custom grids only if are modified records or it was reordered via drag/drop
             */
            if (store.getModifiedRecords().length > 0 || store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
                // store.commitChanges();
            }
        });
        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.adminManager.setSettings', vm.get('adminSettings')),
            Rpc.asyncPromise('rpc.systemManager.setSettings', vm.get('systemSettings')),
            Rpc.asyncPromise('rpc.skinManager.setSettings', vm.get('skinSettings')),
        ], this).then(function() {
            me.loadAdmin();
            me.loadCertificates();
            window.location.reload();
            Util.successToast('Administration'.t() + ' settings saved!');
        }, function(ex) {
            console.error(ex);
            Util.exceptionToast(ex);
        }).always(function() {
            view.setLoading(false);
        });
    },
    generateCertificate: function (btn) {
        var me = this,
            certMode = btn.certMode,
            hostName = btn.hostName,
            netStatus, addressList, i;
        try {
            netStatus = rpc.networkManager.getInterfaceStatus();
        } catch (e) {
            Util.exceptionToast(e);
        }
        addressList = '';
        addressList += hostName;
        for (i = 0; i < netStatus.list.length; i++) {
            var netItem = netStatus.list[i];
            if (netItem.v4Address === null) { continue; }
            addressList += ',';
            addressList += netItem.v4Address;
        }
        Ext.create('Ext.Window', {
            title: btn.getText(),
            layout: 'fit',
            width: 600,
            autoShow: true,
            // height: (certMode === "ROOT" ? 320 : 360),
            height: 320,
            border: true,
            modal: true,
            items: [{
                xtype: 'form',
                layout: 'anchor',
                border: false,
                bodyPadding: 10,
                defaults: {
                    anchor: '100%',
                    labelWidth: 150,
                    labelAlign: 'right'
                    // listeners: {
                    //     render: helptipRenderer
                    // }
                },
                items: [{
                    xtype: 'combo',
                    fieldLabel: 'Country'.t() + ' (C)',
                    // helptip: 'Select the country in which your organization is legally registered."),
                    allowBlank: true,
                    store: me.countries,
                    queryMode: 'local',
                    editable: false
                }, {
                    xtype: 'textfield',
                    fieldLabel: 'State/Province'.t() + ' (ST)',
                    // name: "State',
                    // helptip: i18n._('Name of state, province, region, territory where your organization is located. Please enter the full name. Do not abbreviate.'),
                    allowBlank: false
                }, {
                    xtype: 'textfield',
                    fieldLabel: 'City/Locality'.t() + ' (L)',
                    // name: "Locality',
                    // helptip: i18n._('Name of the city/locality in which your organization is registered/located. Please spell out the name of the city/locality. Do not abbreviate.'),
                    allowBlank: false
                }, {
                    xtype: 'textfield',
                    fieldLabel: 'Organization'.t() + ' (O)',
                    // name: "Organization',
                    // helptip: 'The name under which your business is legally registered. The listed organization must be the legal registrant of the domain name in the certificate request. If you are enrolling as a small business/sole proprietor, please enter the certificate requester's name in the Organization field, and the DBA (doing business as) name in the Organizational Unit field."),
                    allowBlank: false
                }, {
                    xtype: 'textfield',
                    fieldLabel: 'Organizational Unit'.t() + ' (OU)',
                    // name: "OrganizationalUnit',
                    // helptip: 'Optional. Use this field to differentiate between divisions within an organization. If applicable, you may enter the DBA (doing business as) name in this field."),
                    allowBlank: true
                }, {
                    xtype: 'textfield',
                    fieldLabel: 'Common Name'.t() + ' (CN)',
                    // name: "CommonName',
                    // helptip: 'The name entered in the CN (common name) field MUST be the fully-qualified domain name of the website for which you will be using the certificate (example.com). Do not include the http:// or https:// prefixes in your common name. Do NOT enter your personal name in this field."),
                    allowBlank: false,
                    // value: hostName
                }, {
                    xtype: 'textfield',
                    fieldLabel: 'Subject Alternative Names'.t(),
                    // name: "AltNames',
                    // helptip: 'Optional. Use this field to enter a comma seperated list of one or more alternative host names or IP addresses that may be used to access the website for which you will be using the certificate."),
                    allowBlank: true,
                    value: (certMode === 'ROOT' ? '' : addressList),
                    hidden: certMode === 'ROOT'
                }],
                buttons: [{
                    xtype: 'button',
                    text: 'Generate'.t(),
                    name: 'Accept',
                    width: 120,
                    formBind: true,
                    handler: Ext.bind(function() {
                        // this.certGeneratorWorker(certMode);
                    }, this)
                },{
                    xtype: 'button',
                    text: 'Cancel'.t(),
                    name: 'Cancel',
                    width: 120,
                    handler: Ext.bind(function() {
                        // this.certGeneratorWindow.close();
                    }, this)
                }]
            }]
        });
    },
    // generateCertificate: function () {
    // },
    addAccount: function () {
        Ext.MessageBox.show({
            title: 'Administrator Warning'.t(),
            msg: 'This action will add an ADMINISTRATOR account.'.t() + '<br/>' + '<br/>' +
                '<b>' + 'ADMINISTRATORS (also sometimes known as admin or root or superuser) have ADMINISTRATOR access to the server.'.t() + '</b>' + '<br/>' + '<br/>' +
                'Administrator accounts have the ability to do anything including:'.t() + '<br/>' +
                '<ul>' +
                '<li>' + 'Read/Modify any setting'.t() + '</li>' +
                '<li>' + 'Restore/Backup all settings'.t() + '</li>' +
                '<li>' + 'Create more administrators'.t() + '</li>' +
                '<li>' + 'Delete/Modify/Create any file'.t() + '</li>' +
                '<li>' + 'Run any command'.t() + '</li>' +
                '<li>' + 'Install any software'.t() + '</li>' +
                '<li>' + 'Complete control and access identical to what you now possess'.t() + '</li>' +
                '</ul>' + '<br/>' +
                'Do you understand the above statement?'.t() + '<br/>' +
                '<input type="checkbox" id="admin_understand"/> <i>' + 'Yes, I understand.'.t() + '</i>' + '<br/>' +
                '<br/>' +
                'Do you wish to continue?'.t() + '<br/>',
            buttons: Ext.MessageBox.YESNO,
            fn: Ext.bind(function(btn) {
                if (btn === 'yes') {
                    // if (Ext.get('admin_understand').dom.checked) {
                    //     Ung.grid.Panel.prototype.addHandler.call(this, button, e, rowData);
                    // }
                }
            }, this)});
    }
});
Ext.define('Ung.config.administration.AdministrationModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.config.administration',
    data: {
        adminSettings: null,
        systemSettings: null,
        skinSettings: null,
        serverCertificates: null,
        rootCertificateInformation: null,
        serverCertificateVerification: null,
        skinsList: null
    },
    stores: {
        accounts: { data: '{adminSettings.users.list}' },
        certificates: { data: '{serverCertificates.list}' },
        skins: { data: '{skinsList.list}' }
    }
});
Ext.define('Ung.config.administration.view.Admin', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.administration.admin',
    itemId: 'admin',
    viewModel: true,
    title: 'Admin'.t(),
    layout: 'border',
    items: [{
        xtype: 'ungrid',
        // border: false,
        title: 'Admin Accounts'.t(),
        region: 'center',
        bind: '{accounts}',
        listProperty: 'adminSettings.users.list',
        tbar: ['@add'],
        recordActions: ['@delete'],
        emptyRow: {
            javaClass: 'com.untangle.uvm.AdminUserSettings',
            username: '',
            description: '',
            emailAddress: '',
            // "emailAlerts": true,
            // "emailSummaries": true,
            passwordHashBase64: null,
            passwordHashShadow: null,
            password: null
        },
        columns: [{
            header: 'Username'.t(),
            width: 150,
            dataIndex: 'username'
        }, {
            header: 'Description'.t(),
            flex: 1,
            dataIndex: 'description'
        }, {
            header: 'Email Address'.t(),
            width: 150,
            dataIndex: 'emailAddress'
        }, {
            header: 'Email Alerts'.t(),
            dataIndex: 'emailAlerts',
            align: 'center',
            renderer: function (value) {
                return value ? '<i class="fa fa-check"></i>' : '<i class="fa fa-minus"></i>';
            }
        }, {
            header: 'Email Summaries'.t(),
            dataIndex: 'emailSummaries',
            align: 'center',
            renderer: function (value) {
                return value ? '<i class="fa fa-check"></i>' : '<i class="fa fa-minus"></i>';
            }
        }, {
            xtype: 'actioncolumn',
            header: 'Change Password'.t(),
            align: 'center',
            width: 130,
            iconCls: 'fa fa-lock',
            handler: 'changePassword'
        }],
        editorFields: [{
            xtype: 'textfield',
            bind: '{record.username}',
            fieldLabel: 'Username'.t(),
            allowBlank: false,
            emptyText: '[enter username]'.t(),
            blankText: 'The username cannot be blank.'.t()
        },
        Fields.description, {
            xtype: 'textfield',
            bind: '{record.emailAddress}',
            fieldLabel: 'Email Address'.t(),
            emptyText: '[no email]'.t(),
            vtype: 'email'
        }, {
            xtype: 'textfield',
            inputType: 'password',
            bind: '{record.password}',
            fieldLabel: 'Password'.t(),
            allowBlank: false,
            minLength: 3,
            minLengthText: Ext.String.format('The password is shorter than the minimum {0} characters.'.t(), 3)
        }, {
            xtype: 'textfield',
            inputType: 'password',
            fieldLabel: 'Confirm Password'.t(),
            allowBlank: false
        }]
    }, {
        xtype: 'panel',
        region: 'south',
        height: 'auto',
        border: false,
        bodyPadding: 10,
        items: [{
            xtype: 'checkbox',
            fieldLabel: 'Allow HTTP Administration'.t(),
            labelAlign: 'right',
            labelWidth: 250,
            bind: '{systemSettings.httpAdministrationAllowed}'
        }, {
            xtype: 'textfield',
            fieldLabel: 'Default Administration Username Text'.t(),
            labelAlign: 'right',
            maxWidth: 400,
            labelWidth: 250,
            bind: '{adminSettings.defaultUsername}'
        }, {
            xtype: 'fieldset',
            title: 'Note:'.t(),
            padding: 10,
            items: [{
                xtype: 'label',
                html: 'HTTP is open on non-WANs (internal interfaces) for blockpages and other services.'.t() + '<br/>' +
                    'This settings only controls the availability of <b>administration</b> via HTTP.'.t()
            }]
        }]
    }]
});
Ext.define('Ung.config.administration.view.Certificates', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.administration.certificates',
    itemId: 'certificates',
    viewModel: true,
    title: 'Certificates'.t(),
    layout: 'border',
    items: [{
        title: 'Certificate Authority'.t(),
        region: 'center',
        bodyPadding: 10,
        defaults: {
            labelWidth: 150,
            labelAlign: 'right'
        },
        items: [{
            xtype: 'component',
            margin: '0 0 10 0',
            html: 'The Certificate Authority is used to create and sign SSL certificates used by several applications and services such as SSL Inspector and Captive Portal.  It can also be used to sign the internal web server certificate. To eliminate certificate security warnings on client computers and devices, you should download the root certificate and add it to the list of trusted authorities on each client connected to your network.'.t()
        }, {
            xtype: 'displayfield',
            fieldLabel: 'Valid starting'.t(),
            bind: '{rootCertificateInformation.dateValid.time}'
            // id: 'rootca_status_notBefore',
            // value: this.getRootCertificateInformation() == null ? "" : i18n.timestampFormat(this.getRootCertificateInformation().dateValid)
        },{
            xtype: 'displayfield',
            fieldLabel: 'Valid until',
            bind: '{rootCertificateInformation.dateExpires.time}'
            // id: 'rootca_status_notAfter',
            // value: this.getRootCertificateInformation() == null ? "" : i18n.timestampFormat(this.getRootCertificateInformation().dateExpires)
        },{
            xtype: 'displayfield',
            fieldLabel: 'Subject DN'.t(),
            bind: '{rootCertificateInformation.certSubject}'
            // id: 'rootca_status_subjectDN',
            // value: this.getRootCertificateInformation() == null ? "" : this.getRootCertificateInformation().certSubject
        }, {
            xtype: 'container',
            margin: '10 0 0 0',
            items: [{
                xtype: 'button',
                iconCls: 'fa fa-certificate',
                text: 'Generate Certificate Authority',
                certMode: 'ROOT',
                hostName: null,
                handler: 'generateCertificate'
            }, {
                xtype: 'component',
                style: { fontSize: '11px', color: '#999' },
                margin: '5 0 15 0',
                html: 'Click here to re-create the internal certificate authority.  Use this to change the information in the Subject DN of the root certificate.'.t()
            }, {
                xtype: 'button',
                iconCls: 'fa fa-download',
                text: 'Download Root Certificate'
            }, {
                xtype: 'component',
                style: { fontSize: '11px', color: '#999' },
                margin: '5 0 15 0',
                html: 'Click here to download the root certificate.  Installing this certificate on client devices will allow them to trust certificates generated by this server.'.t()
            }, {
                xtype: 'button',
                iconCls: 'fa fa-download',
                text: 'Download Root Certificate Installer'
            }, {
                xtype: 'component',
                style: { fontSize: '11px', color: '#999' },
                margin: '5 0 15 0',
                html: 'Click here to download the root certificate installer.  It installs the root certificate appropriately on a Windows device.'.t()
            }]
        }]
    }, {
        title: 'Server Certificates'.t(),
        region: 'south',
        height: '40%',
        split: true,
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'component',
            padding: 10,
            html: 'The Server Certificates list is used to select the SSL certificate to be used for each service provided by this server.  The <B>HTTPS</B> column selects the certificate used by the internal web server.  The <B>SMTPS</B> column selects the certificate to use for SMTP+STARTTLS when using SSL Inspector to scan inbound email.  The <B>IPSEC</B> column selects the certificate to use for the IPsec IKEv2 VPN server.'.t()
        }, {
            xtype: 'ungrid',
            flex: 1,
            bind: '{certificates}',
            listProperty: 'serverCertificates.list',
            recordActions: ['@delete'],
            bbar: [{
                text: 'Generate Server Certificate'.t(),
                handler: 'generateServerCert',
                iconCls: 'fa fa-certificate'
            }, {
                text: 'Upload Server Certificate'.t(),
                handler: 'generateServerCert',
                iconCls: 'fa fa-upload'
            }, {
                text: 'Create Certificate Signing Request'.t(),
                handler: 'generateServerCert',
                iconCls: 'fa fa-certificate'
            }],
            columns: [{
                header: 'Subject'.t(),
                dataIndex: 'certSubject',
                width: 220
            }, {
                header: 'Issued By'.t(),
                flex: 1,
                dataIndex: 'certIssuer'
            }, {
                header: 'Date Valid'.t(),
                width: 140,
                dataIndex: 'dateValid'
            }, {
                header: 'Date Expires'.t(),
                width: 140,
                dataIndex: 'dateExpires'
            }, {
                header: 'HTTPS'.t(),
                xtype: 'checkcolumn',
                width: 80,
                dataIndex: 'httpsServer'
            }, {
                header: 'SMTPS'.t(),
                xtype: 'checkcolumn',
                width: 80,
                dataIndex: 'smtpsServer'
            }, {
                header: 'IPSEC'.t(),
                xtype: 'checkcolumn',
                width: 80,
                dataIndex: 'ipsecServer'
            }]
        }]
    }, {
        region: 'east',
        title: 'Server Certificate Verification'.t(),
        width: 300,
        collapsible: true,
        animCollapse: false,
        titleCollapse: true,
        bodyPadding: 10,
        items: [{
            xtype: 'component',
            userCls: 'cert-verification',
            bind: '{serverCertificateVerification}'
        }]
    }]
});
Ext.define('Ung.config.administration.view.Skins', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.administration.skins',
    itemId: 'skins',
    viewModel: true,
    title: 'Skins'.t(),
    bodyPadding: 10,
    items: [{
        xtype: 'combo',
        width: 300,
        bind: {
            store: '{skins}',
            value: '{skinSettings.skinName}'
        },
        fieldLabel: '<strong>' + 'Administration Skin'.t() + '</strong>',
        labelAlign: 'top',
        displayField: 'displayName',
        valueField: 'name',
        forceSelection: true,
        editable: false,
        queryMode: 'local'
    }, {
        xtype: 'filefield',
        margin: '10 0 0 0',
        fieldLabel: '<strong>' + 'Upload New Skin'.t() + '</strong>',
        labelAlign: 'top',
        width: 300,
        allowBlank: false,
        validateOnBlur: false
    }, {
        xtype: 'button',
        margin: '5 0 0 0',
        text: 'Upload'.t(),
        iconCls: 'fa fa-upload',
        handler: Ext.bind(function() {
            Ext.Msg.alert('Wait...', 'Not implemented yet!');
            // this.panelSkins.onUpload();
        }, this)
    }]
});
Ext.define('Ung.config.administration.view.Snmp', {
    extend: 'Ext.form.Panel',
    alias: 'widget.config.administration.snmp',
    withValidation: true, // requires validation on save
    itemId: 'snmp',
    viewModel: {
        formulas: {
            snmpEnabled: {
                get: function (get) {
                    return get('systemSettings.snmpSettings.enabled');
                },
                set: function (value) {
                    this.set('systemSettings.snmpSettings.enabled', value);
                    if (!value) {
                        this.set('systemSettings.snmpSettings.sendTraps', value);
                        this.set('systemSettings.snmpSettings.v3Enabled', value);
                    }
                }
            },
            communityString: {
                get: function (get) {
                    var val = get('systemSettings.snmpSettings.communityString');
                    return  val === 'CHANGE_ME' ? 'CHANGE_ME'.t() : val;
                },
                set: function (value) {
                    this.set('systemSettings.snmpSettings.communityString', value);
                }
            },
            sysContact: {
                get: function (get) {
                    var val = get('systemSettings.snmpSettings.sysContact');
                    return  val === 'MY_CONTACT_INFO' ? 'MY_CONTACT_INFO'.t() : val;
                },
                set: function (value) {
                    this.set('systemSettings.snmpSettings.sysContact', value);
                }
            },
            sysLocation: {
                get: function (get) {
                    var val = get('systemSettings.snmpSettings.sysLocation');
                    return  val === 'MY_LOCATION' ? 'MY_LOCATION'.t() : val;
                },
                set: function (value) {
                    this.set('systemSettings.snmpSettings.sysLocation', value);
                }
            },
            trapCommunity: {
                get: function (get) {
                    var val = get('systemSettings.snmpSettings.trapCommunity');
                    return  val === 'MY_TRAP_COMMUNITY' ? 'MY_TRAP_COMMUNITY'.t() : val;
                },
                set: function (value) {
                    this.set('systemSettings.snmpSettings.trapCommunity', value);
                }
            }
        }
    },
    title: 'SNMP'.t(),
    scrollable: 'y',
    bodyPadding: 10,
    defaults: {
        xtype: 'fieldset',
        width: 500,
        layout: 'anchor',
        padding: 10,
        checkboxToggle: true,
        collapsible: true,
        collapsed: true,
        defaults: {
            xtype: 'textfield',
            anchor: '100%',
            labelWidth: 250,
            msgTarget: 'side'
        }
    },
    items: [{
        title: 'Enable SNMP Monitoring'.t(),
        checkbox: {
            bind: '{snmpEnabled}'
        },
        items: [{
            fieldLabel: 'Community'.t(),
            allowBlank: false,
            blankText: 'An SNMP Community must be specified.'.t(),
            bind: {
                value: '{communityString}',
                disabled: '{!snmpEnabled}'
            }
        }, {
            fieldLabel: 'System Contact'.t(),
            bind: {
                value: '{sysContact}',
                disabled: '{!snmpEnabled}'
            }
        }, {
            fieldLabel: 'System Location'.t(),
            bind: {
                value: '{sysLocation}',
                disabled: '{!snmpEnabled}'
            }
        }]
    }, {
        title: 'Enable Traps'.t(),
        checkbox: {
            bind: '{systemSettings.snmpSettings.sendTraps}'
        },
        disabled: true,
        bind: {
            disabled: '{!snmpEnabled}'
        },
        items: [{
            fieldLabel: 'Community'.t(),
            allowBlank: false,
            blankText: 'An Trap Community must be specified.'.t(),
            bind: {
                value: '{systemSettings.snmpSettings.trapCommunity}',
                disabled: '{!systemSettings.snmpSettings.sendTraps}'
            }
        }, {
            fieldLabel: 'Host'.t(),
            allowBlank: false,
            blankText: 'An Trap Host must be specified.'.t(),
            bind: {
                value: '{systemSettings.snmpSettings.trapHost}',
                disabled: '{!systemSettings.snmpSettings.sendTraps}'
            }
        }, {
            xtype: 'numberfield',
            fieldLabel: 'Port'.t(),
            allowDecimals: false,
            minValue: 0,
            allowBlank: false,
            blankText: 'You must provide a valid port.'.t(),
            vtype: 'port',
            bind: {
                value: '{systemSettings.snmpSettings.trapPort}',
                disabled: '{!systemSettings.snmpSettings.sendTraps}'
            }
        }]
    }, {
        title: 'Enable SNMP v3'.t(),
        checkbox: {
            bind: '{systemSettings.snmpSettings.v3Enabled}'
        },
        disabled: true,
        bind: {
            disabled: '{!snmpEnabled}'
        },
        items: [{
            fieldLabel: 'Username'.t(),
            allowBlank: false,
            blankText: 'Username must be specified.'.t(),
            bind: {
                value: '{systemSettings.snmpSettings.v3Username}',
                disabled: '{!systemSettings.snmpSettings.v3Enabled}'
            }
        }, {
            xtype: 'combo',
            fieldLabel: 'Authentication Protocol'.t(),
            store: [['sha', 'SHA'.t()], ['md5', 'MD5'.t()]],
            editable: false,
            queryMode: 'local',
            bind: {
                value: '{systemSettings.snmpSettings.v3AuthenticationProtocol}',
                disabled: '{!systemSettings.snmpSettings.v3Enabled}'
            }
        }, {
            fieldLabel: 'Authentication Passphrase'.t(),
            inputType: 'password',
            allowBlank: false,
            blankText: 'Authentication Passphrase must be specified.'.t(),
            bind: {
                value: '{systemSettings.snmpSettings.v3AuthenticationPassphrase}',
                disabled: '{!systemSettings.snmpSettings.v3Enabled}'
            }
            // validator: passwordValidator,
        }, {
            fieldLabel: 'Confirm Authentication Passphrase'.t(),
            inputType: 'password',
            allowBlank: false,
            blankText: 'Confirm Authentication Passphrase must be specified.'.t(),
            bind: {
                disabled: '{!systemSettings.snmpSettings.v3Enabled}'
            }
            // validator: passwordValidator,
        }, {
            xtype: 'combo',
            fieldLabel: 'Privacy Protocol'.t(),
            store: [['des', 'DES'.t()], ['aes', 'AES'.t()]],
            editable: false,
            queryMode: 'local',
            bind: {
                value: '{systemSettings.snmpSettings.v3PrivacyProtocol}',
                disabled: '{!systemSettings.snmpSettings.v3Enabled}'
            }
        }, {
            fieldLabel: 'Privacy Passphrase'.t(),
            inputType: 'password',
            allowBlank: false,
            blankText: 'Privacy Passphrase must be specified.'.t(),
            bind: {
                value: '{systemSettings.snmpSettings.v3PrivacyPassphrase}',
                disabled: '{!systemSettings.snmpSettings.v3Enabled}'
            }
            // validator: passwordValidator,
        }, {
            fieldLabel: 'Confirm Privacy Passphrase'.t(),
            inputType: 'password',
            allowBlank: false,
            blankText: 'Confirm Privacy Passphrase must be specified.'.t(),
            bind: {
                disabled: '{!systemSettings.snmpSettings.v3Enabled}'
            }
            // validator: passwordValidator,
        }, {
            xtype: 'checkbox',
            fieldLabel: 'Require only SNMP v3'.t(),
            bind: {
                value: '{systemSettings.snmpSettings.v3Required}',
                disabled: '{!systemSettings.snmpSettings.v3Enabled}'
            }
        }]
    }]
});