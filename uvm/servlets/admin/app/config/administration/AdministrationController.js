Ext.define('Ung.config.administration.AdministrationController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config.administration',

    control: {
        '#': {
            beforerender: 'loadAdmin',
            tabchange: 'onTabChange'
        },
        '#certificates': {
            beforerender: 'loadCertificates'
        },
        '#skins': {
            beforerender: 'loadSkins'
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

    certificateManager: rpc.UvmContext.certificateManager(),

    loadAdmin: function (view) {
        this.adminSettings();
        this.systemSettings();
        this.skinSettings();
    },

    loadCertificates: function (view) {
        this.serverCertificates();
        this.rootCertificateInformation();
        this.serverCertificateVerification();
    },

    loadSkins: function () {
        this.skinsList();
    },

    adminSettings: function () {
        var me = this;
        rpc.adminManager.getSettings(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('adminSettings', result);
        });
    },

    systemSettings: function () {
        var me = this;
        rpc.systemManager.getSettings(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('systemSettings', result);
        });
    },

    serverCertificates: function () {
        var me = this;
        this.certificateManager.getServerCertificateList(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('serverCertificates', result);
        });
    },

    rootCertificateInformation: function () {
        var me = this;
        this.certificateManager.getRootCertificateInformation(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('rootCertificateInformation', result);
        });
    },

    serverCertificateVerification: function () {
        var me = this;
        this.certificateManager.validateActiveInspectorCertificates(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('serverCertificateVerification', result);
        });
    },

    skinSettings: function () {
        var me = this;
        rpc.skinManager.getSettings(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('skinSettings', result);
        });
    },

    skinsList: function () {
        var me = this;
        rpc.skinManager.getSkinsList(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('skinsList', result);
        });
    },

    saveSettings: function () {
        var me = this,
            view = this.getView(),
            vm = this.getViewModel();

        if (!Ung.Util.validateForms(view)) {
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
            this.setAdminSettings,
            this.setSkinSettings,
            this.setSystemSettings
        ], this).then(function () {
            view.setLoading(false);

            me.loadAdmin(); me.loadCertificates(); me.loadSkins();

            Ung.Util.successToast('Administration'.t() + ' settings saved!');
        }, function (ex) {
            view.setLoading(false);
            console.error(ex);
            Ung.Util.exceptionToast(ex);
        });
    },

    setAdminSettings: function () {
        var me = this,
            deferred = new Ext.Deferred();
        rpc.adminManager.setSettings(function(result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        }, me.getViewModel().get('adminSettings'));
         return deferred.promise;
    },

    setSkinSettings: function () {
        var me = this,
            deferred = new Ext.Deferred();
        rpc.skinManager.setSettings(function(result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        }, me.getViewModel().get('skinSettings'));
        return deferred.promise;
    },

    setSystemSettings: function () {
        var me = this,
            deferred = new Ext.Deferred();
        console.log(me.getViewModel().get('systemSettings'));
        rpc.systemManager.setSettings(function(result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        }, me.getViewModel().get('systemSettings'));
        return deferred.promise;
    },


    generateCertificate: function (btn) {
        var me = this,
            certMode = btn.certMode,
            hostName = btn.hostName,
            netStatus, addressList, i;

        try {
            netStatus = rpc.networkManager.getInterfaceStatus();
        } catch (e) {
            Ung.Util.exceptionToast(e);
        }

        addressList = "";
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