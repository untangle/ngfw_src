Ext.define('Ung.config.system.view.Shield', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-system-shield',
    itemId: 'shield',
    viewModel: true,
    withValidation: false,
    title: 'Shield'.t(),
    layout: 'border',

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/firewall/denial-of-service', false);
        }
    },

    items: [
        Field.iframeHolder
    ]

});
