Ext.define('Ung.apps.wireguard-vpn.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-wireguard-vpn',
    controller: 'app-wireguard-vpn',

    viewModel: {
        stores: {
            tunnelStatusList: {
                data: '{tunnelStatusData}',
                fields: [{
                    name: 'interface',
                },{
                    name :'public-key',
                },{
                    name: 'private-key',
                },{
                    name: 'listen-port',
                },{
                    name: 'fwmark'
                }]
            }
        },

        data: {
            tunnelStatusData: []
        },

        formulas: {
        }
    },

    items: [
        { xtype: 'app-wireguard-vpn-status' },
        { xtype: 'app-wireguard-vpn-server' }
    ]

});
