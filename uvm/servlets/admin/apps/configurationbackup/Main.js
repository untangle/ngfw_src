Ext.define('Ung.apps.configurationbackup.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.configurationbackup',

    viewModel: {
        data: {
            nodeName: 'untangle-node-configuration-backup',
            appName: 'Configuration Backup'
        }
    },

    items: [
        { xtype: 'app.configurationbackup.status' },
        { xtype: 'app.configurationbackup.googleconnector' }
    ]

});
