Ext.define('Ung.config.system.view.Logs', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-system-logs',
    itemId: 'logs',

    viewModel: true,

    title: 'Logs'.t(),

    bodyPadding: 10,
    scrollable: true,

    defaults: {
        xtype: 'fieldset',
        padding: 10
    },

    items: [{
        title: 'Log Retention'.t(),
        items: [{
            xtype: 'displayfield',
            fieldLabel: 'Disk space used by logs'.t(),
            labelWidth: 250,
            bind:{
                value: '{logDirectorySizeHuman}'
            }
        },{
            fieldLabel: 'For each log type, number of logs to retain'.t(),
            xtype: 'numberfield',
            bind: '{systemSettings.logRetention}',
            toValidate: true,
            labelWidth: 250,
            width: 300,
            allowDecimals: false,
            minValue: 1,
            maxValue: 366,
        }]
    }]

});
