Ext.define('Ung.apps.configurationbackup.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-configuration-backup',
    controller: 'app-configuration-backup',

    viewModel: {
        data: {
            vueMigrated: true
        }
    },

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/services/configuration-backup', false);
            Util.setupVueMessageHandlers(panel, {
                appName: 'configuration-backup',
                enableRemoveHandler: true
            });
        },

    },

    items: [
        Field.iframeHolder
    ]

});
