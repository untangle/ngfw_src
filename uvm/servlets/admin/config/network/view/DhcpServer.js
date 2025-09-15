Ext.define('Ung.config.network.view.DhcpServer', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-dhcp-server',
    itemId: 'dhcp-server',
    viewModel: true,
    scrollable: true,
    layout: 'fit',
    title: 'DHCP Server'.t(),
      listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/network/dhcp', false);
        }
    },

    items: [
        Field.iframeHolder
    ]

});
