Ext.define('Ung.apps.configurationbackup.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-configuration-backup',
    controller: 'app-configuration-backup',

    items: [
        { xtype: 'app-configuration-backup-status' },
        { xtype: 'app-configuration-backup-cloud' },
        { xtype: 'app-configuration-backup-googleconnector' }
    ]

});
