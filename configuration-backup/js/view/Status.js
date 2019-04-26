Ext.define('Ung.apps.configurationbackup.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-configuration-backup-status',
    itemId: 'status',
    title: 'Status'.t(),
    scrollable: true,

    viewModel: true,

    items: [{
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/icons/apps/configuration-backup.svg" width="80" height="80"/>' +
                '<h3>Configuration Backup</h3>' +
                '<p>' + 'Configuration Backup automatically creates backups of settings and uploads them to <i>My Account</i> and <i>Google Drive</i>.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appstate'
        }, {
            xtype: 'appreports'
        }, {
            xtype: 'appremove'
        }]
    }]
});
