Ext.define('Ung.apps.wan-balancer.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-wan-balancer',

    viewModel: {

        data: {
            vueMigrated: true
        }
    },

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/services/wan-balancer', false);
            Util.setupVueMessageHandlers(panel, {
                appName: 'wan-balancer',
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
