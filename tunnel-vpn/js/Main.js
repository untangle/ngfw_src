Ext.define('Ung.apps.tunnel-vpn.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-tunnel-vpn',
    controller: 'app-tunnel-vpn',

    viewModel: {
        formulas: {
            tunnelLog: {
                get: function(get) {
                    var tunnelApp = rpc.appManager.app('tunnel-vpn');
                    var log = tunnelApp.getLogFile();
                    if( log == null){
                        log = 'No Tunnel VPN log information available.'.t();
                    }
                    return log;
                }
            }
        },
        data: {
            tunnelProviderSelected: false,
            tunnelProviderTitle: '',
            tunnelProviderInstructions: '',
            tunnelUsernameHidden: true,
            tunnelPasswordHidden: true
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
            }
        },
    },

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/open-vpn',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            }
        }]
    },

    items: [
        { xtype: 'app-tunnel-vpn-status' },
        { xtype: 'app-tunnel-vpn-tunnels' },
        { xtype: 'app-tunnel-vpn-rules' },
        { xtype: 'app-tunnel-vpn-log' },
    ]

});
