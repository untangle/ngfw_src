Ext.define('Ung.apps.openvpn.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-openvpn',
    controller: 'app-openvpn',

    viewModel: {
        stores: {
            remoteClients: {
                data: '{settings.remoteClients.list}'
            },
            remoteServers: {
                data: '{settings.remoteServers.list}'
            }
        }
    },

    items: [
        { xtype: 'app-openvpn-status' },
        { xtype: 'app-openvpn-server' },
        { xtype: 'app-openvpn-client' },
        { xtype: 'app-openvpn-advanced' }
    ]

});
