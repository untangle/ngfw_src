Ext.define('Ung.config.network.view.Advanced', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-advanced',
    itemId: 'advanced',
    viewModel: true,
    scrollable: true,
    withValidation: true,
    title: 'Advanced'.t(),
    layout: 'fit',

         listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/network/advanced', false);
        }
    },

    items: [
        Field.iframeHolder
    ]

});
