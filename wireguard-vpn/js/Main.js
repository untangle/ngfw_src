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
            },
            tunnels: {
                data:'{settings.tunnels.list}'
            }
        },

        data: {
            tunnelStatusData: []
        },

        formulas: {
            getSiteUrl: {
                get: function(get) {
                    var publicUrl = Rpc.directData('rpc.networkManager.getPublicUrl');
                    return(publicUrl.split(":")[0] + ":" + get('settings.listenPort'));
                }
            }
        }
    },

    items: [
        { xtype: 'app-wireguard-vpn-status' },
        { xtype: 'app-wireguard-vpn-settings' },
        { xtype: 'app-wireguard-vpn-tunnels' }
    ]

});
