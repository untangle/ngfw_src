Ext.define('Ung.apps.livesupport.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.livesupport',

    viewModel: {
        data: {
            nodeName: 'untangle-node-live-support',
            appName: 'Live Support'
        }
    },

    items: [
        { xtype: 'app.livesupport.status' },
    ]

});
