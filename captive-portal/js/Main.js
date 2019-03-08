Ext.define('Ung.apps.captive-portal.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-captive-portal',
    controller: 'app-captive-portal',

    viewModel: {
        stores: {
            captureRules: { data: '{settings.captureRules.list}' },
            passedClients: { data: '{settings.passedClients.list}' },
            passedServers: { data: '{settings.passedServers.list}' }
        }
    },

    items: [
        { xtype: 'app-captive-portal-status' },
        { xtype: 'app-captive-portal-capturerules' },
        { xtype: 'app-captive-portal-passedhosts' },
        { xtype: 'app-captive-portal-captivepage' },
        { xtype: 'app-captive-portal-userauthentication' }
    ]

});
