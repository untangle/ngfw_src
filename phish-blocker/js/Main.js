Ext.define('Ung.apps.phishblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-phish-blocker',

    viewModel: {
        data: {
            title: 'Phish Blocker'.t(),
            iconName: 'phish-blocker',
            vueMigrated: true
        },
    },

    listeners: {
        activate: function (panel) {
            var vm = panel.getViewModel();
            var policyId = vm.get('policyId');
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/apps/' + policyId + '/phish-blocker', false);

            Util.setupVueMessageHandlers(panel, {
                appName: 'phish-blocker',
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
