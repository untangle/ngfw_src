Ext.define('Ung.apps.brandingmanager.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-brandingmanager',

    items: [
        { xtype: 'app-brandingmanager-status' },
        { xtype: 'app-brandingmanager-settings' }
    ]

});
