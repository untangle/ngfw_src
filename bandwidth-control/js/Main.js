Ext.define('Ung.apps.bandwidthcontrol.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-bandwidth-control',

    viewModel: {
        data: {
            title: 'Bandwidth Control'.t(),
            iconName: 'bandwidth-control',
            vueMigrated: true
        },
    },

    listeners: {
        activate: function (panel) {
            var vm = panel.getViewModel();
            var policyId = vm.get('policyId');
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/apps/' + policyId + '/bandwidth-control', false);

            Util.setupVueMessageHandlers(panel, {
                appName: 'bandwidth-control',
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
