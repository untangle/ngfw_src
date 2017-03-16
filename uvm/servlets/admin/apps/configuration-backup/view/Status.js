Ext.define('Ung.apps.configurationbackup.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-configuration-backup-status',
    itemId: 'status',
    title: 'Status'.t(),

    viewModel: true,

    items: [{
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/skins/modern-rack/images/admin/apps/configuration-backup_80x80.png" width="80" height="80"/>' +
                '<h3>Configuration Backup</h3>' +
                '<p>' + 'Configuration Backup automatically creates backups of settings uploads them to <i>My Account</i> and <i>Google Drive</i>.'.t() + '</p>'
        }, {
            xtype: 'appstate'
        }, {
            xtype: 'fieldset',
            title: '<i class="fa fa-files-o"></i> ' + 'Backup Now'.t(),
            padding: 10,
            margin: '20 0',
            cls: 'app-section',

            collapsed: true,
            disabled: true,
            bind: {
                collapsed: '{instance.targetState !== "RUNNING"}',
                disabled: '{instance.targetState !== "RUNNING"}'
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

        }, {
            xtype: 'appreports'
        }, {
            xtype: 'appremove'
        }]
    }]
});
