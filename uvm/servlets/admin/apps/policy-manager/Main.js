Ext.define('Ung.apps.policymanager.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-policy-manager',

    items: [
        { xtype: 'app-policy-manager-status' },
        { xtype: 'app-policy-manager-policies' },
        { xtype: 'app-policy-manager-rules' }
    ]
});
