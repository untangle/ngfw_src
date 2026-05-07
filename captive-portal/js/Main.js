Ext.define('Ung.apps.captive-portal.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-captive-portal',

    viewModel: {
        data: {
            title: 'Captive Portal'.t(),
            iconName: 'captive-portal',
            vueMigrated: true
        },
    },

    listeners: {
        activate: function (panel) {
            var vm = panel.getViewModel();
            var policyId = vm.get('policyId');
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/apps/' + policyId + '/captive-portal', false);

            Util.setupVueMessageHandlers(panel, {
                appName: 'captive-portal',
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
