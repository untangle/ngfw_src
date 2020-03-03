Ext.define('Ung.config.administration.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config-administration',

    control: {
        '#': {
            afterrender: 'loadAdmin'
        },
        '#certificates': {
            beforerender: 'loadCertificates'
        }
    },

    refreshGoogleTask: null,

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

    loadAdmin: function () {
        var me = this, v = me.getView(),vm = me.getViewModel();

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.adminManager.getSettings'),
            Rpc.asyncPromise('rpc.systemManager.getSettings'),
            Rpc.asyncPromise('rpc.skinManager.getSkinsList'),
            Rpc.asyncPromise('rpc.skinManager.getSettings')
        ], this)
        .then(function(result) {
            if(Util.isDestroyed(v, vm)){
                return;
            }
            vm.set({
                adminSettings: result[0],
                systemSettings: result[1],
                skinsList: result[2],
                skinSettings: result[3]
            });

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
        });

        var googleDrive = new Ung.cmp.GoogleDrive();
        vm.set( 'googleDriveIsConfigured', googleDrive.isConfigured() );
        vm.set( 'googleDriveConfigure', function(){ googleDrive.configure(vm.get('policyId')); });

        me.googleRefreshTaskBuild();
    },

    loadCertificates: function () {
        var me = this, v = me.getView(), vm = me.getViewModel();

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.UvmContext.certificateManager.getServerCertificateList'),
            Rpc.asyncPromise('rpc.UvmContext.certificateManager.getRootCertificateInformation'),
            Rpc.asyncPromise('rpc.UvmContext.certificateManager.validateActiveInspectorCertificates'),
            Rpc.asyncPromise('rpc.networkManager.getNetworkSettings'),
        ], this)
        .then(function(result) {
            if(Util.isDestroyed(v, vm)){
                return;
            }
            var hostname = result[3].hostName + (result[3].domainName ? '.' + result[3].domainName : '');
            vm.set({
                serverCertificates: result[0],
                rootCertificateInformation: result[1],
                serverCertificateVerification: result[2],
                hostName: hostname
            });
            vm.set('panel.saveDisabled', false);
            v.setLoading(false);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
        });
    },

    refreshRootCertificate: function () {
        var me = this, v = me.getView().down('#rootCertificateView'), vm = me.getViewModel();

        v.setLoading(true);
        Rpc.asyncData('rpc.UvmContext.certificateManager.getRootCertificateInformation')
        .then(function (result) {
            if(Util.isDestroyed(v, vm)){
                return;
            }
            vm.set('rootCertificateInformation', result);
            v.setLoading(false);
        }, function (ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
        });
    },

    refreshServerCertificate: function () {
        var me = this, v = me.getView().down('#serverCertificateView'), vm = me.getViewModel();

        v.setLoading(true);
        Rpc.asyncData('rpc.UvmContext.certificateManager.getServerCertificateList')
        .then(function (result) {
            if(Util.isDestroyed(v, vm)){
                return;
            }

            vm.set('serverCertificates', result);
            v.setLoading(false);
        }, function (ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
        });
    },

    saveSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

        var subnets = v.down('textfield[name="administrationSubnets"]');
        if (subnets.rendered && !subnets.isValid()) {
            Ung.app.redirectTo('#config/administration/admin');
            Ext.MessageBox.alert('Warning'.t(), 'Invalid subnet.'.t());
            subnets.focus(true);
            return;
        }

        v.setLoading(true);

        v.query('ungrid').forEach(function (grid) {
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
            }
        });

        // set certificates
        v.down('#serverCertificateView').getStore().each(function (cert) {
            if (cert.get('httpsServer')) { vm.set('systemSettings.webCertificate', cert.get('fileName')); }
            if (cert.get('smtpsServer')) { vm.set('systemSettings.mailCertificate', cert.get('fileName')); }
            if (cert.get('ipsecServer')) { vm.set('systemSettings.ipsecCertificate', cert.get('fileName')); }
        });

        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.adminManager.setSettings', vm.get('adminSettings')),
            Rpc.asyncPromise('rpc.skinManager.setSettings', vm.get('skinSettings')),
            Rpc.asyncPromise('rpc.systemManager.setSettings', vm.get('systemSettings'))
        ], this)
        .then(function() {
            // add 3 seconds timeout to avoid exception
            setTimeout(function () {
                if(Util.isDestroyed(me, v)){
                    return;
                }
                me.loadAdmin();
                me.loadCertificates();
                Util.successToast('Administration'.t() + ' settings saved!');

                if(vm.get('skinChanged') == true){
                    window.location.reload();
                }

                Ext.fireEvent('resetfields', v);
                v.setLoading(false);
            }, 3000);
        }, function (ex) {
            if(!Util.isDestroyed(vm, v)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
        });
    },

    generateCertificate: function (btn) {
        var me = this,
            certMode = btn.certMode,
            hostName = btn.certMode === 'ROOT' ? null : this.getViewModel().get('hostName'),
            netStatus, addressList, i;

        try {
            netStatus = Rpc.directData('rpc.networkManager.getInterfaceStatus');
        } catch (e) {
            Util.handleException(e);
            return;
        }

        addressList = '';
        addressList += hostName;

        for (i = 0; i < netStatus.list.length; i++) {
            var netItem = netStatus.list[i];
            if (netItem.v4Address === null) { continue; }
            addressList += ',';
            addressList += netItem.v4Address;
        }

        this.certDialog = this.getView().add({
            xtype: 'window',
            title: btn.getText(),
            certMode: certMode,
            layout: 'fit',
            width: Math.min(Renderer.calculateWith(1), 600),
            autoShow: true,
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
                    labelWidth: 170,
                },
                items: [{
                    xtype: 'textfield',
                    fieldLabel: '* ' + 'Common Name'.t() + ' (CN)',
                    name: 'commonName',
                    // helptip: 'The name entered in the CN (common name) field MUST be the fully-qualified domain name of the website for which you will be using the certificate (example.com). Do not include the http:// or https:// prefixes in your common name. Do NOT enter your personal name in this field."),
                    allowBlank: false,
                    value: hostName
                }, {
                    xtype: 'combo',
                    fieldLabel: '* ' + 'Country'.t() + ' (C)',
                    name: 'country',
                    // helptip: 'Select the country in which your organization is legally registered."),
                    allowBlank: true,
                    store: me.countries,
                    queryMode: 'local',
                    editable: false
                }, {
                    xtype: 'textfield',
                    fieldLabel: '* ' + 'State/Province'.t() + ' (ST)',
                    name: 'state',
                    // helptip: i18n._('Name of state, province, region, territory where your organization is located. Please enter the full name. Do not abbreviate.'),
                    allowBlank: false
                }, {
                    xtype: 'textfield',
                    fieldLabel: '* ' + 'City/Locality'.t() + ' (L)',
                    name: 'locality',
                    // helptip: i18n._('Name of the city/locality in which your organization is registered/located. Please spell out the name of the city/locality. Do not abbreviate.'),
                    allowBlank: false
                }, {
                    xtype: 'textfield',
                    fieldLabel: '* ' + 'Organization'.t() + ' (O)',
                    name: 'organization',
                    // helptip: 'The name under which your business is legally registered. The listed organization must be the legal registrant of the domain name in the certificate request. If you are enrolling as a small business/sole proprietor, please enter the certificate requester's name in the Organization field, and the DBA (doing business as) name in the Organizational Unit field."),
                    allowBlank: false
                }, {
                    xtype: 'textfield',
                    fieldLabel: 'Organizational Unit'.t() + ' (OU)',
                    name: 'organizationalUnit',
                    // helptip: 'Optional. Use this field to differentiate between divisions within an organization. If applicable, you may enter the DBA (doing business as) name in this field."),
                    allowBlank: true
                }, {
                    xtype: 'textfield',
                    fieldLabel: 'Subject Alternative Names'.t(),
                    name: 'altNames',
                    // helptip: 'Optional. Use this field to enter a comma seperated list of one or more alternative host names or IP addresses that may be used to access the website for which you will be using the certificate."),
                    allowBlank: true,
                    value: (certMode === 'ROOT' ? '' : addressList),
                    hidden: certMode === 'ROOT',
                    disabled: certMode === 'ROOT'
                }],
                buttons: [{
                    xtype: 'button',
                    text: 'Cancel'.t(),
                    name: 'Cancel',
                    width: 120,
                    handler: function () {
                        me.certDialog.close();
                    }
                }, {
                    xtype: 'button',
                    text: 'Generate'.t(),
                    name: 'Accept',
                    width: 120,
                    formBind: true,
                    handler: 'certGenerator'
                }]
            }]
        });
        this.certDialog.show();
    },

    downloadRootCertificate: function () {
        var downloadForm = document.getElementById('downloadForm');
        downloadForm.type.value = 'root_certificate_download';
        downloadForm.submit();
    },
    downloadRootCertificateInstaller: function () {
        var downloadForm = document.getElementById('downloadForm');
        downloadForm.type.value = 'root_certificate_installer_download';
        downloadForm.submit();
    },
    certGenerator: function () {
        var me = this,
            form = this.certDialog.down('form'),
            certMode = this.certDialog.certMode,
            values = form.getValues(),
            altNames = values.altNames;

        var certSubject = [
            '/CN=' + values.commonName,
            '/C='  + values.country,
            '/ST='  + values.state,
            '/L='  + values.locality,
            '/O='  + values.organization,
        ];
        if (values.organizationalUnit.length > 0) {
            certSubject.push('/OU=' + values.organizationalUnit);
        }
        certSubject = certSubject.join('');

        if ( (altNames != null)  && (altNames.length > 0) ) {
            var hostnameRegex = /^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$/;
            // Parse subject alt name list. For IP's prefix with both DNS: and IP:, for hostnames prefix with DNS:, otherwise is left unchanged
            var altNameTokens = altNames.split(',');
            var altNamesArray=[];

            for (var i=0; i < altNameTokens.length; i++) {
                var altName = altNameTokens[i].trim();
                if (Ext.form.VTypes.ipAddress(altName)) {
                    altName = 'IP:' + altName + ',DNS:' + altName;
                } else if (hostnameRegex.test(altName)) {
                    altName = 'DNS:' + altName;
                }
                altNamesArray.push(altName);
            }
            altNames = altNamesArray.join(',');
        }

        if (certMode === 'ROOT') {
            me.certDialog.setLoading(true);
            Rpc.asyncData('rpc.UvmContext.certificateManager.generateCertificateAuthority', certSubject, altNames)
            .then(function (result) {
                if(Util.isDestroyed(me)){
                    return;
                }
                Util.successToast('Certificate Authority generation successfully completed.'.t());
                me.certDialog.close();
                me.refreshRootCertificate();
                me.certDialog.setLoading(false);
            }, function (ex) {
                Util.handleException('Error during Certificate Authority generation.  Click OK to continue.'.t());
                if(!Util.isDestroyed(me)){
                    me.certDialog.setLoading(false);
                    return;
                }
            });
        }

        if (certMode === 'SERVER') {
            me.certDialog.setLoading(true);
            Rpc.asyncData('rpc.UvmContext.certificateManager.generateServerCertificate', certSubject, altNames)
                .then(function (result) {
                    if(Util.isDestroyed(me)){
                        return;
                    }
                    if (result === false) {
                        Util.handleException('Certificate generation failed. Please confirm the information provided is valid and try again.'.t());
                        return;
                    }
                    me.certDialog.close();
                    me.refreshServerCertificate();
                }, function (ex) {
                    Util.handleException('Error during certificate generation.'.t());
                }).always(function () {
                    me.certDialog.setLoading(false);
                });
        }

        if (certMode === 'CSR') {
            var downloadForm = document.getElementById('downloadForm');
            downloadForm.type.value = 'certificate_request_download';
            downloadForm.arg1.value = certSubject;
            downloadForm.arg2.value = altNames;
            downloadForm.submit();
            this.certDialog.close();
            return;
        }

    },

    uploadServerCertificate: function () {
        var me = this, v = this.getView();
        this.uploadDialog = v.add({
            xtype: 'window',
            modal: true,
            title: 'Upload Server Certificate'.t(),
            items: [{
                xtype: 'form',
                name: 'upload_form',
                border: false,
                width: Math.min(Renderer.calculateWith(1), 800),
                layout: 'anchor',
                items: [{
                    xtype: 'textarea',
                    id: 'cert_data',
                    fieldLabel: 'Server Certificate'.t(),
                    labelWidth: 80,
                    anchor: "100%",
                    height: 150,
                    margin: 10,
                }, {
                    xtype: 'textarea',
                    id: 'key_data',
                    fieldLabel: 'Certificate Key'.t(),
                    labelWidth: 80,
                    anchor: "100%",
                    height: 150,
                    margin: 10,
                }, {
                    xtype: 'textarea',
                    id: 'extra_data',
                    fieldLabel: 'Optional Intermediate Certificates'.t(),
                    labelWidth: 80,
                    anchor: "100%",
                    height: 200,
                    margin: 10,
                }, {
                    xtype: 'filefield',
                    anchor: '100%',
                    name: 'filename',
                    margin: 10,
                    labelWidth: 50,
                    allowBlank: false,
                    validateOnBlur: false,
                    buttonOnly: true,
                    buttonText: 'Import a certificate or key file'.t(),
                    listeners: {
                        change: 'handleFileImport'
                    }
                }, {
                    xtype: 'hidden',
                    name: 'type',
                    value: 'server_cert'
                }, {
                    xtype: 'hidden',
                    name: 'argument',
                    value: 'upload_server'
                }],
                buttons: [{
                    text: 'Upload Certificate'.t(),
                    formBind: false,
                    handler: function() {
                        var cd = Ext.get('cert_data');
                        var kd = Ext.get('key_data');
                        var ed = Ext.get('extra_data');
                        Rpc.asyncData('rpc.UvmContext.certificateManager.uploadServerCertificate', cd.component.getValue(), kd.component.getValue(), ed.component.getValue())
                        .then(function(status){
                            if (status.result === 0) {
                                Ext.MessageBox.alert('Certificate Upload Success'.t(), status.output);
                                me.uploadDialog.close();
                                me.refreshServerCertificate();
                            } else {
                                Ext.MessageBox.alert('Certificate Upload Error'.t(), status.output);
                            }
                        });
                    }
                }, {
                    text: 'Clear Form'.t(),
                    handler: function() {
                        cd = Ext.get('cert_data');
                        kd = Ext.get('key_data');
                        ed = Ext.get('extra_data');
                        cd.component.setValue("");
                        kd.component.setValue("");
                        ed.component.setValue("");
                    }
                }, {
                    text: 'Close'.t(),
                    handler: function () {
                        me.uploadDialog.close();
                    }
                }]
            }]
        });
        this.uploadDialog.show();
    },

    importSignedRequest: function () {
        var me = this, v = this.getView();
        this.uploadDialog = v.add({
            xtype: 'window',
            modal: true,
            title: 'Import Signing Request Certificate'.t(),
            items: [{
                xtype: 'form',
                name: 'upload_form',
                border: false,
                width: Math.min(Renderer.calculateWith(1), 800),
                layout: 'anchor',
                items: [{
                    xtype: 'textarea',
                    id: 'cert_data',
                    fieldLabel: 'Server Certificate'.t(),
                    labelWidth: 80,
                    anchor: "100%",
                    height: 150,
                    margin: 10,
                }, {
                    xtype: 'textarea',
                    id: 'extra_data',
                    fieldLabel: 'Optional Intermediate Certificates'.t(),
                    labelWidth: 80,
                    anchor: "100%",
                    height: 200,
                    margin: 10,
                }, {
                    xtype: 'filefield',
                    anchor: '100%',
                    name: 'filename',
                    margin: 10,
                    labelWidth: 50,
                    allowBlank: false,
                    validateOnBlur: false,
                    buttonOnly: true,
                    buttonText: 'Import a certificate file'.t(),
                    listeners: {
                        change: 'handleFileImport'
                    }
                }, {
                    xtype: 'hidden',
                    name: 'type',
                    value: 'server_cert'
                }, {
                    xtype: 'hidden',
                    name: 'argument',
                    value: 'import_signed'
                }],
                buttons: [{
                    text: 'Upload Certificate'.t(),
                    formBind: false,
                    handler: function() {
                        cd = Ext.get('cert_data');
                        ed = Ext.get('extra_data');
                        Rpc.asyncData('rpc.UvmContext.certificateManager.importSignedRequest', cd.component.getValue(), ed.component.getValue())
                        .then(function(status){
                            if (status.result === 0) {
                                Ext.MessageBox.alert('Certificate Upload Success'.t(), status.output);
                                me.refreshServerCertificate();
                                me.uploadDialog.close();
                            } else {
                                Ext.MessageBox.alert('Certificate Upload Error'.t(), status.output);
                            }
                        });
                    }
                }, {
                    text: 'Clear Form'.t(),
                    handler: function() {
                        cd = Ext.get('cert_data');
                        ed = Ext.get('extra_data');
                        cd.component.setValue("");
                        ed.component.setValue("");
                    }
                }, {
                    text: 'Close'.t(),
                    handler: function () {
                        me.uploadDialog.close();
                    }
                }]
            }]
        });
        this.uploadDialog.show();
    },

    deleteServerCert: function (view, rowIndex, colIndex, item, e, record) {
        var me = this;
        if (record.get('fileName') === 'apache.pem') {
            Ext.MessageBox.alert('System Certificate'.t(), 'This is the default system certificate and cannot be removed.'.t());
            return;
        }
        if (record.get('httpsServer') || record.get('smtpsServer') || record.get('ipsecServer')) {
            Ext.MessageBox.alert('Certificate In Use'.t(), 'You can not delete a certificate that is assigned to one or more services.'.t());
            return;
        }
        Ext.MessageBox.confirm('Are you sure you want to delete this certificate?'.t(),
            '<strong>SUBJECT:</strong> ' + record.get('certSubject') + '<br/><br/><strong>ISSUER:</strong> ' + record.get('certIssuer'),
            function(button) {
                if (button === 'yes') {
                    if(Util.isDestroyed(record)){
                        return;
                    }
                    Rpc.asyncData('rpc.UvmContext.certificateManager.removeServerCertificate', record.get('fileName'))
                    .then(function (result) {
                        if(Util.isDestroyed(me)){
                            return;
                        }
                        me.refreshServerCertificate();
                        Util.successToast('Certificate removed'.t());
                    });
                }
            });
    },

    skinChange: function(combo, newValue, oldValue){
        var me = this,
            vm = me.getViewModel();

        if( ( oldValue != null ) &&
            ( newValue != oldValue ) ){
            vm.set('skinChanged', true);
        }
    },

    /**
     * The handleFileImport function is used to let the user build a certificate to be uploaded
     * one piece at a time. When importing a cert or key file, the Java code passes the file to
     * the ut-cert-parser script. The script parses the file into certData, keyData, and extraData,
     * which are passed back to us as a JSON object. The object will only contain fields that
     * were actually parsed, so any or all three may be missing from the object we receive.
     * If we find certData it always goes into the cert_data textarea since the parser script
     * always puts the end-entity certificate there. If we find keyData it always goes into the
     * key_data textarea, and extraData is always appended to the extra_data textarea allowing
     * multiple intermediate certificates to be easily added. This function is used for both
     * uploadServerCertificate and importSignedRequest, the second of which doesn't have the
     * key_data textarea so we check to be sure the component exists before setting the value.
     */

    handleFileImport: function(cmp){
        var form = Ext.ComponentQuery.query('form[name=upload_form]')[0];
        var file = Ext.ComponentQuery.query('textfield[name=filename]')[0].value;
        if ( file == null || file.length === 0 ) {
            Ext.MessageBox.alert('Select File'.t(), 'Please choose a file to import.'.t());
            return;
            }
        form.submit({
            url: "upload",
            success: Ext.bind(function( form, action ) {
                var detail = JSON.parse(action.result.msg);
                var cptr = Ext.get('cert_data');
                var kptr = Ext.get('key_data');
                var eptr = Ext.get('extra_data');
                if (detail.certData) {
                    if (cptr) {
                        cptr.component.setValue(detail.certData);
                    }
                }
                if (detail.keyData) {
                    if (kptr) {
                        kptr.component.setValue(detail.keyData);
                    }
                }
                if (detail.extraData) {
                    if (eptr) {
                        var work = eptr.component.getValue();
                        eptr.component.setValue(work + detail.extraData);
                    }
                }
            }, this),
            failure: Ext.bind(function( form, action ) {
                Ext.MessageBox.alert('Import Failure'.t(), action.result.msg);
            }, this)
        });
    },

    googleRefreshTaskBuild: function() {
        var me = this;

        if(me.refreshGoogleTask != null){
            return;
        }

        me.refreshGoogleTask = {
            // update interval in millisecond
            updateFrequency: 3000,
            count:0,
            maxTries: 40,
            started: false,
            intervalId: null,
            app: me,
            start: function() {
                this.stop();
                this.count=0;
                this.intervalId = window.setInterval(this.run, this.updateFrequency);
                this.started = true;
            },
            stop: function() {
                if (this.intervalId !== null) {
                    window.clearInterval(this.intervalId);
                    this.intervalId = null;
                }
                this.started = false;
            },
            run: Ext.bind(function () {
                var me = this, v = this.getView();
                if(!me || !v.rendered) {
                    return;
                }
                if(Util.isDestroyed(me, v)){
                    return;
                }
                me.refreshGoogleTask.count++;

                if ( me.refreshGoogleTask.count > me.refreshGoogleTask.maxTries ) {
                    me.refreshGoogleTask.stop();
                    return;
                }

                Rpc.asyncData('rpc.UvmContext.googleManager.isGoogleDriveConnected')
                .then(function(result){
                    if(Util.isDestroyed(me, v)){
                        return;
                    }
                    var isConnected = result;

                    v.down('[name=fieldsetDriveEnabled]').setVisible(isConnected);
                    v.down('[name=fieldsetDriveDisabled]').setVisible(!isConnected);

                    if ( isConnected ){
                        me.refreshGoogleTask.stop();
                        return;
                    }
                }, function(ex){
                    Util.handleException(ex);
                });

            },this)
        };
    },

    googleDriveConfigure: function(){
        this.refreshGoogleTask.start();
        window.open(Rpc.directData('rpc.UvmContext.googleManager.getAuthorizationUrl', window.location.protocol, window.location.host));
    },

    googleDriveDisconnect: function(){
        var me = this, v = this.getView(), vm = this.getViewModel();
        Rpc.directData('rpc.UvmContext.googleManager.disconnectGoogleDrive');
        me.refreshGoogleTask.run();
        vm.set('settings.googleSettings.authenticationEnabled', false);
    }
});

Ext.define('Ung.cmp.AdminGridController', {
    extend: 'Ung.cmp.GridController',

    alias: 'controller.unadmingrid',

    addRecord: function () {
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
                    if (Ext.get('admin_understand').dom.checked) {
                        this.editorWin(null);
                    }
                }
            }, this)});
    },

});
