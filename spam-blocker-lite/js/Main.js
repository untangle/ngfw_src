Ext.define('Ung.apps.spamblockerlite.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-spam-blocker-lite',
    controller: 'app-spam-blocker-lite',

    items: [
        { xtype: 'app-spam-blocker-lite-status' },
        { xtype: 'app-spam-blocker-lite-email' }
    ]
});
