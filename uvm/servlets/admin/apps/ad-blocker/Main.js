Ext.define('Ung.apps.adblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-ad-blocker',
    controller: 'app-ad-blocker',

    viewModel: {
        stores: {
            rules: { data: '{settings.rules.list}' },
            userRules: { data: '{settings.userRules.list}' },
            cookies: { data: '{settings.cookies.list}' },
            userCookies: { data: '{settings.userCookies.list}' },
            passedUrls: { data: '{settings.passedUrls.list}' },
            passedClients: { data: '{settings.passedClients.list}' }
        }
    },

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/ad-blocker',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            }
        }]
    },

    items: [
        { xtype: 'app-ad-blocker-status' },
        { xtype: 'app-ad-blocker-options' },
        { xtype: 'app-ad-blocker-adfilters' },
        { xtype: 'app-ad-blocker-cookiefilters' },
        { xtype: 'app-ad-blocker-passlists' }
    ]

});
