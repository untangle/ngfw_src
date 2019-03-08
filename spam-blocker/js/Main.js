Ext.define('Ung.apps.spamblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-spam-blocker',
    controller: 'app-spam-blocker',

    items: [
        { xtype: 'app-spam-blocker-status' },
        { xtype: 'app-spam-blocker-email' }
    ]
});
