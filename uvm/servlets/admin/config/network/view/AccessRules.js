Ext.define('Ung.config.network.view.AccessRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-access-rules',
    itemId: 'access-rules',
    viewModel: true,
    scrollable: true,
    withValidation: false,
    title: 'Access Rules'.t(),
    layout: 'border',

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/firewall/access', false);
        }
    },

    items: [
        Field.iframeHolder
    ]
});
