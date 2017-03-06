Ext.define('Ung.apps.policymanager.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-policymanager',

    items: [
        { xtype: 'app-policymanager-status' },
        { xtype: 'app-policymanager-policies' },
        { xtype: 'app-policymanager-rules' }
    ]
});
