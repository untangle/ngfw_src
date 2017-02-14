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