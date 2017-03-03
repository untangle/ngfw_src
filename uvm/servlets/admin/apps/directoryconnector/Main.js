Ext.define('Ung.apps.directoryconnector.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.directoryconnector',

    viewModel: {
        data: {
            nodeName: 'untangle-node-directory-connector',
            appName: 'Directory Connector'
        }
    },

    items: [
        { xtype: 'app.directoryconnector.status' },
        { xtype: 'app.directoryconnector.usernotificationapi' },
        { xtype: 'app.directoryconnector.activedirectory' },
        { xtype: 'app.directoryconnector.radius' },
        { xtype: 'app.directoryconnector.google' },
        { xtype: 'app.directoryconnector.facebook' }
    ]

});
