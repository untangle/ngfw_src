Ext.define('Ung.apps.intrusionprevention.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-intrusion-prevention',

    viewModel: {

        data: {
            vueMigrated: true
        }
    },

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/apps/intrusion-prevention', false);
            Util.setupVueMessageHandlers(panel, {
                appName: 'intrusion-prevention',
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
