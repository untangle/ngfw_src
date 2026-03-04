Ext.define('Ung.config.network.Main', {
    extend: 'Ung.cmp.ConfigPanel',
    alias: 'widget.config-network',
    itemId: 'network',
    layout: 'border',

    viewModel: {
        data: {
            title: 'Network'.t(),
            iconName: 'network',
            vueMigrated: true
        }
    },

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
