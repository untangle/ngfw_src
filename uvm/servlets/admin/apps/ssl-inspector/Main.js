Ext.define('Ung.apps.sslinspector.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-ssl-inspector',
    controller: 'app-ssl-inspector',

    viewModel: {
        stores: {
            ignoreRules: { data: '{settings.ignoreRules.list}' }
        }
    },

    items: [
        { xtype: 'app-ssl-inspector-status' },
        { xtype: 'app-ssl-inspector-configuration' },
        { xtype: 'app-ssl-inspector-rules' }
    ]

});
