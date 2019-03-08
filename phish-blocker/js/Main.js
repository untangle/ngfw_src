Ext.define('Ung.apps.phishblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-phish-blocker',
    controller: 'app-phish-blocker',

    items: [
        { xtype: 'app-phish-blocker-status' },
        { xtype: 'app-phish-blocker-email' }
    ]

});
