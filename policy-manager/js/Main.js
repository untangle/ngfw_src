Ext.define('Ung.apps.policymanager.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-policy-manager',

    controller: 'app-policy-manager',

    viewModel: {
        data: {
            appsData: [],
            newPolicy: null
        },
        stores: {
            appsStore: {
                data: '{appsData}',
                // filters: [{
                //     property: 'type',
                //     value: 'FILTER'
                // }],
                sorters: 'viewPosition'
            },
            rules: { data: '{settings.rules.list}' }
        }
    },

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/policy-manager',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            },
            hidden: true,
            bind: {
                hidden: '{instance.runState !== "RUNNING"}'
            }
        }]
    },

    items: [
        { xtype: 'app-policy-manager-status' },
        { xtype: 'app-policy-manager-policies' },
        { xtype: 'app-policy-manager-rules' }
    ]
});
