Ext.define('Ung.apps.policymanager.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.policymanager',

    viewModel: {
        data: {
            nodeName: 'untangle-node-policy-manager',
            appName: 'Policy Manager'
        }
    },

    items: [
        { xtype: 'app.policymanager.status' },
        { xtype: 'app.policymanager.policies' },
        { xtype: 'app.policymanager.rules' },
    ]

});
