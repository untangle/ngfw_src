Ext.define('Ung.apps.applicationcontrol.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-application-control',
    controller: 'app-application-control',

    viewModel: {
        stores: {
            protoRules: { data: '{settings.protoRules.list}' },
            logicRules: { data: '{settings.logicRules.list}' }
        }
    },

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/application-control',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            }
        }]
    },

    items: [
        { xtype: 'app-application-control-status' },
        { xtype: 'app-application-control-applications' },
        { xtype: 'app-application-control-rules' }
    ]

});
