Ext.define('Ung.apps.webcache.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.webcache',

    viewModel: {
        data: {
            nodeName: 'untangle-node-web-cache',
            appName: 'Web Cache'
        }
    },

    items: [
        { xtype: 'app.webcache.status' },
        { xtype: 'app.webcache.cachebypass' }
    ]

});
