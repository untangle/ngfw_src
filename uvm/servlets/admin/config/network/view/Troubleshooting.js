Ext.define('Ung.config.network.view.Troubleshooting', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-troubleshooting',
    itemId: 'troubleshooting',
    title: 'Troubleshooting'.t(),
    scrollable: true,
    withValidation: false,
    layout: 'border',

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/network/troubleshooting', false);
        }
    },

    items: [
        Field.iframeHolder
    ]
});
