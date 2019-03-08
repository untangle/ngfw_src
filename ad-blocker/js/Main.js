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

    items: [
        { xtype: 'app-ad-blocker-status' },
        { xtype: 'app-ad-blocker-options' },
        { xtype: 'app-ad-blocker-adfilters' },
        { xtype: 'app-ad-blocker-cookiefilters' },
        { xtype: 'app-ad-blocker-passlists' }
    ]

});
