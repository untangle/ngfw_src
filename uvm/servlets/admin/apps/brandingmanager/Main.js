Ext.define('Ung.apps.brandingmanager.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.brandingmanager',

    viewModel: {
        data: {
            nodeName: 'untangle-node-branding-manager',
            appName: 'Branding Manager'
        }
    },

    items: [
        { xtype: 'app.brandingmanager.status' },
        { xtype: 'app.brandingmanager.settings' }
    ]

});
