Ext.define('Ung.config.system.view.Support', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-system-support',
    itemId: 'support',

    viewModel: true,

    title: 'Support'.t(),

    bodyPadding: 10,
    scrollable: true,

    defaults: {
        xtype: 'fieldset',
        padding: 10
    },

    items: [{
        title: 'Support'.t(),
        items: [{
            xtype: 'checkbox',
            boxLabel: 'Connect to Command Center'.t(),
            bind: '{systemSettings.cloudEnabled}'
        }, {
            xtype: 'checkbox',
            disabled: true,
            boxLabel: Ext.String.format('Allow secure remote access to {0} support team'.t(), this.oemName),
            bind: {
                value: '{systemSettings.supportEnabled}',
                disabled: '{!systemSettings.cloudEnabled}'
            }
        }]
    }, {
        title: 'Logs'.t(),
        items: [{
            xtype: 'button',
            text: 'Download System Logs'.t(),
            iconCls: 'fa fa-download',
            handler: 'downloadSystemLogs'
        }]
    }, {
        title: 'Manual Reboot'.t(),
        items: [{
            xtype: 'container',
            layout: {
                type: 'hbox'
            },
            items: [{
                xtype: 'displayfield',
                value: 'Reboot the server.'.t()
            }, {
                xtype: 'button',
                margin: '0 0 0 10',
                text: 'Reboot'.t(),
                iconCls: 'fa fa-refresh',
                handler: 'manualReboot'
            }]
        }]
    }, {
        title: 'Manual Shutdown'.t(),
        items: [{
            xtype: 'container',
            layout: {
                type: 'hbox'
            },
            items: [{
                xtype: 'displayfield',
                value: 'Power off the server.'.t()
            }, {
                xtype: 'button',
                margin: '0 0 0 10',
                text: 'Shutdown'.t(),
                iconCls: 'fa fa-power-off',
                handler: 'manualShutdown'
            }]
        }]
    }, {
        title: 'Setup Wizard'.t(),
        items: [{
            xtype: 'container',
            layout: {
                type: 'hbox'
            },
            items: [{
                xtype: 'displayfield',
                value: 'Launch the Setup Wizard.'.t()
            }, {
                xtype: 'button',
                margin: '0 0 0 10',
                text: 'Setup Wizard'.t(),
                iconCls: 'fa fa-magic',
                handler: function() {
                    location.assign('/setup');
                }
            }]
        }]
    }, {
        title: 'Factory Defaults'.t(),
        items: [{
            xtype: 'container',
            layout: {
                type: 'hbox'
            },
            items: [{
                xtype: 'displayfield',
                value: 'Reset all settings to factory defaults.'.t()
            }, {
                xtype: 'button',
                margin: '0 0 0 10',
                text: 'Reset to Factory Defaults'.t(),
                iconCls: 'fa fa-exclamation-circle',
                handler: 'factoryDefaults'
            }]
        }]
    }]

});
