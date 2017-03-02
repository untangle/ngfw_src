Ext.define('Ung.apps.phishblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.phishblocker',

    viewModel: {
        data: {
            nodeName: 'untangle-node-phish-blocker',
            appName: 'Phish Blocker'
        }
    },

    items: [
        { xtype: 'app.phishblocker.status' },
        { xtype: 'app.phishblocker.email' }
    ]

});
