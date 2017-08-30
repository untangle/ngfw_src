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
                    var stamp = this.getView().appManager.getLastSignatureUpdate();
                    if ((stamp == null) || (stamp === 0)) return('unknown');
                    return(Util.timestampFormat(stamp));
                }
            }
        }

    },

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/virus-blocker-lite',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            },
            hidden: true,
            bind: {
                hidden: '{instance.runState !== "RUNNING"}'
            }
        }]
    },

    items: [
        { xtype: 'app-virus-blocker-lite-status' },
        { xtype: 'app-virus-blocker-lite-scanoptions' },
        { xtype: 'app-virus-blocker-lite-passsites' },
        { xtype: 'app-virus-blocker-lite-advanced' }
    ]

});
