Ext.define('Ung.apps.spamblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-spamblocker',

    viewModel: {
        data: {
            nodeName: 'untangle-node-spam-blocker',
            appName: 'Spam Blocker'
        }
    },

    items: [
        { xtype: 'app-spamblocker-status' },
        { xtype: 'app-spamblocker-email' }
    ]

});
