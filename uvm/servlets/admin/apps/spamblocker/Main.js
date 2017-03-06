Ext.define('Ung.apps.spamblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-spamblocker',
    controller: 'app-spamblocker',

    items: [
        { xtype: 'app-spamblocker-status' },
        { xtype: 'app-spamblocker-email' }
    ]
});
