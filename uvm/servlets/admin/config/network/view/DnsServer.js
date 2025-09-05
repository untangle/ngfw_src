Ext.define('Ung.config.network.view.DnsServer', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-dns-server',
    itemId: 'dns-server',
    scrollable: true,
    withValidation: false,
    viewModel: true,
    title: 'DNS Server'.t(),
    layout: 'border',

       listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/network/dns', false);
        }
    },

    items: [
        Field.iframeHolder
    ]
});
