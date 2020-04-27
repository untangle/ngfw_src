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
            labelWidth: 150,
            bind:{
                value: '{logDirectorySizeHuman}'
            }
        },{
            fieldLabel: 'Log retention'.t(),
            xtype: 'numberfield',
            bind: '{systemSettings.logRetention}',
            toValidate: true,
            labelWidth: 150,
            width: 220,
            allowDecimals: false,
            minValue: 1,
            maxValue: 366,
        }]
    }]

});
