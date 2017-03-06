Ext.define('Ung.apps.captiveportal.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-captiveportal',
    controller: 'app-captiveportal',

    viewModel: {
        stores: {
            captureRules: { data: '{settings.captureRules.list}' },
            passedClients: { data: '{settings.passedClients.list}' },
            passedServers: { data: '{settings.passedServers.list}' }
        }
    },

    items: [
        { xtype: 'app-captiveportal-status' },
        { xtype: 'app-captiveportal-capturerules' },
        { xtype: 'app-captiveportal-passedhosts' },
        { xtype: 'app-captiveportal-captivepage' },
        { xtype: 'app-captiveportal-userauthentication' }
    ]

});
