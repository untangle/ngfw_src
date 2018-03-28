Ext.define('Ung.apps.webcache.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-web-cache',
    controller: 'app-web-cache',

    viewModel: {
        stores: {
            rules: { data: '{settings.rules.list}' }
        }
    },

    items: [
        { xtype: 'app-web-cache-status' },
        { xtype: 'app-web-cache-cachebypass' }
    ]

});
