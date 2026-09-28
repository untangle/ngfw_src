Ext.define('Ung.apps.brandingmanager.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-branding-manager',

    viewModel: {
        data: {
            title: 'Branding Manager'.t(),
            iconName: 'branding-manager',
            vueMigrated: true
        },
    },

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/services/branding-manager', false);

            Util.setupVueMessageHandlers(panel, {
                appName: 'branding-manager',
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
