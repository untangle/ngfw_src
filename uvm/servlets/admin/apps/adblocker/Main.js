Ext.define('Ung.apps.adblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.adblocker',

    viewModel: {
        data: {
            nodeName: 'untangle-node-ad-blocker',
            appName: 'Ad Blocker'
        }
    },

    items: [
        { xtype: 'app.adblocker.status' },
        { xtype: 'app.adblocker.options' },
        { xtype: 'app.adblocker.adfilters' },
        { xtype: 'app.adblocker.cookiefilters' },
        { xtype: 'app.adblocker.passlists' }
    ]

});
