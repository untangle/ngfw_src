Ext.define('Ung.apps.phishblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-phishblocker',
    controller: 'app-phishblocker',

    items: [
        { xtype: 'app-phishblocker-status' },
        { xtype: 'app-phishblocker-email' }
    ]

});
