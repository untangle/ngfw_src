Ext.define('Ung.apps.configurationbackup.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-configurationbackup',
    controller: 'app-configurationbackup',

    items: [
        { xtype: 'app-configurationbackup-status' },
        { xtype: 'app-configurationbackup-googleconnector' }
    ]

});
