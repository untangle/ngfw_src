Ext.define('Ung.apps.adblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-adblocker',
    controller: 'app-adblocker',

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
        { xtype: 'app-adblocker-status' },
        { xtype: 'app-adblocker-options' },
        { xtype: 'app-adblocker-adfilters' },
        { xtype: 'app-adblocker-cookiefilters' },
        { xtype: 'app-adblocker-passlists' }
    ]

});
