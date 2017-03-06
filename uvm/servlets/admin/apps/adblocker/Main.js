Ext.define('Ung.apps.adblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-adblocker',
    controller: 'app-adblocker',

    items: [
        { xtype: 'app-adblocker-status' },
        { xtype: 'app-adblocker-options' },
        { xtype: 'app-adblocker-adfilters' },
        { xtype: 'app-adblocker-cookiefilters' },
        { xtype: 'app-adblocker-passlists' }
    ]

});
