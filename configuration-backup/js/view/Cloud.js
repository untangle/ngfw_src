Ext.define('Ung.apps.configurationbackup.view.Cloud', {
    extend: 'Ext.form.Panel',
    alias: 'widget.app-configuration-backup-cloud',
    itemId: 'cloud',
    title: 'Cloud'.t(),
    scrollable: true,

    viewModel: true,
    bodyPadding: 10,

    items: [{
        title: 'Cloud'.t(),
        bodyPadding: 10,
        items: [{
            xtype: 'fieldset',
            title: '<i class="fa fa-files-o"></i> ' + 'Daily Backup'.t(),
            padding: 10,
            margin: '20 0',
            cls: 'app-section',

            collapsed: true,
            disabled: true,
            bind: {
                collapsed: '{!state.on}',
                disabled: '{!state.on}'
            },
            items: [{
                xtype: 'numberfield',
                allowDecimals: false,
                minValue: 0,
                maxValue: 23,
                fieldLabel: 'Hour'.t(),
                bind: '{settings.hourInDay}'
            }, {
                xtype: 'numberfield',
                allowDecimals: false,
                minValue: 0,
                maxValue: 59,
                fieldLabel: 'Minute'.t(),
                bind: '{settings.minuteInHour}'
            }]
        }, {
            xtype: 'fieldset',
            title: '<i class="fa fa-files-o"></i> ' + 'Backup Now'.t(),
            padding: 10,
            margin: '20 0',
            cls: 'app-section',

            collapsed: true,
            disabled: true,
            bind: {
                collapsed: '{!state.on}',
                disabled: '{!state.on}'
            },
            items: [{
                xtype: 'component',
                html: '<strong>' + 'Force an immediate backup now.'.t() + '</strong>',
                margin: '0 0 10 0'
            }, {
                xtype: 'button',
                text: 'Backup now'.t(),
                handler: 'backupNow'
            }]
        }]
    }]
});
