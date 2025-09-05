Ext.define('Ung.config.network.view.Routes', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-routes',
    itemId: 'routes',
    scrollable: true,
    withValidation: false,
    viewModel: true,
    title: 'Routes'.t(),
    layout: 'border',
    
    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/routing/routes', false);
        }
    },

    items: [
        Field.iframeHolder
    ]
});
