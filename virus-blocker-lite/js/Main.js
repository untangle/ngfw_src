Ext.define('Ung.apps.virusblockerlite.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-virus-blocker-lite',
    controller: 'app-virus-blocker-lite',

    viewModel: {

        stores: {
            passSites: { data: '{settings.passSites.list}' },
            fileExtensions: { data: '{settings.httpFileExtensions.list}' },
            mimeTypes: { data: '{settings.httpMimeTypes.list}' }
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
        { xtype: 'app-virus-blocker-lite-status' },
        { xtype: 'app-virus-blocker-lite-scanoptions' },
        { xtype: 'app-virus-blocker-lite-passsites' },
        { xtype: 'app-virus-blocker-lite-advanced' }
    ]

});
