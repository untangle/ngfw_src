Ext.define('Ung.config.network.Main', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network',
    itemId: 'network',
    layout: 'border',

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/network', false);
        }
    },

    items: [
        Field.iframeHolder
    ]
});
