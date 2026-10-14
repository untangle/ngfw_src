Ext.define('Ung.apps.wireguard-vpn.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-wireguard-vpn',

    viewModel: {
        data: {
            title: 'WireGuard VPN'.t(),
            iconName: 'wireguard-vpn',
            vueMigrated: true
        },
    },

    listeners: {
        activate: function (panel) {
            var vm = panel.getViewModel();
            var policyId = vm.get('policyId');
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/services/wireguard-vpn', false);

            Util.setupVueMessageHandlers(panel, {
                appName: 'wireguard-vpn',
                enableRemoveHandler: true
            });
        },

        destroy: function (panel) {
            Util.cleanupVueMessageHandlers(panel);
        }
    },

    items: [
        Field.iframeHolder
    ]

});
