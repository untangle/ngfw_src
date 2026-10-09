Ext.define('Ung.apps.webcache.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-web-cache',

    viewModel: {
        data: {
            title: 'Web Cache'.t(),
            iconName: 'web-cache',
            vueMigrated: true
        },
    },

    listeners: {
        activate: function (panel) {
            var vm = panel.getViewModel();
            var policyId = vm.get('policyId');
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/apps/' + policyId + '/web-cache', false);

            Util.setupVueMessageHandlers(panel, {
                appName: 'web-cache',
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
