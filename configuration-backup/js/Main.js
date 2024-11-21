Ext.define('Ung.apps.configurationbackup.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-configuration-backup',
    controller: 'app-configuration-backup',

    viewModel: {
        formulas: {
            driveConfiguredText: function (get) {
                return get('googleDriveIsConfigured') ? 'The Google Connector is configured'.t() : 'The Google Connector is unconfigured.'.t();
            }
        }
    },

    items: [
        { xtype: 'app-configuration-backup-status' },
        { xtype: 'app-configuration-backup-cloud' },
        { xtype: 'app-configuration-backup-googleconnector' }
    ]

});
