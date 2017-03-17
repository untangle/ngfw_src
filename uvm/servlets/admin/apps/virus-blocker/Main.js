Ext.define('Ung.apps.virusblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-virus-blocker',
    controller: 'app-virus-blocker',

    viewModel: {
        stores: {
            passSites: { data: '{settings.passSites.list}' },
            fileExtensions: { data: '{settings.httpFileExtensions.list}' },
            mimeTypes: { data: '{settings.httpMimeTypes.list}' }
        }
    },

    items: [
        { xtype: 'app-virus-blocker-status' },
        { xtype: 'app-virus-blocker-scanoptions' },
        { xtype: 'app-virus-blocker-passsites' },
        { xtype: 'app-virus-blocker-advanced' }
    ]

});
