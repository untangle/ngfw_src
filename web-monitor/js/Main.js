Ext.define('Ung.apps.webmonitor.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-web-monitor',
    controller: 'app-web-monitor',

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
                href: '#reports/web-monitor',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            },
            hidden: true,
            bind: {
                hidden: '{instance.runState !== "RUNNING"}'
            }
        }]
    },

    items: [
        { xtype: 'app-web-monitor-status' },
        { xtype: 'app-web-monitor-categories' },
        { xtype: 'app-web-monitor-flagsites' },
        { xtype: 'app-web-monitor-passsites' },
        { xtype: 'app-web-monitor-passclients' },
        { xtype: 'app-web-monitor-rules' },
        { xtype: 'app-web-monitor-advanced' }
    ]

});
