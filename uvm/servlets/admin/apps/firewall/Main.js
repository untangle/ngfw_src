Ext.define('Ung.apps.firewall.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.firewall',

    viewModel: {
        data: {
            nodeName: 'untangle-node-firewall',
            appName: 'Firewall'
        }
    },

    items: [
        { xtype: 'app.firewall.status' },
        { xtype: 'app.firewall.rules' }
    ]

});
