Ext.define('Ung.config.network.view.FilterRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-filter-rules',
    itemId: 'filter-rules',
    viewModel: true,
    scrollable: true,
    withValidation: false,
    title: 'Filter Rules'.t(),
    layout: 'border',

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/firewall/filter', false);
        }
    },

    items: [
        Field.iframeHolder
    ]
});
