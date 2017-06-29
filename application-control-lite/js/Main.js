Ext.define('Ung.apps.applicationcontrollite.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-application-control-lite',
    controller: 'app-application-control-lite',

    viewModel: {

        stores: {
            signatureList: {
                data: '{settings.patterns.list}'
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
                href: '#reports/application-control-lite',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            }
        }]
    },

    items: [
        { xtype: 'app-application-control-lite-status' },
        { xtype: 'app-application-control-lite-signatures' }
    ]

});
