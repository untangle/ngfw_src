Ext.define('Ung.apps.tunnel-vpn.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-tunnel-vpn',
    controller: 'app-tunnel-vpn',

    viewModel: {
        stores: {
            rules: {
                data: '{settings.rules.list}'
            },
            tunnels: {
                data: '{settings.tunnels.list}'
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
    ]

});
