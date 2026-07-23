Ext.define('Ung.apps.applicationcontrollite.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-application-control-lite',

    viewModel: {
        data: {
            title: 'Application Control Lite'.t(),
            iconName: 'application-control-lite',
            vueMigrated: true
        },
    },

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/services/application-control-lite', false);

            Util.setupVueMessageHandlers(panel, {
                appName: 'application-control-lite',
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
