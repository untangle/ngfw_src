Ext.define('Ung.config.network.view.PortForwardRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-port-forward-rules',
    itemId: 'port-forward-rules',
    scrollable: true,
    withValidation: false,
    viewModel: true,
    title: 'Port Forward Rules'.t(),
    layout: 'border',

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/network/port-forward', false);
        }
    },

    items: [
        Field.iframeHolder
    ]
});
