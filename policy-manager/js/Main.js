Ext.define('Ung.apps.policymanager.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-policy-manager',
    layout: 'border',

    viewModel: {
        data: {
            title: 'Policy Manager'.t(),
            iconName: 'policy-manager',
            vueMigrated: true
        },
    },

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/services/policy-manager', false);

            Util.setupVueMessageHandlers(panel, {
                appName: 'policy-manager',
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
