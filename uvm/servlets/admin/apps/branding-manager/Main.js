Ext.define('Ung.apps.brandingmanager.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-branding-manager',
    controller: 'app-branding-manager',

    items: [
        { xtype: 'app-branding-manager-status' },
        { xtype: 'app-branding-manager-settings' }
    ]

});
