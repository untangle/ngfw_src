Ext.define('Ung.apps.firewall.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-firewall',
    
    viewModel: {
        data: {
            title: 'Firewall'.t(),
            iconName: 'firewall',
            vueMigrated: true
        },
    },

    listeners: {
        activate: function (panel) {
            var vm = panel.getViewModel();
            var policyId = vm.get('policyId');
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/apps/' + policyId + '/firewall', false);

            Util.setupVueMessageHandlers(panel, {
                appName: 'firewall',
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
