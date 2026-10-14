Ext.define('Ung.apps.wan-failover.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-wan-failover',
    controller: 'app-wan-failover',

    viewModel: {

        data: {
            title: 'Wan Failover'.t(),
            iconName: 'wan-failover',
            vueMigrated: true
        },

    },

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/services/wan-failover', false);

            Util.setupVueMessageHandlers(panel, {
                appName: 'wan-failover',
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
