Ext.syncRequire('Ung.common.threatprevention');
Ext.define('Ung.apps.threatprevention.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-threat-prevention',
    controller: 'app-threat-prevention',

    viewModel: {
        stores: {
            passRules: { data: '{settings.passRules.list}' },
        }
    },

    items: [
        { xtype: 'app-threat-prevention-status' },
        { xtype: 'app-threat-prevention-reputation' },
        { xtype: 'app-threat-prevention-pass' }
    ]
});
