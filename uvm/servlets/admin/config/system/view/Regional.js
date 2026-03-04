Ext.define('Ung.config.system.view.Regional', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-system-regional',
    itemId: 'regional',
    viewModel: true,
    scrollable: true,
    withValidation: false,
    title: 'Regional'.t(),

    bodyPadding: 10,

    defaults: {
        xtype: 'fieldset',
        padding: 10
    },

    items: [{
        title: 'Time Settings'.t(),
        hidden: true,
        bind: {
            hidden: '{!(isExpertMode || systemSettings.timeSource === "manual" || !time)}'
        },
        items: [{
            xtype: 'radiogroup',
            columns: 1,
            vertical: true,
            simpleValue: true,
            bind: '{systemSettings.timeSource}',
            items: [{
                boxLabel: '<strong>' + 'Synchronize time automatically via NTP'.t() + '</strong>',
                inputValue: 'ntp'
            }, {
                xtype: 'fieldset',
                margin: 5,
                border: false,
                disabled: true,
                hidden: true,
                layout: {
                    type: 'hbox'
                },
                bind: {
                    disabled: '{systemSettings.timeSource !== "ntp"}',
                    hidden: '{systemSettings.timeSource !== "ntp"}'
                },
                items: [{
                    xtype: 'button',
                    margin: '0 10 0 0',
                    text: 'Synchronize Time'.t(),
                    iconCls: 'fa fa-refresh',
                    handler: 'syncTime'
                }, {
                    xtype: 'displayfield',
                    value: 'Click to force instant time synchronization.'.t()
                }]
            }, {
                boxLabel: '<strong>' + 'Set system clock manually'.t() + '</strong>',
                inputValue: 'manual'
            }, {
                xtype: 'datefield',
                width: 180,
                margin: '5 25',
                disabled: true,
                hidden: true,
                value: new Date(),
                format: 'timestamp_fmt'.t(),
                bind: {
                    value: '{manualDate}',
                    format: '{manualDateFormat}',
                    disabled: '{systemSettings.timeSource !== "manual"}',
                    hidden: '{systemSettings.timeSource !== "manual"}'
                }
            }]
        }]
    }]

});
