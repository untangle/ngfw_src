Ext.define('Ung.apps.sslinspector.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.sslinspector',

    viewModel: {
        data: {
            nodeName: 'untangle-casing-ssl-inspector',
            appName: 'SSL Inspector'
        }
    },

    items: [
        { xtype: 'app.sslinspector.status' },
        { xtype: 'app.sslinspector.configuration' },
        { xtype: 'app.sslinspector.rules' }
    ]

});
