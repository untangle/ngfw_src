Ext.define('Ung.apps.webfilter.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-web-filter',
    controller: 'app-web-filter',

    viewModel: {
        stores: {
            categories:    { 
                data: '{settings.categories.list}',
                groupField: 'category'
            },
            searchTerms:   { data: '{settings.searchTerms.list}' },
            blockedUrls:   { data: '{settings.blockedUrls.list}' },
            passedUrls:    { data: '{settings.passedUrls.list}' },
            passedClients: { data: '{settings.passedClients.list}' },
            filterRules:   { data: '{settings.filterRules.list}' }
        }
    },

    items: [
        { xtype: 'app-web-filter-status' },
        { xtype: 'app-web-filter-categories' },
        { xtype: 'app-web-filter-searchterms' },
        { xtype: 'app-web-filter-sitelookup' },
        { xtype: 'app-web-filter-blocksites' },
        { xtype: 'app-web-filter-passsites' },
        { xtype: 'app-web-filter-passclients' },
        { xtype: 'app-web-filter-rules' },
        { xtype: 'app-web-filter-advanced' }
    ]

});
