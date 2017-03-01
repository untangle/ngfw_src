Ext.define('Ung.apps.openvpn.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.openvpn',

    viewModel: {
        data: {
            nodeName: 'untangle-node-openvpn',
            appName: 'OpenVPN'
        }
    },

    items: [
        { xtype: 'app.openvpn.status' },
        { xtype: 'app.openvpn.server' },
        { xtype: 'app.openvpn.client' }
    ]

});
