Ext.define('Ung.apps.dynamic-blocklists.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-dynamic-blocklists',
    layout: 'border',
    
    viewModel: {
        data: {
            title: 'Dynamic Blocklists'.t(),
            iconName: 'dynamic-blocklists',
            vueMigrated: true
        }
    },

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/settings/services/dynamic-blocklist', false);

            Util.setupVueMessageHandlers(panel, {
                appName: 'dynamic-blocklists',
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