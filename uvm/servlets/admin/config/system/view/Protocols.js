Ext.define('Ung.config.system.view.Protocols', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-system-protocols',
    itemId: 'protocols',

    viewModel: {
        formulas: {
            smtpTimeout: {
                get: function (get) {
                    return get('smtpSettings.smtpTimeout') / 1000;
                },
                set: function (value) {
                    this.set('smtpSettings.smtpTimeout', value * 1000);
                }
            }
        }
    },

    title: 'Protocols'.t(),

    bodyPadding: 10,
    scrollable: true,

    defaults: {
        xtype: 'fieldset',
        padding: 10
    },

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: '<i class="fa fa-exclamation-triangle" style="color: red;"></i> '  + 'These settings should not be changed unless instructed to do so by support.'.t()
    }],


    items: [{
        title: 'HTTP'.t(),
        disabled: true,
        bind: {
            disabled: '{!httpSettings}'
        },
        items: [{
            xtype: 'radiogroup',
            columns: 1,
            vertical: true,
            simpleValue: true,
            bind: '{httpSettings.enabled}',
            items: [{
                boxLabel: '<strong>' + 'Enable processing of HTTP traffic.  (This is the default setting)'.t() + '</strong>',
                inputValue: true
            }, {
                boxLabel: '<strong>' + 'Disable processing of HTTP traffic.'.t() + '</strong>',
                inputValue: false
            }]
        }, {
            xtype: 'checkbox',
            boxLabel: 'Log Referer in HTTP events.'.t(),
            bind: '{httpSettings.logReferer}'
        }]
    }, {
        title: 'FTP'.t(),
        disabled: true,
        bind: {
            disabled: '{!ftpSettings}'
        },
        items: [{
            xtype: 'radiogroup',
            columns: 1,
            vertical: true,
            simpleValue: true,
            bind: '{ftpSettings.enabled}',
            items: [{
                boxLabel: '<strong>' + 'Enable processing of FTP traffic.  (This is the default setting)'.t() + '</strong>',
                inputValue: true
            }, {
                boxLabel: '<strong>' + 'Disable processing of FTP traffic.'.t() + '</strong>',
                inputValue: false
            }]
        }]
    }, {
        title: 'SMTP'.t(),
        disabled: true,
        bind: {
            disabled: '{!smtpSettings}'
        },
        items: [{
            xtype: 'radiogroup',
            columns: 1,
            vertical: true,
            simpleValue: true,
            bind: '{smtpSettings.smtpEnabled}',
            items: [{
                boxLabel: '<strong>' + 'Enable processing of SMTP traffic.  (This is the default setting)'.t() + '</strong>',
                inputValue: true
            }, {
                boxLabel: '<strong>' + 'Disable processing of SMTP traffic.'.t() + '</strong>',
                inputValue: false
            }]
        }, {
            xtype: 'numberfield',
            fieldLabel: 'SMTP timeout (seconds)'.t(),
            labelAlign: 'top',
            allowDecimals: false,
            allowNegative: false,
            minValue: 0,
            maxValue: 86400,
            bind: {
                value: '{smtpTimeout}',
                hidden: '{!isExpertMode}'
            },
            hidden: true,
        }]
    }]

});
