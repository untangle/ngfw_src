Ext.define('Ung.apps.virusblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-virusblocker',
    controller: 'app-virusblocker',

    viewModel: {
        stores: {
            passSites: { data: '{settings.passSites.list}' },
            fileExtensions: { data: '{settings.httpFileExtensions.list}' },
            mimeTypes: { data: '{settings.httpMimeTypes.list}' }
        }
    },

    items: [
        { xtype: 'app-virusblocker-status' },
        { xtype: 'app-virusblocker-scanoptions' },
        { xtype: 'app-virusblocker-passsites' },
        { xtype: 'app-virusblocker-advanced' }
    ]

});
