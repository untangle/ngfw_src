Ext.define('Ung.apps.reports.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-reports',

    controller: 'app-reports',

    viewModel: {
        data: {
            title: 'Reports'.t(),
            iconName: 'reports',
            vueMigrated: true
        }
    },

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/services/reports', false);
            Util.setupVueMessageHandlers(panel, {
                appName: 'reports',
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
