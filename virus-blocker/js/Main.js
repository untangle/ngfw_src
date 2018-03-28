Ext.define('Ung.apps.virusblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-virus-blocker',
    controller: 'app-virus-blocker',

    viewModel: {

        stores: {
            passSites: { data: '{settings.passSites.list}' },
            fileExtensions: { data: '{settings.httpFileExtensions.list}', sorters: 'string' },
            mimeTypes: { data: '{settings.httpMimeTypes.list}', sorters: 'string' }
        },

        formulas: {
            getSignatureTimestamp: {
                get: function(get) {
                    var stamp = Rpc.directData(this.getView().appManager, 'getLastSignatureUpdate');
                    if ((stamp == null) || (stamp === 0)) return('unknown');
                    return(Util.timestampFormat(stamp));
                }
            }
        }

    },

    items: [
        { xtype: 'app-virus-blocker-status' },
        { xtype: 'app-virus-blocker-scanoptions' },
        { xtype: 'app-virus-blocker-passsites' },
        { xtype: 'app-virus-blocker-advanced' }
    ]

});
