Ext.define('Ung.apps.webfilter.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-web-filter',
    controller: 'app-web-filter',

    viewModel: {
        stores: {
            categories:    { data: '{settings.categories.list}' },
            blockedUrls:   { data: '{settings.blockedUrls.list}' },
            passedUrls:    { data: '{settings.passedUrls.list}' },
            passedClients: { data: '{settings.passedClients.list}' },
            filterRules:   { data: '{settings.filterRules.list}' }
        }
    },

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/web-filter',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            }
        }]
    },

    items: [
        { xtype: 'app-web-filter-status' },
        { xtype: 'app-web-filter-categories' },
        { xtype: 'app-web-filter-sitelookup' },
        { xtype: 'app-web-filter-blocksites' },
        { xtype: 'app-web-filter-passsites' },
        { xtype: 'app-web-filter-passclients' },
        { xtype: 'app-web-filter-rules' },
        { xtype: 'app-web-filter-advanced' }
    ]

});
