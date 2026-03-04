Ext.define('Ung.config.system.Main', {
    extend: 'Ung.cmp.ConfigPanel',
    alias: 'widget.config-system',
    itemId: 'system',
    layout: 'border',

    viewModel: {
        data: {
            title: 'System'.t(),
            iconName: 'system',
            vueMigrated: true
        }
    },

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/system', false);
        }
    },

    items: [
        Field.iframeHolder
    ]

});
