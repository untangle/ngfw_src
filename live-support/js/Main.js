Ext.define('Ung.apps.livesupport.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-live-support',

    viewModel: {
        data: {
            title: 'Live Support'.t(),
            iconName: 'live-support',
            vueMigrated: true
        },
    },

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/services/live-support', false);

            Util.setupVueMessageHandlers(panel, {
                appName: 'live-support',
                enableRemoveHandler: true
            });
        },

        destroy: function (panel) {
            Util.cleanupVueMessageHandlers(panel);
        }
    },

    items: [
        Field.iframeHolder
    ]
});
