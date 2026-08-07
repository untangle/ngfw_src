Ext.define('Ung.apps.spamblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-spam-blocker',

    viewModel: {
        data: {
            title: 'Spam Blocker'.t(),
            iconName: 'spam-blocker',
            vueMigrated: true
        },
    },

    listeners: {
        activate: function (panel) {
            var vm = panel.getViewModel();
            var policyId = vm.get('policyId');
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/apps/' + policyId + '/spam-blocker', false);

            Util.setupVueMessageHandlers(panel, {
                appName: 'spam-blocker',
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
