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
                href: '#reports/virus-blocker',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            },
            hidden: true,
            bind: {
                hidden: '{instance.runState !== "RUNNING"}'
            }
        }]
    },

    items: [
        { xtype: 'app-virus-blocker-status' },
        { xtype: 'app-virus-blocker-scanoptions' },
        { xtype: 'app-virus-blocker-passsites' },
        { xtype: 'app-virus-blocker-advanced' }
    ]

});
