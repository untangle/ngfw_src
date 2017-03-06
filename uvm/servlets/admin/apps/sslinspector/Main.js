Ext.define('Ung.apps.sslinspector.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-sslinspector',
    controller: 'app-sslinspector',

    viewModel: {
        stores: {
            ignoreRules: { data: '{settings.ignoreRules.list}' }
        }
    },

    items: [
        { xtype: 'app-sslinspector-status' },
        { xtype: 'app-sslinspector-configuration' },
        { xtype: 'app-sslinspector-rules' }
    ]

});
