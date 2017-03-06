Ext.define('Ung.apps.webfilter.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-webfilter',
    controller: 'app-webfilter',

    viewModel: {
        stores: {
            categories:    { data: '{settings.categories.list}' },
            blockedUrls:   { data: '{settings.blockedUrls.list}' },
            passedUrls:    { data: '{settings.passedUrls.list}' },
            passedClients: { data: '{settings.passedClients.list}' },
            filterRules:   { data: '{settings.filterRules.list}' }
        }
    },

    items: [
        { xtype: 'app-webfilter-status' },
        { xtype: 'app-webfilter-categories' },
        { xtype: 'app-webfilter-blocksites' },
        { xtype: 'app-webfilter-passsites' },
        { xtype: 'app-webfilter-passclients' },
        { xtype: 'app-webfilter-rules' },
        { xtype: 'app-webfilter-advanced' }
    ]

});
