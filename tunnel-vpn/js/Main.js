Ext.define('Ung.apps.tunnel-vpn.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-tunnel-vpn',
    controller: 'app-tunnel-vpn',

    viewModel: {
        data: {
            autoReload: false,
            tunnelProviderSelected: false,
            tunnelProviderTitle: '',
            tunnelProviderInstructions: '',
            tunnelUsernameHidden: true,
            tunnelPasswordHidden: true,
            tunnelStatusData: []
        },
        stores: {
            tunnels: {
                data: '{settings.tunnels.list}',
            },
            rules: {
                data: '{settings.rules.list}'
            },
            destinationTunnelList: {
                fields: [ 'index', 'name' ],
                data: '{destinationTunnelData}'
            },
            interfaceList: {
                fields: [ 'index', 'name' ],
                data: '{interfaceData}'
            },
            tunnelStatusList: {
                data: '{tunnelStatusData}',
                fields:[{
                    name: 'tunnelId',
                    sortType: 'asInt'
                }, {
                    name: 'tunnelName',
                }, {
                    name: 'elapsedTime',
                }, {
                    name: 'recvTotal',
                    sortType: 'asInt'
                }, {
                    name: 'xmitTotal',
                    sortType: 'asInt'
                }, {
                    name: 'stateInfo'
                }]
            }
        },
    },

    items: [
        { xtype: 'app-tunnel-vpn-status' },
        { xtype: 'app-tunnel-vpn-tunnels' },
        { xtype: 'app-tunnel-vpn-rules' },
        { xtype: 'app-tunnel-vpn-log' },
    ]

});
