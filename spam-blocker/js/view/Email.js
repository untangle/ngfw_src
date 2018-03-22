Ext.define('Ung.apps.spamblocker.view.Email', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-spam-blocker-email',
    itemId: 'email',
    title: 'Email'.t(),
    scrollable: true,

    viewModel: {
        formulas: {
            strength: {
                get: function (get) {
                    return (get('settings.smtpConfig.strength') / 10).toFixed(1);
                },
                set: function (value) {
                    this.set('settings.smtpConfig.strength', Math.round(value * 10));
                }
            },
            superSpamStrength: {
                get: function (get) {
                    return get('settings.smtpConfig.superSpamStrength') / 10;
                },
                set: function (value) {
                    this.set('settings.smtpConfig.superSpamStrength', Math.round(value * 10));
                }
            }
        }
    },

    tbar: [{
        xtype: 'checkbox',
        boxLabel: '<strong>' + 'Scan SMTP'.t() + '</strong>',
        bind: '{settings.smtpConfig.scan}',
        padding: 5
    }],

    layout: 'fit',

    items: [{
        xtype: 'container',
        padding: 10,
        disabled: true,
        scrollable: 'y',
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        bind: {
            disabled: '{!settings.smtpConfig.scan}'
        },
        defaults: {
            margin: '5 0',
        },
        items: [{
            xtype: 'container',
            layout: { type: 'vbox' },
            items: [{
                xtype: 'container',
                margin: '0 0 10 0',
                layout: { type: 'hbox', align: 'middle' },
                items: [{
                    xtype: 'combo',
                    reference: 'predefinedStrength',
                    publishes: 'value',
                    editable: false,
                    fieldLabel: 'Strength'.t(),
                    width: 300,
                    store: [
                        [50, 'Low (Threshold: 5.0)'.t()],
                        [43, 'Medium (Threshold: 4.3)'.t()],
                        [35, 'High (Threshold: 3.5)'.t()],
                        [33, 'Very High (Threshold: 3.3)'.t()],
                        [30, 'Extreme (Threshold: 3.0)'.t()],
                        [0, 'Custom'.t()]
                    ],
                    queryMode: 'local',
                    listeners: {
                        change: 'setStrength'
                    }
                }, {
                    xtype: 'numberfield',
                    reference: 'customStrength',
                    margin: '0 10',
                    hidden: true,
                    bind: {
                        value: '{strength}',
                        hidden: '{predefinedStrength.value !== 0}'
                    },
                    width: 80,
                    allowDecimals: true,
                    allowBlank: false,
                    step: 0.1,
                    minValue: -2147483648,
                    maxValue: 2147483647
                }, {
                    xtype: 'component',
                    html: 'Strength Value must be a number. Smaller value is higher strength.'.t(),
                    hidden: true,
                    bind: {
                        hidden: '{predefinedStrength.value !== 0}'
                    }
                }]
            }, {
                xtype: 'combo',
                editable: false,
                store: [
                    ['MARK', 'Mark'.t()],
                    ['PASS', 'Pass'.t()],
                    ['DROP', 'Drop'.t()],
                    ['QUARANTINE', 'Quarantine'.t()]
                ],
                // valueField: 'key',
                // displayField: 'name',
                fieldLabel: 'Action'.t(),
                width: 300,
                queryMode: 'local',
                bind: {
                    value: '{settings.smtpConfig.msgAction}'
                }
            }]
        }, {
            xtype: 'fieldset',
            title: 'Drop Super Spam'.t(),
            collapsible: true,
            collapsed: true,
            checkboxToggle: true,
            checkbox: {
                bind: '{settings.smtpConfig.blockSuperSpam}'
            },
            margin: '10 0',
            padding: 10,
            layout: {
                type: 'vbox'
            },
            items: [{
                xtype: 'numberfield',
                labelWidth: 150,
                fieldLabel: 'Super Spam Threshold'.t(),
                bind: '{superSpamStrength}',
                allowDecimals: false,
                allowBlank: false,
                minValue: 0,
                maxValue: 2147483647
            }]
        }, {
            xtype: 'fieldset',
            title: 'Advanced SMTP Configuration'.t(),
            collapsible: true,
            collapsed: true,
            padding: 10,
            layout: {
                type: 'vbox'
            },
            defaults: {
                xtype: 'checkbox'
            },
            items: [{
                boxLabel: 'Enable tarpitting'.t(),
                bind: '{settings.smtpConfig.tarpit}'
            }, {
                boxLabel: 'Enable greylisting'.t(),
                bind: '{settings.smtpConfig.greylist}'
            }, {
                boxLabel: 'Add email headers'.t(),
                bind: '{settings.smtpConfig.addSpamHeaders}'
            }, {
                boxLabel: 'Close connection on scan failure'.t(),
                bind: '{settings.smtpConfig.failClosed}'
            }, {
                boxLabel: 'Scan outbound (WAN) SMTP'.t(),
                bind: '{settings.smtpConfig.scanWanMail}'
            }, {
                boxLabel: 'Allow and ignore TLS sessions'.t(),
                bind: '{settings.smtpConfig.allowTls}'
            }, {
                xtype: 'numberfield',
                fieldLabel: 'CPU Load Limit'.t(),
                labelWidth: 150,
                bind: '{settings.smtpConfig.loadLimit}',
                allowDecimals: true,
                allowBlank: false,
                blankText: 'Value must be a float.'.t(),
                minValue: 0,
                maxValue: 50
            }, {
                xtype: 'numberfield',
                fieldLabel: 'Concurrent Scan Limit'.t(),
                labelWidth: 150,
                bind: '{settings.smtpConfig.scanLimit}',
                allowDecimals: false,
                allowBlank: false,
                blankText: 'Value must be a integer.'.t(),
                minValue: 0,
                maxValue: 100
            }, {
                xtype: 'numberfield',
                fieldLabel: 'Message Size Limit'.t(),
                labelWidth: 150,
                bind: '{settings.smtpConfig.msgSizeLimit}',
                allowDecimals: false,
                allowBlank: false,
                blankText: 'Value must be a integer.'.t(),
                minValue: 0,
                maxValue: 2147483647
            }]
        }]
    }]

});
