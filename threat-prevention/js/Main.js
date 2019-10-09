Ext.syncRequire('Ung.common.ipreputation');
Ext.define('Ung.apps.ipreputation.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-ip-reputation',
    controller: 'app-ip-reputation',

    viewModel: {
        stores: {
            passRules: { data: '{settings.passRules.list}' },
        }
    },

    items: [
        { xtype: 'app-ip-reputation-status' },
        { xtype: 'app-ip-reputation-reputation' },
        { xtype: 'app-ip-reputation-pass' }
    ]
});
