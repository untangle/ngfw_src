Ext.define('Ung.apps.directoryconnector.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-directory-connector',
    controller: 'app-directory-connector',

    viewModel: {
        type: 'app-directory-connector',
    },

    items: [
        { xtype: 'app-directory-connector-status' },
        { xtype: 'app-directory-connector-usernotificationapi' },
        { xtype: 'app-directory-connector-activedirectory' },
        { xtype: 'app-directory-connector-radius' }
    ]

});
