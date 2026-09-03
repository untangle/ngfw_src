Ext.define('Ung.apps.directoryconnector.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-directory-connector',

    viewModel: {
        data: {
            vueMigrated: true
        }
    },

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/services/directory-connector', false);
            Util.setupVueMessageHandlers(panel, {
                appName: 'directory-connector',
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
