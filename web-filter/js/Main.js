Ext.define('Ung.apps.webfilter.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-web-filter',

    viewModel: {
        data: {
            title: 'Web Filter'.t(),
            iconName: 'web-filter',
            vueMigrated: true
        },
    },

    listeners: {
        activate: function (panel) {
            var vm = panel.getViewModel();
            var policyId = vm.get('policyId');
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/apps/' + policyId + '/web-filter', false);

            Util.setupVueMessageHandlers(panel, {
                appName: 'web-filter',
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
