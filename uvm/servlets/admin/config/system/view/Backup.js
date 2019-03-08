Ext.define('Ung.config.system.view.Backup', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-system-backup',
    itemId: 'backup',

    viewModel: true,

    title: 'Backup'.t(),

    bodyPadding: 10,
    scrollable: true,

    defaults: {
        xtype: 'fieldset',
        padding: 10
    },

    items: [{
        title: 'Backup to File'.t(),
        items: [{
            xtype: 'component',
            html: 'Backup can save the current system configuration to a file on your local computer for later restoration. The file name will end with .backup'.t() +
                  '<br />' +
                  'After backing up your current system configuration to a file, you can then restore that configuration through this dialog by clicking on Restore from File.'.t()
        }, {
            xtype: 'button',
            margin: '10 0 0 0',
            text: 'Backup to File'.t(),
            handler: 'backupToFile'
        }]
    }]

});
