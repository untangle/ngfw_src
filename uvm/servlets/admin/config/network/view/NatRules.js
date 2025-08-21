Ext.define('Ung.config.network.view.NatRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-nat-rules',
    itemId: 'nat-rules',
    scrollable: true,
    withValidation: false,
    viewModel: true,
    title: 'NAT Rules'.t(),
    layout: 'border',

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/network/nat', false);
        }
    },

    items: [
        Field.iframeHolder
    ]
});
