Ext.define('Ung.apps.captiveportal.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.captiveportal',

    viewModel: {
        data: {
            nodeName: 'untangle-node-captive-portal',
            appName: 'Captive Portal'
        }
    },

    items: [
        { xtype: 'app.captiveportal.status' },
        { xtype: 'app.captiveportal.capturerules' },
        { xtype: 'app.captiveportal.passedhosts' },
        { xtype: 'app.captiveportal.captivepage' },
        { xtype: 'app.captiveportal.userauthentication' }
    ]

});
