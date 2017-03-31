Ext.define('Ung.apps.policymanager.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-policy-manager',

    controller: 'app-policy-manager',

    viewModel: {
        data: {
            appsData: []
        },
        stores: {
            appsStore: {
                data: '{appsData}',
                filters: [{
                    property: 'type',
                    value: 'FILTER'
                }]
            }
        }
    },

    items: [
        { xtype: 'app-policy-manager-status' },
        { xtype: 'app-policy-manager-policies' },
        { xtype: 'app-policy-manager-rules' }
    ]
});
