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
        rpc.systemManager.setSettings(function(result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        }, me.getViewModel().get('systemSettings'));
        return deferred.promise;
    },








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
                if (btn == "yes") {
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

    actions: {
        addAccount: { text: 'Add Account'.t(), iconCls: 'fa fa-plus', handler: 'addAccount' },
    },

    items: [{
        xtype: 'ungrid',
        border: false,
        title: 'Admin Accounts'.t(),
        region: 'center',

        // tbar: ['@addAccount'],
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
                text: 'Generate Certificate Authority'
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
        xtype: 'ungrid',
        region: 'south',
        height: '40%',
        split: true,
        tbar: [{
            xtype: 'tbtext',
            html: '<p>The Server Certificates list is used to select the SSL certificate to be used for each service provided by this server.The <B>HTTPS</B> column selects the certificate used by the internal web server.  The <B>SMTPS</B> column selects the certificate to use for SMTP+STARTTLS when using SSL Inspector to scan inbound email.  The <B>IPSEC</B> column selects the certificate to use for the IPsec IKEv2 VPN server.'.t()
        }],

        bind: '{certificates}',

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
            dataIndex: 'certSubject'
        }, {
            header: 'Issued By'.t(),
            dataIndex: 'certIssuer'
        }, {
            header: 'Date Valid'.t(),
            dataIndex: 'dateValid'
        }, {
            header: 'Date Expires'.t(),
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
        fieldLabel: 'Administration Skin'.t(),
        labelAlign: 'top',
        displayField: 'displayName',
        valueField: 'name',
        forceSelection: true,
        editable: false,
        queryMode: 'local'
    }
    // {
    //     xtype: 'filefield',
    //     fieldLabel: 'Upload New Skin'.t(),
    //     labelAlign: 'top',
    //     width: 300,
    //     allowBlank: false,
    //     validateOnBlur: false
    // }, {
    //     xtype: 'button',
    //     text: 'Upload'.t(),
    //     handler: Ext.bind(function() {
    //         this.panelSkins.onUpload();
    //     }, this)
    // }
    ]


});
Ext.define('Ung.config.administration.view.Snmp', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.administration.snmp',
    itemId: 'snmp',

    viewModel: {
        formulas: {
            snmpEnabled: function (get) {
                return get('systemSettings.snmpSettings.enabled');
            },
            trapsEnabled: function (get) {
                return get('systemSettings.snmpSettings.sendTraps');
            },
            v3Enabled: function (get) {
                return get('systemSettings.snmpSettings.v3Enabled');
            },
            communityString: function (get) {
                var val = get('systemSettings.snmpSettings.communityString');
                return  val === 'CHANGE_ME' ? 'CHANGE_ME'.t() : val;
            },
            sysContact: function (get) {
                var val = get('systemSettings.snmpSettings.sysContact');
                return  val === 'MY_CONTACT_INFO' ? 'MY_CONTACT_INFO'.t() : val;
            },
            sysLocation: function (get) {
                var val = get('systemSettings.snmpSettings.sysLocation');
                return  val === 'MY_LOCATION' ? 'MY_LOCATION'.t() : val;
            },
            trapCommunity: function (get) {
                var val = get('systemSettings.snmpSettings.trapCommunity');
                return  val === 'MY_TRAP_COMMUNITY' ? 'MY_TRAP_COMMUNITY'.t() : val;
            }
        }
    },

    title: 'SNMP'.t(),

    tbar: [{
        xtype: 'checkbox',
        padding: '8 5',
        boxLabel: 'Enable SNMP Monitoring'.t(),
        bind: '{systemSettings.snmpSettings.enabled}'
    }],

    defaults: {
        xtype: 'textfield',
        // width: 600,
        labelWidth: 300,
        labelAlign: 'right',
        disabled: true,
        msgTarget: 'side'
    },

    bodyPadding: 10,

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
    }, {
        xtype: 'checkbox',
        fieldLabel: 'Enable Traps'.t(),
        bind: {
            value: '{systemSettings.snmpSettings.sendTraps}',
            disabled: '{!snmpEnabled}'
        }
    }, {
        xtype: 'fieldset',
        border: false,
        // layout: 'anchor',
        padding: 0,
        defaults: {
            xtype: 'textfield',
            // width: 500,
            labelWidth: 300,
            labelAlign: 'right',
            msgTarget: 'side'
        },
        bind: {
            disabled: '{!snmpEnabled || !trapsEnabled}'
        },
        items: [{
            fieldLabel: 'Community'.t(),
            allowBlank: false,
            blankText: 'An Trap Community must be specified.'.t(),
            bind: '{systemSettings.snmpSettings.trapCommunity}'
        }, {
            fieldLabel: 'Host'.t(),
            allowBlank: false,
            blankText: 'An Trap Host must be specified.'.t(),
            bind: '{systemSettings.snmpSettings.trapHost}'
        }, {
            xtype: 'numberfield',
            fieldLabel: 'Port'.t(),
            bind: '{systemSettings.snmpSettings.trapPort}',
            allowDecimals: false,
            minValue: 0,
            allowBlank: false,
            blankText: 'You must provide a valid port.'.t(),
            vtype: 'port'
        }]
    }, {
        xtype: 'checkbox',
        fieldLabel: 'Enable SNMP v3'.t(),
        bind: {
            value: '{systemSettings.snmpSettings.v3Enabled}',
            disabled: '{!snmpEnabled}'
        }
    }, {
        xtype: 'fieldset',
        border: false,
        // layout: 'anchor',
        padding: 0,
        defaults: {
            xtype: 'textfield',
            // width: 500,
            labelWidth: 300,
            labelAlign: 'right',
            msgTarget: 'side'
        },
        bind: {
            disabled: '{!snmpEnabled || !v3Enabled}'
        },
        items: [{
            fieldLabel: 'Username'.t(),
            allowBlank: false,
            blankText: 'Username must be specified.'.t(),
            bind: '{systemSettings.snmpSettings.v3Username}'
        }, {
            xtype: 'combo',
            // width: 300,
            fieldLabel: 'Authentication Protocol'.t(),
            store: [['sha', 'SHA'.t()], ['md5', 'MD5'.t()]],
            editable: false,
            queryMode: 'local',
            // items: [
            //     { boxLabel: 'SHA', name: 'rb', inputValue: 'sha' },
            //     { boxLabel: 'MD5', name: 'rb', inputValue: 'md5' }
            // ],
            bind: '{systemSettings.snmpSettings.v3AuthenticationProtocol}'
        }, {
            fieldLabel: 'Authentication Passphrase'.t(),
            inputType: 'password',
            bind: '{systemSettings.snmpSettings.v3AuthenticationPassphrase}',
            allowBlank: false,
            blankText: 'Authentication Passphrase must be specified.'.t(),
            // validator: passwordValidator,
        }, {
            fieldLabel: 'Confirm Authentication Passphrase'.t(),
            inputType: 'password',
            allowBlank: false,
            blankText: 'Confirm Authentication Passphrase must be specified.'.t()
            // validator: passwordValidator,
        }, {
            xtype: 'combo',
            fieldLabel: 'Privacy Protocol'.t(),
            store: [['des', 'DES'.t()], ['aes', 'AES'.t()]],
            editable: false,
            queryMode: 'local',
            bind: '{systemSettings.snmpSettings.v3PrivacyProtocol}'
        }, {
            fieldLabel: 'Privacy Passphrase'.t(),
            inputType: 'password',
            bind: '{systemSettings.snmpSettings.v3PrivacyPassphrase}',
            allowBlank: false,
            blankText: 'Privacy Passphrase must be specified.'.t(),
            // validator: passwordValidator,
        }, {
            fieldLabel: 'Confirm Privacy Passphrase'.t(),
            inputType: 'password',
            allowBlank: false,
            blankText: 'Confirm Privacy Passphrase must be specified.'.t(),
            // validator: passwordValidator,
        }, {
            xtype: 'checkbox',
            fieldLabel: 'Require only SNMP v3'.t(),
            bind: '{systemSettings.snmpSettings.v3Required}'
        }]
    }]

});