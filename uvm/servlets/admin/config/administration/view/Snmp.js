Ext.define('Ung.config.administration.view.Snmp', {
    extend: 'Ext.form.Panel',
    alias: 'widget.config-administration-snmp',
    withValidation: true, // requires validation on save
    itemId: 'snmp',
    scrollable: true,

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
