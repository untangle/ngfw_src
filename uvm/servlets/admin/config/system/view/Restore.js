Ext.define('Ung.config.system.view.Restore', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-system-restore',
    itemId: 'restore',

    viewModel: true,

    title: 'Restore'.t(),

    bodyPadding: 10,
    scrollable: true,

    defaults: {
        xtype: 'fieldset',
        padding: 10
    },

    items: [{
        title: 'Restore from File'.t(),
        items: [{
            xtype: 'component',
            html: 'Restore can restore a previous system configuration to the server from a backup file on your local computer.  The backup file name ends with .backup'.t()
        }, {
            xtype: 'form',
            padding: 10,
            margin: 10,
            border: false,
            url: 'upload',
            defaults: {
                labelAlign: 'right',
                labelWidth: 150
            },
            items: [{
                xtype: 'combo',
                name: 'argument',
                fieldLabel: 'Restore Options'.t(),
                width: 500,
                store: [['', 'Restore all settings.'.t()], ['.*/network.*', 'Restore all except keep current network settings.'.t()]],
                value: '',
                queryMode: 'local',
                allowBlank: false,
                editable: false
            }, {
                xtype: 'fileuploadfield',
                itemId: 'restoreFile',
                margin: '10 0 0 0',
                name: 'file',
                buttonText: 'Restore from File'.t(),
                buttonOnly: true,
                listeners: { 'change': 'restoreFromFile' }
            }, {
                xtype: 'hidden',
                name: 'type',
                value: 'restore'
            }]
        }]
    }]

});
